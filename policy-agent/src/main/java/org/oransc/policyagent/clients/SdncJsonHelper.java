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

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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

/**
 * Common json functionality used by the SDNC clients
 */
@SuppressWarnings("java:S1192") // Same text in several traces
class SdncJsonHelper {
    private static Gson gson = new GsonBuilder() //
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES) //
        .create();
    private static final String OUTPUT = "output";
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private SdncJsonHelper() {
    }

    public static Flux<String> parseJsonArrayOfString(String inputString) {
        try {
            List<String> arrayList = new ArrayList<>();
            if (!inputString.isEmpty()) {
                JSONArray jsonArray = new JSONArray(inputString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    Object value = jsonArray.get(i);
                    arrayList.add(value.toString());
                }
            }
            return Flux.fromIterable(arrayList);
        } catch (JSONException ex) { // invalid json
            logger.debug("Invalid json {}", ex.getMessage());
            return Flux.error(ex);
        }
    }

    public static <T> String createInputJsonString(T params) {
        JsonElement paramsJson = gson.toJsonTree(params);
        JsonObject jsonObj = new JsonObject();
        jsonObj.add("input", paramsJson);
        return gson.toJson(jsonObj);
    }

    public static <T> String createOutputJsonString(T params) {
        JsonElement paramsJson = gson.toJsonTree(params);
        JsonObject jsonObj = new JsonObject();
        jsonObj.add(OUTPUT, paramsJson);
        return gson.toJson(jsonObj);
    }

    public static Mono<JSONObject> getOutput(String response) {
        try {
            JSONObject outputJson = new JSONObject(response);
            JSONObject responseParams = outputJson.getJSONObject(OUTPUT);
            return Mono.just(responseParams);
        } catch (JSONException ex) { // invalid json
            logger.debug("Invalid json {}", ex.getMessage());
            return Mono.error(ex);
        }
    }

    public static Mono<String> getValueFromResponse(String response, String key) {
        return getOutput(response) //
            .flatMap(responseParams -> {
                if (!responseParams.has(key)) {
                    return Mono.just("");
                }
                String value = responseParams.get(key).toString();
                return Mono.just(value);
            });
    }

    public static Mono<String> extractPolicySchema(String inputString) {
        try {
            JSONObject jsonObject = new JSONObject(inputString);
            JSONObject schemaObject = jsonObject.getJSONObject("policySchema");
            String schemaString = schemaObject.toString();
            return Mono.just(schemaString);
        } catch (JSONException ex) { // invalid json
            logger.debug("Invalid json {}", ex.getMessage());
            return Mono.error(ex);
        }
    }
}
