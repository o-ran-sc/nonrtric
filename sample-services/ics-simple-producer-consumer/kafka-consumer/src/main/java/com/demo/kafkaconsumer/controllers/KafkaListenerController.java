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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@EnableConfigurationProperties
public class KafkaListenerController {

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Value("${spring.kafka.id}")
    private String id;

    @GetMapping("/start")
    public Mono<String> startKafkaListener() {
        return Mono.defer(() -> {
            MessageListenerContainer listenerContainer = kafkaListenerEndpointRegistry.getListenerContainer(id);
            if (listenerContainer != null && !listenerContainer.isRunning()) {
                listenerContainer.start();
                return Mono.just("Kafka listener started");
            } else if (listenerContainer != null && listenerContainer.isRunning()) {
                return Mono.just("Kafka listener is already running");
            } else {
                return Mono.just("Kafka listener container not found");
            }
        });
    }

    @GetMapping("/stop")
    public Mono<String> stopKafkaListener() {
        return Mono.defer(() -> {
            MessageListenerContainer listenerContainer = kafkaListenerEndpointRegistry.getListenerContainer(id);
            if (listenerContainer != null && listenerContainer.isRunning()) {
                listenerContainer.stop();
                return Mono.just("Kafka listener stopped");
            } else if (listenerContainer != null && !listenerContainer.isRunning()) {
                return Mono.just("Kafka listener is not running");
            } else {
                return Mono.just("Kafka listener container not found");
            }
        });
    }
}
