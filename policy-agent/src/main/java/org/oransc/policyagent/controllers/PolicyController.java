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
import org.oransc.policyagent.Beans;
import com.google.gson.GsonBuilder;

import java.util.Collection;
import java.util.Vector;

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
    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    @Autowired
    PolicyController(Beans beans) {
        this.beans = beans;
        this.appConfig = beans.getApplicationConfig();
        this.rics = beans.getRics();
        this.types = beans.getPolicyTypes();
        this.policies = beans.getPolicies();
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
        Collection<Policy> result = null;

        if (type != null) {
            result = policies.getForType(type);
            result = filter(result, null, ric, service);
        } else if (service != null) {
            result = policies.getForService(service);
            result = filter(result, type, ric, null);
        } else if (ric != null) {
            result = policies.getForRic(ric);
            result = filter(result, type, null, service);
        } else {
            result = policies.getAll();
        }

        return new ResponseEntity<String>(toJson(result), HttpStatus.OK);
    }

    private boolean include(String filter, String value) {
        return filter == null || value.equals(filter);
    }

    private Collection<Policy> filter(Collection<Policy> collection, String type, String ric, String service) {
        if (type == null && ric == null && service == null) {
            return collection;
        }
        Vector<Policy> filtered = new Vector<>();
        for (Policy p : collection) {
            if (include(type, p.type().name()) && include(ric, p.ric().name())
                && include(service, p.ownerServiceName())) {
                filtered.add(p);
            }
        }
        return filtered;
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
            Ric ricObj = rics.getRic(ric);
            Policy policy = ImmutablePolicy.builder() //
                .id(instanceId) //
                .json(jsonBody) //
                .type(types.getType(type)) //
                .ric(ricObj) //
                .ownerServiceName(service) //
                .build();
            policies.put(policy);
            return new ResponseEntity<String>(HttpStatus.OK);
        } catch (ServiceException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

}
