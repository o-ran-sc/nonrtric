/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2023 Nordix Foundation
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

package org.oran.pmlog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.oran.pmlog.clients.AsyncRestClient;
import org.oran.pmlog.clients.AsyncRestClientFactory;
import org.oran.pmlog.clients.SecurityContext;
import org.oran.pmlog.configuration.ApplicationConfig;
import org.oran.pmlog.configuration.WebClientConfig;
import org.oran.pmlog.configuration.WebClientConfig.HttpProxyConfig;
import org.oran.pmlog.tasks.TopicListeners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = { //
        "server.ssl.key-store=./config/keystore.jks", //
        "app.webclient.trust-store=./config/truststore.jks", //
        "app.webclient.trust-store-used=true" //
})
class ApplicationTest {

    @Autowired
    private ApplicationConfig applicationConfig;

    @Autowired
    TopicListeners topicListeners;

    @Autowired
    SecurityContext securityContext;

    private com.google.gson.Gson gson = new com.google.gson.GsonBuilder().disableHtmlEscaping().create();

    @LocalServerPort
    int port;

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static class TestApplicationConfig extends ApplicationConfig {

        String thisProcessUrl() {
            final String url = "https://localhost:" + getLocalServerHttpPort();
            return url;
        }
    }

    /**
     * Overrides the BeanFactory.
     */
    @TestConfiguration
    static class TestBeanFactory extends BeanFactory {

        @Override
        @Bean
        public ServletWebServerFactory servletContainer() {
            return new TomcatServletWebServerFactory();
        }

        // @Override
        @Bean
        public ApplicationConfig getApplicationConfig() {
            TestApplicationConfig cfg = new TestApplicationConfig();
            return cfg;
        }

    }

    @BeforeEach
    public void init() {}

    @AfterEach
    void reset() {

    }

    @Test
    void generateApiDoc() throws FileNotFoundException {
        String url = "/v3/api-docs";
        ResponseEntity<String> resp = restClient().getForEntity(url).block();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        JSONObject jsonObj = new JSONObject(resp.getBody());
        assertThat(jsonObj.remove("servers")).isNotNull();

        String indented = jsonObj.toString(4);
        try (PrintStream out = new PrintStream(new FileOutputStream("api/pmlog-api.json"))) {
            out.print(indented);
        }
    }

    @Test
    void testJunk() {

    }

    private AsyncRestClient restClient() {
        return restClient(false);
    }

    private String baseUrl() {
        return "https://localhost:" + this.port;
    }

    private AsyncRestClient restClient(boolean useTrustValidation) {
        WebClientConfig config = this.applicationConfig.getWebClientConfig();
        HttpProxyConfig httpProxyConfig = HttpProxyConfig.builder() //
                .httpProxyHost("") //
                .httpProxyPort(0) //
                .build();
        config = WebClientConfig.builder() //
                .keyStoreType(config.getKeyStoreType()) //
                .keyStorePassword(config.getKeyStorePassword()) //
                .keyStore(config.getKeyStore()) //
                .keyPassword(config.getKeyPassword()) //
                .isTrustStoreUsed(useTrustValidation) //
                .trustStore(config.getTrustStore()) //
                .trustStorePassword(config.getTrustStorePassword()) //
                .httpProxyConfig(httpProxyConfig).build();

        AsyncRestClientFactory restClientFactory = new AsyncRestClientFactory(config, securityContext);
        return restClientFactory.createRestClientNoHttpProxy(baseUrl());
    }

}
