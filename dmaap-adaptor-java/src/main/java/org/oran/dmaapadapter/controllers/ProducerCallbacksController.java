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

package org.oran.dmaapadapter.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.oran.dmaapadapter.r1.ProducerJobInfo;
import org.oran.dmaapadapter.repository.InfoTypes;
import org.oran.dmaapadapter.repository.Job;
import org.oran.dmaapadapter.repository.Jobs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("ConfigurationControllerV2")
@Tag(name = ProducerCallbacksController.API_NAME)
public class ProducerCallbacksController {
    private static final Logger logger = LoggerFactory.getLogger(ProducerCallbacksController.class);

    public static final String API_NAME = "Management of configuration";
    public static final String API_DESCRIPTION = "";
    public static final String JOB_URL = "/dmaap_dataproducer/info_job";
    public static final String SUPERVISION_URL = "/dmaap_dataproducer/health_check";
    private static Gson gson = new GsonBuilder().create();
    private final Jobs jobs;
    private final InfoTypes types;

    public ProducerCallbacksController(@Autowired Jobs jobs, @Autowired InfoTypes types) {
        this.jobs = jobs;
        this.types = types;
    }

    @PostMapping(path = JOB_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Callback for Information Job creation/modification",
            description = "The call is invoked to activate or to modify a data subscription. The endpoint is provided by the Information Producer.")
    @ApiResponses(value = { //
            @ApiResponse(responseCode = "200", description = "OK", //
                    content = @Content(schema = @Schema(implementation = VoidResponse.class))) //
    })
    public ResponseEntity<Object> jobCreatedCallback( //
            @RequestBody String body) {
        try {
            ProducerJobInfo request = gson.fromJson(body, ProducerJobInfo.class);

            logger.info("Job started callback {}", request.id);
            Job job = new Job(request.id, request.targetUri, types.getType(request.typeId));
            this.jobs.put(job);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(path = JOB_URL + "/{infoJobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Callback for Information Job deletion",
            description = "The call is invoked to terminate a data subscription. The endpoint is provided by the Information Producer.")
    @ApiResponses(value = { //
            @ApiResponse(responseCode = "200", description = "OK", //
                    content = @Content(schema = @Schema(implementation = VoidResponse.class))) //
    })
    public ResponseEntity<Object> jobDeletedCallback( //
            @PathVariable("infoJobId") String infoJobId) {

        logger.info("Job deleted callback {}", infoJobId);
        this.jobs.remove(infoJobId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(path = SUPERVISION_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Producer supervision",
            description = "The endpoint is provided by the Information Producer and is used for supervision of the producer.")
    @ApiResponses(value = { //
            @ApiResponse(responseCode = "200", description = "The producer is OK", //
                    content = @Content(schema = @Schema(implementation = String.class))) //
    })
    public ResponseEntity<Object> producerSupervision() {
        logger.info("Producer supervision");
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
