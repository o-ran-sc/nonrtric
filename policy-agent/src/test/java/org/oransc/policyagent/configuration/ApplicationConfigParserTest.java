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

import static org.junit.jupiter.api.Assertions.assertAll;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.onap.dmaap.mr.test.clients.ProtocolTypeConstants;
import org.oransc.policyagent.exceptions.ServiceException;
import org.springframework.http.MediaType;

class ApplicationConfigParserTest {

    ApplicationConfigParser parserUnderTest = new ApplicationConfigParser();

    @Test
    void whenCorrectConfig() throws Exception {
        JsonObject jsonRootObject = getJsonRootObject();

        ApplicationConfigParser.ConfigParserResult result = parserUnderTest.parse(jsonRootObject);

        Properties actualPublisherConfig = result.dmaapPublisherConfig();
        assertAll("publisherConfig",
            () -> assertEquals("localhost:6845/events", actualPublisherConfig.get("ServiceName"), "Wrong ServiceName"),
            () -> assertEquals("A1-POLICY-AGENT-WRITE", actualPublisherConfig.get("topic"), "Wrong topic"),
            () -> assertEquals("localhost:6845", actualPublisherConfig.get("host"), "Wrong host"),
            () -> assertEquals(MediaType.APPLICATION_JSON.toString(), actualPublisherConfig.get("contenttype"),
                "Wrong contenttype"),
            () -> assertEquals("admin", actualPublisherConfig.get("userName"), "Wrong userName"),
            () -> assertEquals("admin", actualPublisherConfig.get("password"), "Wrong password"),
            () -> assertEquals(ProtocolTypeConstants.HTTPNOAUTH.toString(), actualPublisherConfig.get("TransportType"),
                "Wrong TransportType"),
            () -> assertEquals("15000", actualPublisherConfig.get("timeout"), "Wrong timeout"),
            () -> assertEquals("100", actualPublisherConfig.get("limit"), "Wrong limit"));

        Properties actualConsumerConfig = result.dmaapConsumerConfig();
        assertAll("consumerConfig",
            () -> assertEquals("localhost:6845/events", actualConsumerConfig.get("ServiceName"), "Wrong ServiceName"),
            () -> assertEquals("A1-POLICY-AGENT-READ", actualConsumerConfig.get("topic"), "Wrong topic"),
            () -> assertEquals("localhost:6845", actualConsumerConfig.get("host"), "Wrong host"),
            () -> assertEquals(MediaType.APPLICATION_JSON.toString(), actualConsumerConfig.get("contenttype"),
                "Wrong contenttype"),
            () -> assertEquals("admin", actualConsumerConfig.get("userName"), "Wrong userName"),
            () -> assertEquals("admin", actualConsumerConfig.get("password"), "Wrong password"),
            () -> assertEquals("users", actualConsumerConfig.get("group"), "Wrong group"),
            () -> assertEquals("policy-agent", actualConsumerConfig.get("id"), "Wrong id"),
            () -> assertEquals(ProtocolTypeConstants.HTTPNOAUTH.toString(), actualConsumerConfig.get("TransportType"),
                "Wrong TransportType"),
            () -> assertEquals("15000", actualConsumerConfig.get("timeout"), "Wrong timeout"),
            () -> assertEquals("100", actualConsumerConfig.get("limit"), "Wrong limit"));

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
    void whenMalformedUrlStreamsSubscribing() throws Exception {
        JsonObject jsonRootObject = getJsonRootObject();
        final String wrongTopicUrl = "WrongTopicUrl";
        JsonObject json = getDmaapInfo(jsonRootObject, "streams_subscribes", "dmaap_subscriber");
        json.addProperty("topic_url", wrongTopicUrl);
        final String expectedMessage = "Could not parse the URL";

        Exception actualException = assertThrows(ServiceException.class, () -> parserUnderTest.parse(jsonRootObject));

        assertEquals(expectedMessage, actualException.getMessage().replace("\"", ""),
            "Wrong error message when the streams subscribes' URL is malformed");
        assertEquals(MalformedURLException.class, actualException.getCause().getClass(),
            "The exception is not a MalformedURLException");
    }

    @Test
    void whenMalformedUrlStreamsPublishing() throws Exception {
        JsonObject jsonRootObject = getJsonRootObject();
        final String wrongTopicUrl = "WrongTopicUrl";
        JsonObject json = getDmaapInfo(jsonRootObject, "streams_publishes", "dmaap_publisher");
        json.addProperty("topic_url", wrongTopicUrl);
        final String expectedMessage = "Could not parse the URL";

        Exception actualException = assertThrows(ServiceException.class, () -> parserUnderTest.parse(jsonRootObject));

        assertEquals(expectedMessage, actualException.getMessage().replace("\"", ""),
            "Wrong error message when the streams publishes' URL is malformed");
        assertEquals(MalformedURLException.class, actualException.getCause().getClass(),
            "The exception is not a MalformedURLException");
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

    @Test
    void whenWrongUrlPathStreamsSubscribing() throws Exception {
        JsonObject jsonRootObject = getJsonRootObject();
        final String wrongTopicUrlString =
            "http://admin:admin@localhost:6845/events/A1-POLICY-AGENT-READ/users/policy-agent/wrong-topic-url";
        final URL wrongTopicUrl = new URL(wrongTopicUrlString);
        JsonObject json = getDmaapInfo(jsonRootObject, "streams_subscribes", "dmaap_subscriber");
        json.addProperty("topic_url", wrongTopicUrlString);
        final String expectedMessage = "The path has incorrect syntax: " + wrongTopicUrl.getPath();

        Exception actualException = assertThrows(ServiceException.class, () -> parserUnderTest.parse(jsonRootObject));

        assertEquals(expectedMessage, actualException.getMessage(),
            "Wrong error message when the streams subscribes' URL has incorrect syntax");
    }

    @Test
    void whenWrongUrlPathStreamsPublishing() throws Exception {
        JsonObject jsonRootObject = getJsonRootObject();
        final String wrongTopicUrlString =
            "http://admin:admin@localhost:6845/events/A1-POLICY-AGENT-WRITE/wrong-topic-url";
        final URL wrongTopicUrl = new URL(wrongTopicUrlString);
        JsonObject json = getDmaapInfo(jsonRootObject, "streams_publishes", "dmaap_publisher");
        json.addProperty("topic_url", wrongTopicUrlString);
        final String expectedMessage = "The path has incorrect syntax: " + wrongTopicUrl.getPath();

        Exception actualException = assertThrows(ServiceException.class, () -> parserUnderTest.parse(jsonRootObject));

        assertEquals(expectedMessage, actualException.getMessage(),
            "Wrong error message when the streams publishes' URL has incorrect syntax");
    }

    JsonObject getDmaapInfo(JsonObject jsonRootObject, String streamsPublishesOrSubscribes,
        String dmaapPublisherOrSubscriber) throws Exception {
        return jsonRootObject.getAsJsonObject("config").getAsJsonObject(streamsPublishesOrSubscribes)
            .getAsJsonObject(dmaapPublisherOrSubscriber).getAsJsonObject("dmaap_info");
    }
}
