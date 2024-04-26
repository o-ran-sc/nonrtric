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

package com.demo.consumer.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.demo.consumer.messages.AbstractSimpleKafka;
import com.demo.consumer.messages.KafkaMessageHandler;
import com.demo.consumer.messages.MessageHelper;
import com.demo.consumer.messages.PropertiesHelper;

import lombok.Getter;
import lombok.Setter;

import java.util.Properties;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Setter
@Getter
public class SimpleConsumer extends AbstractSimpleKafka {
    private static final Logger log = LoggerFactory.getLogger(SimpleConsumer.class);

    private KafkaConsumer<String, String> kafkaConsumer = null;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    @Value("${vars.time:2000}")
    private int TIME_OUT_MS;

    public void run(String topicName, int numberOfRecords, KafkaMessageHandler callback) throws Exception {
        Properties props = PropertiesHelper.getProperties();
        // See if the number of records is provided
        Optional<Integer> recs = Optional.ofNullable(numberOfRecords);

        // adjust the number of records to get accordingly
        Integer numOfRecs = recs.orElseGet(() -> Integer.parseInt(props.getProperty("max.poll.records")));
        props.setProperty("max.poll.records", String.valueOf(numOfRecs));

        // create the consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        // make the consumer available for graceful shutdown
        setKafkaConsumer(consumer);
        consumer.assign(Collections.singleton(new TopicPartition(topicName, 0)));
        int recNum = numOfRecs;
        while (recNum > 0) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(TIME_OUT_MS));
            recNum = records.count();
            if (recNum == 0) {
                log.info(MessageHelper.getSimpleJSONObject("No records retrieved").toJSONString());
                break;
            }

            for (ConsumerRecord<String, String> record : records) {
                callback.processMessage(topicName, record);
                recNum--;
            }
        }
        consumer.close();
    }

    private void close() throws Exception {
        if (this.getKafkaConsumer() == null) {
            log.info(MessageHelper.getSimpleJSONObject("The internal consumer is NULL").toJSONString());
            return;
        }
        log.info(MessageHelper.getSimpleJSONObject("Closing consumer").toJSONString());
        this.getKafkaConsumer().close();
    }

    public void runAlways(String topicName, KafkaMessageHandler callback) throws Exception {
        Properties props = PropertiesHelper.getProperties();
        // make the consumer available for graceful shutdown
        setKafkaConsumer(new KafkaConsumer<>(props));

        // keep running forever or until shutdown() is called from another thread.
        try {
            getKafkaConsumer().subscribe(List.of(topicName));
            while (!closed.get()) {
                ConsumerRecords<String, String> records = getKafkaConsumer().poll(Duration.ofMillis(TIME_OUT_MS));
                if (records.count() == 0) {
                    log.info(MessageHelper.getSimpleJSONObject("No records retrieved").toJSONString());
                }

                for (ConsumerRecord<String, String> record : records) {
                    callback.processMessage(topicName, record);
                    log.info(MessageHelper.getSimpleJSONObject("Topic: " + topicName + "Message: " + record.value()).toJSONString());
                }
            }
        } catch (WakeupException e) {
            // Ignore exception if closing
            if (!closed.get())
                throw e;
        }
    }

    public void shutdown() {
        closed.set(true);
        try {
            log.info(MessageHelper.getSimpleJSONObject("Shutting down consumer").toJSONString());
        } catch (Exception e) {
        }
        getKafkaConsumer().wakeup();
    }

    public KafkaConsumer<String, String> getKafkaConsumer() {
        return kafkaConsumer;
    }

    public void setKafkaConsumer(KafkaConsumer<String, String> kafkaConsumer) {
        this.kafkaConsumer = kafkaConsumer;
    }
}
