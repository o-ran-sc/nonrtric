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

import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@EnableConfigurationProperties
@RestController
@RequestMapping()
public class KafkaController {
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("${spring.kafka.topic}")
    private String topic;

    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;

    @GetMapping("/publish/{name}")
    public Mono<String> postMessageMono(@PathVariable("name") final String name) {
        return Mono.fromFuture(kafkaTemplate.send(topic, name))
                .doOnSuccess(result -> logger.info("Published: " + name))
                .thenReturn("Message Published Successfully")
                .doOnError(err -> logger.error("Unable to Publish: " + name));
    }

}
