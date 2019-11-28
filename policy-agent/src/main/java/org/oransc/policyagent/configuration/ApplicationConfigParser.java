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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.oransc.policyagent.exceptions.ServiceException;

public class ApplicationConfigParser {

    private static final String CONFIG = "config";

    public ApplicationConfigParser() {
    }

    String example;

    public void parse(JsonObject root) throws ServiceException {
        JsonObject config = root.getAsJsonObject(CONFIG);
        example = getAsString(config, "exampleProperty");
    }

    private static JsonElement get(JsonObject obj, String memberName) throws ServiceException {
        JsonElement elem = obj.get(memberName);
        if (elem == null) {
            throw new ServiceException("Could not find member: " + memberName + " in: " + obj);
        }
        return elem;
    }

    private static String getAsString(JsonObject obj, String memberName) throws ServiceException {
        return get(obj, memberName).getAsString();
    }

    private static JsonObject getAsJson(JsonObject obj, String memberName) throws ServiceException {
        return get(obj, memberName).getAsJsonObject();
    }

}
