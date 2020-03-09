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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.oransc.policyagent.clients.A1ClientFactory;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.ImmutablePolicy;
import org.oransc.policyagent.repository.Lock.LockType;
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
    private final A1ClientFactory a1ClientFactory;

    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    @Autowired
    PolicyController(ApplicationConfig config, PolicyTypes types, Policies policies, Rics rics,
        A1ClientFactory a1ClientFactory) {
        this.policyTypes = types;
        this.policies = policies;
        this.rics = rics;
        this.a1ClientFactory = a1ClientFactory;
    }

    @GetMapping("/policy_schemas")
    @ApiOperation(value = "Returns policy type schema definitions")
    @ApiResponses(
        value = {
            @ApiResponse(code = 200, message = "Policy schemas", response = Object.class, responseContainer = "List"), //
            @ApiResponse(code = 404, message = "RIC is not found", response = String.class)})
    public ResponseEntity<String> getPolicySchemas(@RequestParam(name = "ric", required = false) String ricName) {
        synchronized (this.policyTypes) {
            if (ricName == null) {
                Collection<PolicyType> types = this.policyTypes.getAll();
                return new ResponseEntity<>(toPolicyTypeSchemasJson(types), HttpStatus.OK);
            } else {
                try {
                    Collection<PolicyType> types = rics.getRic(ricName).getSupportedPolicyTypes();
                    return new ResponseEntity<>(toPolicyTypeSchemasJson(types), HttpStatus.OK);
                } catch (ServiceException e) {
                    return new ResponseEntity<>(e.toString(), HttpStatus.NOT_FOUND);
                }
            }
        }
    }

    @GetMapping("/policy_schema")
    @ApiOperation(value = "Returns one policy type schema definition")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "Policy schema", response = Object.class),
            @ApiResponse(code = 404, message = "RIC is not found", response = String.class)})
    public ResponseEntity<String> getPolicySchema(@RequestParam(name = "id", required = true) String id) {
        try {
            PolicyType type = policyTypes.getType(id);
            return new ResponseEntity<>(type.schema(), HttpStatus.OK);
        } catch (ServiceException e) {
            return new ResponseEntity<>(e.toString(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/policy_types")
    @ApiOperation(value = "Query policy type names")
    @ApiResponses(
        value = {
            @ApiResponse(
                code = 200,
                message = "Policy type names",
                response = String.class,
                responseContainer = "List"),
            @ApiResponse(code = 404, message = "RIC is not found", response = String.class)})
    public ResponseEntity<String> getPolicyTypes(@RequestParam(name = "ric", required = false) String ricName) {
        synchronized (this.policyTypes) {
            if (ricName == null) {
                Collection<PolicyType> types = this.policyTypes.getAll();
                return new ResponseEntity<>(toPolicyTypeIdsJson(types), HttpStatus.OK);
            } else {
                try {
                    Collection<PolicyType> types = rics.getRic(ricName).getSupportedPolicyTypes();
                    return new ResponseEntity<>(toPolicyTypeIdsJson(types), HttpStatus.OK);
                } catch (ServiceException e) {
                    return new ResponseEntity<>(e.toString(), HttpStatus.NOT_FOUND);
                }
            }
        }
    }

    @GetMapping("/policy")
    @ApiOperation(value = "Returns a policy configuration") //
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "Policy found", response = Object.class), //
            @ApiResponse(code = 404, message = "Policy is not found")} //
    )
    public ResponseEntity<String> getPolicy( //
        @RequestParam(name = "instance", required = true) String instance) {
        try {
            Policy p = policies.getPolicy(instance);
            return new ResponseEntity<>(p.json(), HttpStatus.OK);
        } catch (ServiceException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/policy")
    @ApiOperation(value = "Delete a policy", response = Object.class)
    @ApiResponses(
        value = { //
            @ApiResponse(code = 204, message = "Policy deleted", response = Object.class),
            @ApiResponse(code = 404, message = "Policy is not found", response = String.class),
            @ApiResponse(code = 423, message = "RIC is locked", response = String.class)})
    public Mono<ResponseEntity<Object>> deletePolicy( //
        @RequestParam(name = "instance", required = true) String id) {
        Policy policy = policies.get(id);
        if (policy != null && policy.ric().getState() == Ric.RicState.IDLE) {
            Ric ric = policy.ric();
            return ric.getLock().lock(LockType.SHARED) // //
                .flatMap(lock -> a1ClientFactory.createA1Client(policy.ric())) //
                .doOnNext(notUsed -> policies.remove(policy)) //
                .flatMap(client -> client.deletePolicy(policy)) //
                .doOnNext(notUsed -> ric.getLock().unlockBlocking()) //
                .doOnError(notUsed -> ric.getLock().unlockBlocking()) //
                .flatMap(notUsed -> Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT)));
        } else if (policy != null) {
            return Mono.just(new ResponseEntity<>("Busy, recovering", HttpStatus.LOCKED));
        } else {
            return Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        }
    }

    @PutMapping(path = "/policy")
    @ApiOperation(value = "Put a policy", response = String.class)
    @ApiResponses(
        value = { //
            @ApiResponse(code = 201, message = "Policy created"), //
            @ApiResponse(code = 200, message = "Policy updated"), //
            @ApiResponse(code = 423, message = "RIC is locked", response = String.class), //
            @ApiResponse(code = 404, message = "RIC or policy type is not found", response = String.class), //
            @ApiResponse(code = 405, message = "Change is not allowed", response = String.class)})
    public Mono<ResponseEntity<Object>> putPolicy( //
        @RequestParam(name = "type", required = true) String typeName, //
        @RequestParam(name = "instance", required = true) String instanceId, //
        @RequestParam(name = "ric", required = true) String ricName, //
        @RequestParam(name = "service", required = true) String service, //
        @RequestBody Object jsonBody) {

        String jsonString = gson.toJson(jsonBody);
        Ric ric = rics.get(ricName);
        PolicyType type = policyTypes.get(typeName);
        if (ric != null && type != null && ric.getState() == Ric.RicState.IDLE) {
            Policy policy = ImmutablePolicy.builder() //
                .id(instanceId) //
                .json(jsonString) //
                .type(type) //
                .ric(ric) //
                .ownerServiceName(service) //
                .lastModified(getTimeStampUtc()) //
                .build();

            final boolean isCreate = this.policies.get(policy.id()) == null;

            return ric.getLock().lock(LockType.SHARED) //
                .flatMap(p -> validateModifiedPolicy(policy)) //
                .flatMap(notUsed -> a1ClientFactory.createA1Client(ric)) //
                .flatMap(client -> client.putPolicy(policy)) //
                .doOnNext(notUsed -> policies.put(policy)) //
                .doOnNext(notUsed -> ric.getLock().unlockBlocking()) //
                .doOnError(t -> ric.getLock().unlockBlocking()) //
                .flatMap(notUsed -> Mono.just(new ResponseEntity<>(isCreate ? HttpStatus.CREATED : HttpStatus.OK))) //
                .onErrorResume(t -> Mono.just(new ResponseEntity<>(t.getMessage(), HttpStatus.METHOD_NOT_ALLOWED)));
        }

        return ric == null || type == null ? Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND))
            : Mono.just(new ResponseEntity<>(HttpStatus.LOCKED)); // Recovering
    }

    private Mono<Object> validateModifiedPolicy(Policy policy) {
        // Check that ric is not updated
        Policy current = this.policies.get(policy.id());
        if (current != null && !current.ric().name().equals(policy.ric().name())) {
            return Mono.error(new Exception("Policy cannot change RIC, policyId: " + current.id() + //
                ", RIC name: " + current.ric().name() + //
                ", new name: " + policy.ric().name()));
        }
        return Mono.just("OK");
    }

    @GetMapping("/policies")
    @ApiOperation(value = "Query policies")
    @ApiResponses(
        value = {
            @ApiResponse(code = 200, message = "Policies", response = PolicyInfo.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "RIC or type not found", response = String.class)})
    public ResponseEntity<String> getPolicies( //
        @RequestParam(name = "type", required = false) String type, //
        @RequestParam(name = "ric", required = false) String ric, //
        @RequestParam(name = "service", required = false) String service) //
    {
        if ((type != null && this.policyTypes.get(type) == null)) {
            return new ResponseEntity<>("Policy type not found", HttpStatus.NOT_FOUND);
        }
        if ((ric != null && this.rics.get(ric) == null)) {
            return new ResponseEntity<>("Ric not found", HttpStatus.NOT_FOUND);
        }
        synchronized (policies) {
            Collection<Policy> result = null;

            if (type != null) {
                result = policies.getForType(type);
                result = filter(result, null, ric, service);
            } else if (service != null) {
                result = policies.getForService(service);
                result = filter(result, type, ric, null);
            } else if (ric != null) {
                result = filter(policies.getForRic(ric), type, null, service);
            } else {
                result = policies.getAll();
            }

            String policiesJson;
            try {
                policiesJson = policiesToJson(result);
            } catch (ServiceException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity<>(policiesJson, HttpStatus.OK);
        }
    }

    @GetMapping("/policy_status")
    @ApiOperation(value = "Returns a policy status") //
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "Policy status", response = Object.class), //
            @ApiResponse(code = 404, message = "Policy is not found", response = String.class)} //
    )
    public Mono<ResponseEntity<String>> getPolicyStatus( //
        @RequestParam(name = "instance", required = true) String instance) {
        try {
            Policy policy = policies.getPolicy(instance);

            return a1ClientFactory.createA1Client(policy.ric()) //
                .flatMap(client -> client.getPolicyStatus(policy)) //
                .flatMap(status -> Mono.just(new ResponseEntity<>(status, HttpStatus.OK)));
        } catch (ServiceException e) {
            return Mono.just(new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND));
        }
    }

    private boolean include(String filter, String value) {
        return filter == null || value.equals(filter);
    }

    private Collection<Policy> filter(Collection<Policy> collection, String type, String ric, String service) {
        if (type == null && ric == null && service == null) {
            return collection;
        }
        List<Policy> filtered = new ArrayList<>();
        for (Policy p : collection) {
            if (include(type, p.type().name()) && include(ric, p.ric().name())
                && include(service, p.ownerServiceName())) {
                filtered.add(p);
            }
        }
        return filtered;
    }

    private String policiesToJson(Collection<Policy> policies) throws ServiceException {
        List<PolicyInfo> v = new ArrayList<>(policies.size());
        for (Policy p : policies) {
            PolicyInfo policyInfo = new PolicyInfo();
            policyInfo.id = p.id();
            policyInfo.json = fromJson(p.json());
            policyInfo.ric = p.ric().name();
            policyInfo.type = p.type().name();
            policyInfo.service = p.ownerServiceName();
            policyInfo.lastModified = p.lastModified();
            if (!policyInfo.validate()) {
                throw new ServiceException("BUG, all fields must be set");
            }
            v.add(policyInfo);
        }
        return gson.toJson(v);
    }

    private Object fromJson(String jsonStr) {
        return gson.fromJson(jsonStr, Object.class);
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
        List<String> v = new ArrayList<>(types.size());
        for (PolicyType t : types) {
            v.add(t.name());
        }
        return gson.toJson(v);
    }

    private String getTimeStampUtc() {
        return java.time.Instant.now().toString();
    }

}
