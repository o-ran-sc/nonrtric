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
package com.demo.kafkaconsumer.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@EnableConfigurationProperties
@RestController
@RequestMapping(produces = "application/json")
public class CallbacksController {
    private final Logger logger = LoggerFactory.getLogger(CallbacksController.class);

    @Autowired
    KafkaListenerController kafkaListenerController;

    @PostMapping("/info-type-status")
    public Mono<ResponseEntity<String>> getStatusNotification(@RequestBody String requestBody) {
        logger.info(requestBody);
        if (requestBody.contains("DEREGISTERED")) {
            logger.info("De-register info type");
            return kafkaListenerController.stopKafkaListener()
                .map(message -> new ResponseEntity<>(message, HttpStatus.OK));
        } else {
            logger.info("Register info Job");
            return kafkaListenerController.startKafkaListener()
                .map(message -> new ResponseEntity<>(message, HttpStatus.OK));
        }
    }

}
