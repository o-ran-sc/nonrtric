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

package org.oransc.enrichment.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

import org.oransc.enrichment.controllers.ErrorResponse;
import org.oransc.enrichment.controllers.VoidResponse;
import org.oransc.enrichment.controllers.producer.ProducerJobInfo;
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
import org.springframework.web.bind.annotation.RestController;

@RestController("ProducerSimulatorController")
@Api(tags = {"Data Producer Callbacks"})
public class ProducerSimulatorController {

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String JOB_URL = "/producer_simulator/ei_job";
    public static final String JOB_ERROR_URL = "/producer_simulator/ei_job_error";

    public static final String SUPERVISION_URL = "/producer_simulator/health_check";
    public static final String SUPERVISION_ERROR_URL = "/producer_simulator/health_check_error";

    public static class TestResults {

        public List<ProducerJobInfo> jobsStarted = Collections.synchronizedList(new ArrayList<ProducerJobInfo>());
        public List<String> jobsStopped = Collections.synchronizedList(new ArrayList<String>());
        public int noOfRejectedCreate = 0;
        public int noOfRejectedDelete = 0;
        public boolean errorFound = false;

        public TestResults() {
        }

        public void reset() {
            jobsStarted.clear();
            jobsStopped.clear();
            this.errorFound = false;
            this.noOfRejectedCreate = 0;
            this.noOfRejectedDelete = 0;
        }
    }

    @Getter
    private TestResults testResults = new TestResults();

    @PostMapping(path = JOB_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Callback for EI job creation", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "OK", response = VoidResponse.class)}//
    )
    public ResponseEntity<Object> jobCreatedCallback( //
        @RequestBody ProducerJobInfo request) {
        try {
            this.testResults.jobsStarted.add(request);
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

    @DeleteMapping(path = "/producer_simulator/ei_job/{eiJobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Callback for EI job deletion", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "OK", response = VoidResponse.class)}//
    )
    public ResponseEntity<Object> jobDeletedCallback( //
        @PathVariable("eiJobId") String eiJobId) {
        try {
            logger.info("Job deleted callback {}", eiJobId);
            this.testResults.jobsStopped.add(eiJobId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(path = JOB_ERROR_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Callback for EI job creation, returns error", notes = "", hidden = true)
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "OK", response = VoidResponse.class)}//
    )
    public ResponseEntity<Object> jobCreatedCallbackReturnError( //
        @RequestBody ProducerJobInfo request) {
        logger.info("Job created (returning error) callback {}", request.id);
        this.testResults.noOfRejectedCreate += 1;
        return ErrorResponse.create("Producer returns error on create job", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping(path = JOB_ERROR_URL + "/{eiJobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Callback for EI job creation, returns error", notes = "", hidden = true)
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "OK", response = VoidResponse.class)}//
    )
    public ResponseEntity<Object> jobDeletedCallbackReturnError( //
        @RequestBody ProducerJobInfo request) {
        logger.info("Job created (returning error) callback {}", request.id);
        this.testResults.noOfRejectedDelete += 1;
        return ErrorResponse.create("Producer returns error on delete job", HttpStatus.NOT_FOUND);
    }

    @GetMapping(path = SUPERVISION_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Producer supervision", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "OK", response = String.class)}//
    )
    public ResponseEntity<Object> producerSupervision() {
        logger.info("Producer supervision");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(path = SUPERVISION_ERROR_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Producer supervision error", notes = "", hidden = true)
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "OK", response = String.class)}//
    )
    public ResponseEntity<Object> producerSupervisionError() {
        logger.info("Producer supervision error");
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

}
