/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2021 Nordix Foundation
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

package org.oransc.ics.controllers.r1consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.oransc.ics.clients.AsyncRestClient;
import org.oransc.ics.clients.AsyncRestClientFactory;
import org.oransc.ics.clients.SecurityContext;
import org.oransc.ics.configuration.ApplicationConfig;
import org.oransc.ics.repository.InfoType;
import org.oransc.ics.repository.InfoTypeSubscriptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Callbacks to the Consumer. Notifies consumer according to the API (which this
 * class adapts to)
 */
@Component
public class ConsumerCallbacks implements InfoTypeSubscriptions.ConsumerCallbackHandler {

    private static Gson gson = new GsonBuilder().create();

    private final AsyncRestClient restClient;

    public static final String API_VERSION = "version_1";

    @Autowired
    public ConsumerCallbacks(ApplicationConfig config, InfoTypeSubscriptions infoTypeSubscriptions,
        SecurityContext securityContext) {
        AsyncRestClientFactory restClientFactory =
            new AsyncRestClientFactory(config.getWebClientConfig(), securityContext);
        this.restClient = restClientFactory.createRestClientNoHttpProxy("");
        infoTypeSubscriptions.registerCallbackhandler(this, API_VERSION);
    }

    @Override
    public Mono<String> notifyTypeRegistered(InfoType type, InfoTypeSubscriptions.SubscriptionInfo subscriptionInfo) {
        String body = body(type, ConsumerTypeRegistrationInfo.ConsumerTypeStatusValues.REGISTERED);
        return restClient.post(subscriptionInfo.getCallbackUrl(), body);
    }

    @Override
    public Mono<String> notifyTypeRemoved(InfoType type, InfoTypeSubscriptions.SubscriptionInfo subscriptionInfo) {
        String body = body(type, ConsumerTypeRegistrationInfo.ConsumerTypeStatusValues.DEREGISTERED);
        return restClient.post(subscriptionInfo.getCallbackUrl(), body);
    }

    private String body(InfoType type, ConsumerTypeRegistrationInfo.ConsumerTypeStatusValues status) {
        ConsumerTypeRegistrationInfo info =
            new ConsumerTypeRegistrationInfo(type.getJobDataSchema(), status, type.getId());
        return gson.toJson(info);
    }

}
