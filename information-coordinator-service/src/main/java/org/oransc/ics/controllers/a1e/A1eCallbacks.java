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

package org.oransc.ics.controllers.a1e;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.invoke.MethodHandles;
import java.util.Collection;

import org.oransc.ics.clients.AsyncRestClient;
import org.oransc.ics.clients.AsyncRestClientFactory;
import org.oransc.ics.clients.SecurityContext;
import org.oransc.ics.configuration.ApplicationConfig;
import org.oransc.ics.repository.InfoJob;
import org.oransc.ics.repository.InfoJobs;
import org.oransc.ics.repository.InfoProducers;
import org.oransc.ics.repository.InfoType;
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

    @Autowired
    public A1eCallbacks(ApplicationConfig config, InfoJobs eiJobs, SecurityContext securityContext) {
        AsyncRestClientFactory restClientFactory =
            new AsyncRestClientFactory(config.getWebClientConfig(), securityContext);
        this.restClient = restClientFactory.createRestClientUseHttpProxy("");
        this.eiJobs = eiJobs;
    }

    public Flux<String> notifyJobStatus(Collection<InfoType> eiTypes, InfoProducers eiProducers) {
        return Flux.fromIterable(eiTypes) //
            .flatMap(eiType -> Flux.fromIterable(this.eiJobs.getJobsForType(eiType))) //
            .filter(eiJob -> !eiJob.getJobStatusUrl().isEmpty()) //
            .filter(eiJob -> eiProducers.isJobEnabled(eiJob) != eiJob.isLastStatusReportedEnabled())
            .flatMap(eiJob -> noifyStatusToJobOwner(eiJob, eiProducers));
    }

    private Mono<String> noifyStatusToJobOwner(InfoJob job, InfoProducers eiProducers) {
        boolean isJobEnabled = eiProducers.isJobEnabled(job);
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
