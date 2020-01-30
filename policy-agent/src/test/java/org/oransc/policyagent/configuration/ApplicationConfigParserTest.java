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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
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
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.onap.dmaap.mr.test.clients.ProtocolTypeConstants;
import org.springframework.http.MediaType;

public class ApplicationConfigParserTest {

    @Test
    public void whenCorrectDmaapConfig() throws Exception {
        JsonObject jsonRootObject = getJsonRootObject();

        ApplicationConfigParser parserUnderTest = new ApplicationConfigParser();

        parserUnderTest.parse(jsonRootObject);

        Properties actualPublisherConfig = parserUnderTest.getDmaapPublisherConfig();
        assertAll("publisherConfig",
            () -> assertEquals("localhost:6845/events", actualPublisherConfig.get("ServiceName")),
            () -> assertEquals("A1-POLICY-AGENT-WRITE", actualPublisherConfig.get("topic")),
            () -> assertEquals("localhost:6845", actualPublisherConfig.get("host")),
            () -> assertEquals(MediaType.APPLICATION_JSON.toString(), actualPublisherConfig.get("contenttype")),
            () -> assertEquals("admin", actualPublisherConfig.get("userName")),
            () -> assertEquals("admin", actualPublisherConfig.get("password")),
            () -> assertEquals(ProtocolTypeConstants.HTTPNOAUTH.toString(), actualPublisherConfig.get("TransportType")),
            () -> assertEquals(15000, actualPublisherConfig.get("timeout")),
            () -> assertEquals(1000, actualPublisherConfig.get("limit")));

        Properties actualConsumerConfig = parserUnderTest.getDmaapConsumerConfig();
        assertAll("consumerConfig",
            () -> assertEquals("localhost:6845/events", actualConsumerConfig.get("ServiceName")),
            () -> assertEquals("A1-POLICY-AGENT-READ", actualConsumerConfig.get("topic")),
            () -> assertEquals("localhost:6845", actualConsumerConfig.get("host")),
            () -> assertEquals(MediaType.APPLICATION_JSON.toString(), actualConsumerConfig.get("contenttype")),
            () -> assertEquals("admin", actualConsumerConfig.get("userName")),
            () -> assertEquals("admin", actualConsumerConfig.get("password")),
            () -> assertEquals("users", actualConsumerConfig.get("group")),
            () -> assertEquals("policy-agent", actualConsumerConfig.get("id")),
            () -> assertEquals(ProtocolTypeConstants.HTTPNOAUTH.toString(), actualConsumerConfig.get("TransportType")),
            () -> assertEquals(15000, actualConsumerConfig.get("timeout")),
            () -> assertEquals(1000, actualConsumerConfig.get("limit")));
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

}
