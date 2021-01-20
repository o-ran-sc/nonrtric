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

package org.oransc.enrichment.tasks;

import org.oransc.enrichment.configuration.ApplicationConfig;
import org.oransc.enrichment.controllers.consumer.ConsumerCallbacks;
import org.oransc.enrichment.controllers.producer.ProducerCallbacks;
import org.oransc.enrichment.repository.EiJob;
import org.oransc.enrichment.repository.EiJobs;
import org.oransc.enrichment.repository.EiProducer;
import org.oransc.enrichment.repository.EiProducers;
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
 * Regularly checks the availability of the EI Producers
 */
@Component
@EnableScheduling
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
public class ProducerSupervision {
    private static final Logger logger = LoggerFactory.getLogger(ProducerSupervision.class);

    private final EiProducers eiProducers;
    private final EiJobs eiJobs;
    private final ProducerCallbacks producerCallbacks;
    private final ConsumerCallbacks consumerCallbacks;

    @Autowired
    public ProducerSupervision(ApplicationConfig applicationConfig, EiProducers eiProducers, EiJobs eiJobs,
        ProducerCallbacks producerCallbacks, ConsumerCallbacks consumerCallbacks) {
        this.eiProducers = eiProducers;
        this.eiJobs = eiJobs;
        this.producerCallbacks = producerCallbacks;
        this.consumerCallbacks = consumerCallbacks;
    }

    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void checkAllProducers() {
        logger.debug("Checking producers starting");
        createTask().subscribe(null, null, () -> logger.debug("Checking all Producers completed"));
    }

    public Flux<EiProducer> createTask() {
        return Flux.fromIterable(eiProducers.getAllProducers()) //
            .flatMap(this::checkOneProducer);
    }

    private Mono<EiProducer> checkOneProducer(EiProducer producer) {
        return this.producerCallbacks.healthCheck(producer) //
            .onErrorResume(throwable -> {
                handleNonRespondingProducer(throwable, producer);
                return Mono.empty();
            })//
            .doOnNext(response -> handleRespondingProducer(response, producer))
            .flatMap(response -> checkProducerJobs(producer)) //
            .flatMap(responses -> Mono.just(producer));
    }

    private Mono<?> checkProducerJobs(EiProducer producer) {
        final int MAX_CONCURRENCY = 10;
        return getEiJobs(producer) //
            .filter(eiJob -> !producer.isJobEnabled(eiJob)) //
            .flatMap(eiJob -> producerCallbacks.startEiJob(producer, eiJob, Retry.max(1)), MAX_CONCURRENCY) //
            .collectList() //
            .flatMapMany(startedJobs -> consumerCallbacks.notifyJobStatus(producer.getEiTypes())) //
            .collectList();
    }

    private Flux<EiJob> getEiJobs(EiProducer producer) {
        return Flux.fromIterable(producer.getEiTypes()) //
            .flatMap(eiType -> Flux.fromIterable(eiJobs.getJobsForType(eiType)));
    }

    private void handleNonRespondingProducer(Throwable throwable, EiProducer producer) {
        logger.warn("Unresponsive producer: {} exception: {}", producer.getId(), throwable.getMessage());
        producer.setAliveStatus(false);
        if (producer.isDead()) {
            this.eiProducers.deregisterProducer(producer);
        }
    }

    private void handleRespondingProducer(String response, EiProducer producer) {
        logger.debug("{}", response);
        producer.setAliveStatus(true);
    }

}
