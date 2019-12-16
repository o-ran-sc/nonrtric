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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class A1Client {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public String getBaseUrl(final String nearRtRicUrl) {
        return nearRtRicUrl + "/A1-P/v1";
    }

    public Flux<String> getAllPolicyTypes(String nearRtRicUrl) {
        logger.debug("getAllPolicyTypes nearRtRicUrl = {}", nearRtRicUrl);
        AsyncRestClient client = new AsyncRestClient(getBaseUrl(nearRtRicUrl));
        Mono<String> response = client.get("/policytypes");
        return response.flatMapMany(this::createPolicyTypesFlux);
    }

    public Flux<String> getPoliciesForType(String nearRtRicUrl, String policyTypeId) {
        logger.debug("getPoliciesForType nearRtRicUrl = {}, policyTypeId = {}", nearRtRicUrl, policyTypeId);
        AsyncRestClient client = new AsyncRestClient(getBaseUrl(nearRtRicUrl));
        Mono<String> response = client.get("/policies");
        return response.flatMapMany(policiesString -> createPoliciesFlux(policiesString, policyTypeId));
    }

    public Mono<String> getPolicy(String nearRtRicUrl, String policyId) {
        logger.debug("getPolicy nearRtRicUrl = {}, policyId = {}", nearRtRicUrl, policyId);
        AsyncRestClient client = new AsyncRestClient(getBaseUrl(nearRtRicUrl));
        Mono<String> response = client.get("/policies/" + policyId);
        return response.flatMap(this::createPolicyMono);
    }

    public Mono<String> putPolicy(String nearRtRicUrl, String policyId, String policyString) {
        logger.debug("putPolicy nearRtRicUrl = {}, policyId = {}, policyString = {}", nearRtRicUrl, policyId,
            policyString);
        try {
            new JSONObject(policyString);
        } catch (JSONException ex) { // invalid json
            return Mono.error(ex);
        }
        AsyncRestClient client = new AsyncRestClient(getBaseUrl(nearRtRicUrl));
        Mono<String> response = client.put("/policies/" + policyId, policyString);
        return response.flatMap(this::createPolicyMono);
    }

    public Mono<Void> deletePolicy(String nearRtRicUrl, String policyId) {
        logger.debug("deletePolicy nearRtRicUrl = {}, policyId = {}", nearRtRicUrl, policyId);
        AsyncRestClient client = new AsyncRestClient(getBaseUrl(nearRtRicUrl));
        return client.delete("/policies/" + policyId);
    }

    private Flux<String> createPolicyTypesFlux(String policyTypesString) {
        try {
            List<String> policyTypesList = new ArrayList<>();
            JSONArray policyTypesArray = new JSONArray(policyTypesString);
            for (int i = 0; i < policyTypesArray.length(); i++) {
                policyTypesList.add(policyTypesArray.getJSONObject(i).toString());
            }
            logger.debug("A1 client: policyTypes = {}", policyTypesList);
            return Flux.fromIterable(policyTypesList);
        } catch (JSONException ex) { // invalid json
            return Flux.error(ex);
        }
    }

    private Flux<String> createPoliciesFlux(String policiesString, String policyTypeId) {
        try {
            List<String> policiesList = new ArrayList<>();
            JSONArray policiesArray = new JSONArray(policiesString);
            for (int i = 0; i < policiesArray.length(); i++) {
                JSONObject policyObject = policiesArray.getJSONObject(i);
                if (policyObject.get("policyTypeId").equals(policyTypeId)) {
                    policiesList.add(policyObject.toString());
                }
            }
            logger.debug("A1 client: policies = {}", policiesList);
            return Flux.fromIterable(policiesList);
        } catch (JSONException ex) { // invalid json
            return Flux.error(ex);
        }
    }

    private Mono<String> createPolicyMono(String policyString) {
        try {
            JSONObject policyObject = new JSONObject(policyString);
            String policy = policyObject.toString();
            logger.debug("A1 client: policy = {}", policy);
            return Mono.just(policy);
        } catch (JSONException ex) { // invalid json
            return Mono.error(ex);
        }
    }
}
