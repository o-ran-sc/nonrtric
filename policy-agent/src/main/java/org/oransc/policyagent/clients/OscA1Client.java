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

public class OscA1Client implements A1Client {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AsyncRestClient restClient;

    public OscA1Client(RicConfig ricConfig) {
        String baseUrl = ricConfig.baseUrl() + "/a1-p";
        this.restClient = new AsyncRestClient(baseUrl);
        logger.debug("OscA1Client for ric: {}", ricConfig.name());
    }

    @Override
    public Mono<List<String>> getPolicyTypeIdentities() {
        return restClient.get("/policytypes") //
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

    private Mono<List<String>> getPolicyIdentities(String typeId) {
        return restClient.get("/policytypes/" + typeId + "/policies") //
            .flatMap(this::parseJsonArrayOfString);
    }

    @Override
    public Mono<String> getPolicyTypeSchema(String policyTypeId) {
        return restClient.get("/policytypes/" + policyTypeId) //
            .flatMap(response -> getCreateSchema(response, policyTypeId));
    }

    private Mono<String> getCreateSchema(String policyTypeResponse, String policyTypeId) {
        try {
            JSONObject obj = new JSONObject(policyTypeResponse);
            JSONObject schemaObj = obj.getJSONObject("create_schema");
            schemaObj.put("title", policyTypeId);
            return Mono.just(schemaObj.toString());
        } catch (Exception e) {
            logger.error("Unexcpected response for policy type: {}", policyTypeResponse, e);
            return Mono.error(e);
        }
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        return restClient.put("/policytypes/" + policy.type().name() + "/policies/" + policy.id(), policy.json());
    }

    @Override
    public Mono<String> deletePolicy(Policy policy) {
        return deletePolicy(policy.type().name(), policy.id());
    }

    private Mono<String> deletePolicy(String typeId, String policyId) {
        return restClient.delete("/policytypes/" + typeId + "/policies/" + policyId);
    }

    @Override
    public Mono<A1ProtocolType> getProtocolVersion() {
        return restClient.get("/healthcheck") //
            .flatMap(resp -> Mono.just(A1ProtocolType.OSC_V1));
    }

    @Override
    public Flux<String> deleteAllPolicies() {
        return getPolicyTypeIdentities() //
            .flatMapMany(types -> Flux.fromIterable(types)) //
            .flatMap(typeId -> deletePoliciesForType(typeId)); //
    }

    private Flux<String> deletePoliciesForType(String typeId) {
        return getPolicyIdentities(typeId) //
            .flatMapMany(policyIds -> Flux.fromIterable(policyIds)) //
            .flatMap(policyId -> deletePolicy(typeId, policyId)); //
    }

    private Mono<List<String>> parseJsonArrayOfString(String inputString) {
        try {
            List<String> arrayList = new ArrayList<>();
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

    @Override
    public Mono<String> getPolicyStatus(Policy policy) {
        // /a1-p/policytypes/{policy_type_id}/policies/{policy_instance_id}/status
        return restClient.get("/policytypes/" + policy.type().name() + "/policies/" + policy.id() + "/status");

    }
}
