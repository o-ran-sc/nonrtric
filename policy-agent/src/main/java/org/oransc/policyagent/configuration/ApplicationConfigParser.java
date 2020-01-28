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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import org.onap.dmaap.mr.test.clients.ProtocolTypeConstants;
import org.oransc.policyagent.exceptions.ServiceException;
import org.springframework.http.MediaType;

public class ApplicationConfigParser {

    private static final String CONFIG = "config";

    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    @Getter
    private Vector<RicConfig> ricConfigs;
    @Getter
    private Properties dmaapPublisherConfig;
    @Getter
    private Properties dmaapConsumerConfig;

    public void parse(JsonObject root) throws ServiceException {
        JsonObject agentConfigJson = root.getAsJsonObject(CONFIG);
        ricConfigs = parseRics(agentConfigJson);
        JsonObject dmaapPublisherConfigJson = agentConfigJson.getAsJsonObject("streams_publishes");
        if (dmaapPublisherConfigJson == null) {
            dmaapPublisherConfig = new Properties();
        } else {
            dmaapPublisherConfig = parseDmaapConfig(dmaapPublisherConfigJson);
        }
        JsonObject dmaapConsumerConfigJson = agentConfigJson.getAsJsonObject("streams_subscribes");
        if (dmaapConsumerConfigJson == null) {
            dmaapConsumerConfig = new Properties();
        } else {
            dmaapConsumerConfig = parseDmaapConfig(dmaapConsumerConfigJson);
        }
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

    private Properties parseDmaapConfig(JsonObject consumerCfg) throws ServiceException {
        Set<Entry<String, JsonElement>> topics = consumerCfg.entrySet();
        if (topics.size() != 1) {
            throw new ServiceException("Invalid configuration, number of topic must be one, config: " + topics);
        }
        JsonObject topic = topics.iterator().next().getValue().getAsJsonObject();
        JsonObject dmaapInfo = get(topic, "dmaap_info").getAsJsonObject();
        String topicUrl = getAsString(dmaapInfo, "topic_url");

        Properties dmaapProps = new Properties();
        try {
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
            dmaapProps.put("limit", 1000);
        } catch (MalformedURLException e) {
            throw new ServiceException("Could not parse the URL", e);
        }

        return dmaapProps;
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
