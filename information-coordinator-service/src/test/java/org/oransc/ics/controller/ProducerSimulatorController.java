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

package org.oransc.ics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.Getter;

import org.oransc.ics.controllers.ErrorResponse;
import org.oransc.ics.controllers.VoidResponse;
import org.oransc.ics.controllers.r1consumer.ConsumerConsts;
import org.oransc.ics.controllers.r1producer.ProducerConsts;
import org.oransc.ics.controllers.r1producer.ProducerJobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController("ProducerSimulatorController")
@Tag(name = ProducerConsts.PRODUCER_API_CALLBACKS_NAME, description = ProducerConsts.PRODUCER_API_CALLBACKS_DESCRIPTION)
public class ProducerSimulatorController {

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String JOB_URL = "/example-dataproducer/info-job";
    public static final String JOB_ERROR_URL = "/example-dataproducer/info-job-error";

    public static final String SUPERVISION_URL = "/example-dataproducer/health-check";
    public static final String SUPERVISION_ERROR_URL = "/example-dataproducer/health-check-error";

    public static class TestResults {

        public List<ProducerJobInfo> jobsStarted = Collections.synchronizedList(new ArrayList<ProducerJobInfo>());
        public List<String> jobsStopped = Collections.synchronizedList(new ArrayList<String>());
        public List<Map<String, String>> receivedHeaders =
            Collections.synchronizedList(new ArrayList<Map<String, String>>());
        public int noOfRejectedCreate = 0;
        public int noOfRejectedDelete = 0;
        public boolean errorFound = false;

        public TestResults() {
        }

        public void reset() {
            jobsStarted.clear();
            jobsStopped.clear();
            receivedHeaders.clear();
            this.errorFound = false;
            this.noOfRejectedCreate = 0;
            this.noOfRejectedDelete = 0;
        }
    }

    @Getter
    private TestResults testResults = new TestResults();

    @PostMapping(path = JOB_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Callback for Information Job creation/modification",
        description = "The call is invoked to activate or to modify a data subscription. The endpoint is provided by the Information Producer.")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "OK", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))) //
        })
    public ResponseEntity<Object> jobCreatedCallback( //
        @RequestHeader Map<String, String> headers, //
        @RequestBody ProducerJobInfo request) {
        try {
            logHeaders(headers);
            this.testResults.jobsStarted.add(request);
            this.testResults.receivedHeaders.add(headers);
            logger.info("Job started callback {}", request.id);
            if (request.id == null) {
                throw new NullPointerException("Illegal argument");
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            this.testResults.errorFound = true;
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(path = JOB_URL + "/{infoJobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Callback for Information Job deletion",
        description = "The call is invoked to terminate a data subscription. The endpoint is provided by the Information Producer.")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "OK", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))) //
        })
    public ResponseEntity<Object> jobDeletedCallback( //
        @RequestHeader Map<String, String> headers, @PathVariable(ConsumerConsts.INFO_JOB_ID_PATH) String infoJobId) {
        try {
            logHeaders(headers);
            logger.info("Job deleted callback {}", infoJobId);
            this.testResults.jobsStopped.add(infoJobId);
            this.testResults.receivedHeaders.add(headers);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(path = JOB_ERROR_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Callback for Information Job creation, returns error", description = "", hidden = true)
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "OK", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))) //
        })
    public ResponseEntity<Object> jobCreatedCallbackReturnError( //
        @RequestBody ProducerJobInfo request) {
        logger.info("Job created (returning error) callback {}", request.id);
        this.testResults.noOfRejectedCreate += 1;
        return ErrorResponse.create("Producer returns error on create job", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping(path = JOB_ERROR_URL + "/{infoJobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Callback for Information Job deletion, returns error", description = "", hidden = true)
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "OK", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))) //
        })
    public ResponseEntity<Object> jobDeletedCallbackReturnError( //
        @PathVariable(ConsumerConsts.INFO_JOB_ID_PATH) String infoJobId) {
        logger.info("Job created (returning error) callback {}", infoJobId);
        this.testResults.noOfRejectedDelete += 1;
        return ErrorResponse.create("Producer returns error on delete job", HttpStatus.NOT_FOUND);
    }

    @GetMapping(path = SUPERVISION_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Producer supervision",
        description = "The endpoint is provided by the Information Producer and is used for supervision of the producer.")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "The producer is OK", //
                content = @Content(schema = @Schema(implementation = String.class))) //
        })
    public ResponseEntity<Object> producerSupervision() {
        logger.info("Producer supervision");
        return new ResponseEntity<>("Hunky dory", HttpStatus.OK);
    }

    @GetMapping(path = SUPERVISION_ERROR_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Producer supervision error", description = "", hidden = true)
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "OK", //
                content = @Content(schema = @Schema(implementation = String.class))) //
        })
    public ResponseEntity<Object> producerSupervisionError() {
        logger.info("Producer supervision error");
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    private void logHeaders(Map<String, String> headers) {
        logger.debug("Header begin");
        headers.forEach((key, value) -> logger.debug("  key: {}, value: {}", key, value));
        logger.debug("Header end");
    }

}
