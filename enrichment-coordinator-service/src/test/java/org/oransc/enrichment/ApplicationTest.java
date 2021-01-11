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
import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.oransc.enrichment.clients.AsyncRestClient;
import org.oransc.enrichment.clients.AsyncRestClientFactory;
import org.oransc.enrichment.configuration.ApplicationConfig;
import org.oransc.enrichment.configuration.ImmutableHttpProxyConfig;
import org.oransc.enrichment.configuration.ImmutableWebClientConfig;
import org.oransc.enrichment.configuration.WebClientConfig;
import org.oransc.enrichment.configuration.WebClientConfig.HttpProxyConfig;
import org.oransc.enrichment.controller.ConsumerSimulatorController;
import org.oransc.enrichment.controller.ProducerSimulatorController;
import org.oransc.enrichment.controllers.consumer.ConsumerConsts;
import org.oransc.enrichment.controllers.consumer.ConsumerEiJobInfo;
import org.oransc.enrichment.controllers.consumer.ConsumerEiJobStatus;
import org.oransc.enrichment.controllers.consumer.ConsumerEiTypeInfo;
import org.oransc.enrichment.controllers.producer.ProducerCallbacks;
import org.oransc.enrichment.controllers.producer.ProducerConsts;
import org.oransc.enrichment.controllers.producer.ProducerEiTypeInfo;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

    @Autowired
    ProducerCallbacks producerCallbacks;

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

        JSONObject jsonObj = new JSONObject(resp.getBody());
        jsonObj.remove("host");
        String indented = jsonObj.toString(4);
        try (PrintStream out = new PrintStream(new FileOutputStream("api/ecs-api.json"))) {
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
    void testPutEiType() throws JsonMappingException, JsonProcessingException, ServiceException {
        assertThat(putEiType(EI_TYPE_ID)).isEqualTo(HttpStatus.CREATED);
        assertThat(putEiType(EI_TYPE_ID)).isEqualTo(HttpStatus.OK);
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
    void testDeleteEiType() throws Exception {
        putEiType(EI_TYPE_ID);
        String url = ProducerConsts.API_ROOT + "/eitypes/" + EI_TYPE_ID;
        restClient().delete(url).block();
        assertThat(this.eiTypes.size()).isEqualTo(0);
    }

    @Test
    void testDeleteEiTypeExistingProducer() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        String url = ProducerConsts.API_ROOT + "/eitypes/" + EI_TYPE_ID;
        testErrorCode(restClient().delete(url), HttpStatus.NOT_ACCEPTABLE,
            "The type has active producers: " + EI_PRODUCER_ID);
        assertThat(this.eiTypes.size()).isEqualTo(1);
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

        // One retry --> two calls
        await().untilAsserted(() -> assertThat(simulatorResults.noOfRejectedCreate).isEqualTo(2));
        assertThat(simulatorResults.noOfRejectedCreate).isEqualTo(2);

        resp = restClient().putForEntity(url, body).block();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        EiJob job = this.eiJobs.getJob("jobId");
        assertThat(job.getOwner()).isEqualTo("owner");

        verifyJobStatus(EI_JOB_ID, "ENABLED");
    }

    @Test
    void putEiProducerWithOneType_rejecting() throws JsonMappingException, JsonProcessingException, ServiceException {
        putEiProducerWithOneTypeRejecting("simulateProducerError", EI_TYPE_ID);
        String url = ConsumerConsts.API_ROOT + "/eijobs/" + EI_JOB_ID;
        String body = gson.toJson(eiJobInfo());
        restClient().put(url, body).block();

        ProducerSimulatorController.TestResults simulatorResults = this.producerSimulator.getTestResults();
        // There is one retry -> 2 calls
        await().untilAsserted(() -> assertThat(simulatorResults.noOfRejectedCreate).isEqualTo(2));
        assertThat(simulatorResults.noOfRejectedCreate).isEqualTo(2);

        verifyJobStatus(EI_JOB_ID, "DISABLED");

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
        this.putEiType(EI_TYPE_ID);
        String url = ProducerConsts.API_ROOT + "/eiproducers/eiProducerId";
        String body = gson.toJson(producerEiRegistratioInfo(EI_TYPE_ID));

        ResponseEntity<String> resp = restClient().putForEntity(url, body).block();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(this.eiTypes.size()).isEqualTo(1);
        assertThat(this.eiProducers.getProducersForType(EI_TYPE_ID).size()).isEqualTo(1);
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
        this.putEiType(EI_TYPE_ID);
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
        assertThat(this.eiProducers.getProducerIdsForType(type.getId())).contains("eiProducerId");
        assertThat(this.eiProducers.getProducerIdsForType(type.getId())).contains("eiProducerId2");
        putEiJob(EI_TYPE_ID, "jobId");
        assertThat(this.eiJobs.size()).isEqualTo(1);

        deleteEiProducer("eiProducerId");
        assertThat(this.eiProducers.size()).isEqualTo(1);
        assertThat(this.eiProducers.getProducerIdsForType(EI_TYPE_ID)).doesNotContain("eiProducerId");
        verifyJobStatus("jobId", "ENABLED");

        deleteEiProducer("eiProducerId2");
        assertThat(this.eiProducers.size()).isZero();
        assertThat(this.eiTypes.size()).isEqualTo(1);
        verifyJobStatus("jobId", "DISABLED");
    }

    @Test
    void testJobStatusNotifications() throws JsonMappingException, JsonProcessingException, ServiceException {
        ConsumerSimulatorController.TestResults consumerCalls = this.consumerSimulator.getTestResults();
        ProducerSimulatorController.TestResults producerCalls = this.producerSimulator.getTestResults();

        putEiProducerWithOneType("eiProducerId", EI_TYPE_ID);
        putEiJob(EI_TYPE_ID, "jobId");
        putEiProducerWithOneType("eiProducerId2", EI_TYPE_ID);
        await().untilAsserted(() -> assertThat(producerCalls.jobsStarted.size()).isEqualTo(2));

        deleteEiProducer("eiProducerId2");
        assertThat(this.eiTypes.size()).isEqualTo(1); // The type remains, one producer left
        deleteEiProducer("eiProducerId");
        assertThat(this.eiTypes.size()).isEqualTo(1); // The type remains
        assertThat(this.eiJobs.size()).isEqualTo(1); // The job remains
        await().untilAsserted(() -> assertThat(consumerCalls.status.size()).isEqualTo(1));
        assertThat(consumerCalls.status.get(0).state).isEqualTo(ConsumerEiJobStatus.EiJobStatusValues.DISABLED);

        putEiProducerWithOneType("eiProducerId", EI_TYPE_ID);
        await().untilAsserted(() -> assertThat(consumerCalls.status.size()).isEqualTo(2));
        assertThat(consumerCalls.status.get(1).state).isEqualTo(ConsumerEiJobStatus.EiJobStatusValues.ENABLED);
    }

    @Test
    void testJobStatusNotifications2() throws JsonMappingException, JsonProcessingException, ServiceException {
        // Test replacing a producer with new and removed types

        // Create a job
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        putEiJob(EI_TYPE_ID, EI_JOB_ID);

        // change the type for the producer, the job shall be disabled
        putEiProducerWithOneType(EI_PRODUCER_ID, "junk");
        verifyJobStatus(EI_JOB_ID, "DISABLED");
        ConsumerSimulatorController.TestResults consumerCalls = this.consumerSimulator.getTestResults();
        await().untilAsserted(() -> assertThat(consumerCalls.status.size()).isEqualTo(1));
        assertThat(consumerCalls.status.get(0).state).isEqualTo(ConsumerEiJobStatus.EiJobStatusValues.DISABLED);

        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        verifyJobStatus(EI_JOB_ID, "ENABLED");
        await().untilAsserted(() -> assertThat(consumerCalls.status.size()).isEqualTo(2));
        assertThat(consumerCalls.status.get(1).state).isEqualTo(ConsumerEiJobStatus.EiJobStatusValues.ENABLED);
    }

    @Test
    void testGetProducerEiType() throws JsonMappingException, JsonProcessingException, ServiceException {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);
        String url = ProducerConsts.API_ROOT + "/eitypes/" + EI_TYPE_ID;
        ResponseEntity<String> resp = restClient().getForEntity(url).block();
        ProducerEiTypeInfo info = gson.fromJson(resp.getBody(), ProducerEiTypeInfo.class);
        assertThat(info.jobDataSchema).isNotNull();
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
            putEiJob(EI_TYPE_ID, EI_JOB_ID);
            verifyJobStatus(EI_JOB_ID, "ENABLED");
            deleteEiProducer(EI_PRODUCER_ID);
            verifyJobStatus(EI_JOB_ID, "DISABLED");
        }

        // Job disabled status notification shall be received
        ConsumerSimulatorController.TestResults consumerResults = this.consumerSimulator.getTestResults();
        await().untilAsserted(() -> assertThat(consumerResults.status.size()).isEqualTo(1));
        assertThat(consumerResults.status.get(0).state).isEqualTo(ConsumerEiJobStatus.EiJobStatusValues.DISABLED);

        assertThat(this.eiProducers.size()).isEqualTo(1);
        assertThat(this.eiTypes.size()).isEqualTo(1);
        assertProducerOpState("simulateProducerError", ProducerStatusInfo.OperationalState.ENABLED);

        this.producerSupervision.createTask().blockLast();
        this.producerSupervision.createTask().blockLast();

        // Now we have one producer that is disabled
        assertThat(this.eiProducers.size()).isEqualTo(1);
        assertProducerOpState("simulateProducerError", ProducerStatusInfo.OperationalState.DISABLED);

        // After 3 failed checks, the producer and the type shall be deregisterred
        this.producerSupervision.createTask().blockLast();
        assertThat(this.eiProducers.size()).isEqualTo(0); // The producer is removed
        assertThat(this.eiTypes.size()).isEqualTo(1); // The type remains

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
            EiJobs jobs = new EiJobs(this.applicationConfig, this.producerCallbacks);
            jobs.restoreJobsFromDatabase();
            assertThat(jobs.size()).isEqualTo(2);
            jobs.remove("jobId1", this.eiProducers);
            jobs.remove("jobId2", this.eiProducers);
        }
        {
            // Restore the jobs, no jobs in database
            EiJobs jobs = new EiJobs(this.applicationConfig, this.producerCallbacks);
            jobs.restoreJobsFromDatabase();
            assertThat(jobs.size()).isEqualTo(0);
        }
        logger.warn("Test removing a job when the db file is gone");
        this.eiJobs.remove("jobId1", this.eiProducers);
        assertThat(this.eiJobs.size()).isEqualTo(1);

        ProducerSimulatorController.TestResults simulatorResults = this.producerSimulator.getTestResults();
        await().untilAsserted(() -> assertThat(simulatorResults.jobsStopped.size()).isEqualTo(3));
    }

    @Test
    void testEiTypesDatabase() throws Exception {
        putEiProducerWithOneType(EI_PRODUCER_ID, EI_TYPE_ID);

        assertThat(this.eiTypes.size()).isEqualTo(1);

        {
            // Restore the types
            EiTypes types = new EiTypes(this.applicationConfig);
            types.restoreTypesFromDatabase();
            assertThat(types.size()).isEqualTo(1);

        }
        {
            // Restore the jobs, no jobs in database
            EiTypes types = new EiTypes(this.applicationConfig);
            types.clear();
            types.restoreTypesFromDatabase();
            assertThat(types.size()).isEqualTo(0);
        }
        logger.warn("Test removing a job when the db file is gone");
        this.eiTypes.remove(this.eiTypes.getType(EI_TYPE_ID));
        assertThat(this.eiJobs.size()).isEqualTo(0);
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
        return new ProducerRegistrationInfo(Arrays.asList(typeId), //
            baseUrl() + ProducerSimulatorController.JOB_ERROR_URL,
            baseUrl() + ProducerSimulatorController.SUPERVISION_ERROR_URL);
    }

    ProducerRegistrationInfo producerEiRegistratioInfo(String typeId)
        throws JsonMappingException, JsonProcessingException {
        return new ProducerRegistrationInfo(Arrays.asList(typeId), //
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

    private HttpStatus putEiType(String eiTypeId)
        throws JsonMappingException, JsonProcessingException, ServiceException {
        String url = ProducerConsts.API_ROOT + "/eitypes/" + eiTypeId;
        String body = gson.toJson(producerEiTypeRegistrationInfo(eiTypeId));
        ResponseEntity<String> resp = restClient().putForEntity(url, body).block();
        this.eiTypes.getType(eiTypeId);
        return resp.getStatusCode();

    }

    private EiType putEiProducerWithOneTypeRejecting(String producerId, String eiTypeId)
        throws JsonMappingException, JsonProcessingException, ServiceException {
        this.putEiType(eiTypeId);
        String url = ProducerConsts.API_ROOT + "/eiproducers/" + producerId;
        String body = gson.toJson(producerEiRegistratioInfoRejecting(eiTypeId));
        restClient().putForEntity(url, body).block();
        return this.eiTypes.getType(eiTypeId);
    }

    private EiType putEiProducerWithOneType(String producerId, String eiTypeId)
        throws JsonMappingException, JsonProcessingException, ServiceException {
        this.putEiType(eiTypeId);

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
        return restClientFactory.createRestClientNoHttpProxy(baseUrl());
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
