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

package com.demo.consumer.controllers;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.demo.consumer.consumer.SimpleConsumer;
import com.demo.consumer.messages.ApplicationMessageHandlerImpl;

@RestController
public class ThreadsController {
    private static final Logger log = LoggerFactory.getLogger(ThreadsController.class);

    @Autowired
    private SimpleConsumer simpleConsumer;

    private Thread consumerThread;

    @Async
    @GetMapping("/startConsumer/{topicName}")
    public CompletableFuture<String> startConsumer(@PathVariable("topicName") String topicName) {
        try {
            Thread consumerThread = new Thread(() -> {
                try {
                    simpleConsumer.runAlways(topicName, new ApplicationMessageHandlerImpl());
                } catch (Exception e) {
                    log.error("Error starting consuming on: " + topicName, e.getMessage());
                }
            });
            consumerThread.start();
            return CompletableFuture.completedFuture("Consumer started for topic: " + topicName);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @GetMapping("/stopConsumer")
    public String stopConsumer() {
        if (consumerThread != null && consumerThread.isAlive()) {
            try {
                simpleConsumer.shutdown();
            } catch (Exception e) {
                log.error("Error stopping consumer Thread", e.getMessage());
            }
            return "Consumer stopped successfully";
        } else {
            return "No active consumer to stop";
        }
    }

    @GetMapping("/listen/{numberOfMessages}/on/{topicName}")
    public CompletableFuture<String> listenMessage(@PathVariable int numberOfMessages, @PathVariable String topicName) {
        try {
            Thread consumerThread = new Thread(() -> {
                try {
                    simpleConsumer.run(topicName, numberOfMessages, new ApplicationMessageHandlerImpl());
                } catch (Exception e) {
                    log.error("Error starting consuming on: " + topicName, e.getMessage());
                }
            });
            consumerThread.start();
            return CompletableFuture.completedFuture("Consumer started for topic: " + topicName);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
