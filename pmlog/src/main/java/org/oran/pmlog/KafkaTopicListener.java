/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2023 Nordix Foundation
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

package org.oran.pmlog;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.oran.pmlog.configuration.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
    private Flux<DataFromKafkaTopic> dataFromTopic;
    private static com.google.gson.Gson gson = new com.google.gson.GsonBuilder().disableHtmlEscaping().create();

    public KafkaTopicListener(ApplicationConfig applConfig) {
        this.applicationConfig = applConfig;
    }

    public Flux<DataFromKafkaTopic> getFlux() {
        if (this.dataFromTopic == null) {
            this.dataFromTopic = startReceiveFromTopic(this.applicationConfig.getKafkaClientId());
        }
        return this.dataFromTopic;
    }

    private Flux<DataFromKafkaTopic> startReceiveFromTopic(String clientId) {
        logger.debug("Listening to kafka topic: {}", this.applicationConfig.getKafkaInputTopic());

        return KafkaReceiver.create(kafkaInputProperties(clientId)) //
                .receiveAutoAck() //
                .concatMap(consumerRecord -> consumerRecord) //
                .doOnNext(input -> logger.trace("Received from kafka topic: {}",
                        this.applicationConfig.getKafkaInputTopic())) //
                .doOnError(t -> logger.error("Received error: {}", t.getMessage())) //
                .onErrorResume(t -> Mono.empty()) //
                .doFinally(sig -> logger.error("KafkaTopicListener stopped, reason: {}", sig)) //
                .filter(t -> t.value().length > 0 || t.key().length > 0) //
                .map(input -> new DataFromKafkaTopic(input.headers(), input.key(), input.value())) //
                .publish() //
                .autoConnect(1);
    }

    private ReceiverOptions<byte[], byte[]> kafkaInputProperties(String clientId) {
        Map<String, Object> consumerProps = new HashMap<>();
        if (this.applicationConfig.getKafkaBootStrapServers().isEmpty()) {
            logger.error("No kafka boostrap server is setup");
        }

        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.applicationConfig.getKafkaBootStrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, applicationConfig.getKafkaGroupId());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        consumerProps.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId + "_" + applicationConfig.getKafkaGroupId());

        return ReceiverOptions.<byte[], byte[]>create(consumerProps)
                .subscription(Collections.singleton(this.applicationConfig.getKafkaInputTopic()));
    }

}
