/*-
 * ========================LICENSE_START=================================
 * Copyright (C) 2020 Nordix Foundation. All rights reserved.
 * ======================================================================
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

package org.oransc.rappcatalogue.api;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.oransc.rappcatalogue.exception.HeaderException;
import org.oransc.rappcatalogue.exception.InvalidServiceException;
import org.oransc.rappcatalogue.exception.ServiceNotFoundException;
import org.oransc.rappcatalogue.model.InputService;
import org.oransc.rappcatalogue.model.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.NativeWebRequest;

@org.springframework.stereotype.Service
public class ServicesApiDelegateImpl implements ServicesApiDelegate {

    private static final String LOCATION_HEADER = "Location";

    @Autowired
    private NativeWebRequest nativeWebRequest;

    private ConcurrentHashMap<String, Service> registeredServices = new ConcurrentHashMap<>();

    ServicesApiDelegateImpl(NativeWebRequest nativeWebRequest) {
        this.nativeWebRequest = nativeWebRequest;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.of(nativeWebRequest);
    }

    @Override
    public ResponseEntity<Service> getIndividualService(String serviceName) throws ServiceNotFoundException {
        Service service = registeredServices.get(serviceName);
        if (service != null) {
            return ResponseEntity.ok(service);
        } else {
            throw new ServiceNotFoundException(serviceName);
        }
    }

    @Override
    public ResponseEntity<List<Service>> getServices() {
        return ResponseEntity.ok(new ArrayList<>(registeredServices.values()));
    }

    @Override
    public ResponseEntity<Void> putIndividualService(String serviceName, InputService inputService)
        throws InvalidServiceException, HeaderException {
        if (isServiceValid(inputService)) {
            if (registeredServices.put(serviceName, createService(serviceName, inputService)) == null) {
                try {
                    Optional<NativeWebRequest> request = getRequest();
                    if (request.isPresent()) {
                        addLocationHeaderToResponse(serviceName, request.get());
                    }
                } catch (HeaderException e) {
                    registeredServices.remove(serviceName);
                    throw e;
                }
                return new ResponseEntity<>(HttpStatus.CREATED);
            } else {
                return new ResponseEntity<>(HttpStatus.OK);
            }
        } else {
            throw new InvalidServiceException();
        }
    }

    private void addLocationHeaderToResponse(String serviceName, NativeWebRequest request) throws HeaderException {
        try {
            HttpServletRequest nativeRequest = request.getNativeRequest(HttpServletRequest.class);
            HttpServletResponse nativeResponse = request.getNativeResponse(HttpServletResponse.class);
            if (nativeRequest != null && nativeResponse != null) {
                StringBuffer requestURL = nativeRequest.getRequestURL();
                nativeResponse.addHeader(LOCATION_HEADER, requestURL.toString());
                nativeResponse.getWriter().print("");
            } else {
                throw new HeaderException(LOCATION_HEADER, serviceName,
                    new Exception("Native Request or Response missing"));
            }
        } catch (IOException e) {
            throw new HeaderException(LOCATION_HEADER, serviceName, e);
        }
    }

    @Override
    public ResponseEntity<Void> deleteIndividualService(String serviceName) {
        registeredServices.remove(serviceName);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /*
     * java:S2589: Boolean expressions should not be gratuitous. Even though the
     * version property is marked as @NotNull, it might be null coming from the
     * client, hence the null check is needed.
     */
    @SuppressWarnings("java:S2589")
    private boolean isServiceValid(InputService service) {
        String version = service.getVersion();
        return version != null && !version.isBlank();
    }

    private Service createService(String serviceName, InputService inputService) {
        Service service = new Service();
        service.setName(serviceName);
        service.setDescription(inputService.getDescription());
        service.setDisplayName(inputService.getDisplayName());
        service.setVersion(inputService.getVersion());
        service.setRegistrationDate(getTodaysDate());
        return service;
    }

    private String getTodaysDate() {
        long millis = System.currentTimeMillis();
        Date date = new Date(millis);
        return date.toString();
    }
}
