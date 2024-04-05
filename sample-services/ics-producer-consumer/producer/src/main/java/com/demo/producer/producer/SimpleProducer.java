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
package com.demo.producer.producer;

import java.util.UUID;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.simple.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.demo.producer.messages.AbstractSimpleKafka;
import com.demo.producer.messages.KafkaMessageHandler;
import com.demo.producer.messages.MessageHelper;
import com.demo.producer.messages.PropertiesHelper;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
public class SimpleProducer extends AbstractSimpleKafka {
    private static final Logger log = LoggerFactory.getLogger(SimpleProducer.class);

    @Value("${vars.time:1000}")
    private int TIME;

    private KafkaProducer<String, String> kafkaProducer;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public void run(String topicName, int numberOfMessages, KafkaMessageHandler callback) throws Exception {
        for (int i = 0; i < numberOfMessages; i++) {
            String key = UUID.randomUUID().toString();
            String message = MessageHelper.getRandomString();
            if (this.getTopicName() == null) {
                this.setTopicName(topicName);
            }
            this.send(topicName, key, message);
            Thread.sleep(TIME);
        }
        this.shutdown();
    }

    public void runAlways(String topicName, KafkaMessageHandler callback) throws Exception {
        while (true) {
            String key = UUID.randomUUID().toString();
            String message = MessageHelper.getRandomString();
            this.send(topicName, key, message);
            Thread.sleep(TIME);
        }
    }

    private String topicName = null;

    private void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    private String getTopicName() {
        return this.topicName;
    }

    protected void send(String topicName, String key, String message) throws Exception {
        String source = SimpleProducer.class.getName();
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, key, message);
        JSONObject obj = MessageHelper.getMessageLogEntryJSON(source, topicName, key, message);
        log.info(obj.toJSONString());
        getKafkaProducer().send(producerRecord);
    }

    private KafkaProducer<String, String> getKafkaProducer() throws Exception {
        if (this.kafkaProducer == null) {
            Properties props = PropertiesHelper.getProperties();
            this.kafkaProducer = new KafkaProducer<>(props);
        }
        return this.kafkaProducer;
    }

    public void shutdown(){
        closed.set(true);
        try {
            log.info(MessageHelper.getSimpleJSONObject("Shutting down producer").toJSONString());
            getKafkaProducer().close();
        } catch (Exception e) {
            log.error("Error shutting down the Producer ", e.getMessage());
        }

    }
}
