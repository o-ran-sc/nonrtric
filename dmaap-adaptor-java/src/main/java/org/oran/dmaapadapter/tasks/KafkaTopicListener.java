/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2021 Nordix Foundation
 * %%
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

package org.oran.dmaapadapter.tasks;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.oran.dmaapadapter.configuration.ApplicationConfig;
import org.oran.dmaapadapter.repository.InfoType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

/**
 * The class streams incoming requests from a Kafka topic and sends them further
 * to a multi cast sink, which several other streams can connect to.
 */
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
public class KafkaTopicListener {
    private static final Logger logger = LoggerFactory.getLogger(KafkaTopicListener.class);
    private final ApplicationConfig applicationConfig;
    private final InfoType type;
    private Many<String> output;
    private Disposable topicReceiverTask;

    public KafkaTopicListener(ApplicationConfig applicationConfig, InfoType type) {
        this.applicationConfig = applicationConfig;
        this.type = type;
        start();
    }

    public Many<String> getOutput() {
        return this.output;
    }

    public void start() {
        stop();
        final int CONSUMER_BACKPRESSURE_BUFFER_SIZE = 1024 * 10;
        this.output = Sinks.many().multicast().onBackpressureBuffer(CONSUMER_BACKPRESSURE_BUFFER_SIZE);
        logger.debug("Listening to kafka topic: {} type :{}", this.type.getKafkaInputTopic(), type.getId());
        topicReceiverTask = KafkaReceiver.create(kafkaInputProperties()) //
                .receive() //
                .doOnNext(this::onReceivedData) //
                .subscribe(null, //
                        this::onReceivedError, //
                        () -> logger.warn("KafkaTopicReceiver stopped"));
    }

    private void stop() {
        if (topicReceiverTask != null) {
            topicReceiverTask.dispose();
            topicReceiverTask = null;
        }
    }

    private void onReceivedData(ConsumerRecord<String, String> input) {
        logger.debug("Received from kafka topic: {} :{}", this.type.getKafkaInputTopic(), input.value());
        output.emitNext(input.value(), Sinks.EmitFailureHandler.FAIL_FAST);
    }

    private void onReceivedError(Throwable t) {
        logger.error("KafkaTopicReceiver error: {}", t.getMessage());
    }

    private ReceiverOptions<String, String> kafkaInputProperties() {
        Map<String, Object> consumerProps = new HashMap<>();
        if (this.applicationConfig.getKafkaBootStrapServers().isEmpty()) {
            logger.error("No kafka boostrap server is setup");
        }
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.applicationConfig.getKafkaBootStrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "osc-dmaap-adaptor");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return ReceiverOptions.<String, String>create(consumerProps)
                .subscription(Collections.singleton(this.type.getKafkaInputTopic()));
    }

}
