/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2021 Nordix Foundation
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

package org.oran.dmaapadapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.oran.dmaapadapter.clients.AsyncRestClient;
import org.oran.dmaapadapter.clients.AsyncRestClientFactory;
import org.oran.dmaapadapter.configuration.ApplicationConfig;
import org.oran.dmaapadapter.configuration.ImmutableHttpProxyConfig;
import org.oran.dmaapadapter.configuration.ImmutableWebClientConfig;
import org.oran.dmaapadapter.configuration.WebClientConfig;
import org.oran.dmaapadapter.configuration.WebClientConfig.HttpProxyConfig;
import org.oran.dmaapadapter.r1.ConsumerJobInfo;
import org.oran.dmaapadapter.repository.InfoType;
import org.oran.dmaapadapter.repository.InfoTypes;
import org.oran.dmaapadapter.repository.Jobs;
import org.oran.dmaapadapter.tasks.ProducerRegstrationTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = { //
        "server.ssl.key-store=./config/keystore.jks", //
        "app.webclient.trust-store=./config/truststore.jks", //
        "app.configuration-filepath=./src/test/resources/test_application_configuration.json", //
        "app.ecs-base-url=https://localhost:8434" //
})
class IntegrationWithEcs {

    @Autowired
    private ApplicationConfig applicationConfig;

    @Autowired
    private ProducerRegstrationTask producerRegstrationTask;

    @Autowired
    private Jobs jobs;

    @Autowired
    private InfoTypes types;

    @Autowired
    private ConsumerController consumerController;

    private static Gson gson = new GsonBuilder().create();

    static class TestApplicationConfig extends ApplicationConfig {

        @Override
        public String getEcsBaseUrl() {
            return "https://localhost:8434";
        }

        @Override
        public String getDmaapBaseUrl() {
            return thisProcessUrl();
        }

        @Override
        public String getSelfUrl() {
            return thisProcessUrl();
        }

        private String thisProcessUrl() {
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

        @Override
        @Bean
        public ApplicationConfig getApplicationConfig() {
            TestApplicationConfig cfg = new TestApplicationConfig();
            return cfg;
        }
    }

    @AfterEach
    void reset() {
        this.consumerController.testResults.reset();
        this.jobs.clear();
        this.types.clear();
    }

    private AsyncRestClient restClient(boolean useTrustValidation) {
        WebClientConfig config = this.applicationConfig.getWebClientConfig();
        HttpProxyConfig httpProxyConfig = ImmutableHttpProxyConfig.builder() //
                .httpProxyHost("") //
                .httpProxyPort(0) //
                .build();
        config = ImmutableWebClientConfig.builder() //
                .keyStoreType(config.keyStoreType()) //
                .keyStorePassword(config.keyStorePassword()) //
                .keyStore(config.keyStore()) //
                .keyPassword(config.keyPassword()) //
                .isTrustStoreUsed(useTrustValidation) //
                .trustStore(config.trustStore()) //
                .trustStorePassword(config.trustStorePassword()) //
                .httpProxyConfig(httpProxyConfig).build();

        AsyncRestClientFactory restClientFactory = new AsyncRestClientFactory(config);
        return restClientFactory.createRestClientNoHttpProxy(selfBaseUrl());
    }

    private AsyncRestClient restClient() {
        return restClient(false);
    }

    private String selfBaseUrl() {
        return "https://localhost:" + this.applicationConfig.getLocalServerHttpPort();
    }

    private String ecsBaseUrl() {
        return applicationConfig.getEcsBaseUrl();
    }

    private void createInformationJobInEcs() {
        String url = ecsBaseUrl() + "/data-consumer/v1/info-jobs/jobId";
        String body = gson.toJson(consumerJobInfo());
        try {
            // Delete the job if it already exists
            restClient().delete(url).block();
        } catch (Exception e) {
        }
        restClient().putForEntity(url, body).block();
    }

    private ConsumerJobInfo consumerJobInfo() {
        InfoType type = this.types.getAll().iterator().next();
        return consumerJobInfo(type.getId(), "EI_JOB_ID");
    }

    private Object jsonObject() {
        return jsonObject("{}");
    }

    private Object jsonObject(String json) {
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            throw new NullPointerException(e.toString());
        }
    }

    private ConsumerJobInfo consumerJobInfo(String typeId, String infoJobId) {
        try {
            String targetUri = selfBaseUrl() + ConsumerController.CONSUMER_TARGET_URL;
            return new ConsumerJobInfo(typeId, jsonObject(), "owner", targetUri, "");
        } catch (Exception e) {
            return null;
        }
    }

    @Test
    void testWholeChain() throws Exception {
        await().untilAsserted(() -> assertThat(producerRegstrationTask.isRegisteredInEcs()).isTrue());

        createInformationJobInEcs();

        await().untilAsserted(() -> assertThat(this.jobs.size()).isEqualTo(1));

        DmaapSimulatorController.dmaapResponses.add("DmaapResponse1");
        DmaapSimulatorController.dmaapResponses.add("DmaapResponse2");

        ConsumerController.TestResults results = this.consumerController.testResults;
        await().untilAsserted(() -> assertThat(results.receivedBodies.size()).isEqualTo(2));
        assertThat(results.receivedBodies.get(0)).isEqualTo("DmaapResponse1");

        synchronized (this) {
            // logger.warn("**************** Keeping server alive! " +
            // this.applicationConfig.getLocalServerHttpPort());
            // this.wait();
        }
    }

}
