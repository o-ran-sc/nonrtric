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

import org.oransc.policyagent.Beans;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.ImmutablePolicy;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Rics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PolicyController {

    private final Beans beans;
    private final ApplicationConfig appConfig;
    private final Rics rics;
    private final PolicyTypes types;
    private final Policies policies;

    @Autowired
    PolicyController(Beans beans) {
        this.beans = beans;
        this.appConfig = beans.getApplicationConfig();
        this.rics = beans.getRics();
        this.types = beans.getPolicyTypes();
        this.policies = beans.getPolicies();
    }

    // http://localhost:8080/policy?type=type3&instance=xxx
    @GetMapping("/policy")
    public ResponseEntity<String> getPolicy( //
        @RequestParam(name = "type", required = false, defaultValue = "type1") String typeName, //
        @RequestParam(name = "instance", required = false, defaultValue = "new") String instanceId) {

        return new ResponseEntity<String>("policy" + typeName + instanceId, HttpStatus.OK);
    }

    @PutMapping(path = "/policy")
    public ResponseEntity<String> putPolicy( //
        @RequestParam(name = "type", required = true) String type, //
        @RequestParam(name = "instance", required = true) String instanceId, //
        @RequestParam(name = "ric", required = true) String ric, //
        @RequestParam(name = "service", required = true) String service, //
        @RequestBody String jsonBody) {

        System.out.println("*********************** " + jsonBody);

        try {
            Ric ricObj = rics.getRic(ric);
            Policy policy = ImmutablePolicy.builder() //
                .id(instanceId) //
                .json(jsonBody) //
                .type(types.getType(type)) //
                .ric(ricObj) //
                .ownerServiceName(service) //
                .build();
            policies.put(instanceId, policy);
            return new ResponseEntity<String>(HttpStatus.OK);
        } catch (ServiceException e) {
            System.out.println("*********************** " + e.getMessage());

            return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
        }

    }

    @PutMapping(path = "/policyX")
    public ResponseEntity<String> putPolicyX( //
        // @RequestParam(name = "type", required = false) String type, //
        // @RequestParam(name = "instance", required = true) String instance, //
        // @RequestParam(name = "ric", required = true) String ric, //
        @RequestBody String jsonBody) {
        System.out.println("*********************** " + jsonBody);
        // System.out.println("policy" + type + instance);
        return new ResponseEntity<String>(HttpStatus.OK);
    }

}
