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

package org.oransc.ics.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.immutables.gson.Gson;
import org.oransc.ics.repository.InfoJobs;
import org.oransc.ics.repository.InfoProducers;
import org.oransc.ics.repository.InfoTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController("StatusController")
@Tag(name = StatusController.API_NAME, description = StatusController.API_DESCRIPTION)
public class StatusController {

    public static final String API_NAME = "Service status";
    public static final String API_DESCRIPTION = "API for monitoring of the service";

    @Autowired
    private InfoJobs infoJobs;

    @Autowired
    private InfoTypes infoTypes;

    @Autowired
    private InfoProducers infoProducers;

    @Gson.TypeAdapters
    @Schema(name = "service_status_info")
    public static class StatusInfo {
        @Schema(name = "status", description = "status text")
        @SerializedName("status")
        @JsonProperty(value = "status", required = true)
        public final String status;

        @Schema(name = "no_of_producers", description = "Number of Information Producers")
        @SerializedName("no_of_producers")
        @JsonProperty(value = "no_of_producers", required = true)
        public final int noOfProducers;

        @Schema(name = "no_of_types", description = "Number of Information Types")
        @SerializedName("no_of_types")
        @JsonProperty(value = "no_of_types", required = true)
        public final int noOfTypes;

        @Schema(name = "no_of_jobs", description = "Number of Information Jobs")
        @SerializedName("no_of_jobs")
        @JsonProperty(value = "no_of_jobs", required = true)
        public final int noOfJobs;

        public StatusInfo(String status, InfoProducers producers, InfoTypes types, InfoJobs jobs) {
            this.status = status;
            this.noOfJobs = jobs.size();
            this.noOfProducers = producers.size();
            this.noOfTypes = types.size();
        }
    }

    @GetMapping(path = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Returns status and statistics of this service")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Service is living", //
                content = @Content(schema = @Schema(implementation = StatusInfo.class))) //
        })
    public Mono<ResponseEntity<Object>> getStatus() {
        StatusInfo info = new StatusInfo("hunky dory", this.infoProducers, this.infoTypes, this.infoJobs);
        return Mono.just(new ResponseEntity<>(info, HttpStatus.OK));
    }

}
