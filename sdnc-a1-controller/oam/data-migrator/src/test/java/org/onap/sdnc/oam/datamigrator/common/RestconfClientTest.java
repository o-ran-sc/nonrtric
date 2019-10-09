/*
 * ============LICENSE_START=======================================================
 * ONAP : SDNC
 * ================================================================================
 * Copyright 2019 AMDOCS
 *=================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.sdnc.oam.datamigrator.common;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Rule;
import org.junit.Test;
import org.onap.sdnc.oam.datamigrator.exceptions.RestconfException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RestconfClientTest {

    @Rule
    public WireMockRule service = new WireMockRule(8081);
    private RestconfClient restconfClient = new RestconfClient("http://localhost:8081","admin","Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U");
    private ClassLoader classLoader = getClass().getClassLoader();
    private  String preloadVnfResponseJson = new String(Files.readAllBytes(Paths.get(classLoader.getResource("wiremock/preloadVnfResponse.json").toURI())));
    private String preloadInformationRequestJson = new String(Files.readAllBytes(Paths.get(classLoader.getResource("wiremock/preloadInformationRequest.json").toURI())));
    
    
    JsonObject expectedJsonObject = new JsonParser().parse(preloadVnfResponseJson).getAsJsonObject();

    public RestconfClientTest() throws IOException, URISyntaxException {
    }

    @Test
    public void getPositiveTest() {
        service.stubFor(get(urlEqualTo("/restconf/config/GENERIC-RESOURCE-API:preload-vnfs"))
                .willReturn(aResponse().withStatus(200).withBody(preloadVnfResponseJson)));
        JsonObject actualResponse=null;
        try {
            actualResponse =  restconfClient.get("GENERIC-RESOURCE-API:preload-vnfs");
        } catch (RestconfException e) {
            e.printStackTrace();
        }
        assertEquals(expectedJsonObject,actualResponse);
    }

    @Test
    public void getNegativeTest() {
        service.stubFor(get(urlEqualTo("/restconf/config/GENERIC-RESOURCE-API:preload-vnfs"))
                .willReturn(aResponse().withStatus(404)));
        JsonObject actualResponse=null;
        try {
            actualResponse = restconfClient.get("GENERIC-RESOURCE-API:preload-vnfs");
        } catch (RestconfException e) {
            e.printStackTrace();
        }
        assertNull(actualResponse);
    }

    @Test
    public void putPositiveTest() {
        service.stubFor(put(urlEqualTo("/restconf/config/GENERIC-RESOURCE-API:preload-information"))
                .withRequestBody(WireMock.equalTo(preloadInformationRequestJson)).willReturn(aResponse().withStatus(200)));
        Exception ex = null;
        try {
            restconfClient.put("GENERIC-RESOURCE-API:preload-information", preloadInformationRequestJson);
        } catch (RestconfException e) {
            ex =e;
        }
        assertNull(ex);
    }

    @Test
    public void putNegativeTest() {
        service.stubFor(put(urlEqualTo("/restconf/config/GENERIC-RESOURCE-API:preload-information"))
                .withRequestBody(WireMock.equalTo(preloadInformationRequestJson)).willReturn(aResponse().withStatus(500)));
        try {
            restconfClient.put("GENERIC-RESOURCE-API:preload-information", preloadInformationRequestJson);
        } catch (RestconfException e) {
           assertTrue(e.getErrorMessage().contains("Error during restconf operation: PUT."));
        }
    }
}