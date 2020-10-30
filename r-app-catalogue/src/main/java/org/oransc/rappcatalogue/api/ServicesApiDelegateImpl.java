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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.oransc.rappcatalogue.exception.InvalidServiceException;
import org.oransc.rappcatalogue.exception.ServiceNotFoundException;
import org.oransc.rappcatalogue.model.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@org.springframework.stereotype.Service
public class ServicesApiDelegateImpl implements ServicesApiDelegate {

    private HashMap<String, Service> registeredServices = new HashMap<>();

    @Override
    public ResponseEntity<Service> getIndividualService(String serviceName) {
        Service service = registeredServices.get(serviceName);
        if (service != null) {
            return ResponseEntity.ok(service);
        } else {
            throw new ServiceNotFoundException(serviceName);
        }
    }

    @Override
    public ResponseEntity<List<String>> getServiceNames() {
        return ResponseEntity.ok(new ArrayList<>(registeredServices.keySet()));
    }

    @Override
    public ResponseEntity<Void> putIndividualService(String serviceName, Service service) {
        if (isServiceValid(service)) {
            Service oldService = registeredServices.get(serviceName);
            registeredServices.put(serviceName, service);
            if (oldService == null) {
                return new ResponseEntity<>(HttpStatus.CREATED);
            } else {
                return new ResponseEntity<>(HttpStatus.OK);
            }
        } else {
            throw new InvalidServiceException();
        }
    }

    @Override
    public ResponseEntity<Void> deleteIndividualService(String serviceName) {
        registeredServices.remove(serviceName);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /*
     * java:S2589: Boolean expressions should not be gratuitous.
     * Even though the version property is marked as @NotNull, it might be null coming from the client, hence the null
     * check is needed.
     */
    @SuppressWarnings("java:S2589")
    private boolean isServiceValid(Service service) {
        String version = service.getVersion();
        return version != null && !version.isBlank();
    }
}
