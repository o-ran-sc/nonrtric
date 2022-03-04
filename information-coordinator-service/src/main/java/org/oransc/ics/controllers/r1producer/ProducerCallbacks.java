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

package org.oransc.ics.controllers.r1producer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Collection;

import org.oransc.ics.clients.AsyncRestClient;
import org.oransc.ics.clients.AsyncRestClientFactory;
import org.oransc.ics.clients.SecurityContext;
import org.oransc.ics.configuration.ApplicationConfig;
import org.oransc.ics.repository.InfoJob;
import org.oransc.ics.repository.InfoJobs;
import org.oransc.ics.repository.InfoProducer;
import org.oransc.ics.repository.InfoProducers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Callbacks to the Producer
 */
@SuppressWarnings("java:S3457") // No need to call "toString()" method as formatting and string ..
public class ProducerCallbacks {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static Gson gson = new GsonBuilder().create();

    private final AsyncRestClient restClient;

    public ProducerCallbacks(ApplicationConfig config, SecurityContext securityContext) {
        AsyncRestClientFactory restClientFactory =
            new AsyncRestClientFactory(config.getWebClientConfig(), securityContext);
        this.restClient = restClientFactory.createRestClientNoHttpProxy("");
    }

    public Mono<String> healthCheck(InfoProducer producer) {
        return restClient.get(producer.getProducerSupervisionCallbackUrl());
    }

    public void stopInfoJob(InfoJob infoJob, InfoProducers infoProducers) {
        for (InfoProducer producer : getProducersForJob(infoJob, infoProducers)) {
            String url = producer.getJobCallbackUrl() + "/" + infoJob.getId();
            producer.setJobDisabled(infoJob);
            restClient.delete(url) //
                .subscribe(response -> logger.debug("Producer job deleted OK {}", producer.getId()), //
                    throwable -> logger.warn("Producer job delete failed {} {}", producer.getId(),
                        throwable.getMessage()),
                    null);
        }
    }

    /**
     * Start a job in all producers that suports the job type
     *
     * @param infoJob an Information Job
     * @return the number of producers that returned OK
     */
    public Mono<Integer> startInfoSubscriptionJob(InfoJob infoJob, InfoProducers infoProducers) {
        Retry retrySpec = Retry.fixedDelay(1, Duration.ofSeconds(1));
        return Flux.fromIterable(getProducersForJob(infoJob, infoProducers)) //
            .flatMap(infoProducer -> startInfoJob(infoProducer, infoJob, retrySpec)) //
            .collectList() //
            .map(okResponses -> Integer.valueOf(okResponses.size())); //
    }

    /**
     * Start all jobs for one producer
     *
     * @param producer
     * @param infoJobs
     */
    public Flux<String> startInfoJobs(InfoProducer producer, InfoJobs infoJobs) {
        final int maxNoOfParalellRequests = 10;
        Retry retrySpec = Retry.backoff(3, Duration.ofSeconds(1));

        return Flux.fromIterable(producer.getInfoTypes()) //
            .flatMap(type -> Flux.fromIterable(infoJobs.getJobsForType(type))) //
            .flatMap(job -> startInfoJob(producer, job, retrySpec), maxNoOfParalellRequests);
    }

    public Mono<String> startInfoJob(InfoProducer producer, InfoJob infoJob, Retry retrySpec) {
        ProducerJobInfo request = new ProducerJobInfo(infoJob);
        String body = gson.toJson(request);

        return restClient.post(producer.getJobCallbackUrl(), body) //
            .retryWhen(retrySpec) //
            .doOnNext(resp -> logger.debug("Job subscription {} started OK {}", infoJob.getId(), producer.getId())) //
            .onErrorResume(throwable -> {
                producer.setJobDisabled(infoJob);
                logger.warn("Job subscription failed id: {} url: {}, reason: {}", producer.getId(),
                    producer.getJobCallbackUrl(), throwable.toString());
                return Mono.empty();
            }) //
            .doOnNext(resp -> producer.setJobEnabled(infoJob));
    }

    private Collection<InfoProducer> getProducersForJob(InfoJob infoJob, InfoProducers infoProducers) {
        return infoProducers.getProducersForType(infoJob.getTypeId());
    }

}
