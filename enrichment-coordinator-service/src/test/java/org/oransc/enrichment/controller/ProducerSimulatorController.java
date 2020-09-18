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

import org.oransc.enrichment.clients.ProducerJobInfo;
import org.oransc.enrichment.controllers.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("ProducerSimulatorController")
@Api(tags = {"Producer Simulator"})
public class ProducerSimulatorController {

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String JOB_CREATED_URL = "/producer_simulator/job_created";
    public static final String JOB_DELETED_URL = "/producer_simulator/job_deleted";

    public static class TestResults {

        public List<ProducerJobInfo> jobsStarted = Collections.synchronizedList(new ArrayList<ProducerJobInfo>());
        public List<ProducerJobInfo> jobsStopped = Collections.synchronizedList(new ArrayList<ProducerJobInfo>());
        public boolean errorFound = false;

        public TestResults() {
        }

        public void reset() {
            jobsStarted.clear();
            jobsStopped.clear();
            this.errorFound = false;
        }
    }

    @Getter
    private TestResults testResults = new TestResults();

    @PostMapping(path = JOB_CREATED_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Callback for EI job creation", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "OK", response = void.class)}//
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

    @PostMapping(path = JOB_DELETED_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Callback for EI job deletion", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "OK", response = void.class)}//
    )
    public ResponseEntity<Object> jobDeletedCallback( //
        @RequestBody ProducerJobInfo request) {
        try {
            logger.info("Job deleted callback {}", request.id);
            this.testResults.jobsStopped.add(request);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

}
