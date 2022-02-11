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

import lombok.Getter;

import org.oransc.ics.controllers.VoidResponse;
import org.oransc.ics.controllers.a1e.A1eConsts;
import org.oransc.ics.controllers.a1e.A1eEiJobStatus;
import org.oransc.ics.controllers.r1consumer.ConsumerConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("A1eCallbacksSimulatorController")
@Tag(name = A1eConsts.CONSUMER_API_CALLBACKS_NAME, description = A1eConsts.CONSUMER_API_CALLBACKS_DESCRIPTION)
public class A1eCallbacksSimulatorController {

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static class TestResults {

        public List<A1eEiJobStatus> eiJobStatusCallbacks =
            Collections.synchronizedList(new ArrayList<A1eEiJobStatus>());

        public void reset() {
            eiJobStatusCallbacks.clear();
        }
    }

    @Getter
    private TestResults testResults = new TestResults();

    public static String getJobStatusUrl(String infoJobId) {
        return "/example-dataconsumer/info-jobs/" + infoJobId + "/status";
    }

    @PostMapping(
        path = "/example-dataconsumer/info-jobs/{infoJobId}/status",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Callback for changed Information Job status",
        description = "The primitive is implemented by the data consumer and is invoked when a Information Job status has been changed.")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "OK", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))) //
        })
    public ResponseEntity<Object> jobStatusCallback( //
        @PathVariable(ConsumerConsts.INFO_JOB_ID_PATH) String infoJobId, //
        @RequestBody A1eEiJobStatus status) {
        logger.info("Job status callback status: {} infoJobId: {}", status.state, infoJobId);
        this.testResults.eiJobStatusCallbacks.add(status);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
