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

import org.oransc.enrichment.controllers.VoidResponse;
import org.oransc.enrichment.controllers.a1e.A1eConsts;
import org.oransc.enrichment.controllers.a1e.A1eEiJobStatus;
import org.oransc.enrichment.controllers.r1consumer.ConsumerConsts;
import org.oransc.enrichment.controllers.r1consumer.ConsumerTypeRegistrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("ConsumerSimulatorController")
public class ConsumerSimulatorController {

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static class TestResults {

        public List<A1eEiJobStatus> eiJobStatusCallbacks =
            Collections.synchronizedList(new ArrayList<A1eEiJobStatus>());
        public List<ConsumerTypeRegistrationInfo> typeRegistrationInfoCallbacks =
            Collections.synchronizedList(new ArrayList<ConsumerTypeRegistrationInfo>());

        public void reset() {
            eiJobStatusCallbacks.clear();
            typeRegistrationInfoCallbacks.clear();
        }
    }

    @Getter
    private TestResults testResults = new TestResults();

    public static String getJobStatusUrl(String infoJobId) {
        return "/example_dataconsumer/info_jobs/" + infoJobId + "/status";
    }

    @Tag(name = A1eConsts.CONSUMER_API_CALLBACKS_NAME)
    @PostMapping(
        path = "/example_dataconsumer/info_jobs/{infoJobId}/status",
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
        @PathVariable("infoJobId") String infoJobId, //
        @RequestBody A1eEiJobStatus status) {
        logger.info("Job status callback status: {} infoJobId: {}", status.state, infoJobId);
        this.testResults.eiJobStatusCallbacks.add(status);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private static final String TYPE_STATUS_CALLBACK_URL = "/example_dataconsumer/info_type_status";

    public static String getTypeStatusCallbackUrl() {
        return TYPE_STATUS_CALLBACK_URL;
    }

    @Tag(name = ConsumerConsts.CONSUMER_API_CALLBACKS_NAME)
    @PostMapping(path = TYPE_STATUS_CALLBACK_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Callback for changed Information type registration status",
        description = "The primitive is implemented by the data consumer and is invoked when a Information type status has been changed. <br/>"
            + "Subscription are managed by primitives in '" + ConsumerConsts.CONSUMER_API_NAME + "'")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "OK", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))) //
        })
    public ResponseEntity<Object> typeStatusCallback( //
        @RequestBody ConsumerTypeRegistrationInfo status) {
        logger.info("Job type registration status callback status: {}", status);
        this.testResults.typeRegistrationInfoCallbacks.add(status);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
