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
import static org.junit.Assert.assertTrue;

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
import org.oran.dmaapadapter.repository.InfoTypes;
import org.oran.dmaapadapter.repository.Job;
import org.oran.dmaapadapter.repository.Jobs;
import org.oran.dmaapadapter.tasks.ProducerRegstrationTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SuppressWarnings("java:S3577") // Rename class
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = { //
        "server.ssl.key-store=./config/keystore.jks", //
        "app.webclient.trust-store=./config/truststore.jks", //
        "app.configuration-filepath=./src/test/resources/test_application_configuration.json", //
        "app.ics-base-url=https://localhost:8434" //
})
class IntegrationWithIcs {

    private static final String DMAAP_JOB_ID = "DMAAP_JOB_ID";
    private static final String DMAAP_TYPE_ID = "DmaapInformationType";

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
        public String getIcsBaseUrl() {
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
        assertThat(this.jobs.size()).isZero();
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

    private String icsBaseUrl() {
        return applicationConfig.getIcsBaseUrl();
    }

    private String jobUrl(String jobId) {
        return icsBaseUrl() + "/data-consumer/v1/info-jobs/" + jobId + "?typeCheck=true";
    }

    private void createInformationJobInIcs(String typeId, String jobId, String filter) {
        String body = gson.toJson(consumerJobInfo(typeId, filter));
        try {
            // Delete the job if it already exists
            deleteInformationJobInIcs(jobId);
        } catch (Exception e) {
        }
        restClient().putForEntity(jobUrl(jobId), body).block();
    }

    private void deleteInformationJobInIcs(String jobId) {
        restClient().delete(jobUrl(jobId)).block();
    }

    private ConsumerJobInfo consumerJobInfo(String typeId, String filter) {
        return consumerJobInfo(typeId, DMAAP_JOB_ID, filter);
    }

    private Object jsonObject(String json) {
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            throw new NullPointerException(e.toString());
        }
    }

    private String quote(String str) {
        return "\"" + str + "\"";
    }

    private String consumerUri() {
        return selfBaseUrl() + ConsumerController.CONSUMER_TARGET_URL;
    }

    private ConsumerJobInfo consumerJobInfo(String typeId, String infoJobId, String filter) {
        try {

            String jsonStr = "{ \"filter\" :" + quote(filter) + "}";
            return new ConsumerJobInfo(typeId, jsonObject(jsonStr), "owner", consumerUri(), "");
        } catch (Exception e) {
            return null;
        }
    }

    private void testErrorCode(Mono<?> request, HttpStatus expStatus, String responseContains) {
        testErrorCode(request, expStatus, responseContains, true);
    }

    private void testErrorCode(Mono<?> request, HttpStatus expStatus, String responseContains,
            boolean expectApplicationProblemJsonMediaType) {
        StepVerifier.create(request) //
                .expectSubscription() //
                .expectErrorMatches(
                        t -> checkWebClientError(t, expStatus, responseContains, expectApplicationProblemJsonMediaType)) //
                .verify();
    }

    private boolean checkWebClientError(Throwable throwable, HttpStatus expStatus, String responseContains,
            boolean expectApplicationProblemJsonMediaType) {
        assertTrue(throwable instanceof WebClientResponseException);
        WebClientResponseException responseException = (WebClientResponseException) throwable;
        assertThat(responseException.getStatusCode()).isEqualTo(expStatus);
        assertThat(responseException.getResponseBodyAsString()).contains(responseContains);
        if (expectApplicationProblemJsonMediaType) {
            assertThat(responseException.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        }
        return true;
    }

    @Test
    void testCreateKafkaJob() {
        await().untilAsserted(() -> assertThat(producerRegstrationTask.isRegisteredInIcs()).isTrue());
        final String TYPE_ID = "KafkaInformationType";

        Job.Parameters param = new Job.Parameters("filter", new Job.BufferTimeout(123, 456), 1);

        ConsumerJobInfo jobInfo =
                new ConsumerJobInfo(TYPE_ID, jsonObject(gson.toJson(param)), "owner", consumerUri(), "");
        String body = gson.toJson(jobInfo);

        restClient().putForEntity(jobUrl("KAFKA_JOB_ID"), body).block();

        await().untilAsserted(() -> assertThat(this.jobs.size()).isEqualTo(1));

        deleteInformationJobInIcs("KAFKA_JOB_ID");
        await().untilAsserted(() -> assertThat(this.jobs.size()).isZero());
    }

    @Test
    void testKafkaJobParameterOutOfRange() {

        await().untilAsserted(() -> assertThat(producerRegstrationTask.isRegisteredInIcs()).isTrue());
        final String TYPE_ID = "KafkaInformationType";

        Job.Parameters param = new Job.Parameters("filter", new Job.BufferTimeout(123, 170 * 1000), 1);

        ConsumerJobInfo jobInfo =
                new ConsumerJobInfo(TYPE_ID, jsonObject(gson.toJson(param)), "owner", consumerUri(), "");
        String body = gson.toJson(jobInfo);

        testErrorCode(restClient().put(jobUrl("KAFKA_JOB_ID"), body), HttpStatus.BAD_REQUEST,
                "Json validation failure");

    }

    @Test
    void testDmaapMessage() throws Exception {
        await().untilAsserted(() -> assertThat(producerRegstrationTask.isRegisteredInIcs()).isTrue());

        createInformationJobInIcs(DMAAP_TYPE_ID, DMAAP_JOB_ID, ".*DmaapResponse.*");

        await().untilAsserted(() -> assertThat(this.jobs.size()).isEqualTo(1));

        DmaapSimulatorController.dmaapResponses.add("DmaapResponse1");
        DmaapSimulatorController.dmaapResponses.add("DmaapResponse2");
        DmaapSimulatorController.dmaapResponses.add("Junk");

        ConsumerController.TestResults results = this.consumerController.testResults;
        await().untilAsserted(() -> assertThat(results.receivedBodies.size()).isEqualTo(2));
        assertThat(results.receivedBodies.get(0)).isEqualTo("DmaapResponse1");

        deleteInformationJobInIcs(DMAAP_JOB_ID);

        await().untilAsserted(() -> assertThat(this.jobs.size()).isZero());
    }

}
