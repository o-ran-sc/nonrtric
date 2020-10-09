/*-
 * ========================LICENSE_START=================================
 * ONAP : ccsdk oran
 * ======================================================================
 * Copyright (C) 2020 Nordix Foundation. All rights reserved.
 * ======================================================================
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

import org.oransc.enrichment.clients.AsyncRestClient;
import org.oransc.enrichment.clients.AsyncRestClientFactory;
import org.oransc.enrichment.configuration.ApplicationConfig;
import org.oransc.enrichment.repository.EiJobs;
import org.oransc.enrichment.repository.EiProducer;
import org.oransc.enrichment.repository.EiProducers;
import org.oransc.enrichment.repository.EiTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    private final EiTypes eiTypes;
    private final AsyncRestClient restClient;

    @Autowired
    public ProducerSupervision(ApplicationConfig applicationConfig, EiProducers eiProducers, EiJobs eiJobs,
        EiTypes eiTypes) {
        AsyncRestClientFactory restClientFactory = new AsyncRestClientFactory(applicationConfig.getWebClientConfig());
        this.restClient = restClientFactory.createRestClient("");
        this.eiJobs = eiJobs;
        this.eiProducers = eiProducers;
        this.eiTypes = eiTypes;
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
        return restClient.get(producer.getProducerSupervisionCallbackUrl()) //
            .onErrorResume(throwable -> {
                handleNonRespondingProducer(throwable, producer);
                return Mono.empty();
            })//
            .doOnNext(response -> handleRespondingProducer(response, producer))
            .flatMap(response -> Mono.just(producer));
    }

    private void handleNonRespondingProducer(Throwable throwable, EiProducer producer) {
        logger.warn("Unresponsive producer: {} exception: {}", producer.getId(), throwable.getMessage());
        producer.setAliveStatus(false);
        if (producer.isDead()) {
            this.eiProducers.deregisterProducer(producer, this.eiTypes, this.eiJobs);
        }
    }

    private void handleRespondingProducer(String response, EiProducer producer) {
        logger.debug("{}", response);
        producer.setAliveStatus(true);
    }

}
