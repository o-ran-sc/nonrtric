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

package com.demo.producer.controllers;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.demo.producer.messages.KafkaMessageHandlerImpl;
import com.demo.producer.messages.ApplicationMessageHandlerImpl;
import com.demo.producer.producer.SimpleProducer;

@RestController
public class ThreadsController {
    private static final Logger log = LoggerFactory.getLogger(ThreadsController.class);

    @Autowired
    private SimpleProducer simpleProducer;

    private Thread producerThread;

    @Async
    @GetMapping("/startProducer/{topicName}")
    public CompletableFuture<String> startProducer(@PathVariable("topicName") String topicName) {
        try {
            producerThread = new Thread(() -> {
                try {
                    simpleProducer.runAlways(topicName, new ApplicationMessageHandlerImpl());
                } catch (Exception e) {
                    log.error("Error starting producer on: " + topicName, e.getMessage());
                }
            });
            producerThread.start();
            return CompletableFuture.completedFuture("Producer started for topic: " + topicName);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @GetMapping("/stopProducer")
    public String stopProducer() {
        if (producerThread != null && producerThread.isAlive()) {
            try {
                simpleProducer.shutdown();
            } catch (Exception e) {
                log.error("Error stopping producer Thread", e.getMessage());
            }
            return "Producer stopped successfully";
        } else {
            return "No active producer to stop";
        }
    }

    @GetMapping("/publish/{numberOfMessages}/on/{topicName}")
    public CompletableFuture<String> publishNMessage(@PathVariable int numberOfMessages, @PathVariable String topicName) {
        try {
            producerThread = new Thread(() -> {
                try {
                    simpleProducer.run(topicName, numberOfMessages, new KafkaMessageHandlerImpl());
                } catch (Exception e) {
                    log.error("Error producing " + numberOfMessages + "on " + topicName, e.getMessage());
                }
            });
            producerThread.start();
            return CompletableFuture.completedFuture("Producer started for topic: " + topicName);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
