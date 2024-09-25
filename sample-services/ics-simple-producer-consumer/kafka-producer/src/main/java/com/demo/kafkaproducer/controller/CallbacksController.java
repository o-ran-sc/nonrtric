/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 *
 * Copyright (C) 2024: OpenInfra Foundation Europe
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
package com.demo.kafkaproducer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@EnableConfigurationProperties
@RestController
@RequestMapping(produces = "application/json")
public class CallbacksController {
    private final Logger logger = LoggerFactory.getLogger(CallbacksController.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    KafkaController kafkaController;

    @GetMapping("/health-check") // defined in ICS ProducerSupervisionCallbackUrl
    public ResponseEntity<String> getHealthCheck() {
        logger.info("Post Info Type Status");
        return new ResponseEntity<>("Ok", HttpStatus.OK);
    }

    @PostMapping("/info-job") // defined in ICS JobCallbackUrl
    public ResponseEntity<String> startJob(@RequestBody String requestBody) { // defined in ICS CallbackBody
        logger.info("Start Job");
        kafkaController.postMessageMono(requestBody);
        return new ResponseEntity<>("Ok", HttpStatus.OK);
    }

    @DeleteMapping("/info-job/{jobID}") // defined in ICS JobCallbackUrl
    public ResponseEntity<String> stopJob() {
        logger.info("Stop Job");
        // Call the shutdown endpoint
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);
        String shutdownUrl = "http://localhost:8080" + "/actuator/shutdown";
        return restTemplate.postForEntity(shutdownUrl, entity, String.class);
        //return new ResponseEntity<>("Ok", HttpStatus.OK);
    }
}