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
import java.util.List;

import org.json.JSONObject;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class OscA1Client implements A1Client {
    private static final String URL_PREFIX = "/a1-p";

    private static final String POLICY_TYPES = "/policytypes";
    private static final String CREATE_SCHEMA = "create_schema";
    private static final String TITLE = "title";

    private static final String HEALTHCHECK = "/healthcheck";

    private static final UriComponentsBuilder POLICY_TYPE_SCHEMA_URI =
        UriComponentsBuilder.fromPath("/policytypes/{policy-type-name}");

    private static final UriComponentsBuilder POLICY_URI =
        UriComponentsBuilder.fromPath("/policytypes/{policy-type-name}/policies/{policy-id}");

    private static final UriComponentsBuilder POLICY_IDS_URI =
        UriComponentsBuilder.fromPath("/policytypes/{policy-type-name}/policies");

    private static final UriComponentsBuilder POLICY_STATUS_URI =
        UriComponentsBuilder.fromPath("/policytypes/{policy-type-name}/policies/{policy-id}/status");

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AsyncRestClient restClient;

    public OscA1Client(RicConfig ricConfig) {
        String baseUrl = ricConfig.baseUrl() + URL_PREFIX;
        this.restClient = new AsyncRestClient(baseUrl);
        if (logger.isDebugEnabled()) {
            logger.debug("OscA1Client for ric: {}", ricConfig.name());
        }
    }

    public OscA1Client(AsyncRestClient restClient) {
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
        String uri = POLICY_TYPE_SCHEMA_URI.buildAndExpand(policyTypeId).toUriString();
        return restClient.get(uri) //
            .flatMap(response -> getCreateSchema(response, policyTypeId));
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        String uri = POLICY_URI.buildAndExpand(policy.type().name(), policy.id()).toUriString();
        return restClient.put(uri, policy.json());
    }

    @Override
    public Mono<String> deletePolicy(Policy policy) {
        return deletePolicyById(policy.type().name(), policy.id());
    }

    @Override
    public Mono<A1ProtocolType> getProtocolVersion() {
        return restClient.get(HEALTHCHECK) //
            .flatMap(notUsed -> Mono.just(A1ProtocolType.OSC_V1));
    }

    @Override
    public Flux<String> deleteAllPolicies() {
        return getPolicyTypeIds() //
            .flatMap(this::deletePoliciesForType);
    }

    @Override
    public Mono<String> getPolicyStatus(Policy policy) {
        String uri = POLICY_STATUS_URI.buildAndExpand(policy.type().name(), policy.id()).toUriString();
        return restClient.get(uri);

    }

    private Flux<String> getPolicyTypeIds() {
        return restClient.get(POLICY_TYPES) //
            .flatMapMany(JsonHelper::parseJsonArrayOfString);
    }

    private Flux<String> getPolicyIdentitiesByType(String typeId) {
        return restClient.get(POLICY_IDS_URI.buildAndExpand(typeId).toUriString()) //
            .flatMapMany(JsonHelper::parseJsonArrayOfString);
    }

    private Mono<String> getCreateSchema(String policyTypeResponse, String policyTypeId) {
        try {
            JSONObject obj = new JSONObject(policyTypeResponse);
            JSONObject schemaObj = obj.getJSONObject(CREATE_SCHEMA);
            schemaObj.put(TITLE, policyTypeId);
            return Mono.just(schemaObj.toString());
        } catch (Exception e) {
            logger.error("Unexcpected response for policy type: {}", policyTypeResponse, e);
            return Mono.error(e);
        }
    }

    private Mono<String> deletePolicyById(String typeId, String policyId) {
        String uri = POLICY_URI.buildAndExpand(typeId, policyId).toUriString();
        return restClient.delete(uri);
    }

    private Flux<String> deletePoliciesForType(String typeId) {
        return getPolicyIdentitiesByType(typeId) //
            .flatMap(policyId -> deletePolicyById(typeId, policyId));
    }
}
