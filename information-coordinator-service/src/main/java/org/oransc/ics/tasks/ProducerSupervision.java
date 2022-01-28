/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2020 Nordix Foundation
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

package org.oransc.ics.tasks;

import org.oransc.ics.configuration.ApplicationConfig;
import org.oransc.ics.controllers.a1e.A1eCallbacks;
import org.oransc.ics.controllers.r1producer.ProducerCallbacks;
import org.oransc.ics.repository.InfoJob;
import org.oransc.ics.repository.InfoJobs;
import org.oransc.ics.repository.InfoProducer;
import org.oransc.ics.repository.InfoProducers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Regularly checks the availability of the Info Producers
 */
@Component
@EnableScheduling
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
public class ProducerSupervision {
    private static final Logger logger = LoggerFactory.getLogger(ProducerSupervision.class);

    private final InfoProducers infoProducers;
    private final InfoJobs infoJobs;
    private final ProducerCallbacks producerCallbacks;
    private final A1eCallbacks consumerCallbacks;

    @Autowired
    public ProducerSupervision(ApplicationConfig applicationConfig, InfoProducers infoProducers, InfoJobs infoJobs,
        ProducerCallbacks producerCallbacks, A1eCallbacks consumerCallbacks) {
        this.infoProducers = infoProducers;
        this.infoJobs = infoJobs;
        this.producerCallbacks = producerCallbacks;
        this.consumerCallbacks = consumerCallbacks;
    }

    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void checkAllProducers() {
        logger.debug("Checking producers starting");
        createTask().subscribe(null, null, () -> logger.debug("Checking all Producers completed"));
    }

    public Flux<InfoProducer> createTask() {
        return Flux.fromIterable(infoProducers.getAllProducers()) //
            .flatMap(this::checkOneProducer);
    }

    private Mono<InfoProducer> checkOneProducer(InfoProducer producer) {
        return this.producerCallbacks.healthCheck(producer) //
            .onErrorResume(throwable -> {
                handleNonRespondingProducer(throwable, producer);
                return Mono.empty();
            })//
            .doOnNext(response -> handleRespondingProducer(response, producer))
            .flatMap(response -> checkProducerJobs(producer)) //
            .map(responses -> producer);
    }

    private Mono<?> checkProducerJobs(InfoProducer producer) {
        final int MAX_CONCURRENCY = 10;
        return getEiJobs(producer) //
            .filter(infoJob -> !producer.isJobEnabled(infoJob)) //
            .flatMap(infoJob -> producerCallbacks.startInfoJob(producer, infoJob, Retry.max(1)), MAX_CONCURRENCY) //
            .collectList() //
            .flatMapMany(startedJobs -> consumerCallbacks.notifyJobStatus(producer.getInfoTypes(), infoProducers)) //
            .collectList();
    }

    private Flux<InfoJob> getEiJobs(InfoProducer producer) {
        return Flux.fromIterable(producer.getInfoTypes()) //
            .flatMap(infoType -> Flux.fromIterable(infoJobs.getJobsForType(infoType)));
    }

    private void handleNonRespondingProducer(Throwable throwable, InfoProducer producer) {
        logger.warn("Unresponsive producer: {} exception: {}", producer.getId(), throwable.getMessage());
        producer.setAliveStatus(false);
        if (producer.isDead()) {
            this.infoProducers.deregisterProducer(producer);
        }
    }

    private void handleRespondingProducer(String response, InfoProducer producer) {
        logger.debug("{}", response);
        producer.setAliveStatus(true);
    }

}
