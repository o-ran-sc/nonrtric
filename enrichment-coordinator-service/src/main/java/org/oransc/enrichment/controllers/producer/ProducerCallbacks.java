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
import java.time.Duration;
import java.util.Collection;
import java.util.Vector;

import org.oransc.enrichment.clients.AsyncRestClient;
import org.oransc.enrichment.clients.AsyncRestClientFactory;
import org.oransc.enrichment.configuration.ApplicationConfig;
import org.oransc.enrichment.repository.EiJob;
import org.oransc.enrichment.repository.EiJobs;
import org.oransc.enrichment.repository.EiProducer;
import org.oransc.enrichment.repository.EiProducers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Callbacks to the EiProducer
 */
@SuppressWarnings("java:S3457") // No need to call "toString()" method as formatting and string ..
public class ProducerCallbacks {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static Gson gson = new GsonBuilder().create();

    private final AsyncRestClient restClient;

    public ProducerCallbacks(ApplicationConfig config) {
        AsyncRestClientFactory restClientFactory = new AsyncRestClientFactory(config.getWebClientConfig());
        this.restClient = restClientFactory.createRestClientNoHttpProxy("");
    }

    public void stopEiJob(EiJob eiJob, EiProducers eiProducers) {
        for (EiProducer producer : getProducers(eiJob, eiProducers)) {
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
    public Mono<Integer> startEiJob(EiJob eiJob, EiProducers eiProducers) {
        Retry retrySpec = Retry.fixedDelay(1, Duration.ofSeconds(1));
        return Flux.fromIterable(getProducers(eiJob, eiProducers)) //
            .flatMap(eiProducer -> postStartEiJob(eiProducer, eiJob, retrySpec)) //
            .collectList() //
            .flatMap(okResponses -> Mono.just(Integer.valueOf(okResponses.size()))); //
    }

    /**
     * Restart all jobs for one producer
     * 
     * @param producer
     * @param eiJobs
     */
    public void restartEiJobs(EiProducer producer, EiJobs eiJobs) {
        final int maxNoOfParalellRequests = 10;
        Retry retrySpec = Retry.backoff(3, Duration.ofSeconds(1));

        Flux.fromIterable(producer.getEiTypes()) //
            .flatMap(type -> Flux.fromIterable(eiJobs.getJobsForType(type))) //
            .flatMap(job -> postStartEiJob(producer, job, retrySpec), maxNoOfParalellRequests) //
            .onErrorResume(t -> {
                logger.error("Could not restart EI Job for producer: {}, reason :{}", producer.getId(), t.getMessage());
                return Flux.empty();
            }) //
            .subscribe();
    }

    private Mono<String> postStartEiJob(EiProducer producer, EiJob eiJob, Retry retrySpec) {
        ProducerJobInfo request = new ProducerJobInfo(eiJob);
        String body = gson.toJson(request);

        return restClient.post(producer.getJobCallbackUrl(), body) //
            .retryWhen(retrySpec) //
            .doOnNext(resp -> logger.debug("Job subscription {} started OK {}", eiJob.getId(), producer.getId())) //
            .onErrorResume(throwable -> {
                logger.warn("Job subscription failed {}", producer.getId(), throwable.toString());
                return Mono.empty();
            });
    }

    private Collection<EiProducer> getProducers(EiJob eiJob, EiProducers eiProducers) {
        try {
            return eiProducers.getProducersForType(eiJob.getTypeId());
        } catch (Exception e) {
            return new Vector<>();
        }
    }

}
