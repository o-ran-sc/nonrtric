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

import java.util.List;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.Policy;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class StdA1Client implements A1Client {
    private static final String INTERFACE_VERSION = "/A1-P/v1";

    private static final String POLICYTYPES = "/policytypes";
    private static final String POLICY_TYPE_ID = "policyTypeId";

    private static final String POLICIES = "/policies";

    private static final UriComponentsBuilder GET_POLICY_TYPE_SCHEMA_URI_BUILDER =
        UriComponentsBuilder.fromPath("/policytypes/{policy-type-name}");

    private static final UriComponentsBuilder PUT_POLICY_URI_BUILDER =
        UriComponentsBuilder.fromPath("/policies/{policy-id}").queryParam(POLICY_TYPE_ID, "{policy-type-name}");

    private static final UriComponentsBuilder DELETE_POLICY_URI_BUILDER =
        UriComponentsBuilder.fromPath("/policies/{policy-id}");

    private static final UriComponentsBuilder GET_POLICY_STATUS_URI_BUILDER =
        UriComponentsBuilder.fromPath("/policies/{policy-id}/status");

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
            .flatMap(JsonHelper::validateJson);
    }

    @Override
    public Mono<List<String>> getPolicyTypeIdentities() {
        return restClient.get(POLICYTYPES) //
            .flatMapMany(JsonHelper::parseJsonArrayOfString) //
            .collectList();
    }

    @Override
    public Mono<String> getPolicyTypeSchema(String policyTypeId) {
        String uri = GET_POLICY_TYPE_SCHEMA_URI_BUILDER.buildAndExpand(policyTypeId).toUriString();
        return restClient.get(uri) //
            .flatMap(JsonHelper::extractPolicySchema);
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
            .flatMapMany(JsonHelper::parseJsonArrayOfString);
    }

    private Mono<String> deletePolicyById(String policyId) {
        String uri = DELETE_POLICY_URI_BUILDER.buildAndExpand(policyId).toUriString();
        return restClient.delete(uri);
    }
}
