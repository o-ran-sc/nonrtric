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

import java.time.Duration;
import java.util.Collection;
import java.util.Vector;

import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.Service;
import org.oransc.policyagent.repository.Services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceController {

    private final Services services;
    private final Policies policies;

    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    @Autowired
    ServiceController(Services services, Policies policies) {
        this.services = services;
        this.policies = policies;
    }

    @GetMapping("/services")
    @ApiOperation(value = "Returns service information")
    @ApiResponses(
        value = {@ApiResponse(code = 200, message = "OK", response = ServiceStatus.class, responseContainer = "List")})
    public ResponseEntity<String> getServices( //
        @RequestParam(name = "name", required = false) String name) {

        Collection<ServiceStatus> servicesStatus = new Vector<>();
        synchronized (this.services) {
            for (Service s : this.services.getAll()) {
                if (name == null || name.equals(s.getName())) {
                    servicesStatus.add(toServiceStatus(s));
                }
            }
        }

        String res = gson.toJson(servicesStatus);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }

    private ServiceStatus toServiceStatus(Service s) {
        return new ServiceStatus(s.getName(), s.getKeepAliveInterval().toSeconds(), s.timeSinceLastPing().toSeconds());
    }

    @ApiOperation(value = "Register a service")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = String.class)})
    @PutMapping("/service")
    public ResponseEntity<String> putService( //
        @RequestBody ServiceRegistrationInfo registrationInfo) {
        try {
            this.services.put(toService(registrationInfo));
            return new ResponseEntity<String>("OK", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.NO_CONTENT);
        }
    }

    @ApiOperation(value = "Delete a service")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")})
    @DeleteMapping("/services")
    public ResponseEntity<String> deleteService( //
        @RequestParam(name = "name", required = true) String name) {
        try {
            Service service = removeService(name);
            // Remove the policies from the repo and let the consistency monitoring
            // do the rest.
            removePolicies(service);
            return new ResponseEntity<String>("OK", HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.NO_CONTENT);
        }
    }

    @ApiOperation(value = "Keep the poilicies alive for a service")
    @ApiResponses(
        value = {@ApiResponse(code = 200, message = "Policies timeout supervision refreshed"),
            @ApiResponse(code = 404, message = "The service is not found, needs re-registration")})
    @PostMapping("/services/keepalive")
    public ResponseEntity<String> keepAliveService( //
        @RequestParam(name = "name", required = true) String name) {
        try {
            services.getService(name).ping();
            return new ResponseEntity<String>("OK", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    private Service removeService(String name) throws ServiceException {
        synchronized (this.services) {
            Service service = this.services.getService(name);
            this.services.remove(service.getName());
            return service;
        }
    }

    private void removePolicies(Service service) {
        synchronized (this.policies) {
            Vector<Policy> policyList = new Vector<>(this.policies.getForService(service.getName()));
            for (Policy policy : policyList) {
                this.policies.remove(policy);
            }
        }
    }

    private Service toService(ServiceRegistrationInfo s) {
        return new Service(s.name, Duration.ofSeconds(s.keepAliveIntervalSeconds), s.callbackUrl);
    }

}
