/*-
 * ========================LICENSE_START=================================
 * ONAP : ccsdk oran
 * ======================================================================
 * Copyright (C) 2019-2020 Nordix Foundation. All rights reserved.
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

package org.oransc.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.oransc.enrichment.clients.AsyncRestClient;
import org.oransc.enrichment.configuration.ApplicationConfig;
import org.oransc.enrichment.configuration.ImmutableWebClientConfig;
import org.oransc.enrichment.configuration.WebClientConfig;
import org.oransc.enrichment.controllers.consumer.ConsumerConsts;
import org.oransc.enrichment.controllers.consumer.ConsumerEiJobInfo;
import org.oransc.enrichment.repository.EiJob;
import org.oransc.enrichment.repository.EiJobs;
import org.oransc.enrichment.repository.EiType;
import org.oransc.enrichment.repository.EiTypes;
import org.oransc.enrichment.repository.ImmutableEiJob;
import org.oransc.enrichment.repository.ImmutableEiType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = { //
        "server.ssl.key-store=./config/keystore.jks", //
        "app.webclient.trust-store=./config/truststore.jks"})
class ApplicationTest {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationTest.class);

    @Autowired
    ApplicationContext context;

    @Autowired
    EiJobs eiJobs;

    @Autowired
    EiTypes eiTypes;

    @Autowired
    ApplicationConfig applicationConfig;

    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    /**
     * Overrides the BeanFactory.
     */
    @TestConfiguration
    static class TestBeanFactory {

    }

    @LocalServerPort
    private int port;

    @BeforeEach
    void reset() {
        this.eiJobs.clear();
        this.eiTypes.clear();
    }

    @Test
    void getEiTypes() throws Exception {
        addEiType("test");
        String url = "/eitypes";
        String rsp = restClient().get(url).block();
        assertThat(rsp).isEqualTo("[\"test\"]");
    }

    @Test
    void getEiType() throws Exception {
        addEiType("test");
        String url = "/eitypes/test";
        String rsp = restClient().get(url).block();
        assertThat(rsp).contains("job_data_schema");
    }

    @Test
    void getEiTypeNotFound() throws Exception {
        String url = "/eitypes/junk";
        testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND, "Could not find EI Job: junk");
    }

    @Test
    void getEiJobsIds() throws Exception {
        addEiJob("typeId", "jobId");
        String url = "/eitypes/typeId/eijobs";
        String rsp = restClient().get(url).block();
        assertThat(rsp).isEqualTo("[\"jobId\"]");
    }

    @Test
    void getEiJobTypeNotFound() throws Exception {
        String url = "/eitypes/junk/eijobs";
        testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND, "Could not find EI Job: junk");
    }

    @Test
    void getEiJob() throws Exception {
        addEiJob("typeId", "jobId");
        String url = "/eitypes/typeId/eijobs/jobId";
        String rsp = restClient().get(url).block();
        assertThat(rsp).contains("job_data");
    }

    @Test
    void getEiJobStatus() throws Exception {
        addEiJob("typeId", "jobId");
        String url = "/eitypes/typeId/eijobs/jobId/status";
        String rsp = restClient().get(url).block();
        assertThat(rsp).contains("ENABLED");
    }

    // Status TBD

    @Test
    void deleteEiJob() throws Exception {
        addEiJob("typeId", "jobId");
        assertThat(this.eiJobs.size()).isEqualTo(1);
        String url = "/eitypes/typeId/eijobs/jobId";
        restClient().delete(url).block();
        assertThat(this.eiJobs.size()).isEqualTo(0);
    }

    @Test
    void putEiJob() throws Exception {
        addEiType("typeId");

        String url = "/eitypes/typeId/eijobs/jobId";
        String body = gson.toJson(eiJobInfo());
        ResponseEntity<String> resp = restClient().putForEntity(url, body).block();
        assertThat(this.eiJobs.size()).isEqualTo(1);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        resp = restClient().putForEntity(url, body).block();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        EiJob job = this.eiJobs.getJob("jobId");
        assertThat(job.owner()).isEqualTo("owner");
    }

    ConsumerEiJobInfo eiJobInfo() {
        return new ConsumerEiJobInfo(jsonObject(), "owner");
    }

    // @Test
    @SuppressWarnings("squid:S2699")
    void runMock() throws Exception {
        logger.info("Keeping server alive! " + this.port);
        synchronized (this) {
            this.wait();
        }
    }

    JsonObject jsonObject() {
        JsonObject jsonObj = new JsonObject();
        JsonElement e = new JsonPrimitive(111);
        jsonObj.add("param", e);
        return jsonObj;
    }

    private EiJob addEiJob(String typeId, String jobId) {
        addEiType(typeId);
        EiJob job = ImmutableEiJob.builder() //
            .id(jobId) //
            .typeId(typeId) //
            .owner("owner") //
            .jobData(jsonObject()) //
            .build();
        this.eiJobs.put(job);
        return job;
    }

    private EiType addEiType(String typeId) {
        EiType t = ImmutableEiType.builder() //
            .id(typeId) //
            .jobDataSchema(jsonObject()) //
            .build();
        this.eiTypes.put(t);
        return t;
    }

    private String baseUrl() {
        return "https://localhost:" + this.port + ConsumerConsts.A1E_API_ROOT;
    }

    private AsyncRestClient restClient(boolean useTrustValidation) {
        WebClientConfig config = this.applicationConfig.getWebClientConfig();
        config = ImmutableWebClientConfig.builder() //
            .keyStoreType(config.keyStoreType()) //
            .keyStorePassword(config.keyStorePassword()) //
            .keyStore(config.keyStore()) //
            .keyPassword(config.keyPassword()) //
            .isTrustStoreUsed(useTrustValidation) //
            .trustStore(config.trustStore()) //
            .trustStorePassword(config.trustStorePassword()) //
            .build();

        return new AsyncRestClient(baseUrl(), config);
    }

    private AsyncRestClient restClient() {
        return restClient(false);
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

}
