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
    private static final String URL_PREFIX = "/A1-P/v1";

    private static final String POLICY_TYPES_URI = "/policytypes";
    private static final String POLICY_TYPE_ID = "policyTypeId";

    private static final String POLICIES_URI = "/policies";

    private static final UriComponentsBuilder POLICY_TYPE_SCHEMA_URI =
        UriComponentsBuilder.fromPath("/policytypes/{policy-type-name}");

    private static final UriComponentsBuilder POLICY_URI =
        UriComponentsBuilder.fromPath("/policies/{policy-id}").queryParam(POLICY_TYPE_ID, "{policy-type-name}");

    private static final UriComponentsBuilder POLICY_DELETE_URI =
        UriComponentsBuilder.fromPath("/policies/{policy-id}");

    private static final UriComponentsBuilder POLICY_STATUS_URI =
        UriComponentsBuilder.fromPath("/policies/{policy-id}/status");

    private final AsyncRestClient restClient;

    public StdA1Client(RicConfig ricConfig) {
        String baseUrl = ricConfig.baseUrl() + URL_PREFIX;
        this.restClient = new AsyncRestClient(baseUrl);
    }

    public StdA1Client(AsyncRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Mono<List<String>> getPolicyIdentities() {
        return getPolicyIds() //
            .collectList();
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        String uri = POLICY_URI.buildAndExpand(policy.id(), policy.type().name()).toUriString();
        return restClient.put(uri, policy.json()) //
            .flatMap(JsonHelper::validateJson);
    }

    @Override
    public Mono<List<String>> getPolicyTypeIdentities() {
        return restClient.get(POLICY_TYPES_URI) //
            .flatMapMany(JsonHelper::parseJsonArrayOfString) //
            .collectList();
    }

    @Override
    public Mono<String> getPolicyTypeSchema(String policyTypeId) {
        String uri = POLICY_TYPE_SCHEMA_URI.buildAndExpand(policyTypeId).toUriString();
        return restClient.get(uri) //
            .flatMap(JsonHelper::extractPolicySchema);
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
        return getPolicyTypeIdentities() //
            .flatMap(x -> Mono.just(A1ProtocolType.STD_V1));
    }

    @Override
    public Mono<String> getPolicyStatus(Policy policy) {
        String uri = POLICY_STATUS_URI.buildAndExpand(policy.id()).toUriString();
        return restClient.get(uri);
    }

    private Flux<String> getPolicyIds() {
        return restClient.get(POLICIES_URI) //
            .flatMapMany(JsonHelper::parseJsonArrayOfString);
    }

    private Mono<String> deletePolicyById(String policyId) {
        String uri = POLICY_DELETE_URI.buildAndExpand(policyId).toUriString();
        return restClient.delete(uri);
    }
}
