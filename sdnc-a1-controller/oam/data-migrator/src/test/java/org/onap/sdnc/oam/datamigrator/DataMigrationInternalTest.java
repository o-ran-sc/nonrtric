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
package org.onap.sdnc.oam.datamigrator;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class DataMigrationInternalTest {

    @Rule
    public WireMockRule source = new WireMockRule(8081);
    @Rule
    public WireMockRule target = new WireMockRule(8082);
    
    private static final Logger LOG = LoggerFactory.getLogger(DataMigrationInternal.class);
    DataMigrationInternal dataMigrationInternal = new DataMigrationInternal(LOG);
    private ClassLoader classLoader = getClass().getClassLoader();
    private  String preloadVnfResponseJson = new String(Files.readAllBytes(Paths.get(classLoader.getResource("wiremock/preloadVnfResponse.json").toURI())));
    private String preloadInformationRequestJson = new String(Files.readAllBytes(Paths.get(classLoader.getResource("wiremock/preloadInformationRequest.json").toURI())));

    public DataMigrationInternalTest() throws IOException, URISyntaxException {
    }

    @Test
    public void runPositiveTest() {
        String [] args = {"-c","migration/props"};
        PrintStream oldOutputStream = System.out;
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(myOut));
        source.stubFor(get(urlEqualTo("/restconf/config/GENERIC-RESOURCE-API:preload-vnfs")).willReturn(
                aResponse()
                        .withStatus(200)
                        .withBody(preloadVnfResponseJson)));
        target.stubFor(put(urlEqualTo("/restconf/config/GENERIC-RESOURCE-API:preload-information")).withRequestBody(WireMock.equalTo(preloadInformationRequestJson)).willReturn(
                aResponse()
                        .withStatus(200)));
        dataMigrationInternal.run(args);
        String content = myOut.toString();
        assertThat("Migration failed", content.contains("MIGRATE operation completed Successfully."));
        System.setOut(oldOutputStream);
    }

  @Test
    public void runTestWithNoData() {
        String [] args = {"-c","migration/props"};
      PrintStream oldOutputStream = System.out;
      final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
      System.setOut(new PrintStream(myOut));
      source.stubFor(get(urlEqualTo("/restconf/config/GENERIC-RESOURCE-API:preload-vnfs"))
              .willReturn(aResponse().withStatus(404)));
      target.stubFor(put(urlEqualTo("/restconf/config/GENERIC-RESOURCE-API:preload-information"))
              .withRequestBody(WireMock.equalTo(preloadInformationRequestJson)).willReturn(aResponse().withStatus(200)));
      dataMigrationInternal.run(args);
      String content = myOut.toString();
      assertThat("Migration failed", content.contains("MIGRATE operation completed Successfully."));
      System.setOut(oldOutputStream);
    }
}