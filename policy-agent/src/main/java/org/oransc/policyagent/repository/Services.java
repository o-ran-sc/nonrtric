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

package org.oransc.policyagent.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.oransc.policyagent.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Services {
    private static final Logger logger = LoggerFactory.getLogger(Services.class);

    private Map<String, Service> registeredServices = new HashMap<>();

    public synchronized Service getService(String name) throws ServiceException {
        Service s = registeredServices.get(name);
        if (s == null) {
            throw new ServiceException("Could not find service: " + name);
        }
        return s;
    }

    public synchronized Service get(String name) {
        return registeredServices.get(name);
    }

    public synchronized void put(Service service) {
        logger.debug("Put service: {}", service.getName());
        service.keepAlive();
        registeredServices.put(service.getName(), service);
    }

    public synchronized Iterable<Service> getAll() {
        return new Vector<>(registeredServices.values());
    }

    public synchronized void remove(String name) {
        registeredServices.remove(name);
    }

    public synchronized int size() {
        return registeredServices.size();
    }

    public void clear() {
        registeredServices.clear();
    }
}
