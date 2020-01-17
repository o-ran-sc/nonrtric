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

import org.oransc.policyagent.clients.A1Client;
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
import reactor.core.publisher.Mono;

@RestController
@Api(value = "Policy Management API")
public class PolicyController {

    private final Rics rics;
    private final PolicyTypes policyTypes;
    private final Policies policies;
    private final A1Client a1Client;

    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    @Autowired
    PolicyController(ApplicationConfig config, PolicyTypes types, Policies policies, Rics rics, A1Client a1Client) {
        this.policyTypes = types;
        this.policies = policies;
        this.rics = rics;
        this.a1Client = a1Client;
    }

    @GetMapping("/policy_schemas")
    @ApiOperation(value = "Returns policy type schema definitions")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Policy Types found")})
    public ResponseEntity<String> getPolicySchemas(@RequestParam(name = "ric", required = false) String ricName) {
        synchronized (this.policyTypes) {
            if (ricName == null) {
                Collection<PolicyType> types = this.policyTypes.getAll();
                return new ResponseEntity<String>(toPolicyTypeSchemasJson(types), HttpStatus.OK);
            } else {
                try {
                    Collection<PolicyType> types = rics.getRic(ricName).getSupportedPolicyTypes();
                    return new ResponseEntity<String>(toPolicyTypeSchemasJson(types), HttpStatus.OK);
                } catch (ServiceException e) {
                    return new ResponseEntity<String>(e.toString(), HttpStatus.NOT_FOUND);
                }
            }
        }
    }

    @GetMapping("/policy_schema")
    @ApiOperation(value = "Returns one policy type schema definition")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Policy Type found")})
    public ResponseEntity<String> getPolicySchema(@RequestParam(name = "id", required = true) String id) {
        try {
            PolicyType type = policyTypes.getType(id);
            return new ResponseEntity<String>(type.schema(), HttpStatus.OK);
        } catch (ServiceException e) {
            return new ResponseEntity<String>(e.toString(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/policy_types")
    @ApiOperation(value = "Returns policy types")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Policy Types found")})
    public ResponseEntity<String> getPolicyTypes(@RequestParam(name = "ric", required = false) String ricName) {
        synchronized (this.policyTypes) {
            if (ricName == null) {
                Collection<PolicyType> types = this.policyTypes.getAll();
                return new ResponseEntity<String>(toPolicyTypeIdsJson(types), HttpStatus.OK);
            } else {
                try {
                    Collection<PolicyType> types = rics.getRic(ricName).getSupportedPolicyTypes();
                    return new ResponseEntity<String>(toPolicyTypeIdsJson(types), HttpStatus.OK);
                } catch (ServiceException e) {
                    return new ResponseEntity<String>(e.toString(), HttpStatus.NOT_FOUND);
                }
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
    public Mono<ResponseEntity<Void>> deletePolicy( //
        @RequestParam(name = "instance", required = true) String id) {
        Policy policy = policies.get(id);
        if (policy != null && policy.ric().state().equals(Ric.RicState.IDLE)) {
            return a1Client.deletePolicy(policy.ric().getConfig().baseUrl(), id) //
                .doOnEach(notUsed -> policies.removeId(id)) //
                .flatMap(notUsed -> {
                    return Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT));
                });
        } else {
            return Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        }
    }

    @PutMapping(path = "/policy")
    @ApiOperation(value = "Create the policy")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Policy created")})
    public Mono<ResponseEntity<String>> putPolicy( //
        @RequestParam(name = "type", required = true) String typeName, //
        @RequestParam(name = "instance", required = true) String instanceId, //
        @RequestParam(name = "ric", required = true) String ricName, //
        @RequestParam(name = "service", required = true) String service, //
        @RequestBody String jsonBody) {

        Ric ric = rics.get(ricName);
        PolicyType type = policyTypes.get(typeName);
        if (ric != null && type != null && ric.state().equals(Ric.RicState.IDLE)) {
            Policy policy = ImmutablePolicy.builder() //
                .id(instanceId) //
                .json(jsonBody) //
                .type(type) //
                .ric(ric) //
                .ownerServiceName(service) //
                .lastModified(getTimeStampUTC()) //
                .build();
            return a1Client.putPolicy(policy) //
                .doOnNext(notUsed -> policies.put(policy)) //
                .flatMap(notUsed -> {
                    return Mono.just(new ResponseEntity<>(HttpStatus.CREATED));
                });
        }
        return Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/policies")
    @ApiOperation(value = "Returns the policies")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Policies found")})
    public ResponseEntity<String> getPolicies( //
        @RequestParam(name = "type", required = false) String type, //
        @RequestParam(name = "ric", required = false) String ric, //
        @RequestParam(name = "service", required = false) String service) //
    {
        synchronized (policies) {
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

    private String toPolicyTypeSchemasJson(Collection<PolicyType> types) {
        StringBuilder result = new StringBuilder();
        result.append("[");
        boolean first = true;
        for (PolicyType t : types) {
            if (!first) {
                result.append(",");
            }
            first = false;
            result.append(t.schema());
        }
        result.append("]");
        return result.toString();
    }

    private String toPolicyTypeIdsJson(Collection<PolicyType> types) {
        Vector<String> v = new Vector<>(types.size());
        for (PolicyType t : types) {
            v.add(t.name());
        }
        return gson.toJson(v);
    }

    private String getTimeStampUTC() {
        return java.time.Instant.now().toString();
    }

}
