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

import java.util.Optional;
import java.util.Vector;

import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Rics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RicRepositoryController {

    private final ApplicationConfig appConfig;

    @Autowired
    private Rics rics;

    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    @Autowired
    RicRepositoryController(ApplicationConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * Example: http://localhost:8080/rics?managedElementId=kista_1
     */
    @GetMapping("/ric")
    @ApiOperation(value = "Returns the name of a RIC managing one Mananged Element")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "RIC is fond"), //
            @ApiResponse(code = 404, message = "RIC is not fond") //
        })
    public ResponseEntity<String> getRic(
        @RequestParam(name = "managedElementId", required = false, defaultValue = "") String managedElementId) {

        Optional<RicConfig> config = appConfig.lookupRicConfigForManagedElement(managedElementId);

        if (config.isPresent()) {
            return new ResponseEntity<>(config.get().name(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("", HttpStatus.NOT_FOUND);
        }
    }

    /**
     * @return a Json array of all RIC data
     *         Example: http://localhost:8080/ric
     */
    @GetMapping("/rics")
    @ApiOperation(value = "Returns defined NearRT RIC:s as Json")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "OK") //
        })
    public ResponseEntity<String> getRics() {
        Vector<RicInfo> result = new Vector<>();
        for (Ric ric : rics.getRics()) {
            result.add(ImmutableRicInfo.builder() //
                .name(ric.name()) //
                .managedElementIds(ric.getManagedNodes()) //
                .build());
        }

        return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
    }

}
