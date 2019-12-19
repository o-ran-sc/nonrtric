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

public class A1ClientImpl implements A1Client {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static String getBaseUrl(final String nearRtRicUrl) {
        return nearRtRicUrl + "/A1-P/v1";
    }

    @Override
    public Flux<String> getPolicyTypeIdentities(String nearRtRicUrl) {
        logger.debug("getPolicyTypeIdentities nearRtRicUrl = {}", nearRtRicUrl);
        AsyncRestClient client = new AsyncRestClient(getBaseUrl(nearRtRicUrl));
        Mono<String> response = client.get("/policytypes/identities");
        return response.flatMapMany(this::createFlux);
    }

    @Override
    public Flux<String> getPolicyIdentities(String nearRtRicUrl) {
        logger.debug("getPolicyIdentities nearRtRicUrl = {}", nearRtRicUrl);
        AsyncRestClient client = new AsyncRestClient(getBaseUrl(nearRtRicUrl));
        Mono<String> response = client.get("/policies/identities");
        return response.flatMapMany(this::createFlux);
    }

    @Override
    public Mono<String> getPolicyType(String nearRtRicUrl, String policyTypeId) {
        logger.debug("getPolicyType nearRtRicUrl = {}, policyTypeId = {}", nearRtRicUrl, policyTypeId);
        AsyncRestClient client = new AsyncRestClient(getBaseUrl(nearRtRicUrl));
        Mono<String> response = client.get("/policytypes/" + policyTypeId);
        return response.flatMap(this::createMono);
    }

    @Override
    public Mono<String> putPolicy(String nearRtRicUrl, String policyId, String policyString) {
        logger.debug("putPolicy nearRtRicUrl = {}, policyId = {}, policyString = {}", nearRtRicUrl, policyId,
            policyString);
        AsyncRestClient client = new AsyncRestClient(getBaseUrl(nearRtRicUrl));
        Mono<String> response = client.put("/policies/" + policyId, policyString);
        return response.flatMap(this::createMono);
    }

    @Override
    public Mono<Void> deletePolicy(String nearRtRicUrl, String policyId) {
        logger.debug("deletePolicy nearRtRicUrl = {}, policyId = {}", nearRtRicUrl, policyId);
        AsyncRestClient client = new AsyncRestClient(getBaseUrl(nearRtRicUrl));
        return client.delete("/policies/" + policyId);
    }

    private Flux<String> createFlux(String inputString) {
        try {
            List<String> arrayList = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(inputString);
            for (int i = 0; i < jsonArray.length(); i++) {
                arrayList.add(jsonArray.getString(i));
            }
            logger.debug("A1 client: received list = {}", arrayList);
            return Flux.fromIterable(arrayList);
        } catch (JSONException ex) { // invalid json
            return Flux.error(ex);
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
