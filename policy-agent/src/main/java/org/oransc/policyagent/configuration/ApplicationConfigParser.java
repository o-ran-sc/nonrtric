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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import org.onap.dmaap.mr.test.clients.ProtocolTypeConstants;
import org.oransc.policyagent.exceptions.ServiceException;
import org.springframework.http.MediaType;

public class ApplicationConfigParser {

    private static final String CONFIG = "config";

    @Getter
    private List<RicConfig> ricConfigs;
    @Getter
    private Properties dmaapPublisherConfig = new Properties();
    @Getter
    private Properties dmaapConsumerConfig = new Properties();

    public void parse(JsonObject root) throws ServiceException {
        JsonObject agentConfigJson = root.getAsJsonObject(CONFIG);
        ricConfigs = parseRics(agentConfigJson);

        JsonObject json = agentConfigJson.getAsJsonObject("streams_publishes");
        if (json != null) {
            this.dmaapPublisherConfig = parseDmaapConfig(json);
        }

        json = agentConfigJson.getAsJsonObject("streams_subscribes");
        if (json != null) {
            this.dmaapConsumerConfig = parseDmaapConfig(json);
        }

    }

    private List<RicConfig> parseRics(JsonObject config) throws ServiceException {
        List<RicConfig> result = new ArrayList<>();
        for (JsonElement ricElem : getAsJsonArray(config, "ric")) {
            JsonObject ricAsJson = ricElem.getAsJsonObject();
            ImmutableRicConfig ricConfig = ImmutableRicConfig.builder() //
                .name(ricAsJson.get("name").getAsString()) //
                .baseUrl(ricAsJson.get("baseUrl").getAsString()) //
                .managedElementIds(parseManagedElementIds(ricAsJson.get("managedElementIds").getAsJsonArray())) //
                .build();
            result.add(ricConfig);
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
            throw new ServiceException("Could not find member: " + memberName + " in: " + obj);
        }
        return elem;
    }

    private JsonArray getAsJsonArray(JsonObject obj, String memberName) throws ServiceException {
        return get(obj, memberName).getAsJsonArray();
    }

    private Properties parseDmaapConfig(JsonObject streamCfg) throws ServiceException {
        Set<Entry<String, JsonElement>> streamConfigEntries = streamCfg.entrySet();
        if (streamConfigEntries.size() != 1) {
            throw new ServiceException(
                "Invalid configuration. Number of streams must be one, config: " + streamConfigEntries);
        }
        JsonObject streamConfigEntry = streamConfigEntries.iterator().next().getValue().getAsJsonObject();
        JsonObject dmaapInfo = get(streamConfigEntry, "dmaap_info").getAsJsonObject();
        String topicUrl = getAsString(dmaapInfo, "topic_url");

        try {
            Properties dmaapProps = new Properties();
            URL url = new URL(topicUrl);
            String passwd = "";
            String userName = "";
            if (url.getUserInfo() != null) {
                String[] userInfo = url.getUserInfo().split(":");
                userName = userInfo[0];
                passwd = userInfo[1];
            }
            String urlPath = url.getPath();
            DmaapUrlPath path = parseDmaapUrlPath(urlPath);

            dmaapProps.put("ServiceName", url.getHost() + ":" + url.getPort() + "/events");
            dmaapProps.put("topic", path.dmaapTopicName);
            dmaapProps.put("host", url.getHost() + ":" + url.getPort());
            dmaapProps.put("contenttype", MediaType.APPLICATION_JSON.toString());
            dmaapProps.put("userName", userName);
            dmaapProps.put("password", passwd);
            dmaapProps.put("group", path.consumerGroup);
            dmaapProps.put("id", path.consumerId);
            dmaapProps.put("TransportType", ProtocolTypeConstants.HTTPNOAUTH.toString());
            dmaapProps.put("timeout", 15000);
            dmaapProps.put("limit", 100);
            dmaapProps.put("maxBatchSize", "10");
            dmaapProps.put("maxAgeMs", "10000");
            dmaapProps.put("compress", true);
            dmaapProps.put("MessageSentThreadOccurance", "2");
            return dmaapProps;
        } catch (MalformedURLException e) {
            throw new ServiceException("Could not parse the URL", e);
        }

    }

    private static @NotNull String getAsString(JsonObject obj, String memberName) throws ServiceException {
        return get(obj, memberName).getAsString();
    }

    private class DmaapUrlPath {
        final String dmaapTopicName;
        final String consumerGroup;
        final String consumerId;

        DmaapUrlPath(String dmaapTopicName, String consumerGroup, String consumerId) {
            this.dmaapTopicName = dmaapTopicName;
            this.consumerGroup = consumerGroup;
            this.consumerId = consumerId;
        }
    }

    private DmaapUrlPath parseDmaapUrlPath(String urlPath) throws ServiceException {
        String[] tokens = urlPath.split("/"); // /events/A1-P/users/sdnc1
        if (!(tokens.length == 3 ^ tokens.length == 5)) {
            throw new ServiceException("The path has incorrect syntax: " + urlPath);
        }

        final String dmaapTopicName = tokens[2]; // /events/A1-P
        String consumerGroup = ""; // users
        String consumerId = ""; // sdnc1
        if (tokens.length == 5) {
            consumerGroup = tokens[3];
            consumerId = tokens[4];
        }
        return new DmaapUrlPath(dmaapTopicName, consumerGroup, consumerId);
    }
}
