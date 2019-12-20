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

package org.oransc.policyagent.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Vector;

import org.oransc.policyagent.exceptions.ServiceException;

class ApplicationConfigParser {

    private static final String CONFIG = "config";
    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    private Vector<RicConfig> ricConfig;

    public ApplicationConfigParser() {
    }

    public void parse(JsonObject root) throws ServiceException {
        JsonObject config = root.getAsJsonObject(CONFIG);
        ricConfig = parseRics(config);
    }

    public Vector<RicConfig> getRicConfigs() {
        return this.ricConfig;
    }

    private Vector<RicConfig> parseRics(JsonObject config) throws ServiceException {
        Vector<RicConfig> result = new Vector<RicConfig>();
        for (JsonElement ricElem : getAsJsonArray(config, "ric")) {
            result.add(gson.fromJson(ricElem.getAsJsonObject(), ImmutableRicConfig.class));
        }
        return result;
    }

    private static JsonElement get(JsonObject obj, String memberName) throws ServiceException {
        JsonElement elem = obj.get(memberName);
        if (elem == null) {
            throw new ServiceException("Could not find member: " + memberName + " in: " + obj);
        }
        return elem;
    }

    private JsonArray getAsJsonArray(JsonObject obj, String memberName) throws ServiceException {
        return get(obj, memberName).getAsJsonArray();
    }

}
