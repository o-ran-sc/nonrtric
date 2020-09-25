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
import org.oransc.enrichment.configuration.ImmutableWebClientConfig;
import org.oransc.enrichment.configuration.WebClientConfig;
import org.oransc.enrichment.repository.EiJob;
import org.oransc.enrichment.repository.EiProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Callbacks to the EiProducer
 */
@SuppressWarnings("java:S3457") // No need to call "toString()" method as formatting and string ..
public class ProducerCallbacks {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    @Autowired
    ApplicationConfig applicationConfig;

    public void notifyProducersJobCreated(EiJob eiJob) {
        for (EiProducer producer : eiJob.type().getProducers()) {
            notifyProducerJobStarted(producer, eiJob);
        }
    }

    public void notifyProducersJobDeleted(EiJob eiJob) {
        AsyncRestClient restClient = restClient(false);
        ProducerJobInfo request = new ProducerJobInfo(eiJob);
        String body = gson.toJson(request);
        for (EiProducer producer : eiJob.type().getProducers()) {
            restClient.post(producer.jobDeletionCallbackUrl(), body) //
                .subscribe(notUsed -> logger.debug("Job subscription started OK {}", producer.id()), //
                    throwable -> logger.warn("Job subscription failed {}", producer.id(), throwable.toString()), null);
        }
    }

    public void notifyProducerJobStarted(EiProducer producer, EiJob eiJob) {
        AsyncRestClient restClient = restClient(false);
        ProducerJobInfo request = new ProducerJobInfo(eiJob);
        String body = gson.toJson(request);

        restClient.post(producer.jobCreationCallbackUrl(), body) //
            .subscribe(notUsed -> logger.debug("Job subscription started OK {}", producer.id()), //
                throwable -> logger.warn("Job subscription failed {}", producer.id(), throwable.toString()), null);

    }

    private AsyncRestClient restClient(boolean useTrustValidation) {
        WebClientConfig config = this.applicationConfig.getWebClientConfig();
        config = ImmutableWebClientConfig.builder() //
            .keyStoreType(config.keyStoreType()) //
            .keyStorePassword(config.keyStorePassword()) //
            .keyStore(config.keyStore()) //
            .keyPassword(config.keyPassword()) //
            .isTrustStoreUsed(useTrustValidation) //
            .trustStore(config.trustStore()) //
            .trustStorePassword(config.trustStorePassword()) //
            .build();

        return new AsyncRestClient("", config);
    }

}
