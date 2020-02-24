/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2019 Nordix Foundation
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
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SdnrOnapA1Client implements A1Client {
    private static final String URL_PREFIX = "/A1-ADAPTER-API:";
    private static final String POLICY_TYPE_ID = "policy-type-id";
    private static final String POLICY_INSTANCE_ID = "policy-instance-id";
    private static final String POLICY_INSTANCE = "policy-instance";
    private static final String NEAR_RT_RIC_ID = "near-rt-ric-id";
    private static final String PROPERTIES = "properties";

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String a1ControllerBaseUrl;
    private String a1ControllerUsername;
    private String a1ControllerPassword;
    private final RicConfig ricConfig;
    private final AsyncRestClient restClient;

    public SdnrOnapA1Client(RicConfig ricConfig, String baseUrl, String username, String password) {
        this.ricConfig = ricConfig;
        this.a1ControllerBaseUrl = baseUrl;
        this.a1ControllerUsername = username;
        this.a1ControllerPassword = password;
        this.restClient = new AsyncRestClient(a1ControllerBaseUrl + "/restconf/operations");
        if (logger.isDebugEnabled()) {
            logger.debug("SdnrOnapA1Client for ric: {}, a1ControllerBaseUrl: {}", this.ricConfig.name(),
                a1ControllerBaseUrl);
        }
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
        JSONObject paramsJson = new JSONObject();
        paramsJson.put(NEAR_RT_RIC_ID, ricConfig.baseUrl());
        paramsJson.put(POLICY_TYPE_ID, policyTypeId);
        String inputJsonString = JsonHelper.createInputJsonString(paramsJson);
        logger.debug("POST getPolicyType inputJsonString = {}", inputJsonString);

        return restClient
            .postWithAuthHeader(URL_PREFIX + "getPolicyType", inputJsonString, a1ControllerUsername,
                a1ControllerPassword) //
            .flatMap(response -> JsonHelper.getValueFromResponse(response, "policy-type")) //
            .flatMap(JsonHelper::extractPolicySchema);
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        JSONObject paramsJson = new JSONObject();
        paramsJson.put(NEAR_RT_RIC_ID, ricConfig.baseUrl());
        paramsJson.put(POLICY_INSTANCE_ID, policy.id());
        paramsJson.put(POLICY_TYPE_ID, policy.type().name());
        paramsJson.put(POLICY_INSTANCE, policy.json());
        paramsJson.put(PROPERTIES, new JSONArray());
        String inputJsonString = JsonHelper.createInputJsonString(paramsJson);
        logger.debug("POST putPolicy inputJsonString = {}", inputJsonString);

        return restClient.postWithAuthHeader(URL_PREFIX + "createPolicyInstance", inputJsonString,
            a1ControllerUsername, a1ControllerPassword);
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
            .flatMap(notUsed -> Mono.just(A1ProtocolType.SDNR_ONAP));
    }

    @Override
    public Mono<String> getPolicyStatus(Policy policy) {
        return Mono.error(new Exception("Status not implemented in the controller"));
    }

    private Flux<String> getPolicyTypeIds() {
        JSONObject paramsJson = new JSONObject();
        paramsJson.put(NEAR_RT_RIC_ID, ricConfig.baseUrl());
        String inputJsonString = JsonHelper.createInputJsonString(paramsJson);
        logger.debug("POST getPolicyTypeIdentities inputJsonString = {}", inputJsonString);

        return restClient
            .postWithAuthHeader("/A1-ADAPTER-API:getPolicyTypes", inputJsonString, a1ControllerUsername,
                a1ControllerPassword) //
            .flatMap(response -> JsonHelper.getValueFromResponse(response, "policy-type-id-list")) //
            .flatMapMany(JsonHelper::parseJsonArrayOfString);
    }

    private Flux<String> getPolicyIdentitiesByType(String policyTypeId) {
        JSONObject paramsJson = new JSONObject();
        paramsJson.put(NEAR_RT_RIC_ID, ricConfig.baseUrl());
        paramsJson.put(POLICY_TYPE_ID, policyTypeId);
        String inputJsonString = JsonHelper.createInputJsonString(paramsJson);
        logger.debug("POST getPolicyIdentities inputJsonString = {}", inputJsonString);

        return restClient
            .postWithAuthHeader("/A1-ADAPTER-API:getPolicyInstances", inputJsonString, a1ControllerUsername,
                a1ControllerPassword) //
            .flatMap(response -> JsonHelper.getValueFromResponse(response, "policy-instance-id-list")) //
            .flatMapMany(JsonHelper::parseJsonArrayOfString);
    }

    private Flux<String> deletePoliciesForType(String typeId) {
        return getPolicyIdentitiesByType(typeId) //
            .flatMap(policyId -> deletePolicyByTypeId(typeId, policyId)); //
    }

    private Mono<String> deletePolicyByTypeId(String policyTypeId, String policyId) {
        JSONObject paramsJson = new JSONObject();
        paramsJson.put(NEAR_RT_RIC_ID, ricConfig.baseUrl());
        paramsJson.put(POLICY_INSTANCE_ID, policyId);
        paramsJson.put(POLICY_TYPE_ID, policyTypeId);
        String inputJsonString = JsonHelper.createInputJsonString(paramsJson);
        logger.debug("POST deletePolicy inputJsonString = {}", inputJsonString);

        return restClient.postWithAuthHeader("/A1-ADAPTER-API:deletePolicyInstance", inputJsonString,
            a1ControllerUsername, a1ControllerPassword);
    }
}
