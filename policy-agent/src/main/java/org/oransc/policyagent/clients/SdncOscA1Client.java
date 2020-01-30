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

public class SdncOscA1Client implements A1Client {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String A1_CONTROLLER_URL = "http://a1-controller-container:8181/restconf/operations";
    private static final String A1_CONTROLLER_USERNAME = "admin";
    private static final String A1_CONTROLLER_PASSWORD = "Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U";

    private final RicConfig ricConfig;
    private final AsyncRestClient restClient;

    public SdncOscA1Client(RicConfig ricConfig) {
        this.ricConfig = ricConfig;
        this.restClient = new AsyncRestClient(A1_CONTROLLER_URL);
        logger.debug("SdncOscA1Client for ric: {}", this.ricConfig.name());
    }

    @Override
    public Mono<List<String>> getPolicyTypeIdentities() {
        JSONObject paramsJson = new JSONObject();
        paramsJson.put("near-rt-ric-url", ricConfig.baseUrl());
        String inputJsonString = createInputJsonString(paramsJson);
        logger.debug("POST getPolicyTypeIdentities inputJsonString = {}", inputJsonString);

        return restClient
            .postWithAuthHeader("/A1-ADAPTER-API:getPolicyTypeIdentities", inputJsonString, A1_CONTROLLER_USERNAME,
                A1_CONTROLLER_PASSWORD) //
            .flatMap(response -> getValueFromResponse(response, "policy-type-id-list")) //
            .flatMap(this::parseJsonArrayOfString);
    }

    @Override
    public Mono<List<String>> getPolicyIdentities() {
        JSONObject paramsJson = new JSONObject();
        paramsJson.put("near-rt-ric-url", ricConfig.baseUrl());
        String inputJsonString = createInputJsonString(paramsJson);
        logger.debug("POST getPolicyIdentities inputJsonString = {}", inputJsonString);

        return restClient
            .postWithAuthHeader("/A1-ADAPTER-API:getPolicyIdentities", inputJsonString, A1_CONTROLLER_USERNAME,
                A1_CONTROLLER_PASSWORD) //
            .flatMap(response -> getValueFromResponse(response, "policy-id-list")) //
            .flatMap(this::parseJsonArrayOfString);
    }

    @Override
    public Mono<String> getPolicyTypeSchema(String policyTypeId) {
        JSONObject paramsJson = new JSONObject();
        paramsJson.put("near-rt-ric-url", ricConfig.baseUrl());
        paramsJson.put("policy-type-id", policyTypeId);
        String inputJsonString = createInputJsonString(paramsJson);
        logger.debug("POST getPolicyType inputJsonString = {}", inputJsonString);

        return restClient
            .postWithAuthHeader("/A1-ADAPTER-API:getPolicyType", inputJsonString, A1_CONTROLLER_USERNAME,
                A1_CONTROLLER_PASSWORD) //
            .flatMap(response -> getValueFromResponse(response, "policy-type")) //
            .flatMap(this::extractPolicySchema);
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        JSONObject paramsJson = new JSONObject();
        paramsJson.put("near-rt-ric-url", ricConfig.baseUrl());
        paramsJson.put("policy-id", policy.id());
        paramsJson.put("policy-type-id", policy.type().name());
        paramsJson.put("policy", policy.json());
        String inputJsonString = createInputJsonString(paramsJson);
        logger.debug("POST putPolicy inputJsonString = {}", inputJsonString);

        return restClient
            .postWithAuthHeader("/A1-ADAPTER-API:putPolicy", inputJsonString, A1_CONTROLLER_USERNAME,
                A1_CONTROLLER_PASSWORD) //
            .flatMap(response -> getValueFromResponse(response, "returned-policy")) //
            .flatMap(this::validateJson);
    }

    @Override
    public Mono<String> deletePolicy(Policy policy) {
        return deletePolicy(policy.id());
    }

    @Override
    public Flux<String> deleteAllPolicies() {
        return getPolicyIdentities() //
            .flatMapMany(policyIds -> Flux.fromIterable(policyIds)) // )
            .flatMap(policyId -> deletePolicy(policyId)); //
    }

    public Mono<String> deletePolicy(String policyId) {
        JSONObject paramsJson = new JSONObject();
        paramsJson.put("near-rt-ric-url", ricConfig.baseUrl());
        paramsJson.put("policy-id", policyId);
        String inputJsonString = createInputJsonString(paramsJson);
        logger.debug("POST deletePolicy inputJsonString = {}", inputJsonString);

        return restClient.postWithAuthHeader("/A1-ADAPTER-API:deletePolicy", inputJsonString, A1_CONTROLLER_USERNAME,
            A1_CONTROLLER_PASSWORD);
    }

    @Override
    public Mono<A1ProtocolType> getProtocolVersion() {
        return getPolicyTypeIdentities() //
            .flatMap(x -> Mono.just(A1ProtocolType.SDNC_OSC));
    }

    private String createInputJsonString(JSONObject paramsJson) {
        JSONObject inputJson = new JSONObject();
        inputJson.put("input", paramsJson);
        return inputJson.toString();
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

    private Mono<String> validateJson(String inputString) {
        try {
            new JSONObject(inputString);
            return Mono.just(inputString);
        } catch (JSONException ex) { // invalid json
            return Mono.error(ex);
        }
    }
}
