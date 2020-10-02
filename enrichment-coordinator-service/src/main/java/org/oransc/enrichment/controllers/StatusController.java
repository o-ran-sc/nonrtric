/*-
 * ========================LICENSE_START=================================
 * ONAP : ccsdk oran
 * ======================================================================
 * Copyright (C) 2020 Nordix Foundation. All rights reserved.
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

package org.oransc.enrichment.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.immutables.gson.Gson;
import org.oransc.enrichment.repository.EiJobs;
import org.oransc.enrichment.repository.EiProducers;
import org.oransc.enrichment.repository.EiTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController("StatusController")
@Api(tags = "Service status")
public class StatusController {

    @Autowired
    private EiJobs eiJobs;

    @Autowired
    private EiTypes eiTypes;

    @Autowired
    private EiProducers eiProducers;

    @Gson.TypeAdapters
    @ApiModel(value = "status_info")
    public static class StatusInfo {
        @ApiModelProperty(value = "status text")
        @SerializedName("status")
        @JsonProperty(value = "status", required = true)
        public final String status;

        @ApiModelProperty(value = "Number of EI producers")
        @SerializedName("no_of_producers")
        @JsonProperty(value = "no_of_producers", required = true)
        public final int noOfProducers;

        @ApiModelProperty(value = "Number of EI types")
        @SerializedName("no_of_types")
        @JsonProperty(value = "no_of_types", required = true)
        public final int noOfTypes;

        @ApiModelProperty(value = "Number of EI jobs")
        @SerializedName("no_of_jobs")
        @JsonProperty(value = "no_of_jobs", required = true)
        public final int noOfJobs;

        public StatusInfo(String status, EiProducers producers, EiTypes types, EiJobs jobs) {
            this.status = status;
            this.noOfJobs = jobs.size();
            this.noOfProducers = producers.size();
            this.noOfTypes = types.size();
        }
    }

    @GetMapping(path = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Returns status and statistics of this service")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "Service is living", response = StatusInfo.class) //
        })
    public Mono<ResponseEntity<Object>> getStatus() {
        StatusInfo info = new StatusInfo("hunky dory", this.eiProducers, this.eiTypes, this.eiJobs);
        return Mono.just(new ResponseEntity<>(info, HttpStatus.OK));
    }

}
