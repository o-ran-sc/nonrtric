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
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.oran.pmlog.clients.AsyncRestClient;
import org.oran.pmlog.clients.AsyncRestClientFactory;
import org.oran.pmlog.clients.SecurityContext;
import org.oran.pmlog.configuration.ApplicationConfig;
import org.oran.pmlog.configuration.ConsumerJobInfo;
import org.oran.pmlog.configuration.WebClientConfig;
import org.oran.pmlog.configuration.WebClientConfig.HttpProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;

@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = { //
        "server.ssl.key-store=./config/keystore.jks", //
        "app.webclient.trust-store=./config/truststore.jks", //
        "app.webclient.trust-store-used=true" //
})
class ApplicationTest {

    @Autowired
    private ApplicationConfig applicationConfig;

    @Autowired
    SecurityContext securityContext;

    @Autowired
    private IcsSimulatorController icsSimulatorController;

    @Autowired
    private ConsumerRegstrationTask consumerRegstrationTask;

    private com.google.gson.Gson gson = new com.google.gson.GsonBuilder().disableHtmlEscaping().create();

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static class TestApplicationConfig extends ApplicationConfig {

        @Override
        public String getIcsBaseUrl() {
            return thisProcessUrl();
        }

        String thisProcessUrl() {
            final String url = "https://localhost:" + getLocalServerHttpsPort();
            return url;
        }
    }

    /**
     * Overrides the BeanFactory.
     */
    @TestConfiguration
    static class TestBeanFactory extends BeanFactory {

        static final TestApplicationConfig applicationConfig = new TestApplicationConfig();
        static KafkaTopicListener kafkaListener;
        static InfluxStore influxStore;

        @Override
        @Bean
        public ServletWebServerFactory servletContainer() {
            return new TomcatServletWebServerFactory();
        }

        @Override
        @Bean
        public ApplicationConfig getApplicationConfig() {
            return applicationConfig;
        }

        @Override
        @Bean
        public KafkaTopicListener getKafkaTopicListener(@Autowired ApplicationConfig appConfig) {
            kafkaListener = new KafkaTopicListener(applicationConfig);
            return kafkaListener;
        }

        @Override
        @Bean
        public InfluxStore getInfluxStore(@Autowired ApplicationConfig appConfig,
                @Autowired KafkaTopicListener listener) {
            influxStore = spy(new InfluxStore(applicationConfig));
            return influxStore;
        }

    }

    @BeforeEach
    public void init() {}

    @AfterEach
    void reset() {
        this.icsSimulatorController.testResults.reset();
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
    void testPmReport() throws IOException {
        String path = "./src/test/resources/pm_report.json";
        String str = Files.readString(Path.of(path), Charset.defaultCharset());
        DataFromKafkaTopic data = new DataFromKafkaTopic(null, null, str.getBytes());

        Mockito.doNothing().when(TestBeanFactory.influxStore).store(any(), any());
        TestBeanFactory.influxStore.start(Flux.just(data));

        Mockito.verify(TestBeanFactory.influxStore, Mockito.times(1)).store(any(), any());
    }

    @Test
    void testJobCreation() throws Exception {
        await().untilAsserted(() -> assertThat(consumerRegstrationTask.isRegisteredInIcs()).isTrue());
        ConsumerJobInfo createdJob = this.icsSimulatorController.testResults.createdJob;
        assertThat(createdJob).isNotNull();
        assertThat(createdJob.jobDefinition.getDeliveryInfo().getTopic())
                .isEqualTo(applicationConfig.getKafkaInputTopic());
    }

    private AsyncRestClient restClient() {
        return restClient(false);
    }

    private String baseUrl() {
        return "https://localhost:" + this.applicationConfig.getLocalServerHttpsPort();
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
