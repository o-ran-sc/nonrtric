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

import lombok.Getter;

import org.oran.dmaapadapter.repository.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks.Many;

/**
 * The class streams data from a multi cast sink and sends the data to the Job
 * owner via REST calls.
 */
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
public class KafkaJobDataConsumer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaJobDataConsumer.class);
    @Getter
    private final Job job;
    private Disposable subscription;
    private final ErrorStats errorStats = new ErrorStats();

    private class ErrorStats {
        private int consumerFaultCounter = 0;
        private boolean kafkaError = false; // eg. overflow

        public void handleOkFromConsumer() {
            this.consumerFaultCounter = 0;
        }

        public void handleException(Throwable t) {
            if (t instanceof WebClientResponseException) {
                ++this.consumerFaultCounter;
            } else {
                kafkaError = true;
            }
        }

        public boolean isItHopeless() {
            final int STOP_AFTER_ERRORS = 5;
            return kafkaError || consumerFaultCounter > STOP_AFTER_ERRORS;
        }

        public void resetKafkaErrors() {
            kafkaError = false;
        }
    }

    public KafkaJobDataConsumer(Job job) {
        this.job = job;
    }

    public synchronized void start(Many<String> input) {
        stop();
        this.errorStats.resetKafkaErrors();
        this.subscription = getMessagesFromKafka(input, job) //
                .doOnNext(data -> logger.debug("Sending to consumer {} {} {}", job.getId(), job.getCallbackUrl(), data))
                .flatMap(body -> job.getConsumerRestClient().post("", body), job.getParameters().getMaxConcurrency()) //
                .onErrorResume(this::handleError) //
                .subscribe(this::handleConsumerSentOk, //
                        t -> stop(), //
                        () -> logger.warn("KafkaMessageConsumer stopped jobId: {}", job.getId()));
    }

    public synchronized void stop() {
        if (this.subscription != null) {
            subscription.dispose();
            subscription = null;
        }
    }

    public synchronized boolean isRunning() {
        return this.subscription != null;
    }

    private Flux<String> getMessagesFromKafka(Many<String> input, Job job) {
        Flux<String> result = input.asFlux() //
                .filter(job::isFilterMatch);

        if (job.isBuffered()) {
            result = result.bufferTimeout( //
                    job.getParameters().getBufferTimeout().getMaxSize(), //
                    job.getParameters().getBufferTimeout().getMaxTime()) //
                    .map(Object::toString);
        }
        return result;
    }

    private Mono<String> handleError(Throwable t) {
        logger.warn("exception: {} job: {}", t.getMessage(), job.getId());
        this.errorStats.handleException(t);
        if (this.errorStats.isItHopeless()) {
            return Mono.error(t);
        } else {
            return Mono.empty(); // Ignore
        }
    }

    private void handleConsumerSentOk(String data) {
        this.errorStats.handleOkFromConsumer();
    }

}
