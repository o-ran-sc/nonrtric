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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.oransc.policyagent.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Policies {
    private static final Logger logger = LoggerFactory.getLogger(Policies.class);

    private Map<String, Policy> policiesId = new HashMap<>();
    private Map<String, Map<String, Policy>> policiesRic = new HashMap<>();
    private Map<String, Map<String, Policy>> policiesService = new HashMap<>();
    private Map<String, Map<String, Policy>> policiesType = new HashMap<>();

    public Policies() {
    }

    public synchronized void put(Policy policy) {
        policiesId.put(policy.id(), policy);
        multiMapPut(policiesRic, policy.ric().name(), policy);
        multiMapPut(policiesService, policy.ownerServiceName(), policy);
        multiMapPut(policiesType, policy.type().name(), policy);
    }

    private void multiMapPut(Map<String, Map<String, Policy>> multiMap, String key, Policy value) {
        Map<String, Policy> map = multiMap.get(key);
        if (map == null) {
            map = new HashMap<>();
            multiMap.put(key, map);
        }
        map.put(value.id(), value);
    }

    private void multiMapRemove(Map<String, Map<String, Policy>> multiMap, String key, Policy value) {
        Map<String, Policy> map = multiMap.get(key);
        if (map != null) {
            map.remove(value.id());
            if (map.isEmpty()) {
                multiMap.remove(key);
            }
        }
    }

    private Collection<Policy> multiMapGet(Map<String, Map<String, Policy>> multiMap, String key) {
        Map<String, Policy> map = multiMap.get(key);
        if (map == null) {
            return new Vector<Policy>();
        }
        return map.values();
    }

    public synchronized Policy get(String id) throws ServiceException {
        Policy p = policiesId.get(id);
        if (p == null) {
            throw new ServiceException("Could not find policy: " + id);
        }
        return p;
    }

    public synchronized Collection<Policy> getAll() {
        return policiesId.values();
    }

    public synchronized Collection<Policy> getForService(String service) {
        return multiMapGet(policiesService, service);
    }

    public synchronized Collection<Policy> getForRic(String ric) {
        return multiMapGet(policiesRic, ric);
    }

    public synchronized Collection<Policy> getForType(String type) {
        return multiMapGet(policiesType, type);
    }

    public synchronized Policy removeId(String id) {
        Policy p = policiesId.get(id);
        if (p != null) {
            remove(p);
        }
        return p;
    }

    public synchronized void remove(Policy policy) {
        policiesId.remove(policy.id());
        multiMapRemove(policiesRic, policy.ric().name(), policy);
        multiMapRemove(policiesService, policy.ownerServiceName(), policy);
        multiMapRemove(policiesType, policy.type().name(), policy);
    }

    public synchronized int size() {
        return policiesId.size();
    }

    public void clear() {
        for (String id : policiesId.keySet()) {
            removeId(id);
        }
    }

}
