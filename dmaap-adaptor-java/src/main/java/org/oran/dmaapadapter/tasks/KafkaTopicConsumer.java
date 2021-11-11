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
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.oran.dmaapadapter.configuration.ApplicationConfig;
import org.oran.dmaapadapter.repository.InfoType;
import org.oran.dmaapadapter.repository.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

/**
 * The class fetches incoming requests from DMAAP and sends them further to the
 * consumers that has a job for this InformationType.
 */
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
public class KafkaTopicConsumer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaTopicConsumer.class);
    private final ApplicationConfig applicationConfig;
    private final InfoType type;
    private final Many<String> consumerDistributor;

    public KafkaTopicConsumer(ApplicationConfig applicationConfig, InfoType type) {
        this.applicationConfig = applicationConfig;

        final int CONSUMER_BACKPRESSURE_BUFFER_SIZE = 1024 * 10;
        this.consumerDistributor = Sinks.many().multicast().onBackpressureBuffer(CONSUMER_BACKPRESSURE_BUFFER_SIZE);

        this.type = type;
        startKafkaTopicReceiver();
    }

    private Disposable startKafkaTopicReceiver() {
        return KafkaReceiver.create(kafkaInputProperties()) //
                .receive() //
                .doOnNext(this::onReceivedData) //
                .subscribe(null, //
                        throwable -> logger.error("KafkaTopicReceiver error: {}", throwable.getMessage()), //
                        () -> logger.warn("KafkaTopicReceiver stopped"));
    }

    private void onReceivedData(ConsumerRecord<Integer, String> input) {
        logger.debug("Received from kafka topic: {} :{}", this.type.getKafkaInputTopic(), input.value());
        consumerDistributor.emitNext(input.value(), Sinks.EmitFailureHandler.FAIL_FAST);
    }

    public Disposable startDistributeToConsumer(Job job) {
        final int CONCURRENCY = 10; // Has to be 1 to guarantee correct order.

        return getMessagesFromKafka(job) //
                .doOnNext(data -> logger.debug("Sending to consumer {} {} {}", job.getId(), job.getCallbackUrl(), data))
                .flatMap(body -> job.getConsumerRestClient().post("", body), CONCURRENCY) //
                .onErrorResume(this::handleConsumerErrorResponse) //
                .subscribe(null, //
                        throwable -> logger.error("KafkaMessageConsumer error: {}", throwable.getMessage()), //
                        () -> logger.warn("KafkaMessageConsumer stopped {}", job.getType().getId()));
    }

    private Flux<String> getMessagesFromKafka(Job job) {
        if (job.isBuffered()) {
            return consumerDistributor.asFlux() //
                    .filter(job::isFilterMatch) //
                    .bufferTimeout( //
                            job.getParameters().getBufferTimeout().getMaxSize(), //
                            job.getParameters().getBufferTimeout().getMaxTime()) //
                    .map(Object::toString);
        } else {
            return consumerDistributor.asFlux() //
                    .filter(job::isFilterMatch);
        }
    }

    private Mono<String> handleConsumerErrorResponse(Throwable t) {
        logger.warn("error from CONSUMER {}", t.getMessage());
        return Mono.empty();
    }

    private ReceiverOptions<Integer, String> kafkaInputProperties() {
        Map<String, Object> consumerProps = new HashMap<>();
        if (this.applicationConfig.getKafkaBootStrapServers().isEmpty()) {
            logger.error("No kafka boostrap server is setup");
        }
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.applicationConfig.getKafkaBootStrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "osc-dmaap-adaptor");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return ReceiverOptions.<Integer, String>create(consumerProps)
                .subscription(Collections.singleton(this.type.getKafkaInputTopic()));
    }

}
