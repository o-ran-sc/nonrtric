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
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.Collection;
import java.util.Vector;

import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.ImmutablePolicy;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Rics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(value = "Policy Management API")
public class PolicyController {

    private final Rics rics;
    private final PolicyTypes policyTypes;
    private final Policies policies;
    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    @Autowired
    PolicyController(ApplicationConfig config, PolicyTypes types, Policies policies, Rics rics) {
        this.policyTypes = types;
        this.policies = policies;
        this.rics = rics;
    }

    @GetMapping("/policy_types")
    @ApiOperation(value = "Returns all the policy types")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Policy Types found")})
    public ResponseEntity<String> getPolicyTypes(@RequestParam(name = "ric", required = false) String ricName) {
        if (ricName == null) {
            Collection<PolicyType> types = this.policyTypes.getAll();
            return new ResponseEntity<String>(policyTypesToJson(types), HttpStatus.OK);
        } else {
            try {
                Collection<PolicyType> types = rics.getRic(ricName).getSupportedPolicyTypes();
                return new ResponseEntity<String>(policyTypesToJson(types), HttpStatus.OK);
            } catch (ServiceException e) {
                return new ResponseEntity<String>(e.toString(), HttpStatus.NOT_FOUND);
            }
        }
    }

    @GetMapping("/policy")
    @ApiOperation(value = "Returns the policy")
    @ApiResponses(
        value = {@ApiResponse(code = 200, message = "Policy found"),
            @ApiResponse(code = 204, message = "Policy is not found")})
    public ResponseEntity<String> getPolicy( //
        @RequestParam(name = "instance", required = true) String instance) {
        try {
            Policy p = policies.getPolicy(instance);
            return new ResponseEntity<String>(p.json(), HttpStatus.OK);
        } catch (ServiceException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.NO_CONTENT);
        }
    }

    @DeleteMapping("/policy")
    @ApiOperation(value = "Deletes the policy")
    @ApiResponses(value = {@ApiResponse(code = 204, message = "Policy deleted")})
    public ResponseEntity<Void> deletePolicy( //
        @RequestParam(name = "instance", required = true) String instance) {

        policies.removeId(instance);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/policies")
    @ApiOperation(value = "Returns the policies")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Polcies found")})
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

        return new ResponseEntity<String>(policiesToJson(result), HttpStatus.OK);
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

    private String policiesToJson(Collection<Policy> policies) {
        Vector<PolicyInfo> v = new Vector<>(policies.size());
        for (Policy p : policies) {
            PolicyInfo policyInfo = ImmutablePolicyInfo.builder() //
                .json(p.json()) //
                .id(p.id()) //
                .ric(p.ric().name()) //
                .type(p.type().name()) //
                .service(p.ownerServiceName()) //
                .lastModified(p.lastModified()) //
                .build();
            v.add(policyInfo);
        }
        return gson.toJson(v);
    }

    private String policyTypesToJson(Collection<PolicyType> types) {
        Vector<PolicyTypeInfo> v = new Vector<>(types.size());
        for (PolicyType t : types) {
            PolicyTypeInfo policyInfo = ImmutablePolicyTypeInfo.builder() //
                .name(t.name()) //
                .build();
            v.add(policyInfo);
        }
        return gson.toJson(v);
    }

    private String getTimeStampUTC() {
        return java.time.Instant.now().toString();
    }

    @PutMapping(path = "/policy")
    @ApiOperation(value = "Create the policy")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Policy created")})
    public ResponseEntity<String> putPolicy( //
        @RequestParam(name = "type", required = true) String type, //
        @RequestParam(name = "instance", required = true) String instanceId, //
        @RequestParam(name = "ric", required = true) String ric, //
        @RequestParam(name = "service", required = true) String service, //
        @RequestBody String jsonBody) {

        try {
            // services.getService(service).ping();
            Ric ricObj = rics.getRic(ric);
            Policy policy = ImmutablePolicy.builder() //
                .id(instanceId) //
                .json(jsonBody) //
                .type(policyTypes.getType(type)) //
                .ric(ricObj) //
                .ownerServiceName(service) //
                .lastModified(getTimeStampUTC()) //
                .build();
            policies.put(policy);
            return new ResponseEntity<String>(HttpStatus.CREATED);
        } catch (ServiceException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

}
