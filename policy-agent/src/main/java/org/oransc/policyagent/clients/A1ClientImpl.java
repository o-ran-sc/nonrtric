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
import org.oransc.policyagent.repository.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class A1ClientImpl implements A1Client {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static String getBaseUrl(final String nearRtRicUrl) {
        return nearRtRicUrl + "/A1-P/v1";
    }

    protected AsyncRestClient createClient(final String nearRtRicUrl) {
        return new AsyncRestClient(getBaseUrl(nearRtRicUrl));
    }

    @Override
    public Mono<Collection<String>> getPolicyTypeIdentities(String nearRtRicUrl) {
        logger.debug("getPolicyTypeIdentities nearRtRicUrl = {}", nearRtRicUrl);
        AsyncRestClient client = createClient(nearRtRicUrl);
        return client.get("/policytypes/identities") //
            .flatMap(this::parseJsonArrayOfString);
    }

    @Override
    public Mono<Collection<String>> getPolicyIdentities(String nearRtRicUrl) {
        logger.debug("getPolicyIdentities nearRtRicUrl = {}", nearRtRicUrl);
        AsyncRestClient client = createClient(nearRtRicUrl);
        return client.get("/policies/identities") //
            .flatMap(this::parseJsonArrayOfString);
    }

    @Override
    public Mono<String> getPolicyType(String nearRtRicUrl, String policyTypeId) {
        logger.debug("getPolicyType nearRtRicUrl = {}, policyTypeId = {}", nearRtRicUrl, policyTypeId);
        AsyncRestClient client = createClient(nearRtRicUrl);
        Mono<String> response = client.get("/policytypes/" + policyTypeId);
        return response.flatMap(this::createMono);
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        logger.debug("putPolicy nearRtRicUrl = {}, policyId = {}, policyString = {}", //
            policy.ric().getConfig().baseUrl(), policy.id(), policy.json());
        AsyncRestClient client = createClient(policy.ric().getConfig().baseUrl());
        // TODO update when simulator is updated to include policy type
        // Mono<String> response = client.put("/policies/" + policy.id() + "?policyTypeId=" + policy.type().name(),
        // policy.json());
        Mono<String> response = client.put("/policies/" + policy.id(), policy.json());

        return response.flatMap(this::createMono);
    }

    @Override
    public Mono<String> deletePolicy(String nearRtRicUrl, String policyId) {
        logger.debug("deletePolicy nearRtRicUrl = {}, policyId = {}", nearRtRicUrl, policyId);
        AsyncRestClient client = createClient(nearRtRicUrl);
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
}
