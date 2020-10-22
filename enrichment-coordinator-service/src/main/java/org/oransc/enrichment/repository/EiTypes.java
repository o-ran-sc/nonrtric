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

package org.oransc.enrichment.repository;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.oransc.enrichment.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic representation of all EI types in the system.
 */
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
public class EiTypes {
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Map<String, EiType> allEiTypes = new HashMap<>();

    public synchronized void put(EiType type) {
        allEiTypes.put(type.getId(), type);
    }

    public synchronized Collection<EiType> getAllEiTypes() {
        return new Vector<>(allEiTypes.values());
    }

    public synchronized EiType getType(String id) throws ServiceException {
        EiType type = allEiTypes.get(id);
        if (type == null) {
            throw new ServiceException("Could not find EI type: " + id);
        }
        return type;
    }

    public synchronized EiType get(String id) {
        return allEiTypes.get(id);
    }

    public synchronized void remove(String id) {
        allEiTypes.remove(id);
    }

    public synchronized void remove(EiType type) {
        this.remove(type.getId());
    }

    public synchronized int size() {
        return allEiTypes.size();
    }

    public synchronized void clear() {
        this.allEiTypes.clear();
    }

    public void deregisterType(EiType type, EiJobs eiJobs) {
        this.remove(type);
        for (EiJob job : eiJobs.getJobsForType(type.getId())) {
            eiJobs.remove(job);
            this.logger.warn("Deleted job {} because no producers left", job.id());
        }
    }

}
