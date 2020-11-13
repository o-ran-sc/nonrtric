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

package org.oransc.enrichment.controllers.producer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Vector;

import org.oransc.enrichment.clients.AsyncRestClient;
import org.oransc.enrichment.clients.AsyncRestClientFactory;
import org.oransc.enrichment.configuration.ApplicationConfig;
import org.oransc.enrichment.repository.EiJob;
import org.oransc.enrichment.repository.EiProducer;
import org.oransc.enrichment.repository.EiTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Callbacks to the EiProducer
 */
@Component
@SuppressWarnings("java:S3457") // No need to call "toString()" method as formatting and string ..
public class ProducerCallbacks {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static Gson gson = new GsonBuilder().create();

    private final AsyncRestClient restClient;
    private final EiTypes eiTypes;

    @Autowired
    public ProducerCallbacks(ApplicationConfig config, EiTypes eiTypes) {
        AsyncRestClientFactory restClientFactory = new AsyncRestClientFactory(config.getWebClientConfig());
        this.restClient = restClientFactory.createRestClient("");
        this.eiTypes = eiTypes;
    }

    public void notifyProducersJobDeleted(EiJob eiJob) {
        for (EiProducer producer : getProducers(eiJob)) {
            String url = producer.getJobCallbackUrl() + "/" + eiJob.getId();
            restClient.delete(url) //
                .subscribe(notUsed -> logger.debug("Producer job deleted OK {}", producer.getId()), //
                    throwable -> logger.warn("Producer job delete failed {} {}", producer.getId(),
                        throwable.getMessage()),
                    null);
        }
    }

    /**
     * Calls all producers for an EiJob activation.
     * 
     * @param eiJob an EI job
     * @return the number of producers that returned OK
     */
    public Mono<Integer> notifyProducersJobStarted(EiJob eiJob) {
        return Flux.fromIterable(getProducers(eiJob)) //
            .flatMap(eiProducer -> notifyProducerJobStarted(eiProducer, eiJob)) //
            .collectList() //
            .flatMap(okResponses -> Mono.just(Integer.valueOf(okResponses.size()))); //
    }

    /**
     * Calls one producer for an EiJob activation.
     * 
     * @param producer a producer
     * @param eiJob an EI job
     * @return the body of the response from the REST call
     */
    public Mono<String> notifyProducerJobStarted(EiProducer producer, EiJob eiJob) {
        ProducerJobInfo request = new ProducerJobInfo(eiJob);
        String body = gson.toJson(request);

        return restClient.post(producer.getJobCallbackUrl(), body)
            .doOnNext(resp -> logger.debug("Job subscription started OK {}", producer.getId()))
            .onErrorResume(throwable -> {
                logger.warn("Job subscription failed {}", producer.getId(), throwable.toString());
                return Mono.empty();
            });
    }

    private Collection<EiProducer> getProducers(EiJob eiJob) {
        try {
            return this.eiTypes.getType(eiJob.getTypeId()).getProducers();
        } catch (Exception e) {
            return new Vector<>();
        }
    }

}
