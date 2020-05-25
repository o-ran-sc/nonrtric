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

package org.oransc.policyagent.clients;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.immutables.gson.Gson;
import org.immutables.value.Value;
import org.oransc.policyagent.configuration.ControllerConfig;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Client for accessing the A1 adapter in the SDNC controller in ONAP
 */
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
public class SdncOnapA1Client implements A1Client {
    @Value.Immutable
    @Gson.TypeAdapters
    interface SdncOnapAdapterInput {
        public String nearRtRicId();

        public Optional<String> policyTypeId();

        public Optional<String> policyInstanceId();

        public Optional<String> policyInstance();

        public Optional<List<String>> properties();
    }

    private static final String URL_PREFIX = "/A1-ADAPTER-API:";

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ControllerConfig controllerConfig;
    private final RicConfig ricConfig;
    private final AsyncRestClient restClient;

    public SdncOnapA1Client(RicConfig ricConfig, ControllerConfig controllerConfig) {
        this(ricConfig, controllerConfig, new AsyncRestClient(controllerConfig.baseUrl() + "/restconf/operations"));
        logger.debug("SdncOnapA1Client for ric: {}, a1ControllerBaseUrl: {}", ricConfig.name(),
            controllerConfig.baseUrl());
    }

    public SdncOnapA1Client(RicConfig ricConfig, ControllerConfig controllerConfig, AsyncRestClient restClient) {
        this.ricConfig = ricConfig;
        this.controllerConfig = controllerConfig;
        this.restClient = restClient;
    }

    @Override
    public Mono<List<String>> getPolicyTypeIdentities() {
        return getPolicyTypeIds() //
            .collectList();
    }

    @Override
    public Mono<List<String>> getPolicyIdentities() {
        return getPolicyTypeIds() //
            .flatMap(this::getPolicyIdentitiesByType) //
            .collectList();
    }

    @Override
    public Mono<String> getPolicyTypeSchema(String policyTypeId) {
        SdncOnapAdapterInput inputParams = ImmutableSdncOnapAdapterInput.builder() //
            .nearRtRicId(ricConfig.baseUrl()) //
            .policyTypeId(policyTypeId) //
            .build();
        String inputJsonString = SdncJsonHelper.createInputJsonString(inputParams);
        logger.debug("POST getPolicyType inputJsonString = {}", inputJsonString);

        return restClient
            .postWithAuthHeader(URL_PREFIX + "getPolicyType", inputJsonString, controllerConfig.userName(),
                controllerConfig.password()) //
            .flatMap(response -> SdncJsonHelper.getValueFromResponse(response, "policy-type")) //
            .flatMap(SdncJsonHelper::extractPolicySchema);
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        SdncOnapAdapterInput inputParams = ImmutableSdncOnapAdapterInput.builder() //
            .nearRtRicId(ricConfig.baseUrl()) //
            .policyTypeId(policy.type().name()) //
            .policyInstanceId(policy.id()) //
            .policyInstance(policy.json()) //
            .properties(new ArrayList<>()) //
            .build();

        String inputJsonString = SdncJsonHelper.createInputJsonString(inputParams);
        logger.debug("POST putPolicy inputJsonString = {}", inputJsonString);

        return restClient.postWithAuthHeader(URL_PREFIX + "createPolicyInstance", inputJsonString,
            controllerConfig.userName(), controllerConfig.password());
    }

    @Override
    public Mono<String> deletePolicy(Policy policy) {
        return deletePolicyByTypeId(policy.type().name(), policy.id());
    }

    @Override
    public Flux<String> deleteAllPolicies() {
        return getPolicyTypeIds() //
            .flatMap(this::deletePoliciesForType); //
    }

    @Override
    public Mono<A1ProtocolType> getProtocolVersion() {
        return getPolicyTypeIdentities() //
            .flatMap(notUsed -> Mono.just(A1ProtocolType.SDNC_ONAP));
    }

    @Override
    public Mono<String> getPolicyStatus(Policy policy) {
        return Mono.error(new Exception("Status not implemented in the controller"));
    }

    private Flux<String> getPolicyTypeIds() {
        SdncOnapAdapterInput inputParams = ImmutableSdncOnapAdapterInput.builder() //
            .nearRtRicId(ricConfig.baseUrl()) //
            .build();
        String inputJsonString = SdncJsonHelper.createInputJsonString(inputParams);
        logger.debug("POST getPolicyTypeIdentities inputJsonString = {}", inputJsonString);

        return restClient
            .postWithAuthHeader(URL_PREFIX + "getPolicyTypes", inputJsonString, controllerConfig.userName(),
                controllerConfig.password()) //
            .flatMap(response -> SdncJsonHelper.getValueFromResponse(response, "policy-type-id-list")) //
            .flatMapMany(SdncJsonHelper::parseJsonArrayOfString);
    }

    private Flux<String> getPolicyIdentitiesByType(String policyTypeId) {
        SdncOnapAdapterInput inputParams = ImmutableSdncOnapAdapterInput.builder() //
            .nearRtRicId(ricConfig.baseUrl()) //
            .policyTypeId(policyTypeId) //
            .build();
        String inputJsonString = SdncJsonHelper.createInputJsonString(inputParams);
        logger.debug("POST getPolicyIdentities inputJsonString = {}", inputJsonString);

        return restClient
            .postWithAuthHeader(URL_PREFIX + "getPolicyInstances", inputJsonString, controllerConfig.userName(),
                controllerConfig.password()) //
            .flatMap(response -> SdncJsonHelper.getValueFromResponse(response, "policy-instance-id-list")) //
            .flatMapMany(SdncJsonHelper::parseJsonArrayOfString);
    }

    private Flux<String> deletePoliciesForType(String typeId) {
        return getPolicyIdentitiesByType(typeId) //
            .flatMap(policyId -> deletePolicyByTypeId(typeId, policyId)); //
    }

    private Mono<String> deletePolicyByTypeId(String policyTypeId, String policyId) {
        SdncOnapAdapterInput inputParams = ImmutableSdncOnapAdapterInput.builder() //
            .nearRtRicId(ricConfig.baseUrl()) //
            .policyTypeId(policyTypeId) //
            .policyInstanceId(policyId) //
            .build();
        String inputJsonString = SdncJsonHelper.createInputJsonString(inputParams);
        logger.debug("POST deletePolicy inputJsonString = {}", inputJsonString);

        return restClient.postWithAuthHeader(URL_PREFIX + "deletePolicyInstance", inputJsonString,
            controllerConfig.userName(), controllerConfig.password());
    }
}
