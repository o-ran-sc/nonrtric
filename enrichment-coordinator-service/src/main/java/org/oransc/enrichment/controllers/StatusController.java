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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.immutables.gson.Gson;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController("StatusController")
@Api(tags = "Service status")
public class StatusController {

    @Gson.TypeAdapters
    @ApiModel(value = "status_info")
    class StatusInfo {
        @ApiModelProperty(value = "status text")
        public final String status;

        StatusInfo(String status) {
            this.status = status;
        }
    }

    @GetMapping(path = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Returns status and statistics of this service")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "Service is living", response = StatusInfo.class) //
        })
    public Mono<ResponseEntity<Object>> getStatus() {
        StatusInfo info = new StatusInfo("hunky dory");
        return Mono.just(new ResponseEntity<>(info, HttpStatus.OK));
    }

}
