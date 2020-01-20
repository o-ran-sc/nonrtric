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
import java.util.Collection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class A1ClientImpl implements A1Client {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final RicConfig ricConfig;

    public A1ClientImpl(RicConfig ricConfig) {
        this.ricConfig = ricConfig;
    }

    private String getBaseUrl() {
        return ricConfig.baseUrl() + "/A1-P/v1";
    }

    protected AsyncRestClient createClient() {
        return new AsyncRestClient(getBaseUrl());
    }

    @Override
    public Mono<Collection<String>> getPolicyTypeIdentities() {
        logger.debug("getPolicyTypeIdentities nearRtRicUrl = {}", ricConfig.baseUrl());
        AsyncRestClient client = createClient();
        return client.get("/policytypes/identities") //
            .flatMap(this::parseJsonArrayOfString);
    }

    @Override
    public Mono<Collection<String>> getPolicyIdentities() {
        logger.debug("getPolicyIdentities nearRtRicUrl = {}", ricConfig.baseUrl());
        AsyncRestClient client = createClient();
        return client.get("/policies/identities") //
            .flatMap(this::parseJsonArrayOfString);
    }

    @Override
    public Mono<String> getPolicyTypeSchema(String policyTypeId) {
        logger.debug("getPolicyType nearRtRicUrl = {}, policyTypeId = {}", ricConfig.baseUrl(), policyTypeId);
        AsyncRestClient client = createClient();
        Mono<String> response = client.get("/policytypes/" + policyTypeId);
        return response.flatMap(this::createMono);
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        logger.debug("putPolicy nearRtRicUrl = {}, policyId = {}, policyString = {}", //
            policy.ric().getConfig().baseUrl(), policy.id(), policy.json());
        AsyncRestClient client = createClient();
        // TODO update when simulator is updated to include policy type
        // Mono<String> response = client.put("/policies/" + policy.id() + "?policyTypeId=" + policy.type().name(),
        // policy.json());
        Mono<String> response = client.put("/policies/" + policy.id(), policy.json());

        return response.flatMap(this::createMono);
    }

    @Override
    public Mono<String> deletePolicy(String policyId) {
        logger.debug("deletePolicy nearRtRicUrl = {}, policyId = {}", ricConfig.baseUrl(), policyId);
        AsyncRestClient client = createClient();
        return client.delete("/policies/" + policyId);
    }

    private Mono<Collection<String>> parseJsonArrayOfString(String inputString) {
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

    @Override
    public Mono<A1ProtocolType> getProtocolVersion() {
        return getPolicyTypeIdentities() //
            .flatMap(x -> Mono.just(A1ProtocolType.STD_V1));
    }
}
