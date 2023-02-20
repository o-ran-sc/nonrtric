/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2023 Nordix Foundation
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

package org.oran.pmlog.tasks;

import java.time.Duration;

import lombok.Getter;

import org.oran.pmlog.clients.AsyncRestClient;
import org.oran.pmlog.clients.AsyncRestClientFactory;
import org.oran.pmlog.clients.SecurityContext;
import org.oran.pmlog.configuration.ApplicationConfig;
import org.oran.pmlog.configuration.ConsumerJobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Registers the types and this producer in Innformation Coordinator Service.
 * This is done when needed.
 */
@Component
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
public class ConsumerRegstrationTask {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerRegstrationTask.class);
    private final AsyncRestClient restClient;
    private final ApplicationConfig applicationConfig;

    private static com.google.gson.Gson gson = new com.google.gson.GsonBuilder() //
            .disableHtmlEscaping() //
            .excludeFieldsWithoutExposeAnnotation() //
            .create();

    @Getter
    private boolean isRegisteredInIcs = false;

    public ConsumerRegstrationTask(@Autowired ApplicationConfig applicationConfig,
            @Autowired SecurityContext securityContext) {
        AsyncRestClientFactory restClientFactory =
                new AsyncRestClientFactory(applicationConfig.getWebClientConfig(), securityContext);
        this.restClient = restClientFactory.createRestClientNoHttpProxy("");
        this.applicationConfig = applicationConfig;

        createSubscription();
    }

    private void createSubscription() {
        putInfoJob() //
                .doOnError(this::handleRegistrationFailure)
                .retryWhen(Retry.fixedDelay(100, Duration.ofMillis(30 * 1000))) //
                .subscribe( //
                        null, //
                        this::handleRegistrationFailure, //
                        this::handleRegistrationCompleted);
    }

    private void handleRegistrationCompleted() {
        logger.info("Registration of subscription/info job succeeded");
        isRegisteredInIcs = true;
    }

    private void handleRegistrationFailure(Throwable t) {
        logger.warn("Creation of subscription/info job failed {}", t.getMessage());
    }

    private Mono<ResponseEntity<String>> putInfoJob() {
        try {
            ConsumerJobInfo consumerJobInfo = this.applicationConfig.getConsumerJobInfo();
            String jobId = consumerJobInfo.jobId;
            String url = applicationConfig.getIcsBaseUrl() + "/data-consumer/v1/info-jobs/" + jobId;

            String body = gson.toJson(consumerJobInfo);
            return restClient.putForEntity(url, body);
        } catch (Exception e) {
            logger.error("Registration of subscription failed {}", e.getMessage());
            return Mono.error(e);
        }
    }

}
