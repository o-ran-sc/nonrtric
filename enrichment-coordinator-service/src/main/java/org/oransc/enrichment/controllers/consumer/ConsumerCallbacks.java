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

package org.oransc.enrichment.controllers.consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.invoke.MethodHandles;

import org.oransc.enrichment.clients.AsyncRestClient;
import org.oransc.enrichment.clients.AsyncRestClientFactory;
import org.oransc.enrichment.configuration.ApplicationConfig;
import org.oransc.enrichment.repository.EiJob;
import org.oransc.enrichment.repository.EiJobs;
import org.oransc.enrichment.repository.EiProducer;
import org.oransc.enrichment.repository.EiType;
import org.oransc.enrichment.repository.EiTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Callbacks to the EiProducer
 */
@Component
@SuppressWarnings("java:S3457") // No need to call "toString()" method as formatting and string ..
public class ConsumerCallbacks {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static Gson gson = new GsonBuilder().create();

    private final AsyncRestClient restClient;
    private final EiTypes eiTypes;
    private final EiJobs eiJobs;

    @Autowired
    public ConsumerCallbacks(ApplicationConfig config, EiTypes eiTypes, EiJobs eiJobs) {
        AsyncRestClientFactory restClientFactory = new AsyncRestClientFactory(config.getWebClientConfig());
        this.restClient = restClientFactory.createRestClient("");
        this.eiTypes = eiTypes;
        this.eiJobs = eiJobs;
    }

    public void notifyConsumersProducerDeleted(EiProducer eiProducer) {
        for (EiType type : eiProducer.getEiTypes()) {
            if (this.eiTypes.get(type.getId()) == null) {
                for (EiJob job : this.eiJobs.getJobsForType(type)) {
                    noifyJobOwner(job, new ConsumerEiJobStatus(ConsumerEiJobStatus.EiJobStatusValues.DISABLED));
                }
            }
        }
    }

    public void notifyConsumersTypeAdded(EiType eiType) {
        for (EiJob job : this.eiJobs.getJobsForType(eiType)) {
            noifyJobOwner(job, new ConsumerEiJobStatus(ConsumerEiJobStatus.EiJobStatusValues.ENABLED));
        }
    }

    private void noifyJobOwner(EiJob job, ConsumerEiJobStatus status) {
        if (!job.jobStatusUrl().isEmpty()) {
            String body = gson.toJson(status);
            this.restClient.post(job.jobStatusUrl(), body) //
                .subscribe(notUsed -> logger.debug("Consumer notified OK {}", job.id()), //
                    throwable -> logger.warn("Consumer notify failed {} {}", job.jobStatusUrl(), throwable.toString()), //
                    null);
        }
    }

}
