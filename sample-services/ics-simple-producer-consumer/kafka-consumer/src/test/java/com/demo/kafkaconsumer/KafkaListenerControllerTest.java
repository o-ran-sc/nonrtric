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
package com.demo.kafkaconsumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

import com.demo.kafkaconsumer.controllers.CallbacksController;
import com.demo.kafkaconsumer.controllers.KafkaListenerController;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
public class KafkaListenerControllerTest {

    @Mock
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Mock
    private MessageListenerContainer listenerContainer;

    @InjectMocks
    private KafkaListenerController kafkaListenerController;

    @Autowired
    private KafkaListenerController realKafkaListenerController;

    @Autowired
    private CallbacksController callbacksController;

    private String id = "testid";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(kafkaListenerEndpointRegistry.getListenerContainer(id)).thenReturn(listenerContainer);
    }

    @AfterEach
    public void tearDown() {
        reset(kafkaListenerEndpointRegistry, listenerContainer);
    }

    @Test
    public void testStartKafkaListener() {
        when(listenerContainer.isRunning()).thenReturn(true);

        Mono<String> result = realKafkaListenerController.startKafkaListener();

        StepVerifier.create(result)
                .expectNext("Kafka listener is already running")
                .verifyComplete();
    }

    @Test
    public void testStartKafkaListenerContainerNotFound() {
        when(kafkaListenerEndpointRegistry.getListenerContainer(id)).thenReturn(null);

        Mono<String> result = kafkaListenerController.startKafkaListener();

        StepVerifier.create(result)
                .expectNext("Kafka listener container not found")
                .verifyComplete();
    }

    @Test
    public void testStopKafkaListenerContainerNotFound() {
        when(kafkaListenerEndpointRegistry.getListenerContainer(id)).thenReturn(null);

        Mono<String> result = kafkaListenerController.stopKafkaListener();

        StepVerifier.create(result)
                .expectNext("Kafka listener container not found")
                .verifyComplete();
    }

    @Test
    public void testGetStatusNotification_Deregistered() {
        String requestBody = "DEREGISTERED";

        ResponseEntity<String> response = callbacksController.getStatusNotification(requestBody).block();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Kafka listener stopped", response.getBody());
    }

    @Test
    public void testGetStatusNotification_Registered() {
        String requestBody = "REGISTERED";

        ResponseEntity<String> response = callbacksController.getStatusNotification(requestBody).block();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Kafka listener started", response.getBody());
    }
}
