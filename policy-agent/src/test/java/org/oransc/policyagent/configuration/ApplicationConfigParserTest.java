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

package org.oransc.policyagent.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.oransc.policyagent.exceptions.ServiceException;

class ApplicationConfigParserTest {

    ApplicationConfigParser parserUnderTest = new ApplicationConfigParser();

    @Test
    void whenCorrectConfig() throws Exception {
        JsonObject jsonRootObject = getJsonRootObject();

        ApplicationConfigParser.ConfigParserResult result = parserUnderTest.parse(jsonRootObject);

        String topicUrl = result.dmaapProducerTopicUrl();
        assertEquals("http://admin:admin@localhost:6845/events/A1-POLICY-AGENT-WRITE", topicUrl, "controller contents");

        topicUrl = result.dmaapConsumerTopicUrl();
        assertEquals("http://admin:admin@localhost:6845/events/A1-POLICY-AGENT-READ/users/policy-agent", topicUrl,
            "controller contents");

        Map<String, ControllerConfig> controllers = result.controllerConfigs();
        assertEquals(1, controllers.size(), "size");
        ImmutableControllerConfig expectedControllerConfig = ImmutableControllerConfig.builder() //
            .baseUrl("http://localhost:8083/") //
            .name("controller1") //
            .userName("user") //
            .password("password") //
            .build(); //
        assertEquals(expectedControllerConfig, controllers.get("controller1"), "controller contents");
    }

    private JsonObject getJsonRootObject() throws JsonIOException, JsonSyntaxException, IOException {
        JsonObject rootObject = JsonParser.parseReader(new InputStreamReader(getCorrectJson())).getAsJsonObject();
        return rootObject;
    }

    private static InputStream getCorrectJson() throws IOException {
        URL url = ApplicationConfigParser.class.getClassLoader()
            .getResource("test_application_configuration_with_dmaap_config.json");
        String string = Resources.toString(url, Charsets.UTF_8);
        return new ByteArrayInputStream((string.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void whenDmaapConfigHasSeveralStreamsPublishing() throws Exception {
        JsonObject jsonRootObject = getJsonRootObject();
        JsonObject json = jsonRootObject.getAsJsonObject("config").getAsJsonObject("streams_publishes");
        JsonObject fake_info_object = new JsonObject();
        fake_info_object.addProperty("fake_info", "fake");
        json.add("fake_info_object", new Gson().toJsonTree(fake_info_object));
        DataPublishing data = new Gson().fromJson(json.toString(), DataPublishing.class);
        final String expectedMessage =
            "Invalid configuration. Number of streams must be one, config: " + data.toString();

        Exception actualException = assertThrows(ServiceException.class, () -> parserUnderTest.parse(jsonRootObject));

        assertEquals(expectedMessage, actualException.getMessage(),
            "Wrong error message when the DMaaP config has several streams publishing");
    }

    class DataPublishing {
        private JsonObject dmaap_publisher;
        private JsonObject fake_info_object;

        @Override
        public String toString() {
            return String.format("[dmaap_publisher=%s, fake_info_object=%s]", dmaap_publisher.toString(),
                fake_info_object.toString());
        }
    }

    @Test
    void whenDmaapConfigHasSeveralStreamsSubscribing() throws Exception {
        JsonObject jsonRootObject = getJsonRootObject();
        JsonObject json = jsonRootObject.getAsJsonObject("config").getAsJsonObject("streams_subscribes");
        JsonObject fake_info_object = new JsonObject();
        fake_info_object.addProperty("fake_info", "fake");
        json.add("fake_info_object", new Gson().toJsonTree(fake_info_object));
        DataSubscribing data = new Gson().fromJson(json.toString(), DataSubscribing.class);
        final String expectedMessage =
            "Invalid configuration. Number of streams must be one, config: " + data.toString();

        Exception actualException = assertThrows(ServiceException.class, () -> parserUnderTest.parse(jsonRootObject));

        assertEquals(expectedMessage, actualException.getMessage(),
            "Wrong error message when the DMaaP config has several streams subscribing");
    }

    private class DataSubscribing {
        private JsonObject dmaap_subscriber;
        private JsonObject fake_info_object;

        @Override
        public String toString() {
            return String.format("[dmaap_subscriber=%s, fake_info_object=%s]", dmaap_subscriber.toString(),
                fake_info_object.toString());
        }
    }

    @Test
    void whenWrongMemberNameInObject() throws Exception {
        JsonObject jsonRootObject = getJsonRootObject();
        JsonObject json = jsonRootObject.getAsJsonObject("config");
        json.remove("ric");
        final String message = "Could not find member: 'ric' in: " + json;

        Exception actualException = assertThrows(ServiceException.class, () -> parserUnderTest.parse(jsonRootObject));

        assertEquals(message, actualException.getMessage(), "Wrong error message when wrong member name in object");
    }

    JsonObject getDmaapInfo(JsonObject jsonRootObject, String streamsPublishesOrSubscribes,
        String dmaapPublisherOrSubscriber) throws Exception {
        return jsonRootObject.getAsJsonObject("config").getAsJsonObject(streamsPublishesOrSubscribes)
            .getAsJsonObject(dmaapPublisherOrSubscriber).getAsJsonObject("dmaap_info");
    }
}
