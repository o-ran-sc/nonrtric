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

package org.oran.dmaapadapter.tasks;

import com.google.gson.JsonParser;

import lombok.Getter;

import org.oran.dmaapadapter.clients.AsyncRestClient;
import org.oran.dmaapadapter.clients.AsyncRestClientFactory;
import org.oran.dmaapadapter.configuration.ApplicationConfig;
import org.oran.dmaapadapter.controllers.ProducerCallbacksController;
import org.oran.dmaapadapter.r1.ProducerInfoTypeInfo;
import org.oran.dmaapadapter.r1.ProducerRegistrationInfo;
import org.oran.dmaapadapter.repository.InfoType;
import org.oran.dmaapadapter.repository.InfoTypes;
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
    @Getter
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
        checkRegistration() //
                .filter(isRegisterred -> !isRegisterred) //
                .flatMap(isRegisterred -> registerTypesAndProducer()) //
                .subscribe( //
                        null, //
                        this::handleRegistrationFailure, //
                        this::handleRegistrationCompleted);
    }

    private void handleRegistrationCompleted() {
        logger.debug("Registering types and producer succeeded");
        isRegisteredInEcs = true;
    }

    private void handleRegistrationFailure(Throwable t) {
        logger.warn("Registration failed {}", t.getMessage());
        isRegisteredInEcs = false;
    }

    private Mono<Boolean> checkRegistration() {
        final String url = applicationConfig.getEcsBaseUrl() + "/data-producer/v1/info-producers/" + PRODUCER_ID;
        return restClient.get(url) //
                .flatMap(this::isRegisterredInfoCorrect) //
                .onErrorResume(t -> Mono.just(Boolean.FALSE));
    }

    private Mono<Boolean> isRegisterredInfoCorrect(String registerredInfoStr) {
        ProducerRegistrationInfo registerredInfo = gson.fromJson(registerredInfoStr, ProducerRegistrationInfo.class);
        if (isEqual(producerRegistrationInfo(), registerredInfo)) {
            logger.trace("Already registered");
            return Mono.just(Boolean.TRUE);
        } else {
            return Mono.just(Boolean.FALSE);
        }
    }

    private String registerTypeUrl(InfoType type) {
        return applicationConfig.getEcsBaseUrl() + "/data-producer/v1/info-types/" + type.getId();
    }

    private Mono<String> registerTypesAndProducer() {
        final String producerUrl =
                applicationConfig.getEcsBaseUrl() + "/data-producer/v1/info-producers/" + PRODUCER_ID;

        return Flux.fromIterable(this.types.getAll()) //
                .doOnNext(type -> logger.info("Registering type {}", type.getId())) //
                .flatMap(type -> restClient.put(registerTypeUrl(type), gson.toJson(typeRegistrationInfo()))) //
                .collectList() //
                .doOnNext(type -> logger.info("Registering producer")) //
                .flatMap(resp -> restClient.put(producerUrl, gson.toJson(producerRegistrationInfo())));
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

    private boolean isEqual(ProducerRegistrationInfo a, ProducerRegistrationInfo b) {
        return a.jobCallbackUrl.equals(b.jobCallbackUrl) //
                && a.producerSupervisionCallbackUrl.equals(b.producerSupervisionCallbackUrl) //
                && a.supportedTypeIds.size() == b.supportedTypeIds.size();
    }

    private ProducerRegistrationInfo producerRegistrationInfo() {

        return ProducerRegistrationInfo.builder() //
                .jobCallbackUrl(baseUrl() + ProducerCallbacksController.JOB_URL) //
                .producerSupervisionCallbackUrl(baseUrl() + ProducerCallbacksController.SUPERVISION_URL) //
                .supportedTypeIds(this.types.typeIds()) //
                .build();
    }

    private String baseUrl() {
        return this.applicationConfig.getSelfUrl();
    }
}
