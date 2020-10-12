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

package org.oransc.enrichment.clients;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.invoke.MethodHandles;

import org.oransc.enrichment.configuration.ApplicationConfig;
import org.oransc.enrichment.repository.EiJob;
import org.oransc.enrichment.repository.EiProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Callbacks to the EiProducer
 */
@SuppressWarnings("java:S3457") // No need to call "toString()" method as formatting and string ..
public class ProducerCallbacks {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    private final AsyncRestClient restClient;

    public ProducerCallbacks(ApplicationConfig config) {
        AsyncRestClientFactory restClientFactory = new AsyncRestClientFactory(config.getWebClientConfig());
        this.restClient = restClientFactory.createRestClient("");
    }

    public void notifyProducersJobDeleted(EiJob eiJob) {
        ProducerJobInfo request = new ProducerJobInfo(eiJob);
        String body = gson.toJson(request);
        for (EiProducer producer : eiJob.type().getProducers()) {
            restClient.post(producer.getJobDeletionCallbackUrl(), body) //
                .subscribe(notUsed -> logger.debug("Job deleted OK {}", producer.getId()), //
                    throwable -> logger.warn("Job delete failed {}", producer.getId(), throwable.toString()), null);
        }
    }

    /**
     * Calls all producers for an EiJob activation.
     * 
     * @param eiJob an EI job
     * @return the number of producers that returned OK
     */
    public Mono<Integer> notifyProducersJobStarted(EiJob eiJob) {
        return Flux.fromIterable(eiJob.type().getProducers()) //
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

        return restClient.post(producer.getJobCreationCallbackUrl(), body)
            .doOnNext(resp -> logger.debug("Job subscription started OK {}", producer.getId()))
            .onErrorResume(throwable -> {
                logger.warn("Job subscription failed {}", producer.getId(), throwable.toString());
                return Mono.empty();
            });
    }

}
