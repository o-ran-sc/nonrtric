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

import java.util.Arrays;
import java.util.Vector;

import org.json.JSONObject;
import org.oransc.policyagent.configuration.ImmutableRicConfig;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.ImmutablePolicy;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.Ric;
import reactor.core.publisher.Mono;

public class A1ClientHelper {

    private A1ClientHelper() {
    }

    protected static Mono<String> createOutputJsonResponse(String key, String value) {
        JSONObject paramsJson = new JSONObject();
        paramsJson.put(key, value);
        JSONObject responseJson = new JSONObject();
        responseJson.put("output", paramsJson);
        return Mono.just(responseJson.toString());
    }

    protected static Ric createRic(String url) {
        RicConfig cfg = ImmutableRicConfig.builder().name("ric") //
            .baseUrl(url) //
            .managedElementIds(new Vector<String>(Arrays.asList("kista_1", "kista_2"))) //
            .controllerName("") //
            .build();
        return new Ric(cfg);
    }

    protected static Policy createPolicy(String nearRtRicUrl, String policyId, String json, String type) {
        return ImmutablePolicy.builder() //
            .id(policyId) //
            .json(json) //
            .ownerServiceName("service") //
            .ric(createRic(nearRtRicUrl)) //
            .type(createPolicyType(type)) //
            .lastModified("now") //
            .isTransient(false) //
            .build();
    }

    protected static PolicyType createPolicyType(String name) {
        return ImmutablePolicyType.builder().name(name).schema("schema").build();
    }

    protected static String getCreateSchema(String policyType, String policyTypeId) {
        JSONObject obj = new JSONObject(policyType);
        JSONObject schemaObj = obj.getJSONObject("create_schema");
        schemaObj.put("title", policyTypeId);
        return schemaObj.toString();
    }
}
