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
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.oransc.enrichment.clients.AsyncRestClient;
import org.oransc.enrichment.clients.ProducerJobInfo;
import org.oransc.enrichment.configuration.ApplicationConfig;
import org.oransc.enrichment.configuration.ImmutableWebClientConfig;
import org.oransc.enrichment.configuration.WebClientConfig;
import org.oransc.enrichment.controller.ProducerSimulatorController;
import org.oransc.enrichment.controllers.consumer.ConsumerConsts;
import org.oransc.enrichment.controllers.consumer.ConsumerEiJobInfo;
import org.oransc.enrichment.controllers.consumer.ConsumerEiTypeInfo;
import org.oransc.enrichment.controllers.producer.ProducerConsts;
import org.oransc.enrichment.controllers.producer.ProducerRegistrationInfo;
import org.oransc.enrichment.controllers.producer.ProducerRegistrationInfo.ProducerEiTypeRegistrationInfo;
import org.oransc.enrichment.exceptions.ServiceException;
import org.oransc.enrichment.repository.EiJob;
import org.oransc.enrichment.repository.EiJobs;
import org.oransc.enrichment.repository.EiProducers;
import org.oransc.enrichment.repository.EiType;
import org.oransc.enrichment.repository.EiTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
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
    private final String EI_TYPE_ID = "typeId";
    private final String EI_PRODUCER_ID = "producerId";
    private final String EI_JOB_PROPERTY = "\"property1\"";

    @Autowired
    ApplicationContext context;

    @Autowired
    EiJobs eiJobs;

    @Autowired
    EiTypes eiTypes;

    @Autowired
    EiProducers eiProducers;

    @Autowired
    ApplicationConfig applicationConfig;

    @Autowired
    ProducerSimulatorController producerSimulator;

    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    /**
     * Overrides the BeanFactory.
     */
    @TestConfiguration
    static class TestBeanFactory {
        @Bean
        public ServletWebServerFactory servletContainer() {
            return new TomcatServletWebServerFactory();
        }
    }

    @LocalServerPort
    private int port;

    @BeforeEach
    void reset() {
        this.eiJobs.clear();
        this.eiTypes.clear();
        this.eiProducers.clear();
        this.producerSimulator.getTestResults().reset();
    }

    @AfterEach
    void check() {
        assertThat(this.producerSimulator.getTestResults().errorFound).isFalse();
    }

    @Test
    void testGetEiTypes() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, "test");
        String url = ConsumerConsts.API_ROOT + "/eitypes";
        String rsp = restClient().get(url).block();
        assertThat(rsp).isEqualTo("[\"test\"]");
    }

    @Test
    void testGetEiType() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, "test");
        String url = ConsumerConsts.API_ROOT + "/eitypes/test";
        String rsp = restClient().get(url).block();
        ConsumerEiTypeInfo info = gson.fromJson(rsp, ConsumerEiTypeInfo.class);
        assertThat(info.jobParametersSchema).isNotNull();
    }

    @Test
    void testGetEiTypeNotFound() throws Exception {
        String url = ConsumerConsts.API_ROOT + "/eitypes/junk";
        testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND, "Could not find EI type: junk");
    }

    @Test
    void testGetEiJobsIds() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        putEiJob(EI_TYPE_ID, "jobId");
        String url = ConsumerConsts.API_ROOT + "/eitypes/typeId/eijobs";
        String rsp = restClient().get(url).block();
        assertThat(rsp).isEqualTo("[\"jobId\"]");
    }

    @Test
    void testGetEiJobTypeNotFound() throws Exception {
        String url = ConsumerConsts.API_ROOT + "/eitypes/junk/eijobs";
        testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND, "Could not find EI type: junk");
    }

    @Test
    void testGetEiJob() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        putEiJob(EI_TYPE_ID, "jobId");
        String url = ConsumerConsts.API_ROOT + "/eitypes/typeId/eijobs/jobId";
        String rsp = restClient().get(url).block();
        ConsumerEiJobInfo info = gson.fromJson(rsp, ConsumerEiJobInfo.class);
        assertThat(info.owner).isEqualTo("owner");
    }

    @Test
    void testGetEiJobNotFound() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        String url = ConsumerConsts.API_ROOT + "/eitypes/typeId/eijobs/junk";
        testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND, "Could not find EI job: junk");
    }

    @Test
    void testGetEiJobStatus() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        putEiJob(EI_TYPE_ID, "jobId");
        String url = ConsumerConsts.API_ROOT + "/eitypes/typeId/eijobs/jobId/status";
        String rsp = restClient().get(url).block();
        assertThat(rsp).contains("ENABLED");
    }

    // Status TBD

    @Test
    void testDeleteEiJob() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        putEiJob(EI_TYPE_ID, "jobId");
        assertThat(this.eiJobs.size()).isEqualTo(1);
        String url = ConsumerConsts.API_ROOT + "/eitypes/typeId/eijobs/jobId";
        restClient().delete(url).block();
        assertThat(this.eiJobs.size()).isZero();

        ProducerSimulatorController.TestResults simulatorResults = this.producerSimulator.getTestResults();
        await().untilAsserted(() -> assertThat(simulatorResults.jobsStopped.size()).isEqualTo(1));
        assertThat(simulatorResults.jobsStopped.get(0).id).isEqualTo("jobId");
    }

    @Test
    void testDeleteEiJobNotFound() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        String url = ConsumerConsts.API_ROOT + "/eitypes/typeId/eijobs/junk";
        testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND, "Could not find EI job: junk");
    }

    @Test
    void testPutEiJob() throws Exception {
        // Test that one producer accepting a job is enough
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        putEiProducerWithOneTypeRejecting("simulateProducerError", EI_TYPE_ID);

        String url = ConsumerConsts.API_ROOT + "/eitypes/typeId/eijobs/jobId";
        String body = gson.toJson(eiJobInfo());
        ResponseEntity<String> resp = restClient().putForEntity(url, body).block();
        assertThat(this.eiJobs.size()).isEqualTo(1);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ProducerSimulatorController.TestResults simulatorResults = this.producerSimulator.getTestResults();
        await().untilAsserted(() -> assertThat(simulatorResults.jobsStarted.size()).isEqualTo(1));
        ProducerJobInfo request = simulatorResults.jobsStarted.get(0);
        assertThat(request.id).isEqualTo("jobId");

        assertThat(simulatorResults.noOfRejectedCreate).isEqualTo(1);

        resp = restClient().putForEntity(url, body).block();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        EiJob job = this.eiJobs.getJob("jobId");
        assertThat(job.owner()).isEqualTo("owner");
    }

    @Test
    void putEiProducerWithOneType_rejecting() throws JsonMappingException, JsonProcessingException, ServiceException {
        putEiProducerWithOneTypeRejecting("simulateProducerError", EI_TYPE_ID);
        String url = ConsumerConsts.API_ROOT + "/eitypes/typeId/eijobs/jobId";
        String body = gson.toJson(eiJobInfo());
        testErrorCode(restClient().put(url, body), HttpStatus.CONFLICT, "Job not accepted by any producers");

        ProducerSimulatorController.TestResults simulatorResults = this.producerSimulator.getTestResults();
        assertThat(simulatorResults.noOfRejectedCreate).isEqualTo(1);
    }

    @Test
    void testPutEiJob_jsonSchemavalidationError() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);

        String url = ConsumerConsts.API_ROOT + "/eitypes/typeId/eijobs/jobId";
        // The element with name "property1" is mandatory in the schema
        ConsumerEiJobInfo jobInfo =
            new ConsumerEiJobInfo(jsonObject("{ \"XXstring\" : \"value\" }"), "owner", "targetUri");
        String body = gson.toJson(jobInfo);

        testErrorCode(restClient().put(url, body), HttpStatus.CONFLICT, "Json validation failure");
    }

    @Test
    void testGetEiProducerTypes() throws Exception {
        final String EI_TYPE_ID_2 = EI_TYPE_ID + "_2";
        putEiProducerWithOneType("producer1", EI_TYPE_ID);
        putEiJob(EI_TYPE_ID, "jobId");
        putEiProducerWithOneType("producer2", EI_TYPE_ID_2);
        putEiJob(EI_TYPE_ID_2, "jobId2");
        String url = ProducerConsts.API_ROOT + "/eitypes";

        ResponseEntity<String> resp = restClient().getForEntity(url).block();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains(EI_TYPE_ID);
        assertThat(resp.getBody()).contains(EI_TYPE_ID_2);
    }

    @Test
    void testReplacingEiProducerTypes() throws Exception {
        final String REPLACED_TYPE_ID = "replaced";
        putEiProducerWithOneType(EI_PRODUCER_ID, REPLACED_TYPE_ID);
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);

        String url = ProducerConsts.API_ROOT + "/eitypes";

        ResponseEntity<String> resp = restClient().getForEntity(url).block();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains(EI_TYPE_ID);
        assertThat(resp.getBody()).doesNotContain(REPLACED_TYPE_ID);
    }

    @Test
    void testChangingEiTypeGetRejected() throws Exception {
        putEiProducerWithOneType("producer1", "typeId1");
        putEiProducerWithOneType("producer2", "typeId2");
        putEiJob("typeId1", "jobId");

        String url = ConsumerConsts.API_ROOT + "/eitypes/typeId2/eijobs/jobId";
        String body = gson.toJson(eiJobInfo());
        testErrorCode(restClient().put(url, body), HttpStatus.CONFLICT,
            "Not allowed to change type for existing EI job");
    }

    @Test
    void testPutEiProducer() throws Exception {
        String url = ProducerConsts.API_ROOT + "/eiproducers/eiProducerId";
        String body = gson.toJson(producerEiRegistratioInfo(EI_TYPE_ID));

        ResponseEntity<String> resp = restClient().putForEntity(url, body).block();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(this.eiTypes.size()).isEqualTo(1);
        EiType type = this.eiTypes.getType(EI_TYPE_ID);
        assertThat(type.getProducerIds()).contains("eiProducerId");
        assertThat(this.eiProducers.size()).isEqualTo(1);
        assertThat(this.eiProducers.get("eiProducerId").eiTypes().iterator().next().getId()).isEqualTo(EI_TYPE_ID);

        resp = restClient().putForEntity(url, body).block();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        resp = restClient().getForEntity(url).block();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo(body);
    }

    @Test
    void testPutEiProducerExistingJob() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        putEiJob(EI_TYPE_ID, "jobId");
        String url = ProducerConsts.API_ROOT + "/eiproducers/eiProducerId";
        String body = gson.toJson(producerEiRegistratioInfo(EI_TYPE_ID));
        restClient().putForEntity(url, body).block();

        ProducerSimulatorController.TestResults simulatorResults = this.producerSimulator.getTestResults();
        await().untilAsserted(() -> assertThat(simulatorResults.jobsStarted.size()).isEqualTo(2));
        ProducerJobInfo request = simulatorResults.jobsStarted.get(0);
        assertThat(request.id).isEqualTo("jobId");
    }

    @Test
    void testPutProducerAndEiJob() throws Exception {
        String url = ProducerConsts.API_ROOT + "/eiproducers/eiProducerId";
        String body = gson.toJson(producerEiRegistratioInfo(EI_TYPE_ID));
        restClient().putForEntity(url, body).block();
        assertThat(this.eiTypes.size()).isEqualTo(1);
        this.eiTypes.getType(EI_TYPE_ID);

        url = ConsumerConsts.API_ROOT + "/eitypes/typeId/eijobs/jobId";
        body = gson.toJson(eiJobInfo());
        restClient().putForEntity(url, body).block();

        ProducerSimulatorController.TestResults simulatorResults = this.producerSimulator.getTestResults();
        await().untilAsserted(() -> assertThat(simulatorResults.jobsStarted.size()).isEqualTo(1));
        ProducerJobInfo request = simulatorResults.jobsStarted.get(0);
        assertThat(request.id).isEqualTo("jobId");
    }

    @Test
    void testGetEiJobsForProducer() throws JsonMappingException, JsonProcessingException, ServiceException {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        putEiJob(EI_TYPE_ID, "jobId1");
        putEiJob(EI_TYPE_ID, "jobId2");

        // PUT a consumer
        String url = ProducerConsts.API_ROOT + "/eiproducers/eiProducerId";
        String body = gson.toJson(producerEiRegistratioInfo(EI_TYPE_ID));
        restClient().putForEntity(url, body).block();

        url = ProducerConsts.API_ROOT + "/eiproducers/eiProducerId/eijobs";
        ResponseEntity<String> resp = restClient().getForEntity(url).block();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ProducerJobInfo[] parsedResp = gson.fromJson(resp.getBody(), ProducerJobInfo[].class);
        assertThat(parsedResp[0].typeId).isEqualTo(EI_TYPE_ID);
        assertThat(parsedResp[1].typeId).isEqualTo(EI_TYPE_ID);
    }

    @Test
    void testDeleteEiProducer() throws Exception {
        putEiProducerWithOneType("eiProducerId", EI_TYPE_ID);
        putEiProducerWithOneType("eiProducerId2", EI_TYPE_ID);

        assertThat(this.eiProducers.size()).isEqualTo(2);
        EiType type = this.eiTypes.getType(EI_TYPE_ID);
        assertThat(type.getProducerIds()).contains("eiProducerId");
        assertThat(type.getProducerIds()).contains("eiProducerId2");
        putEiJob(EI_TYPE_ID, "jobId");
        assertThat(this.eiJobs.size()).isEqualTo(1);

        String url = ProducerConsts.API_ROOT + "/eiproducers/eiProducerId";
        restClient().deleteForEntity(url).block();
        assertThat(this.eiProducers.size()).isEqualTo(1);
        assertThat(this.eiTypes.getType(EI_TYPE_ID).getProducerIds()).doesNotContain("eiProducerId");
        assertThat(this.eiJobs.size()).isEqualTo(1);

        String url2 = ProducerConsts.API_ROOT + "/eiproducers/eiProducerId2";
        restClient().deleteForEntity(url2).block();
        assertThat(this.eiProducers.size()).isZero();
        assertThat(this.eiTypes.size()).isZero();
        assertThat(this.eiJobs.size()).isZero();
    }

    @Test
    void testGetProducerEiType() throws JsonMappingException, JsonProcessingException, ServiceException {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        String url = ProducerConsts.API_ROOT + "/eitypes/" + EI_TYPE_ID;
        ResponseEntity<String> resp = restClient().getForEntity(url).block();
        assertThat(resp.getBody()).contains(EI_PRODUCER_ID);
    }

    @Test
    void testGetProducerIdentifiers() throws JsonMappingException, JsonProcessingException, ServiceException {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        String url = ProducerConsts.API_ROOT + "/eiproducers";
        ResponseEntity<String> resp = restClient().getForEntity(url).block();
        assertThat(resp.getBody()).contains(EI_PRODUCER_ID);
    }

    ProducerEiTypeRegistrationInfo producerEiTypeRegistrationInfo(String typeId)
        throws JsonMappingException, JsonProcessingException {
        return new ProducerEiTypeRegistrationInfo(jsonSchemaObject(), typeId);
    }

    ProducerRegistrationInfo producerEiRegistratioInfoRejecting(String typeId)
        throws JsonMappingException, JsonProcessingException {
        Collection<ProducerEiTypeRegistrationInfo> types = new ArrayList<>();
        types.add(producerEiTypeRegistrationInfo(typeId));
        return new ProducerRegistrationInfo(types, baseUrl() + ProducerSimulatorController.JOB_CREATED_ERROR_URL,
            baseUrl() + ProducerSimulatorController.JOB_DELETED_ERROR_URL);
    }

    ProducerRegistrationInfo producerEiRegistratioInfo(String typeId)
        throws JsonMappingException, JsonProcessingException {
        Collection<ProducerEiTypeRegistrationInfo> types = new ArrayList<>();
        types.add(producerEiTypeRegistrationInfo(typeId));
        return new ProducerRegistrationInfo(types, baseUrl() + ProducerSimulatorController.JOB_CREATED_URL,
            baseUrl() + ProducerSimulatorController.JOB_DELETED_URL);
    }

    ConsumerEiJobInfo eiJobInfo() throws JsonMappingException, JsonProcessingException {
        return new ConsumerEiJobInfo(jsonObject(), "owner", "targetUri");
    }

    Object jsonObject(String json) {
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            throw new NullPointerException(e.toString());
        }
    }

    Object jsonSchemaObject() {
        // a json schema with one mandatory property named "string"
        String schemaStr = "{" //
            + "\"$schema\": \"http://json-schema.org/draft-04/schema#\"," //
            + "\"type\": \"object\"," //
            + "\"properties\": {" //
            + EI_JOB_PROPERTY + " : {" //
            + "    \"type\": \"string\"" //
            + "  }" //
            + "}," //
            + "\"required\": [" //
            + EI_JOB_PROPERTY //
            + "]" //
            + "}"; //
        return jsonObject(schemaStr);
    }

    Object jsonObject() {
        return jsonObject("{ " + EI_JOB_PROPERTY + " : \"value\" }");
    }

    private EiJob putEiJob(String eiTypeId, String jobId)
        throws JsonMappingException, JsonProcessingException, ServiceException {

        String url = ConsumerConsts.API_ROOT + "/eitypes/" + eiTypeId + "/eijobs/" + jobId;
        String body = gson.toJson(eiJobInfo());
        restClient().putForEntity(url, body).block();

        return this.eiJobs.getJob(jobId);
    }

    private EiType putEiProducerWithOneTypeRejecting(String producerId, String eiTypeId)
        throws JsonMappingException, JsonProcessingException, ServiceException {
        String url = ProducerConsts.API_ROOT + "/eiproducers/" + producerId;
        String body = gson.toJson(producerEiRegistratioInfoRejecting(eiTypeId));

        restClient().putForEntity(url, body).block();
        return this.eiTypes.getType(eiTypeId);
    }

    private EiType putEiProducerWithOneType(String producerId, String eiTypeId)
        throws JsonMappingException, JsonProcessingException, ServiceException {
        String url = ProducerConsts.API_ROOT + "/eiproducers/" + producerId;
        String body = gson.toJson(producerEiRegistratioInfo(eiTypeId));

        restClient().putForEntity(url, body).block();
        return this.eiTypes.getType(eiTypeId);
    }

    private String baseUrl() {
        return "https://localhost:" + this.port;
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
