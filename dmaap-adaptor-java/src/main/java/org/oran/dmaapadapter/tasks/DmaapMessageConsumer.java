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

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

/**
 * The class fetches incoming requests from DMAAP and sends them further to the
 * consumers that has a job for this InformationType.
 */

public class DmaapMessageConsumer {
    private static final Duration TIME_BETWEEN_DMAAP_RETRIES = Duration.ofSeconds(10);
    private static final Logger logger = LoggerFactory.getLogger(DmaapMessageConsumer.class);
    private final ApplicationConfig applicationConfig;
    private final AsyncRestClient restClient;
    private final InfoType type;
    private final Jobs jobs;
    private final InfiniteFlux infiniteSubmitter = new InfiniteFlux();

    /** Submits new elements until stopped */
    private static class InfiniteFlux {
        private FluxSink<Integer> sink;
        private int counter = 0;

        public synchronized Flux<Integer> start() {
            stop();
            return Flux.create(this::next).doOnRequest(this::onRequest);
        }

        public synchronized void stop() {
            if (this.sink != null) {
                this.sink.complete();
                this.sink = null;
            }
        }

        void onRequest(long no) {
            logger.debug("InfiniteFlux.onRequest {}", no);
            for (long i = 0; i < no; ++i) {
                sink.next(counter++);
            }
        }

        void next(FluxSink<Integer> sink) {
            logger.debug("InfiniteFlux.next");
            this.sink = sink;
            sink.next(counter++);
        }
    }

    public DmaapMessageConsumer(ApplicationConfig applicationConfig, InfoType type, Jobs jobs) {
        this.applicationConfig = applicationConfig;
        AsyncRestClientFactory restclientFactory = new AsyncRestClientFactory(applicationConfig.getWebClientConfig());
        this.restClient = restclientFactory.createRestClientNoHttpProxy("");
        this.type = type;
        this.jobs = jobs;
    }

    public void start() {
        infiniteSubmitter.stop();

        createTask().subscribe(//
                value -> logger.debug("DmaapMessageConsumer next: {}", value), //
                throwable -> logger.error("DmaapMessageConsumer error: {}", throwable.getMessage()), //
                () -> logger.warn("DmaapMessageConsumer stopped") //
        );
    }

    protected Flux<String> createTask() {
        final int CONCURRENCY = 5;
        return infiniteSubmitter.start() //
                .flatMap(notUsed -> getFromMessageRouter(getDmaapUrl()), 1) //
                .filter(body -> body.length() > 3) // DMAAP will return "[]" sometimes. That is thrown away.
                .doOnNext(message -> logger.debug("Message Reveived from DMAAP : {}", message)) //
                .flatMap(this::handleReceivedMessage, CONCURRENCY);
    }

    private String getDmaapUrl() {
        return this.applicationConfig.getDmaapBaseUrl() + type.getDmaapTopicUrl();
    }

    private Mono<String> handleErrorResponse(Throwable t) {
        logger.debug("error from DMAAP {}", t.getMessage());
        return Mono.delay(TIME_BETWEEN_DMAAP_RETRIES) //
                .flatMap(notUsed -> Mono.empty());
    }

    protected Mono<String> getFromMessageRouter(String topicUrl) {
        logger.trace("getFromMessageRouter {}", topicUrl);
        return restClient.get(topicUrl) //
                .onErrorResume(this::handleErrorResponse);
    }

    protected Flux<String> handleReceivedMessage(String body) {
        logger.debug("Received from DMAAP {}", body);
        final int CONCURRENCY = 5;

        // Distibute the body to all jobs for this type
        return Flux.fromIterable(this.jobs.getJobsForType(this.type)) //
                .doOnNext(job -> logger.debug("Sending to consumer {}", job.getCallbackUrl()))
                .flatMap(job -> restClient.post(job.getCallbackUrl(), body), CONCURRENCY) //
                .onErrorResume(this::handleErrorResponse);
    }

}
