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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.immutables.gson.Gson;
import org.immutables.value.Value;
import org.oransc.policyagent.exceptions.ServiceException;

/**
 * Parser for the Json representing of the component configuration.
 */
public class ApplicationConfigParser {

    private static final String CONFIG = "config";
    private static final String CONTROLLER = "controller";

    @Value.Immutable
    @Gson.TypeAdapters
    public interface ConfigParserResult {
        List<RicConfig> ricConfigs();

        Map<String, ControllerConfig> controllerConfigs();

        String dmaapConsumerTopicUrl();

        String dmaapProducerTopicUrl();

    }

    public ConfigParserResult parse(JsonObject root) throws ServiceException {

        String dmaapProducerTopicUrl = "";
        String dmaapConsumerTopicUrl = "";

        JsonObject agentConfigJson = root.getAsJsonObject(CONFIG);

        if (agentConfigJson == null) {
            throw new ServiceException("Missing root configuration \"" + CONFIG + "\" in JSON: " + root);
        }

        JsonObject json = agentConfigJson.getAsJsonObject("streams_publishes");
        if (json != null) {
            dmaapProducerTopicUrl = parseDmaapConfig(json);
        }

        json = agentConfigJson.getAsJsonObject("streams_subscribes");
        if (json != null) {
            dmaapConsumerTopicUrl = parseDmaapConfig(json);
        }

        List<RicConfig> ricConfigs = parseRics(agentConfigJson);
        Map<String, ControllerConfig> controllerConfigs = parseControllerConfigs(agentConfigJson);
        checkConfigurationConsistency(ricConfigs, controllerConfigs);

        return ImmutableConfigParserResult.builder() //
            .dmaapConsumerTopicUrl(dmaapConsumerTopicUrl) //
            .dmaapProducerTopicUrl(dmaapProducerTopicUrl) //
            .ricConfigs(ricConfigs) //
            .controllerConfigs(controllerConfigs) //
            .build();
    }

    private void checkConfigurationConsistency(List<RicConfig> ricConfigs,
        Map<String, ControllerConfig> controllerConfigs) throws ServiceException {
        Set<String> ricUrls = new HashSet<>();
        Set<String> ricNames = new HashSet<>();
        for (RicConfig ric : ricConfigs) {
            if (!ricUrls.add(ric.baseUrl())) {
                throw new ServiceException("Configuration error, more than one RIC URL: " + ric.baseUrl());
            }
            if (!ricNames.add(ric.name())) {
                throw new ServiceException("Configuration error, more than one RIC with name: " + ric.name());
            }
            if (!ric.controllerName().isEmpty() && controllerConfigs.get(ric.controllerName()) == null) {
                throw new ServiceException(
                    "Configuration error, controller configuration not found: " + ric.controllerName());
            }

        }
    }

    private List<RicConfig> parseRics(JsonObject config) throws ServiceException {
        List<RicConfig> result = new ArrayList<>();
        for (JsonElement ricElem : getAsJsonArray(config, "ric")) {
            JsonObject ricAsJson = ricElem.getAsJsonObject();
            JsonElement controllerNameElement = ricAsJson.get(CONTROLLER);
            ImmutableRicConfig ricConfig = ImmutableRicConfig.builder() //
                .name(get(ricAsJson, "name").getAsString()) //
                .baseUrl(get(ricAsJson, "baseUrl").getAsString()) //
                .managedElementIds(parseManagedElementIds(get(ricAsJson, "managedElementIds").getAsJsonArray())) //
                .controllerName(controllerNameElement != null ? controllerNameElement.getAsString() : "") //
                .build();
            result.add(ricConfig);
        }
        return result;
    }

    Map<String, ControllerConfig> parseControllerConfigs(JsonObject config) throws ServiceException {
        if (config.get(CONTROLLER) == null) {
            return new HashMap<>();
        }
        Map<String, ControllerConfig> result = new HashMap<>();
        for (JsonElement element : getAsJsonArray(config, CONTROLLER)) {
            JsonObject controllerAsJson = element.getAsJsonObject();
            ImmutableControllerConfig controllerConfig = ImmutableControllerConfig.builder() //
                .name(get(controllerAsJson, "name").getAsString()) //
                .baseUrl(get(controllerAsJson, "baseUrl").getAsString()) //
                .password(get(controllerAsJson, "password").getAsString()) //
                .userName(get(controllerAsJson, "userName").getAsString()) // )
                .build();

            if (result.put(controllerConfig.name(), controllerConfig) != null) {
                throw new ServiceException(
                    "Configuration error, more than one controller with name: " + controllerConfig.name());
            }
        }
        return result;
    }

    private List<String> parseManagedElementIds(JsonArray asJsonObject) {
        Iterator<JsonElement> iterator = asJsonObject.iterator();
        List<String> managedElementIds = new ArrayList<>();
        while (iterator.hasNext()) {
            managedElementIds.add(iterator.next().getAsString());

        }
        return managedElementIds;
    }

    private static JsonElement get(JsonObject obj, String memberName) throws ServiceException {
        JsonElement elem = obj.get(memberName);
        if (elem == null) {
            throw new ServiceException("Could not find member: '" + memberName + "' in: " + obj);
        }
        return elem;
    }

    private JsonArray getAsJsonArray(JsonObject obj, String memberName) throws ServiceException {
        return get(obj, memberName).getAsJsonArray();
    }

    private String parseDmaapConfig(JsonObject streamCfg) throws ServiceException {
        Set<Entry<String, JsonElement>> streamConfigEntries = streamCfg.entrySet();
        if (streamConfigEntries.size() != 1) {
            throw new ServiceException(
                "Invalid configuration. Number of streams must be one, config: " + streamConfigEntries);
        }
        JsonObject streamConfigEntry = streamConfigEntries.iterator().next().getValue().getAsJsonObject();
        JsonObject dmaapInfo = get(streamConfigEntry, "dmaap_info").getAsJsonObject();
        return getAsString(dmaapInfo, "topic_url");
    }

    private static @NotNull String getAsString(JsonObject obj, String memberName) throws ServiceException {
        return get(obj, memberName).getAsString();
    }
}
