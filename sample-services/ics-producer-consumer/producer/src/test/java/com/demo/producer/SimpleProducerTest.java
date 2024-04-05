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

package com.demo.producer;

import com.demo.producer.producer.SimpleProducer;
import org.apache.kafka.clients.producer.KafkaProducer;

import com.demo.producer.messages.KafkaMessageHandler;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SimpleProducerTest {

    private final int wait = 1000;
    private final String topicName = "testTopic";

    @Mock
    private KafkaProducer<String, String> kafkaProducer;

    @InjectMocks
    @Autowired
    private SimpleProducer simpleProducer;

    private AutoCloseable closable;

    @BeforeEach
    void setUp() {
        closable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void close() throws Exception {
        closable.close();
    }

    @Test
    @SuppressWarnings("unchecked") //sending only Strings
    void testRun() throws Exception {
        int numberOfMessages = 10;
        KafkaMessageHandler callback = mock(KafkaMessageHandler.class);

        simpleProducer.run(topicName, numberOfMessages, callback);

        verify(kafkaProducer, times(numberOfMessages)).send(any(ProducerRecord.class));
        verify(kafkaProducer, times(1)).close();
    }

    @Test
    @SuppressWarnings("unchecked") //sending only Strings
    void testRunAlways() throws Exception {
        KafkaMessageHandler callback = mock(KafkaMessageHandler.class);
        simpleProducer.setTIME(wait);
        // Mocking behavior to break out of the loop after a few iterations
        doAnswer(invocation -> {
            simpleProducer.shutdown();
            return null;
        }).when(kafkaProducer).send(any(ProducerRecord.class));

        // Invoking runAlways() in a separate thread to avoid an infinite loop
        Thread thread = new Thread(() -> {
            try {
                simpleProducer.runAlways(topicName, callback);
            } catch (Exception e) {
            }
        });
        thread.start();

        // Let the thread execute for some time (e.g., 1 second)
        Thread.sleep(wait);

        // Interrupting the thread to stop the infinite loop
        thread.interrupt();

        verify(kafkaProducer, atLeastOnce()).send(any(ProducerRecord.class));
        verify(kafkaProducer, times(1)).close();
    }

}
