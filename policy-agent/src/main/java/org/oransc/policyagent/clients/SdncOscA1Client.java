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
import java.util.Arrays;
import java.util.List;

import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
public class SdncOscA1Client implements A1Client {

    private static final String URL_PREFIX = "/A1-ADAPTER-API:";
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final String a1ControllerUsername;
    private final String a1ControllerPassword;
    private final RicConfig ricConfig;
    private final AsyncRestClient restClient;

    public SdncOscA1Client(RicConfig ricConfig, String baseUrl, String username, String password) {
        this(ricConfig, username, password, new AsyncRestClient(baseUrl + "/restconf/operations"));
        logger.debug("SdncOscA1Client for ric: {}, a1ControllerBaseUrl: {}", ricConfig.name(), baseUrl);
    }

    public SdncOscA1Client(RicConfig ricConfig, String username, String password, AsyncRestClient restClient) {
        this.ricConfig = ricConfig;
        this.a1ControllerUsername = username;
        this.a1ControllerPassword = password;
        this.restClient = restClient;
    }

    @Override
    public Mono<List<String>> getPolicyTypeIdentities() {
        return Mono.just(Arrays.asList(""));
    }

    @Override
    public Mono<List<String>> getPolicyIdentities() {
        return getPolicyIds() //
            .collectList();
    }

    @Override
    public Mono<String> getPolicyTypeSchema(String policyTypeId) {
        return Mono.just("{}");
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        SdncOscAdapterInput inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(ricConfig.baseUrl()) //
            .policyTypeId(policy.type().name()) //
            .policyId(policy.id()) //
            .policy(policy.json()) //
            .build();
        String inputJsonString = JsonHelper.createInputJsonString(inputParams);
        return restClient
            .postWithAuthHeader(URL_PREFIX + "putPolicy", inputJsonString, a1ControllerUsername, a1ControllerPassword)
            .flatMap(response -> JsonHelper.getValueFromResponse(response, "returned-policy")) //
            .flatMap(JsonHelper::validateJson);
    }

    @Override
    public Mono<String> deletePolicy(Policy policy) {
        return deletePolicyById(policy.id());
    }

    @Override
    public Flux<String> deleteAllPolicies() {
        return getPolicyIds() //
            .flatMap(this::deletePolicyById); //
    }

    @Override
    public Mono<A1ProtocolType> getProtocolVersion() {
        return getPolicyIdentities() //
            .flatMap(x -> Mono.just(A1ProtocolType.SDNC_OSC));
    }

    @Override
    public Mono<String> getPolicyStatus(Policy policy) {
        SdncOscAdapterInput inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(ricConfig.baseUrl()) //
            .policyId(policy.id()) //
            .build();
        String inputJsonString = JsonHelper.createInputJsonString(inputParams);
        logger.debug("POST getPolicyStatus inputJsonString = {}", inputJsonString);

        return restClient
            .postWithAuthHeader(URL_PREFIX + "getPolicyStatus", inputJsonString, a1ControllerUsername,
                a1ControllerPassword) //
            .flatMap(response -> JsonHelper.getValueFromResponse(response, "policy-status"));
    }

    private Flux<String> getPolicyIds() {
        SdncOscAdapterInput inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(ricConfig.baseUrl()) //
            .build();
        String inputJsonString = JsonHelper.createInputJsonString(inputParams);
        return restClient
            .postWithAuthHeader(URL_PREFIX + "getPolicyIdentities", inputJsonString, a1ControllerUsername,
                a1ControllerPassword) //
            .flatMap(response -> JsonHelper.getValueFromResponse(response, "policy-id-list")) //
            .flatMapMany(JsonHelper::parseJsonArrayOfString);
    }

    private Mono<String> deletePolicyById(String policyId) {
        SdncOscAdapterInput inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(ricConfig.baseUrl()) //
            .policyId(policyId) //
            .build();

        String inputJsonString = JsonHelper.createInputJsonString(inputParams);
        return restClient.postWithAuthHeader(URL_PREFIX + "deletePolicy", inputJsonString, a1ControllerUsername,
            a1ControllerPassword);
    }
}
