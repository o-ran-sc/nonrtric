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

package com.demo.consumer;

import com.demo.consumer.consumer.SimpleConsumer;
import com.demo.consumer.messages.KafkaMessageHandler;
import com.demo.consumer.messages.PropertiesHelper;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.time.Duration;

import java.util.Properties;

import static org.mockito.Mockito.*;

class SimpleConsumerTest {

    private static final long TIME_OUT_MS = 1000;

    @Mock
    private KafkaConsumer<String, String> kafkaConsumer;

    @InjectMocks
    private SimpleConsumer simpleConsumer;

    private AutoCloseable closable;

    @BeforeEach
    void setUp() throws Exception {
        closable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closable.close();
    }

    @Test
    void testRun() throws Exception {
        // Mocking the properties object returned by PropertiesHelper.getProperties()
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9092");
        properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        // Mocking PropertiesHelper.getProperties() to return the mocked Properties object
        try (MockedStatic<PropertiesHelper> propertiesHelperMockedStatic = Mockito.mockStatic(PropertiesHelper.class)) {
            propertiesHelperMockedStatic.when(PropertiesHelper::getProperties).thenReturn(properties);

            String topicName = "testTopic";
            int numberOfMessages = 10;
            KafkaMessageHandler callback = mock(KafkaMessageHandler.class);

            simpleConsumer.run(topicName, numberOfMessages, callback);
            verify(kafkaConsumer, times(0)).poll(Duration.ofMillis(TIME_OUT_MS));
        }
    }

}
