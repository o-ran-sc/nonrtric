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

import java.util.Collection;
import java.util.Vector;

import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.ImmutablePolicy;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyTypes;
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

    private final ApplicationConfig appConfig;
    private final PolicyTypes types;
    private final Policies policies;
    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    @Autowired
    PolicyController(ApplicationConfig config, PolicyTypes types, Policies policies) {
        this.appConfig = config;
        this.types = types;
        this.policies = policies;
    }

    @GetMapping("/policy")
    public ResponseEntity<String> getPolicy( //
        @RequestParam(name = "instance", required = false, defaultValue = "new") String instance) {
        try {
            Policy p = policies.get(instance);
            return new ResponseEntity<String>(p.json(), HttpStatus.OK);

        } catch (ServiceException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.NO_CONTENT);
        }
    }

    @GetMapping("/policies")
    public ResponseEntity<String> getPolicies( //
        @RequestParam(name = "type", required = false) String type, //
        @RequestParam(name = "ric", required = false) String ric, //
        @RequestParam(name = "service", required = false) String service) //
    {
        try {
            Collection<Policy> result = null;

            if (type != null) {
                assertNull(result);
                result = policies.getForType(type);
            } else if (ric != null) {
                assertNull(result);
                result = policies.getForRic(ric);
            } else if (service != null) {
                assertNull(result);
                result = policies.getForService(service);
            } else {
                result = new Vector<>();
            }

            return new ResponseEntity<String>(toJson(result), HttpStatus.OK);

        } catch (ServiceException e) {
            return new ResponseEntity<String>("Only one qeury parameter allowed", HttpStatus.NO_CONTENT);
        }
    }

    private void assertNull(Object o) throws ServiceException {
        if (o != null) {
            throw new ServiceException("Null expected");
        }
    }

    private String toJson(Collection<Policy> policies) {
        Vector<PolicyInfo> v = new Vector<>(policies.size());
        for (Policy p : policies) {
            PolicyInfo policyInfo = ImmutablePolicyInfo.builder() //
                .json(p.json()) //
                .name(p.id()) //
                .ric(p.ric().name()) //
                .type(p.type().name()).build();
            v.add(policyInfo);
        }
        return gson.toJson(v);
    }

    @PutMapping(path = "/policy")
    public ResponseEntity<String> putPolicy( //
        @RequestParam(name = "type", required = true) String type, //
        @RequestParam(name = "instance", required = true) String instanceId, //
        @RequestParam(name = "ric", required = true) String ric, //
        @RequestParam(name = "service", required = true) String service, //
        @RequestBody String jsonBody) {

        try {
            Policy policy = ImmutablePolicy.builder() //
                .id(instanceId) //
                .json(jsonBody) //
                .type(types.getType(type)) //
                .ric(appConfig.getRic(ric)) //
                .ownerServiceName(service) //
                .build();
            policies.put(policy);
            return new ResponseEntity<String>(HttpStatus.OK);
        } catch (ServiceException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

}
