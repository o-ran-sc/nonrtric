/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * 
 * Copyright (C) 2024 Nordix Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================LICENSE_END===================================
 */

package com.demo.consumer.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.consumer.repository.InfoType;
import com.demo.consumer.repository.InfoTypes;
import com.demo.consumer.repository.Job.Parameters;
import com.demo.consumer.consumer.SimpleConsumer;
import com.demo.consumer.dme.ConsumerJobInfo;
import com.demo.consumer.dme.ConsumerStatusInfo;
import com.demo.consumer.repository.Jobs;
import com.demo.consumer.messages.ApplicationMessageHandlerImpl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@RestController
@RequestMapping(path = "/consumer", produces = "application/json")
public class ConsumerController {
    private static final Logger log = LoggerFactory.getLogger(ConsumerController.class);

    private static Gson gson = new GsonBuilder().create();

    private final Jobs jobs;
    private final InfoTypes types;
    private String topicName = "mytopic";

    public ConsumerController(@Autowired Jobs jobs, @Autowired InfoTypes types) {
        this.jobs = jobs;
        this.types = types;
        InfoType type1 = InfoType.builder().build();
        Parameters p = Parameters.builder().build();
        type1.setId("type1");
        type1.setKafkaInputTopic("mytopic");
        type1.setInputJobType("type1");
        type1.setInputJobDefinition(p);
        types.put(type1);
    }

    @GetMapping("/listen/{numberOfMessages}")
    public ResponseEntity<?> listenMessage(@PathVariable int numberOfMessages) {
        try {
            new SimpleConsumer().run(topicName, new ApplicationMessageHandlerImpl(), numberOfMessages);
            return ResponseEntity.ok("Messages Listened successfully ..");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    @PostMapping("/job/{infoJobId}")
    public void startinfojob(@RequestBody String requestBody, @PathVariable String infoJobId) {
        ConsumerJobInfo request = gson.fromJson(requestBody, ConsumerJobInfo.class);
        log.info("Add Job Info" + infoJobId, request);
        try {
            this.jobs.addJob(request.infoTypeId, types.getType(request.infoTypeId), request.owner,
                    toJobParameters(request.jobDefinition));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/info-type-status")
    public void statusChange(@RequestBody String requestBody) {
        ConsumerStatusInfo request = gson.fromJson(requestBody, ConsumerStatusInfo.class);
        log.info("Add Status Job Info", request);
    }

    private Parameters toJobParameters(Object jobData) {
        String json = gson.toJson(jobData);
        return gson.fromJson(json, Parameters.class);
    }
}
