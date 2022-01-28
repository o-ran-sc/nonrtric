/*-
 * ========================LICENSE_START=================================
 * Copyright (C) 2021 Nordix Foundation. All rights reserved.
 * ======================================================================
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

package org.oransc.rappcatalogue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.client.TestRestTemplate.HttpClientOption;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = { //
        "server.ssl.key-store=./config/r-app-catalogue-keystore.jks", //
        "server.http-port=0"})
public class HttpsRequestTest {

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword; // inject password from config

    @Value("${server.ssl.key-store}")
    private String keyStore; // inject keyStore from config

    @LocalServerPort
    private int port;

    @Autowired
    private AbstractConfigurableWebServerFactory webServerFactory;

    @Test
    public void testSsl() {
        assertEquals(true, this.webServerFactory.getSsl().isEnabled());
    }

    @Test
    public void rest_OverPlainHttp_GetsBadRequestRequiresTLS() throws Exception {
        TestRestTemplate template = new TestRestTemplate();
        ResponseEntity<String> responseEntity =
            template.getForEntity("http://localhost:" + port + "/services", String.class);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("This combination of host and port requires TLS"));
    }

    @Test
    public void rest_WithoutSSLConfiguration_ThrowsSSLExceptionUnableFindValidCertPath() throws Exception {
        TestRestTemplate template = new TestRestTemplate();

        ResourceAccessException thrown = assertThrows(ResourceAccessException.class, () -> {
            template.getForEntity("https://localhost:" + port + "/services", String.class);
        });
        assertTrue(thrown.getMessage().contains("unable to find valid certification path to requested target"));
    }

    @Test
    public void rest_WithTwoWaySSL_AuthenticatesAndGetsExpectedResponse() throws Exception {

        SSLContext sslContext = new SSLContextBuilder().loadKeyMaterial(ResourceUtils.getFile(keyStore),
            keyStorePassword.toCharArray(), keyStorePassword.toCharArray()).build();

        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);
        HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplateBuilder rtb =
            new RestTemplateBuilder().requestFactory(() -> factory).rootUri("https://localhost:" + port);

        TestRestTemplate template = new TestRestTemplate(rtb, null, null, HttpClientOption.SSL);

        ResponseEntity<String> responseEntity = template.getForEntity("/services", String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("[]", responseEntity.getBody());
    }

}
