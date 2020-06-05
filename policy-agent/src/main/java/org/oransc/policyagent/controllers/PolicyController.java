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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.Getter;

import org.oransc.policyagent.clients.A1ClientFactory;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.ImmutablePolicy;
import org.oransc.policyagent.repository.Lock.LockType;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Rics;
import org.oransc.policyagent.repository.Service;
import org.oransc.policyagent.repository.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@RestController
@Api(tags = "A1 Policy Management")
public class PolicyController {

    public static class RejectionException extends Exception {
        private static final long serialVersionUID = 1L;
        @Getter
        private final HttpStatus status;

        public RejectionException(String message, HttpStatus status) {
            super(message);
            this.status = status;
        }
    }

    @Autowired
    private Rics rics;
    @Autowired
    private PolicyTypes policyTypes;
    @Autowired
    private Policies policies;
    @Autowired
    private A1ClientFactory a1ClientFactory;
    @Autowired
    private Services services;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    @GetMapping("/policy_schemas")
    @ApiOperation(value = "Returns policy type schema definitions")
    @ApiResponses(
        value = {
            @ApiResponse(code = 200, message = "Policy schemas", response = Object.class, responseContainer = "List"), //
            @ApiResponse(code = 404, message = "RIC is not found", response = String.class)})
    public ResponseEntity<String> getPolicySchemas(@ApiParam(name = "ric", required = false, value = "The name of " +//
        "the Near-RT RIC to get the definitions for.")@RequestParam(name = "ric", required = false) String ricName) {
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

    @GetMapping("/policy_schema")
    @ApiOperation(value = "Returns one policy type schema definition")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "Policy schema", response = Object.class),
            @ApiResponse(code = 404, message = "RIC is not found", response = String.class)})
    public ResponseEntity<String> getPolicySchema(@ApiParam(name = "id", required = true, value = "The ID of the " +//
        "policy type to get the definition for.")@RequestParam(name = "id", required = true) String id) {
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
    public ResponseEntity<String> getPolicyTypes(@ApiParam(name = "ric", required = false, value = "The name of " +//
        "the Near-RT RIC to get types for.")@RequestParam(name = "ric", required = false) String ricName) {
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

    @GetMapping("/policy")
    @ApiOperation(value = "Returns a policy configuration") //
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "Policy found", response = Object.class), //
            @ApiResponse(code = 404, message = "Policy is not found")} //
    )
    public ResponseEntity<String> getPolicy( //
        @ApiParam(name = "id", required = true, value = "The ID of the policy instance.")@RequestParam(name = "id", //
            required = true) String id) {
        try {
            Policy p = policies.getPolicy(id);
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
            @ApiResponse(code = 423, message = "RIC is not operational", response = String.class)})
    public Mono<ResponseEntity<Object>> deletePolicy( //
        @ApiParam(name = "id", required = true, value = "The ID of the policy instance.")@RequestParam(name = "id", //
            required = true) String id) {
        try {
            Policy policy = policies.getPolicy(id);
            keepServiceAlive(policy.ownerServiceName());
            Ric ric = policy.ric();
            return ric.getLock().lock(LockType.SHARED) //
                .flatMap(notUsed -> assertRicStateIdle(ric)) //
                .flatMap(notUsed -> a1ClientFactory.createA1Client(policy.ric())) //
                .doOnNext(notUsed -> policies.remove(policy)) //
                .flatMap(client -> client.deletePolicy(policy)) //
                .doOnNext(notUsed -> ric.getLock().unlockBlocking()) //
                .doOnError(notUsed -> ric.getLock().unlockBlocking()) //
                .flatMap(notUsed -> Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT)))
                .onErrorResume(this::handleException);
        } catch (ServiceException e) {
            return Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        }
    }

    @PutMapping(path = "/policy")
    @ApiOperation(value = "Put a policy", response = String.class)
    @ApiResponses(
        value = { //
            @ApiResponse(code = 201, message = "Policy created", response = Object.class), //
            @ApiResponse(code = 200, message = "Policy updated", response = Object.class), //
            @ApiResponse(code = 423, message = "RIC is not operational", response = String.class), //
            @ApiResponse(code = 404, message = "RIC or policy type is not found", response = String.class) //
        })
    public Mono<ResponseEntity<Object>> putPolicy( //
        @ApiParam(name = "type", required = false, value = "The name of the policy type.") //
            @RequestParam(name = "type", required = false, defaultValue = "") String typeName, //
        @ApiParam(name = "id", required = true, value = "The ID of the policy instance.")@RequestParam(name = "id", //
            required = true) String instanceId, //
        @ApiParam(name = "ric", required = true, value = "The name of the Near-RT RIC where the policy will be " +//
            "created.")@RequestParam(name = "ric", required = true) String ricName, //
        @ApiParam(name = "service", required = true, value = "The name of the service creating the policy.") //
            @RequestParam(name = "service", required = true) String service, //
        @ApiParam(name = "transient", required = false, value = "If the policy is transient or not (boolean " +//
            "defaulted to false). A policy is transient if it will be forgotten when the service needs to " +//
            "reconnect to the Near-RT RIC.")@RequestParam(name = "transient", required = false, //
            defaultValue = "false") boolean isTransient, //
        @RequestBody Object jsonBody) {

        String jsonString = gson.toJson(jsonBody);
        Ric ric = rics.get(ricName);
        PolicyType type = policyTypes.get(typeName);
        keepServiceAlive(service);
        if (ric == null || type == null) {
            return Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        }
        Policy policy = ImmutablePolicy.builder() //
            .id(instanceId) //
            .json(jsonString) //
            .type(type) //
            .ric(ric) //
            .ownerServiceName(service) //
            .lastModified(getTimeStampUtc()) //
            .isTransient(isTransient) //
            .build();

        final boolean isCreate = this.policies.get(policy.id()) == null;

        return ric.getLock().lock(LockType.SHARED) //
            .flatMap(notUsed -> assertRicStateIdle(ric)) //
            .flatMap(notUsed -> checkSupportedType(ric, type)) //
            .flatMap(notUsed -> validateModifiedPolicy(policy)) //
            .flatMap(notUsed -> a1ClientFactory.createA1Client(ric)) //
            .flatMap(client -> client.putPolicy(policy)) //
            .doOnNext(notUsed -> policies.put(policy)) //
            .doOnNext(notUsed -> ric.getLock().unlockBlocking()) //
            .doOnError(trowable -> ric.getLock().unlockBlocking()) //
            .flatMap(notUsed -> Mono.just(new ResponseEntity<>(isCreate ? HttpStatus.CREATED : HttpStatus.OK))) //
            .onErrorResume(this::handleException);
    }

    @SuppressWarnings({"unchecked"})
    private <T> Mono<ResponseEntity<T>> createResponseEntity(String message, HttpStatus status) {
        ResponseEntity<T> re = new ResponseEntity<>((T) message, status);
        return Mono.just(re);
    }

    private <T> Mono<ResponseEntity<T>> handleException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException e = (WebClientResponseException) throwable;
            return createResponseEntity(e.getResponseBodyAsString(), e.getStatusCode());
        } else if (throwable instanceof RejectionException) {
            RejectionException e = (RejectionException) throwable;
            return createResponseEntity(e.getMessage(), e.getStatus());
        } else {
            return createResponseEntity(throwable.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Mono<Object> validateModifiedPolicy(Policy policy) {
        // Check that ric is not updated
        Policy current = this.policies.get(policy.id());
        if (current != null && !current.ric().name().equals(policy.ric().name())) {
            RejectionException e = new RejectionException("Policy cannot change RIC, policyId: " + current.id() + //
                ", RIC name: " + current.ric().name() + //
                ", new name: " + policy.ric().name(), HttpStatus.CONFLICT);
            return Mono.error(e);
        }
        return Mono.just("OK");
    }

    private Mono<Object> checkSupportedType(Ric ric, PolicyType type) {
        if (!ric.isSupportingType(type.name())) {
            RejectionException e = new RejectionException(
                "Type: " + type.name() + " not supported by RIC: " + ric.name(), HttpStatus.NOT_FOUND);
            return Mono.error(e);
        }
        return Mono.just("OK");
    }

    private Mono<Object> assertRicStateIdle(Ric ric) {
        if (ric.getState() == Ric.RicState.AVAILABLE) {
            return Mono.just("OK");
        } else {
            RejectionException e = new RejectionException(
                "Ric is not operational, RIC name: " + ric.name() + ", state: " + ric.getState(), HttpStatus.LOCKED);
            return Mono.error(e);
        }
    }

    @GetMapping("/policies")
    @ApiOperation(value = "Query policies")
    @ApiResponses(
        value = {
            @ApiResponse(code = 200, message = "Policies", response = PolicyInfo.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "RIC or type not found", response = String.class)})
    public ResponseEntity<String> getPolicies( //
        @ApiParam(name = "type", required = false, value = "The name of the policy type to get policies for.") //
            @RequestParam(name = "type", required = false) String type, //
        @ApiParam(name = "ric", required = false, value = "The name of the Near-RT RIC to get policies for.") //
            @RequestParam(name = "ric", required = false) String ric, //
        @ApiParam(name = "service", required = false, value = "The name of the service to get policies for.") //
            @RequestParam(name = "service", required = false) String service) //
    {
        if ((type != null && this.policyTypes.get(type) == null)) {
            return new ResponseEntity<>("Policy type not found", HttpStatus.NOT_FOUND);
        }
        if ((ric != null && this.rics.get(ric) == null)) {
            return new ResponseEntity<>("RIC not found", HttpStatus.NOT_FOUND);
        }

        String filteredPolicies = policiesToJson(filter(type, ric, service));
        return new ResponseEntity<>(filteredPolicies, HttpStatus.OK);
    }

    @GetMapping("/policy_ids")
    @ApiOperation(value = "Query policies, only IDs returned")
    @ApiResponses(
        value = {@ApiResponse(code = 200, message = "Policy ids", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "RIC or type not found", response = String.class)})
    public ResponseEntity<String> getPolicyIds( //
        @ApiParam(name = "type", required = false, value = "The name of the policy type to get policies for.") //
            @RequestParam(name = "type", required = false) String type, //
        @ApiParam(name = "ric", required = false, value = "The name of the Near-RT RIC to get policies for.") //
            @RequestParam(name = "ric", required = false) String ric, //
        @ApiParam(name = "service", required = false, value = "The name of the service to get policies for.") //
            @RequestParam(name = "service", required = false) String service) //
    {
        if ((type != null && this.policyTypes.get(type) == null)) {
            return new ResponseEntity<>("Policy type not found", HttpStatus.NOT_FOUND);
        }
        if ((ric != null && this.rics.get(ric) == null)) {
            return new ResponseEntity<>("RIC not found", HttpStatus.NOT_FOUND);
        }

        String policyIdsJson = toPolicyIdsJson(filter(type, ric, service));
        return new ResponseEntity<>(policyIdsJson, HttpStatus.OK);
    }

    @GetMapping("/policy_status")
    @ApiOperation(value = "Returns a policy status") //
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "Policy status", response = Object.class), //
            @ApiResponse(code = 404, message = "Policy is not found", response = String.class)} //
    )
    public Mono<ResponseEntity<String>> getPolicyStatus( //
        @ApiParam(name = "id", required = true, value = "The ID of the policy.")@RequestParam(name = "id", //
            required = true) String id) {
        try {
            Policy policy = policies.getPolicy(id);

            return a1ClientFactory.createA1Client(policy.ric()) //
                .flatMap(client -> client.getPolicyStatus(policy)) //
                .flatMap(status -> Mono.just(new ResponseEntity<>(status, HttpStatus.OK)))
                .onErrorResume(this::handleException);
        } catch (ServiceException e) {
            return Mono.just(new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND));
        }
    }

    private void keepServiceAlive(String name) {
        Service s = this.services.get(name);
        if (s != null) {
            s.keepAlive();
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

    private Collection<Policy> filter(String type, String ric, String service) {
        if (type != null) {
            return filter(policies.getForType(type), null, ric, service);
        } else if (service != null) {
            return filter(policies.getForService(service), type, ric, null);
        } else if (ric != null) {
            return filter(policies.getForRic(ric), type, null, service);
        } else {
            return policies.getAll();
        }
    }

    private String policiesToJson(Collection<Policy> policies) {
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
                logger.error("BUG, all fields must be set");
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

    private String toPolicyIdsJson(Collection<Policy> policies) {
        List<String> v = new ArrayList<>(policies.size());
        for (Policy p : policies) {
            v.add(p.id());
        }
        return gson.toJson(v);
    }

    private String getTimeStampUtc() {
        return java.time.Instant.now().toString();
    }

}
