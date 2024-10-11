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
package com.demo.kafkaproducer;

import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import com.demo.kafkaproducer.controller.KafkaController;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class KafkaProducerControllerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private KafkaController kafkaController;

    private String topic = "test-topic";

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        // Set the value of the topic field (since @Value won't be processed in unit tests)
        ReflectionTestUtils.setField(kafkaController, "topic", topic);
    }

    @Test
    public void testPostMessageMono_Success() {
        // Arrange
        String name = "testMessage";
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(topic, name)).thenReturn(future);

        // Act
        Mono<String> result = kafkaController.postMessageMono(name);

        // Assert
        StepVerifier.create(result)
                .expectNext("Message Published Successfully")
                .verifyComplete();

        verify(kafkaTemplate, times(1)).send(topic, name);
    }

    @Test
    public void testPostMessageMono_Failure() {
        // Arrange
        String name = "testMessage";
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.failedFuture(new RuntimeException("Kafka error"));

        when(kafkaTemplate.send(topic, name)).thenReturn(future);

        // Act
        Mono<String> result = kafkaController.postMessageMono(name);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(kafkaTemplate, times(1)).send(topic, name);
    }

}
