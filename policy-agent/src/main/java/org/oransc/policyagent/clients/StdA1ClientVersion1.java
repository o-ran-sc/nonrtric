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

import java.util.Arrays;
import java.util.List;

import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.Policy;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class StdA1ClientVersion1 implements A1Client {
    private final AsyncRestClient restClient;

    public StdA1ClientVersion1(RicConfig ricConfig) {
        final String urlPrefix = "/A1-P/v1";
        String baseUrl = ricConfig.baseUrl() + urlPrefix;
        this.restClient = new AsyncRestClient(baseUrl);
    }

    public StdA1ClientVersion1(AsyncRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Mono<List<String>> getPolicyIdentities() {
        return getPolicyIds() //
            .collectList();
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        final UriComponentsBuilder policyUri = UriComponentsBuilder.fromPath("/policies/{policy-id}");
        final String uri = policyUri.buildAndExpand(policy.id()).toUriString();
        return restClient.put(uri, policy.json()) //
            .flatMap(JsonHelper::validateJson);
    }

    @Override
    public Mono<List<String>> getPolicyTypeIdentities() {
        return Mono.just(Arrays.asList(""));
    }

    @Override
    public Mono<String> getPolicyTypeSchema(String policyTypeId) {
        return Mono.just("{}");
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
            .flatMap(x -> Mono.just(A1ProtocolType.STD_V1_1));
    }

    @Override
    public Mono<String> getPolicyStatus(Policy policy) {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/policies/{policy-id}/status");
        String uri = builder.buildAndExpand(policy.id()).toUriString();
        return restClient.get(uri);
    }

    private Flux<String> getPolicyIds() {
        final String uri = "/policies";
        return restClient.get(uri) //
            .flatMapMany(JsonHelper::parseJsonArrayOfString);
    }

    private Mono<String> deletePolicyById(String policyId) {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/policies/{policy-id}");
        String uri = builder.buildAndExpand(policyId).toUriString();
        return restClient.delete(uri);
    }
}
