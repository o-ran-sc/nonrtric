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

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SdnrOnapA1Client implements A1Client {
    private static final String URL_PREFIX = "/A1-ADAPTER-API:";

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String a1ControllerUsername;
    private final String a1ControllerPassword;
    private final RicConfig ricConfig;
    private final AsyncRestClient restClient;

    private static Gson gson = new GsonBuilder() //
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES) //
        .create(); //

    public SdnrOnapA1Client(RicConfig ricConfig, String baseUrl, String username, String password) {
        this(ricConfig, username, password, new AsyncRestClient(baseUrl + "/restconf/operations"));
        if (logger.isDebugEnabled()) {
            logger.debug("SdnrOnapA1Client for ric: {}, a1ControllerBaseUrl: {}", ricConfig.name(), baseUrl);
        }
    }

    public SdnrOnapA1Client(RicConfig ricConfig, String username, String password, AsyncRestClient restClient) {
        this.ricConfig = ricConfig;
        this.a1ControllerUsername = username;
        this.a1ControllerPassword = password;
        this.restClient = restClient;
    }

    @Override
    public Mono<List<String>> getPolicyTypeIdentities() {
        SdnrOnapAdapterInput inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(ricConfig.baseUrl()) //
            .build();
        String inputJsonString = createInputJsonString(inputParams);
        logger.debug("POST getPolicyTypeIdentities inputJsonString = {}", inputJsonString);

        return restClient
            .postWithAuthHeader(URL_PREFIX + "getPolicyTypes", inputJsonString, a1ControllerUsername,
                a1ControllerPassword) //
            .flatMap(response -> getValueFromResponse(response, "policy-type-id-list")) //
            .flatMap(this::parseJsonArrayOfString);
    }

    @Override
    public Mono<List<String>> getPolicyIdentities() {
        return getPolicyTypeIdentities() //
            .flatMapMany(types -> Flux.fromIterable(types)) //
            .flatMap(type -> getPolicyIdentities(type)) //
            .flatMap(policyIds -> Flux.fromIterable(policyIds)) //
            .collectList();
    }

    public Mono<List<String>> getPolicyIdentities(String policyTypeId) {
        SdnrOnapAdapterInput inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(ricConfig.baseUrl()) //
            .policyTypeId(policyTypeId) //
            .build();
        String inputJsonString = createInputJsonString(inputParams);
        logger.debug("POST getPolicyIdentities inputJsonString = {}", inputJsonString);

        return restClient
            .postWithAuthHeader(URL_PREFIX + "getPolicyInstances", inputJsonString, a1ControllerUsername,
                a1ControllerPassword) //
            .flatMap(response -> getValueFromResponse(response, "policy-instance-id-list")) //
            .flatMap(this::parseJsonArrayOfString);
    }

    @Override
    public Mono<String> getPolicyTypeSchema(String policyTypeId) {
        SdnrOnapAdapterInput inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(ricConfig.baseUrl()) //
            .policyTypeId(policyTypeId) //
            .build();
        String inputJsonString = createInputJsonString(inputParams);
        logger.debug("POST getPolicyType inputJsonString = {}", inputJsonString);

        return restClient
            .postWithAuthHeader(URL_PREFIX + "getPolicyType", inputJsonString, a1ControllerUsername,
                a1ControllerPassword) //
            .flatMap(response -> getValueFromResponse(response, "policy-type")) //
            .flatMap(this::extractPolicySchema);
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        SdnrOnapAdapterInput inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(ricConfig.baseUrl()) //
            .policyTypeId(policy.type().name()) //
            .policyInstanceId(policy.id()) //
            .policyInstance(policy.json()) //
            .properties(new ArrayList<String>()) //
            .build();
        String inputJsonString = createInputJsonString(inputParams);
        logger.debug("POST putPolicy inputJsonString = {}", inputJsonString);

        return restClient.postWithAuthHeader(URL_PREFIX + "createPolicyInstance", inputJsonString,
            a1ControllerUsername, a1ControllerPassword);
    }

    public Mono<String> deletePolicyByIds(String policyTypeId, String policyId) {
        SdnrOnapAdapterInput inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(ricConfig.baseUrl()) //
            .policyTypeId(policyTypeId) //
            .policyInstanceId(policyId) //
            .build();
        String inputJsonString = createInputJsonString(inputParams);
        logger.debug("POST deletePolicy inputJsonString = {}", inputJsonString);

        return restClient.postWithAuthHeader(URL_PREFIX + "deletePolicyInstance", inputJsonString,
            a1ControllerUsername, a1ControllerPassword);
    }

    @Override
    public Mono<String> deletePolicy(Policy policy) {
        return deletePolicyByIds(policy.type().name(), policy.id());
    }

    @Override
    public Flux<String> deleteAllPolicies() {
        return getPolicyTypeIdentities() //
            .flatMapMany(types -> Flux.fromIterable(types)) //
            .flatMap(typeId -> deletePoliciesForType(typeId)); //
    }

    @Override
    public Mono<A1ProtocolType> getProtocolVersion() {
        return getPolicyTypeIdentities() //
            .flatMap(x -> Mono.just(A1ProtocolType.SDNR_ONAP));
    }

    @Override
    public Mono<String> getPolicyStatus(Policy policy) {
        return Mono.error(new Exception("Status not implemented in the controller"));
    }

    private Mono<String> getValueFromResponse(String response, String key) {
        logger.debug("A1 client: response = {}", response);
        try {
            JSONObject outputJson = new JSONObject(response);
            JSONObject responseParams = outputJson.getJSONObject("output");
            if (!responseParams.has(key)) {
                return Mono.just("");
            }
            String value = responseParams.get(key).toString();
            return Mono.just(value);
        } catch (JSONException ex) { // invalid json
            return Mono.error(ex);
        }
    }

    private Mono<List<String>> parseJsonArrayOfString(String inputString) {
        try {
            List<String> arrayList = new ArrayList<>();
            if (inputString.isEmpty()) {
                return Mono.just(arrayList);
            }
            JSONArray jsonArray = new JSONArray(inputString);
            for (int i = 0; i < jsonArray.length(); i++) {
                arrayList.add(jsonArray.getString(i));
            }
            logger.debug("A1 client: received list = {}", arrayList);
            return Mono.just(arrayList);
        } catch (JSONException ex) { // invalid json
            return Mono.error(ex);
        }
    }

    private Mono<String> extractPolicySchema(String inputString) {
        try {
            JSONObject jsonObject = new JSONObject(inputString);
            JSONObject schemaObject = jsonObject.getJSONObject("policySchema");
            String schemaString = schemaObject.toString();
            return Mono.just(schemaString);
        } catch (JSONException ex) { // invalid json
            return Mono.error(ex);
        }
    }

    private Flux<String> deletePoliciesForType(String typeId) {
        return getPolicyIdentities(typeId) //
            .flatMapMany(policyIds -> Flux.fromIterable(policyIds)) //
            .flatMap(policyId -> deletePolicyByIds(typeId, policyId)); //
    }

    private String createInputJsonString(SdnrOnapAdapterInput inputParams) {
        JSONObject inputJson = new JSONObject();
        inputJson.put("input", new JSONObject(gson.toJson(inputParams)));
        return inputJson.toString();
    }
}
