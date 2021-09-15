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

package org.oran.dmmapadapter.tasks;

import com.google.gson.JsonParser;

import org.oran.dmmapadapter.clients.AsyncRestClient;
import org.oran.dmmapadapter.clients.AsyncRestClientFactory;
import org.oran.dmmapadapter.configuration.ApplicationConfig;
import org.oran.dmmapadapter.controllers.ProducerCallbacksController;
import org.oran.dmmapadapter.exceptions.ServiceException;
import org.oran.dmmapadapter.r1.ProducerInfoTypeInfo;
import org.oran.dmmapadapter.r1.ProducerRegistrationInfo;
import org.oran.dmmapadapter.repository.InfoType;
import org.oran.dmmapadapter.repository.InfoTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Registers the types and this producer in ECS. This is done when needed.
 */
@Component
@EnableScheduling
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
public class ProducerRegstrationTask {

    private static final Logger logger = LoggerFactory.getLogger(ProducerRegstrationTask.class);
    private final AsyncRestClient restClient;
    private final ApplicationConfig applicationConfig;
    private final InfoTypes types;
    private static com.google.gson.Gson gson = new com.google.gson.GsonBuilder().create();

    private static final String PRODUCER_ID = "DmaapGenericInfoProducer";
    private boolean isRegisteredInEcs = false;
    private static final int REGISTRATION_SUPERVISION_INTERVAL_MS = 1000 * 5;

    public ProducerRegstrationTask(@Autowired ApplicationConfig applicationConfig, @Autowired InfoTypes types) {
        AsyncRestClientFactory restClientFactory = new AsyncRestClientFactory(applicationConfig.getWebClientConfig());
        this.restClient = restClientFactory.createRestClientNoHttpProxy("");
        this.applicationConfig = applicationConfig;
        this.types = types;
    }

    @Scheduled(fixedRate = REGISTRATION_SUPERVISION_INTERVAL_MS)
    public void supervisionTask() {
        logger.debug("Checking producers starting");
        createTask().subscribe(null, null, () -> logger.debug("Producer registration completed"));
    }

    public Mono<Object> createTask() {
        return checkProducerRegistration() //
                .doOnError(t -> isRegisteredInEcs = false) //
                .onErrorResume(t -> registerTypesAndProducer());
    }

    public boolean isRegisteredInEcs() {
        return this.isRegisteredInEcs;
    }

    private Mono<Object> checkProducerRegistration() {
        final String url = applicationConfig.getEcsBaseUrl() + "/data-producer/v1/info-producers/" + PRODUCER_ID;
        return restClient.get(url) //
                .flatMap(this::checkRegistrationInfo) //
        ;
    }

    private String registerTypeUrl(InfoType type) {
        String url = applicationConfig.getEcsBaseUrl() + "/data-producer/v1/info-types/" + type.getId();
        return url;
    }

    private Mono<String> registerTypesAndProducer() {
        final String producerUrl =
                applicationConfig.getEcsBaseUrl() + "/data-producer/v1/info-producers/" + PRODUCER_ID;

        return Flux.fromIterable(this.types.getAll()) //
                .doOnNext(type -> logger.info("Registering type {}", type.getId())) //
                .flatMap(type -> restClient.put(registerTypeUrl(type), gson.toJson(typeRegistrationInfo()))) //
                .collectList() //
                .flatMap(resp -> restClient.put(producerUrl, gson.toJson(producerRegistrationInfo()))) //
                .onErrorResume(t -> {
                    logger.warn("Registration failed {}", t.getMessage());
                    isRegisteredInEcs = false;
                    return Mono.empty();
                }) //
                .doOnNext(x -> logger.debug("Registering types and producer completed"));
    }

    private Object typeSpecifcInfoObject() {
        return jsonObject("{}");
    }

    private ProducerInfoTypeInfo typeRegistrationInfo() {
        return new ProducerInfoTypeInfo(jsonSchemaObject(), typeSpecifcInfoObject());
    }

    private Object jsonSchemaObject() {
        // An object with no properties
        String schemaStr = "{" //
                + "\"type\": \"object\"," //
                + "\"properties\": {}," //
                + "\"additionalProperties\": false" //
                + "}"; //
        return jsonObject(schemaStr);
    }

    private Object jsonObject(String json) {
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            logger.error("Bug, error in JSON: {}", json);
            throw new NullPointerException(e.toString());
        }
    }

    private Mono<String> checkRegistrationInfo(String resp) {
        ProducerRegistrationInfo info = gson.fromJson(resp, ProducerRegistrationInfo.class);
        if (isEqual(producerRegistrationInfo(), info)) {
            logger.debug("Already registered");
            this.isRegisteredInEcs = true;
            return Mono.empty();
        } else {
            return Mono.error(new ServiceException("Producer registration will be started"));
        }
    }

    private boolean isEqual(ProducerRegistrationInfo a, ProducerRegistrationInfo b) {
        return a.jobCallbackUrl.equals(b.jobCallbackUrl) //
                && a.producerSupervisionCallbackUrl.equals(b.producerSupervisionCallbackUrl) //
                && a.supportedTypeIds.size() == b.supportedTypeIds.size();
    }

    private ProducerRegistrationInfo producerRegistrationInfo() {

        return ProducerRegistrationInfo.builder() //
                .jobCallbackUrl(baseUrl() + ProducerCallbacksController.JOB_URL) //
                .producerSupervisionCallbackUrl(baseUrl() + ProducerCallbacksController.SUPERVISION_URL) //
                .supportedTypeIds(types.typeIds()) //
                .build();
    }

    private String baseUrl() {
        return this.applicationConfig.getSelfUrl();
    }
}
