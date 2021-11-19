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

import java.time.Duration;

import org.oran.dmaapadapter.clients.AsyncRestClient;
import org.oran.dmaapadapter.clients.AsyncRestClientFactory;
import org.oran.dmaapadapter.configuration.ApplicationConfig;
import org.oran.dmaapadapter.repository.InfoType;
import org.oran.dmaapadapter.repository.Jobs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The class fetches incoming requests from DMAAP and sends them further to the
 * consumers that has a job for this InformationType.
 */
public class DmaapTopicConsumer {
    private static final Duration TIME_BETWEEN_DMAAP_RETRIES = Duration.ofSeconds(10);
    private static final Logger logger = LoggerFactory.getLogger(DmaapTopicConsumer.class);

    private final AsyncRestClient dmaapRestClient;
    protected final ApplicationConfig applicationConfig;
    protected final InfoType type;
    protected final Jobs jobs;

    public DmaapTopicConsumer(ApplicationConfig applicationConfig, InfoType type, Jobs jobs) {
        AsyncRestClientFactory restclientFactory = new AsyncRestClientFactory(applicationConfig.getWebClientConfig());
        this.dmaapRestClient = restclientFactory.createRestClientNoHttpProxy("");
        this.applicationConfig = applicationConfig;
        this.type = type;
        this.jobs = jobs;
    }

    public void start() {
        Flux.range(0, Integer.MAX_VALUE) //
                .flatMap(notUsed -> getFromMessageRouter(getDmaapUrl()), 1) //
                .flatMap(this::pushDataToConsumers) //
                .subscribe(//
                        null, //
                        throwable -> logger.error("DmaapMessageConsumer error: {}", throwable.getMessage()), //
                        this::onComplete); //
    }

    private void onComplete() {
        logger.warn("DmaapMessageConsumer completed {}", type.getId());
        start();
    }

    private String getDmaapUrl() {
        return this.applicationConfig.getDmaapBaseUrl() + type.getDmaapTopicUrl();
    }

    private Mono<String> handleDmaapErrorResponse(Throwable t) {
        logger.debug("error from DMAAP {} {}", t.getMessage(), type.getDmaapTopicUrl());
        return Mono.delay(TIME_BETWEEN_DMAAP_RETRIES) //
                .flatMap(notUsed -> Mono.empty());
    }

    private Mono<String> getFromMessageRouter(String topicUrl) {
        logger.trace("getFromMessageRouter {}", topicUrl);
        return dmaapRestClient.get(topicUrl) //
                .filter(body -> body.length() > 3) // DMAAP will return "[]" sometimes. That is thrown away.
                .doOnNext(message -> logger.debug("Message from DMAAP topic: {} : {}", topicUrl, message)) //
                .onErrorResume(this::handleDmaapErrorResponse); //
    }

    private Mono<String> handleConsumerErrorResponse(Throwable t) {
        logger.warn("error from CONSUMER {}", t.getMessage());
        return Mono.empty();
    }

    protected Flux<String> pushDataToConsumers(String body) {
        logger.debug("Received data {}", body);
        final int CONCURRENCY = 50;

        // Distibute the body to all jobs for this type
        return Flux.fromIterable(this.jobs.getJobsForType(this.type)) //
                .filter(job -> job.isFilterMatch(body)) //
                .doOnNext(job -> logger.debug("Sending to consumer {}", job.getCallbackUrl())) //
                .flatMap(job -> job.getConsumerRestClient().post("", body, MediaType.APPLICATION_JSON), CONCURRENCY) //
                .onErrorResume(this::handleConsumerErrorResponse);
    }
}
