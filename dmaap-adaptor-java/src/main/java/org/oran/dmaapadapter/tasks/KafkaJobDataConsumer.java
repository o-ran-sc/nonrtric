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
    private final Many<String> input;
    private final Job job;
    private Disposable subscription;
    private int errorCounter = 0;

    KafkaJobDataConsumer(Many<String> input, Job job) {
        this.input = input;
        this.job = job;
    }

    public synchronized void start() {
        stop();
        this.subscription = getMessagesFromKafka(job) //
                .doOnNext(data -> logger.debug("Sending to consumer {} {} {}", job.getId(), job.getCallbackUrl(), data))
                .flatMap(body -> job.getConsumerRestClient().post("", body), job.getParameters().getMaxConcurrency()) //
                .onErrorResume(this::handleError) //
                .subscribe(this::handleConsumerSentOk, //
                        this::handleErrorInStream, //
                        () -> logger.debug("KafkaMessageConsumer stopped, jobId: {}, type: {}", job.getId(),
                                job.getType().getId()));
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

    private Flux<String> getMessagesFromKafka(Job job) {
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
        logger.warn("exception: {} job: {}", t.getMessage(), job);

        final int STOP_AFTER_ERRORS = 5;
        if (t instanceof WebClientResponseException) {
            if (++this.errorCounter > STOP_AFTER_ERRORS) {
                logger.error("Stopping job {}", job);
                return Mono.error(t);
            } else {
                return Mono.empty(); // Discard
            }
        } else {
            // This can happen if there is an overflow.
            return Mono.empty();
        }
    }

    private void handleConsumerSentOk(String data) {
        this.errorCounter = 0;
    }

    private void handleErrorInStream(Throwable t) {
        logger.error("KafkaMessageConsumer jobId: {}, error: {}", job.getId(), t.getMessage());
        this.subscription = null;
    }

}
