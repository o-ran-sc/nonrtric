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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Rics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "RIC Repository")
public class RicRepositoryController {

    @Autowired
    private Rics rics;

    @Autowired
    PolicyTypes types;

    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    /**
     * Example: http://localhost:8081/rics?managedElementId=kista_1
     */
    @GetMapping("/ric")
    @ApiOperation(value = "Returns the name of a RIC managing one Mananged Element")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "RIC is found", response = String.class), //
            @ApiResponse(code = 404, message = "RIC is not found", response = String.class) //
        })
    public ResponseEntity<String> getRic( //
        @ApiParam(name = "managedElementId", required = true, value = "The ID of the Managed Element") @RequestParam(
            name = "managedElementId",
            required = true) String managedElementId) {

        Optional<Ric> ric = this.rics.lookupRicForManagedElement(managedElementId);

        if (ric.isPresent()) {
            return new ResponseEntity<>(ric.get().name(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("No RIC found", HttpStatus.NOT_FOUND);
        }
    }

    /**
     * @return a Json array of all RIC data Example: http://localhost:8081/ric
     */
    @GetMapping("/rics")
    @ApiOperation(value = "Query Near-RT RIC information")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "OK", response = RicInfo.class, responseContainer = "List"), //
            @ApiResponse(code = 404, message = "Policy type is not found", response = String.class)})
    public ResponseEntity<String> getRics( //
        @ApiParam(name = "policyType", required = false, value = "The name of the policy type") @RequestParam(
            name = "policyType",
            required = false) String supportingPolicyType) {

        if ((supportingPolicyType != null) && (this.types.get(supportingPolicyType) == null)) {
            return new ResponseEntity<>("Policy type not found", HttpStatus.NOT_FOUND);
        }

        List<RicInfo> result = new ArrayList<>();
        for (Ric ric : rics.getRics()) {
            if (supportingPolicyType == null || ric.isSupportingType(supportingPolicyType)) {
                result.add(new RicInfo(ric.name(), ric.getManagedElementIds(), ric.getSupportedPolicyTypeNames(),
                    ric.getState().toString()));
            }
        }

        return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
    }

}
