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

import java.time.Duration;
import java.util.Collection;
import java.util.Vector;

import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.Service;
import org.oransc.policyagent.repository.Services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceController {

    private final ApplicationConfig appConfig;
    private final Services services;
    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    @Autowired
    ServiceController(ApplicationConfig config, Services services) {
        this.appConfig = config;
        this.services = services;
    }

    @GetMapping("/service")
    public ResponseEntity<String> getService( //
        @RequestParam(name = "name", required = true) String name) {
        try {
            Service s = services.getService(name);
            String res = gson.toJson(toServiceStatus(s));
            return new ResponseEntity<String>(res, HttpStatus.OK);

        } catch (ServiceException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.NO_CONTENT);
        }
    }

    private ServiceStatus toServiceStatus(Service s) {
        return ImmutableServiceStatus.builder() //
            .name(s.getName()) //
            .keepAliveInterval(s.getKeepAliveInterval().toSeconds()) //
            .timeSincePing(s.timeSinceLastPing().toSeconds()) //
            .build();
    }

    @PutMapping("/service")
    public ResponseEntity<String> putService( //
        @RequestBody String jsonBody) {
        try {
            ServiceRegistrationInfo s = gson.fromJson(jsonBody, ImmutableServiceRegistrationInfo.class);
            this.services.put(toService(s));
            return new ResponseEntity<String>("OK", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.NO_CONTENT);
        }
    }

    private Service toService(ServiceRegistrationInfo s) {
        return new Service(s.name(), Duration.ofSeconds(s.keepAliveInterval()));
    }

    @GetMapping("/services")
    public ResponseEntity<?> getServices() {
        Collection<Service> allServices = this.services.getAll();
        Collection<ServiceStatus> result = new Vector<>(allServices.size());
        for (Service s : allServices) {
            result.add(toServiceStatus(s));
        }
        return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
    }

    @PutMapping("/service/ping")
    public ResponseEntity<String> ping( //
        @RequestBody String name) {
        try {
            Service s = services.getService(name);
            s.ping();
            return new ResponseEntity<String>("OK", HttpStatus.OK);
        } catch (ServiceException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.NO_CONTENT);
        }
    }

}
