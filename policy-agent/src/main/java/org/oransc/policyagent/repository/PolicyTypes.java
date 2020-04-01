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

public class PolicyTypes {
    private Map<String, PolicyType> types = new HashMap<>();

    public synchronized PolicyType getType(String name) throws ServiceException {
        PolicyType t = types.get(name);
        if (t == null) {
            throw new ServiceException("Could not find type: " + name);
        }
        return t;
    }

    public synchronized PolicyType get(String name) {
        return types.get(name);
    }

    public synchronized void put(PolicyType type) {
        types.put(type.name(), type);
    }

    public synchronized boolean contains(String policyType) {
        return types.containsKey(policyType);
    }

    public synchronized Collection<PolicyType> getAll() {
        return new Vector<>(types.values());
    }

    public synchronized int size() {
        return types.size();
    }

    public synchronized void clear() {
        this.types.clear();
    }
}
