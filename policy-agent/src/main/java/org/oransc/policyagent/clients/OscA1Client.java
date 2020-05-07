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
import org.oransc.policyagent.configuration.WebClientConfig;
import org.oransc.policyagent.repository.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Client for accessing OSC A1 REST API
 */
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
public class OscA1Client implements A1Client {
    static final int CONCURRENCY_RIC = 1; // How may paralell requests that is sent to one NearRT RIC

    public static class UriBuilder implements A1UriBuilder {
        private final RicConfig ricConfig;

        public UriBuilder(RicConfig ricConfig) {
            this.ricConfig = ricConfig;
        }

        @Override
        public String createPutPolicyUri(String type, String policyId) {
            return createPolicyUri(type, policyId);
        }

        /**
         * /a1-p/policytypes/{policy_type_id}/policies
         */
        public String createGetPolicyIdsUri(String type) {
            return createPolicyTypeUri(type) + "/policies";
        }

        @Override
        public String createDeleteUri(String type, String policyId) {
            return createPolicyUri(type, policyId);
        }

        /**
         * ​/a1-p​/policytypes​/{policy_type_id}​/policies​/{policy_instance_id}​/status
         */
        @Override
        public String createGetPolicyStatusUri(String type, String policyId) {
            return createPolicyUri(type, policyId) + "/status";
        }

        /**
         * ​/a1-p​/healthcheck
         */
        public String createHealtcheckUri() {
            return baseUri() + "/healthcheck";
        }

        /**
         * /a1-p/policytypes/{policy_type_id}
         */
        public String createGetSchemaUri(String type) {
            return this.createPolicyTypeUri(type);
        }

        /**
         * ​/a1-p​/policytypes​/{policy_type_id}
         */
        public String createPolicyTypesUri() {
            return baseUri() + "/policytypes";
        }

        /**
         * ​/a1-p​/policytypes​/{policy_type_id}​/policies​/{policy_instance_id}
         */
        private String createPolicyUri(String type, String id) {
            return createPolicyTypeUri(type) + "/policies/" + id;
        }

        /**
         * /a1-p/policytypes/{policy_type_id}
         */
        private String createPolicyTypeUri(String type) {
            return createPolicyTypesUri() + "/" + type;
        }

        private String baseUri() {
            return ricConfig.baseUrl() + "/a1-p";
        }
    }

    private static final String TITLE = "title";
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final AsyncRestClient restClient;
    private final UriBuilder uri;

    public OscA1Client(RicConfig ricConfig, WebClientConfig config) {
        this(ricConfig, new AsyncRestClient("", config));
    }

    public OscA1Client(RicConfig ricConfig, AsyncRestClient restClient) {
        this.restClient = restClient;
        logger.debug("OscA1Client for ric: {}", ricConfig.name());

        uri = new UriBuilder(ricConfig);
    }

    public static Mono<String> extractCreateSchema(String policyTypeResponse, String policyTypeId) {
        try {
            JSONObject obj = new JSONObject(policyTypeResponse);
            JSONObject schemaObj = obj.getJSONObject("create_schema");
            schemaObj.put(TITLE, policyTypeId);
            return Mono.just(schemaObj.toString());
        } catch (Exception e) {
            String exceptionString = e.toString();
            logger.error("Unexpected response for policy type: {}, exception: {}", policyTypeResponse, exceptionString);
            return Mono.error(e);
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
        String schemaUri = uri.createGetSchemaUri(policyTypeId);
        return restClient.get(schemaUri) //
            .flatMap(response -> extractCreateSchema(response, policyTypeId));
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        String policyUri = this.uri.createPutPolicyUri(policy.type().name(), policy.id());
        return restClient.put(policyUri, policy.json());
    }

    @Override
    public Mono<String> deletePolicy(Policy policy) {
        return deletePolicyById(policy.type().name(), policy.id());
    }

    @Override
    public Mono<A1ProtocolType> getProtocolVersion() {
        return restClient.get(uri.createHealtcheckUri()) //
            .flatMap(notUsed -> Mono.just(A1ProtocolType.OSC_V1));
    }

    @Override
    public Flux<String> deleteAllPolicies() {
        return getPolicyTypeIds() //
            .flatMap(this::deletePoliciesForType, CONCURRENCY_RIC);
    }

    @Override
    public Mono<String> getPolicyStatus(Policy policy) {
        String statusUri = uri.createGetPolicyStatusUri(policy.type().name(), policy.id());
        return restClient.get(statusUri);

    }

    private Flux<String> getPolicyTypeIds() {
        return restClient.get(uri.createPolicyTypesUri()) //
            .flatMapMany(SdncJsonHelper::parseJsonArrayOfString);
    }

    private Flux<String> getPolicyIdentitiesByType(String typeId) {
        return restClient.get(uri.createGetPolicyIdsUri(typeId)) //
            .flatMapMany(SdncJsonHelper::parseJsonArrayOfString);
    }

    private Mono<String> deletePolicyById(String typeId, String policyId) {
        String policyUri = uri.createDeleteUri(typeId, policyId);
        return restClient.delete(policyUri);
    }

    private Flux<String> deletePoliciesForType(String typeId) {
        return getPolicyIdentitiesByType(typeId) //
            .flatMap(policyId -> deletePolicyById(typeId, policyId), CONCURRENCY_RIC);
    }
}
