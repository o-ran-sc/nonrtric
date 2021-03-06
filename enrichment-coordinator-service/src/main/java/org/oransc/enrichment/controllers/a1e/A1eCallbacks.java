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

package org.oransc.enrichment.controllers.a1e;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.invoke.MethodHandles;
import java.util.Collection;

import org.oransc.enrichment.clients.AsyncRestClient;
import org.oransc.enrichment.clients.AsyncRestClientFactory;
import org.oransc.enrichment.configuration.ApplicationConfig;
import org.oransc.enrichment.repository.InfoJob;
import org.oransc.enrichment.repository.InfoJobs;
import org.oransc.enrichment.repository.InfoProducers;
import org.oransc.enrichment.repository.InfoType;
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
public class A1eCallbacks {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static Gson gson = new GsonBuilder().create();

    private final AsyncRestClient restClient;
    private final InfoJobs eiJobs;
    private final InfoProducers eiProducers;

    @Autowired
    public A1eCallbacks(ApplicationConfig config, InfoJobs eiJobs, InfoProducers eiProducers) {
        AsyncRestClientFactory restClientFactory = new AsyncRestClientFactory(config.getWebClientConfig());
        this.restClient = restClientFactory.createRestClientUseHttpProxy("");
        this.eiJobs = eiJobs;
        this.eiProducers = eiProducers;
    }

    public Flux<String> notifyJobStatus(Collection<InfoType> eiTypes) {
        return Flux.fromIterable(eiTypes) //
            .flatMap(eiType -> Flux.fromIterable(this.eiJobs.getJobsForType(eiType))) //
            .filter(eiJob -> !eiJob.getJobStatusUrl().isEmpty()) //
            .filter(eiJob -> this.eiProducers.isJobEnabled(eiJob) != eiJob.isLastStatusReportedEnabled())
            .flatMap(this::noifyStatusToJobOwner);
    }

    private Mono<String> noifyStatusToJobOwner(InfoJob job) {
        boolean isJobEnabled = this.eiProducers.isJobEnabled(job);
        A1eEiJobStatus status = isJobEnabled ? new A1eEiJobStatus(A1eEiJobStatus.EiJobStatusValues.ENABLED)
            : new A1eEiJobStatus(A1eEiJobStatus.EiJobStatusValues.DISABLED);
        String body = gson.toJson(status);
        return this.restClient.post(job.getJobStatusUrl(), body) //
            .doOnNext(response -> logger.debug("Consumer notified OK {}", job.getId())) //
            .doOnNext(response -> job.setLastReportedStatus(isJobEnabled)) //
            .onErrorResume(throwable -> {
                logger.warn("Consumer notify failed {} {}", job.getJobStatusUrl(), throwable.toString());
                return Mono.empty();
            });
    }
}
