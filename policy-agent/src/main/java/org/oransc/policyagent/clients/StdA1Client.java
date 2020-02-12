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
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class StdA1Client implements A1Client {
    private static final String INTERFACE_VERSION = "/A1-P/v1";

    private static final String POLICYTYPES = "/policytypes";
    private static final String POLICY_TYPE_ID = "policyTypeId";

    private static final String POLICIES = "/policies";
    private static final String POLICY_SCHEMA = "policySchema";

    private static final UriComponentsBuilder GET_POLICY_TYPE_SCHEMA_URI_BUILDER =
        UriComponentsBuilder.fromPath("/policytypes/{policy-type-name}");

    private static final UriComponentsBuilder PUT_POLICY_URI_BUILDER =
        UriComponentsBuilder.fromPath("/policies/{policy-id}").queryParam(POLICY_TYPE_ID, "{policy-type-name}");

    private static final UriComponentsBuilder DELETE_POLICY_URI_BUILDER =
        UriComponentsBuilder.fromPath("/policies/{policy-id}");

    private static final UriComponentsBuilder GET_POLICY_STATUS_URI_BUILDER =
        UriComponentsBuilder.fromPath("/policies/{policy-id}/status");

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AsyncRestClient restClient;

    public StdA1Client(RicConfig ricConfig) {
        String baseUrl = ricConfig.baseUrl() + INTERFACE_VERSION;
        this.restClient = new AsyncRestClient(baseUrl);
    }

    public StdA1Client(AsyncRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Mono<List<String>> getPolicyIdentities() {
        return getPolicies() //
            .collectList();
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        String uri = PUT_POLICY_URI_BUILDER.buildAndExpand(policy.id(), policy.type().name()).toUriString();
        return restClient.put(uri, policy.json()) //
            .flatMap(this::validateJson);
    }

    @Override
    public Mono<List<String>> getPolicyTypeIdentities() {
        return restClient.get(POLICYTYPES) //
            .flatMapMany(this::parseJsonArrayOfString) //
            .collectList();
    }

    @Override
    public Mono<String> getPolicyTypeSchema(String policyTypeId) {
        String uri = GET_POLICY_TYPE_SCHEMA_URI_BUILDER.buildAndExpand(policyTypeId).toUriString();
        return restClient.get(uri) //
            .flatMap(this::extractPolicySchema);
    }

    @Override
    public Mono<String> deletePolicy(Policy policy) {
        return deletePolicyById(policy.id());
    }

    @Override
    public Flux<String> deleteAllPolicies() {
        return getPolicies() //
            .flatMap(this::deletePolicyById); //
    }

    @Override
    public Mono<A1ProtocolType> getProtocolVersion() {
        return getPolicyTypeIdentities() //
            .flatMap(x -> Mono.just(A1ProtocolType.STD_V1));
    }

    @Override
    public Mono<String> getPolicyStatus(Policy policy) {
        String uri = GET_POLICY_STATUS_URI_BUILDER.buildAndExpand(policy.id()).toUriString();
        return restClient.get(uri);
    }

    private Flux<String> getPolicies() {
        return restClient.get(POLICIES) //
            .flatMapMany(this::parseJsonArrayOfString);
    }

    private Mono<String> deletePolicyById(String policyId) {
        String uri = DELETE_POLICY_URI_BUILDER.buildAndExpand(policyId).toUriString();
        return restClient.delete(uri);
    }

    private Flux<String> parseJsonArrayOfString(String inputString) {
        try {
            List<String> arrayList = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(inputString);
            for (int i = 0; i < jsonArray.length(); i++) {
                arrayList.add(jsonArray.getString(i));
            }
            logger.debug("A1 client: received list = {}", arrayList);
            return Flux.fromIterable(arrayList);
        } catch (JSONException ex) { // invalid json
            return Flux.error(ex);
        }
    }

    private Mono<String> extractPolicySchema(String inputString) {
        try {
            JSONObject jsonObject = new JSONObject(inputString);
            JSONObject schemaObject = jsonObject.getJSONObject(POLICY_SCHEMA);
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
