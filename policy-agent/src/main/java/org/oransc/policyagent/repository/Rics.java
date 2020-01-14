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

import org.oransc.policyagent.exceptions.ServiceException;

/**
 * Dynamic representation of all Rics in the system.
 */
public class Rics {
    Map<String, Ric> rics = new HashMap<>();

    public synchronized void put(Ric ric) {
        rics.put(ric.name(), ric);
    }

    public Collection<Ric> getRics() {
        return rics.values();
    }

    public Ric getRic(String name) throws ServiceException {
        Ric ric = rics.get(name);
        if (ric == null) {
            throw new ServiceException("Could not find ric: " + name);
        }
        return ric;
    }

    public Ric get(String name) {
        return rics.get(name);
    }

    public int size() {
        return rics.size();
    }

    public void clear() {
        this.rics.clear();
    }
}
