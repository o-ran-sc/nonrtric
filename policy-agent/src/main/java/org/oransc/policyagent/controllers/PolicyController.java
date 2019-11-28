/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2019 Nordix Foundation
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
package org.oransc.policyagent.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.net.http.HttpHeaders;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class PolicyController {

    // http://localhost:8080/policy?type=type3&instance=xxx
    @GetMapping("/policy")
    public String getPolicy(@RequestParam(name = "type", required = false, defaultValue = "type1") String typeName,
        @RequestParam(name = "instance", required = false, defaultValue = "new") String instanceId) {
        System.out.println("**** getPolicy " + typeName);

        return "policy" + typeName + instanceId;
    }

    public String getHello() {
        return "Howdy";
    }

    @GetMapping("/status")
    @ApiOperation(value = "Returns status and statistics of DATAFILE service")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "DATAFILE service is living"),
            @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
            @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
            @ApiResponse(code = 404, message = "The resource you were trying to reach is not found") //
        })
    public Mono<ResponseEntity<String>> getStatus(@RequestHeader HttpHeaders headers) {
        Mono<ResponseEntity<String>> response = Mono.just(new ResponseEntity<>("hunky dory", HttpStatus.OK));
        return response;
    }

}
