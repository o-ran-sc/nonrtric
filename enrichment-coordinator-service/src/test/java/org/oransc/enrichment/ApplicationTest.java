/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2020 Nordix Foundation
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

package org.oransc.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.oransc.enrichment.clients.AsyncRestClient;
import org.oransc.enrichment.clients.AsyncRestClientFactory;
import org.oransc.enrichment.configuration.ApplicationConfig;
import org.oransc.enrichment.configuration.ImmutableWebClientConfig;
import org.oransc.enrichment.configuration.WebClientConfig;
import org.oransc.enrichment.controller.ConsumerSimulatorController;
import org.oransc.enrichment.controller.ProducerSimulatorController;
import org.oransc.enrichment.controllers.consumer.ConsumerConsts;
import org.oransc.enrichment.controllers.consumer.ConsumerEiJobInfo;
import org.oransc.enrichment.controllers.consumer.ConsumerEiJobStatus;
import org.oransc.enrichment.controllers.consumer.ConsumerEiTypeInfo;
import org.oransc.enrichment.controllers.producer.ProducerConsts;
import org.oransc.enrichment.controllers.producer.ProducerJobInfo;
import org.oransc.enrichment.controllers.producer.ProducerRegistrationInfo;
import org.oransc.enrichment.controllers.producer.ProducerRegistrationInfo.ProducerEiTypeRegistrationInfo;
import org.oransc.enrichment.controllers.producer.ProducerStatusInfo;
import org.oransc.enrichment.exceptions.ServiceException;
import org.oransc.enrichment.repository.EiJob;
import org.oransc.enrichment.repository.EiJobs;
import org.oransc.enrichment.repository.EiProducers;
import org.oransc.enrichment.repository.EiType;
import org.oransc.enrichment.repository.EiTypes;
import org.oransc.enrichment.tasks.ProducerSupervision;
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
        "app.webclient.trust-store=./config/truststore.jks", //
        "app.vardata-directory=./target"})
class ApplicationTest {
    private final String EI_TYPE_ID = "typeId";
    private final String EI_PRODUCER_ID = "producerId";
    private final String EI_JOB_PROPERTY = "\"property1\"";
    private final String EI_JOB_ID = "jobId";

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

    @Autowired
    ConsumerSimulatorController consumerSimulator;

    @Autowired
    ProducerSupervision producerSupervision;

    private static Gson gson = new GsonBuilder().create();

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
        this.consumerSimulator.getTestResults().reset();
    }

    @AfterEach
    void check() {
        assertThat(this.producerSimulator.getTestResults().errorFound).isFalse();
    }

    @Test
    void createApiDoc() throws FileNotFoundException {
        String url = "/v2/api-docs";
        ResponseEntity<String> resp = restClient().getForEntity(url).block();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        String indented = (new JSONObject(resp.getBody())).toString(4);
        try (PrintStream out = new PrintStream(new FileOutputStream("docs/api.json"))) {
            out.print(indented);
        }
    }

    @Test
    void testGetEiTypes() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, "test");
        String url = ConsumerConsts.API_ROOT + "/eitypes";
        String rsp = restClient().get(url).block();
        assertThat(rsp).isEqualTo("[\"test\"]");
    }

    @Test
    void testGetEiTypesEmpty() throws Exception {
        String url = ConsumerConsts.API_ROOT + "/eitypes";
        String rsp = restClient().get(url).block();
        assertThat(rsp).isEqualTo("[]");
    }

    @Test
    void testGetEiType() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, "test");
        String url = ConsumerConsts.API_ROOT + "/eitypes/test";
        String rsp = restClient().get(url).block();
        ConsumerEiTypeInfo info = gson.fromJson(rsp, ConsumerEiTypeInfo.class);
        assertThat(info).isNotNull();
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
        final String JOB_ID_JSON = "[\"jobId\"]";
        String url = ConsumerConsts.API_ROOT + "/eijobs?eiTypeId=typeId";
        String rsp = restClient().get(url).block();
        assertThat(rsp).isEqualTo(JOB_ID_JSON);

        url = ConsumerConsts.API_ROOT + "/eijobs?owner=owner";
        rsp = restClient().get(url).block();
        assertThat(rsp).isEqualTo(JOB_ID_JSON);

        url = ConsumerConsts.API_ROOT + "/eijobs?owner=JUNK";
        rsp = restClient().get(url).block();
        assertThat(rsp).isEqualTo("[]");

        url = ConsumerConsts.API_ROOT + "/eijobs";
        rsp = restClient().get(url).block();
        assertThat(rsp).isEqualTo(JOB_ID_JSON);

        url = ConsumerConsts.API_ROOT + "/eijobs?eiTypeId=typeId&&owner=owner";
        rsp = restClient().get(url).block();
        assertThat(rsp).isEqualTo(JOB_ID_JSON);

        url = ConsumerConsts.API_ROOT + "/eijobs?eiTypeId=JUNK";
        rsp = restClient().get(url).block();
        assertThat(rsp).isEqualTo("[]");
    }

    @Test
    void testGetEiJob() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        putEiJob(EI_TYPE_ID, "jobId");
        String url = ConsumerConsts.API_ROOT + "/eijobs/jobId";
        String rsp = restClient().get(url).block();
        ConsumerEiJobInfo info = gson.fromJson(rsp, ConsumerEiJobInfo.class);
        assertThat(info.owner).isEqualTo("owner");
        assertThat(info.eiTypeId).isEqualTo(EI_TYPE_ID);
    }

    @Test
    void testGetEiJobNotFound() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        String url = ConsumerConsts.API_ROOT + "/eijobs/junk";
        testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND, "Could not find EI job: junk");
    }

    @Test
    void testGetEiJobStatus() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        putEiJob(EI_TYPE_ID, "jobId");

        verifyJobStatus("jobId", "ENABLED");
    }

    @Test
    void testDeleteEiJob() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        putEiJob(EI_TYPE_ID, "jobId");
        assertThat(this.eiJobs.size()).isEqualTo(1);
        String url = ConsumerConsts.API_ROOT + "/eijobs/jobId";
        restClient().delete(url).block();
        assertThat(this.eiJobs.size()).isZero();

        ProducerSimulatorController.TestResults simulatorResults = this.producerSimulator.getTestResults();
        await().untilAsserted(() -> assertThat(simulatorResults.jobsStopped.size()).isEqualTo(1));
        assertThat(simulatorResults.jobsStopped.get(0)).isEqualTo("jobId");
    }

    @Test
    void testDeleteEiJobNotFound() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        String url = ConsumerConsts.API_ROOT + "/eijobs/junk";
        testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND, "Could not find EI job: junk");
    }

    @Test
    void testPutEiJob() throws Exception {
        // Test that one producer accepting a job is enough
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        putEiProducerWithOneTypeRejecting("simulateProducerError", EI_TYPE_ID);

        String url = ConsumerConsts.API_ROOT + "/eijobs/jobId";
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
        String url = ConsumerConsts.API_ROOT + "/eijobs/jobId";
        String body = gson.toJson(eiJobInfo());
        testErrorCode(restClient().put(url, body), HttpStatus.CONFLICT, "Job not accepted by any producers");

        ProducerSimulatorController.TestResults simulatorResults = this.producerSimulator.getTestResults();
        assertThat(simulatorResults.noOfRejectedCreate).isEqualTo(1);
    }

    @Test
    void testPutEiJob_jsonSchemavalidationError() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);

        String url = ConsumerConsts.API_ROOT + "/eijobs/jobId";
        // The element with name "property1" is mandatory in the schema
        ConsumerEiJobInfo jobInfo = new ConsumerEiJobInfo("typeId", jsonObject("{ \"XXstring\" : \"value\" }"), "owner",
            "targetUri", "jobStatusUrl");
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

        String url = ConsumerConsts.API_ROOT + "/eijobs/jobId";
        String body = gson.toJson(eiJobInfo("typeId2", "jobId"));
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
        assertThat(this.eiProducers.get("eiProducerId").getEiTypes().iterator().next().getId()).isEqualTo(EI_TYPE_ID);

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

        url = ConsumerConsts.API_ROOT + "/eijobs/jobId";
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

        deleteEiProducer("eiProducerId");
        assertThat(this.eiProducers.size()).isEqualTo(1);
        assertThat(this.eiTypes.getType(EI_TYPE_ID).getProducerIds()).doesNotContain("eiProducerId");
        verifyJobStatus("jobId", "ENABLED");

        deleteEiProducer("eiProducerId2");
        assertThat(this.eiProducers.size()).isZero();
        assertThat(this.eiTypes.size()).isZero();
        verifyJobStatus("jobId", "DISABLED");
    }

    @Test
    void testJobStatusNotifications() throws JsonMappingException, JsonProcessingException, ServiceException {
        putEiProducerWithOneType("eiProducerId", EI_TYPE_ID);
        putEiJob(EI_TYPE_ID, "jobId");

        deleteEiProducer("eiProducerId");
        assertThat(this.eiTypes.size()).isZero(); // The type is gone
        assertThat(this.eiJobs.size()).isEqualTo(1); // The job remains
        ConsumerSimulatorController.TestResults consumerResults = this.consumerSimulator.getTestResults();
        await().untilAsserted(() -> assertThat(consumerResults.status.size()).isEqualTo(1));
        assertThat(consumerResults.status.get(0).state).isEqualTo(ConsumerEiJobStatus.EiJobStatusValues.DISABLED);

        putEiProducerWithOneType("eiProducerId", EI_TYPE_ID);
        await().untilAsserted(() -> assertThat(consumerResults.status.size()).isEqualTo(2));
        assertThat(consumerResults.status.get(1).state).isEqualTo(ConsumerEiJobStatus.EiJobStatusValues.ENABLED);
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

    @Test
    void testProducerSupervision() throws JsonMappingException, JsonProcessingException, ServiceException {
        putEiProducerWithOneTypeRejecting("simulateProducerError", EI_TYPE_ID);

        {
            // Create a job
            putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
            putEiJob(EI_TYPE_ID, "jobId");
            deleteEiProducer(EI_PRODUCER_ID);
        }

        assertThat(this.eiProducers.size()).isEqualTo(1);
        assertThat(this.eiTypes.size()).isEqualTo(1);
        assertProducerOpState("simulateProducerError", ProducerStatusInfo.OperationalState.ENABLED);

        this.producerSupervision.createTask().blockLast();
        this.producerSupervision.createTask().blockLast();
        assertThat(this.eiProducers.size()).isEqualTo(1);
        assertProducerOpState("simulateProducerError", ProducerStatusInfo.OperationalState.DISABLED);

        // After 3 failed checks, the producer and the type shall be deregisterred
        this.producerSupervision.createTask().blockLast();
        assertThat(this.eiProducers.size()).isEqualTo(0);
        assertThat(this.eiTypes.size()).isEqualTo(0);

        // Job disabled status notification shall be received
        ConsumerSimulatorController.TestResults consumerResults = this.consumerSimulator.getTestResults();
        await().untilAsserted(() -> assertThat(consumerResults.status.size()).isEqualTo(1));
        assertThat(consumerResults.status.get(0).state).isEqualTo(ConsumerEiJobStatus.EiJobStatusValues.DISABLED);
    }

    @Test
    void testGetStatus() throws JsonMappingException, JsonProcessingException, ServiceException {
        putEiProducerWithOneTypeRejecting("simulateProducerError", EI_TYPE_ID);
        putEiProducerWithOneTypeRejecting("simulateProducerError2", EI_TYPE_ID);

        String url = "/status";
        ResponseEntity<String> resp = restClient().getForEntity(url).block();
        assertThat(resp.getBody()).contains("hunky dory");
    }

    @Test
    void testEiJobDatabase() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        putEiJob(EI_TYPE_ID, "jobId1");
        putEiJob(EI_TYPE_ID, "jobId2");

        assertThat(this.eiJobs.size()).isEqualTo(2);

        {
            // Restore the jobs
            EiJobs jobs = new EiJobs(this.applicationConfig);
            jobs.restoreJobsFromDatabase();
            assertThat(jobs.size()).isEqualTo(2);
            jobs.remove("jobId1");
            jobs.remove("jobId2");
        }
        {
            // Restore the jobs, no jobs in database
            EiJobs jobs = new EiJobs(this.applicationConfig);
            jobs.restoreJobsFromDatabase();
            assertThat(jobs.size()).isEqualTo(0);
        }

        this.eiJobs.remove("jobId1"); // removing a job when the db file is gone
        assertThat(this.eiJobs.size()).isEqualTo(1);
    }

    private void deleteEiProducer(String eiProducerId) {
        String url = ProducerConsts.API_ROOT + "/eiproducers/" + eiProducerId;
        restClient().deleteForEntity(url).block();
    }

    private void verifyJobStatus(String jobId, String expStatus) {
        String url = ConsumerConsts.API_ROOT + "/eijobs/" + jobId + "/status";
        String rsp = restClient().get(url).block();
        assertThat(rsp).contains(expStatus);
    }

    private void assertProducerOpState(String producerId,
        ProducerStatusInfo.OperationalState expectedOperationalState) {
        String statusUrl = ProducerConsts.API_ROOT + "/eiproducers/" + producerId + "/status";
        ResponseEntity<String> resp = restClient().getForEntity(statusUrl).block();
        ProducerStatusInfo statusInfo = gson.fromJson(resp.getBody(), ProducerStatusInfo.class);
        assertThat(statusInfo.opState).isEqualTo(expectedOperationalState);
    }

    ProducerEiTypeRegistrationInfo producerEiTypeRegistrationInfo(String typeId)
        throws JsonMappingException, JsonProcessingException {
        return new ProducerEiTypeRegistrationInfo(jsonSchemaObject(), typeId);
    }

    ProducerRegistrationInfo producerEiRegistratioInfoRejecting(String typeId)
        throws JsonMappingException, JsonProcessingException {
        Collection<ProducerEiTypeRegistrationInfo> types = new ArrayList<>();
        types.add(producerEiTypeRegistrationInfo(typeId));
        return new ProducerRegistrationInfo(types, //
            baseUrl() + ProducerSimulatorController.JOB_ERROR_URL,
            baseUrl() + ProducerSimulatorController.SUPERVISION_ERROR_URL);
    }

    ProducerRegistrationInfo producerEiRegistratioInfo(String typeId)
        throws JsonMappingException, JsonProcessingException {
        Collection<ProducerEiTypeRegistrationInfo> types = new ArrayList<>();
        types.add(producerEiTypeRegistrationInfo(typeId));
        return new ProducerRegistrationInfo(types, //
            baseUrl() + ProducerSimulatorController.JOB_URL, baseUrl() + ProducerSimulatorController.SUPERVISION_URL);
    }

    private ConsumerEiJobInfo eiJobInfo() throws JsonMappingException, JsonProcessingException {
        return eiJobInfo(EI_TYPE_ID, EI_JOB_ID);
    }

    ConsumerEiJobInfo eiJobInfo(String typeId, String eiJobId) throws JsonMappingException, JsonProcessingException {
        return new ConsumerEiJobInfo(typeId, jsonObject(), "owner", "targetUri",
            baseUrl() + ConsumerSimulatorController.getJobStatusUrl(eiJobId));
    }

    private Object jsonObject(String json) {
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            throw new NullPointerException(e.toString());
        }
    }

    private Object jsonSchemaObject() {
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

    private Object jsonObject() {
        return jsonObject("{ " + EI_JOB_PROPERTY + " : \"value\" }");
    }

    private EiJob putEiJob(String eiTypeId, String jobId)
        throws JsonMappingException, JsonProcessingException, ServiceException {

        String url = ConsumerConsts.API_ROOT + "/eijobs/" + jobId;
        String body = gson.toJson(eiJobInfo(eiTypeId, jobId));
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

        AsyncRestClientFactory restClientFactory = new AsyncRestClientFactory(config);
        return restClientFactory.createRestClient(baseUrl());
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
