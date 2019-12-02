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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.Vector;

import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.configuration.RicConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class PolicyController {

    private final ApplicationConfig appConfig;
    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    @Autowired
    PolicyController(ApplicationConfig config) {
        this.appConfig = config;
    }

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
    @ApiOperation(value = "Returns status and statistics of the service")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "DATAFILE service is living"),
            @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
            @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
            @ApiResponse(code = 404, message = "The resource you were trying to reach is not found") //
        })
    public Mono<ResponseEntity<String>> getStatus() {
        Mono<ResponseEntity<String>> response = Mono.just(new ResponseEntity<>("hunky dory", HttpStatus.OK));
        return response;
    }

    // http://localhost:8080/rics?managedElementId=kista_1
    @GetMapping("/rics")
    @ApiOperation(value = "Returns defined NearRT RIC:s")
    public ResponseEntity<String> getRics(
        @RequestParam(name = "managedElementId", required = false, defaultValue = "") String managedElementId) {
        Vector<RicInfo> result = new Vector<RicInfo>();
        Vector<RicConfig> config = getRicConfigs(managedElementId);

        for (RicConfig ricConfig : config) {
            RicInfo ric = ImmutableRicInfo.builder() //
                .managedElementIds(ricConfig.managedElementIds()) //
                .name(ricConfig.name()) //
                .build();
            result.add(ric);
        }

        return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
    }

    private Vector<RicConfig> getRicConfigs(String managedElementId) {
        if (managedElementId.equals("")) {
            return this.appConfig.getRicConfigs();
        }

        Vector<RicConfig> result = new Vector<RicConfig>(1);
        appConfig.getRicConfig(managedElementId).ifPresent((config) -> {
            result.add(config);
        });
        return result;
    }

}
