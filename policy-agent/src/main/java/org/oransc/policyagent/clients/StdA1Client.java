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

public class StdA1Client implements A1Client {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final RicConfig ricConfig;
    private final AsyncRestClient restClient;

    public StdA1Client(RicConfig ricConfig) {
        this.ricConfig = ricConfig;
        String baseUrl = ricConfig.baseUrl() + "/A1-P/v1";
        this.restClient = new AsyncRestClient(baseUrl);
    }

    public StdA1Client(RicConfig ricConfig, AsyncRestClient restClient) {
        this.ricConfig = ricConfig;
        this.restClient = restClient;
    }

    @Override
    public Mono<List<String>> getPolicyTypeIdentities() {
        logger.debug("getPolicyTypeIdentities nearRtRicUrl = {}", ricConfig.baseUrl());
        return restClient.get("/policytypes/identities") //
            .flatMap(this::parseJsonArrayOfString);
    }

    @Override
    public Mono<List<String>> getPolicyIdentities() {
        logger.debug("getPolicyIdentities nearRtRicUrl = {}", ricConfig.baseUrl());
        return restClient.get("/policies/identities") //
            .flatMap(this::parseJsonArrayOfString);
    }

    @Override
    public Mono<String> getPolicyTypeSchema(String policyTypeId) {
        logger.debug("getPolicyType nearRtRicUrl = {}, policyTypeId = {}", ricConfig.baseUrl(), policyTypeId);
        Mono<String> response = restClient.get("/policytypes/" + policyTypeId);
        return response.flatMap(this::createMono);
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        logger.debug("putPolicy nearRtRicUrl = {}, policyId = {}, policyString = {}", //
            policy.ric().getConfig().baseUrl(), policy.id(), policy.json());
        // TODO update when simulator is updated to include policy type
        // Mono<String> response = client.put("/policies/" + policy.id() + "?policyTypeId=" + policy.type().name(),
        // policy.json());
        Mono<String> response = restClient.put("/policies/" + policy.id(), policy.json());

        return response.flatMap(this::createMono);
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

    @Override
    public Mono<A1ProtocolType> getProtocolVersion() {
        return getPolicyTypeIdentities() //
            .flatMap(x -> Mono.just(A1ProtocolType.STD_V1));
    }

    private Mono<String> deletePolicy(String policyId) {
        return restClient.delete("/policies/" + policyId);
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

    private Mono<String> createMono(String inputString) {
        try {
            JSONObject jsonObject = new JSONObject(inputString);
            String jsonString = jsonObject.toString();
            logger.debug("A1 client: received string = {}", jsonString);
            return Mono.just(jsonString);
        } catch (JSONException ex) { // invalid json
            return Mono.error(ex);
        }
    }

}
