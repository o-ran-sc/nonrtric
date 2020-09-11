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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.oransc.enrichment.exceptions.ServiceException;

/**
 * Dynamic representation of all Rics in the system.
 */
public class EiJobs {
    private Map<String, EiJob> allEiJobs = new HashMap<>();
    private Map<String, Map<String, EiJob>> jobsByType = new HashMap<>();

    public synchronized void put(EiJob job) {
        allEiJobs.put(job.id(), job);
        multiMapPut(this.jobsByType, job.typeId(), job);
    }

    public synchronized Collection<EiJob> getJobs() {
        return new Vector<>(allEiJobs.values());
    }

    public synchronized EiJob getJob(String id) throws ServiceException {
        EiJob ric = allEiJobs.get(id);
        if (ric == null) {
            throw new ServiceException("Could not find EI Job: " + id);
        }
        return ric;
    }

    public synchronized Collection<EiJob> getJobsForType(String typeId) {
        return multiMapGet(this.jobsByType, typeId);
    }

    public synchronized EiJob get(String id) {
        return allEiJobs.get(id);
    }

    public synchronized EiJob remove(String id) {
        EiJob job = allEiJobs.get(id);
        if (job != null) {
            remove(job);
        }
        return job;
    }

    public synchronized void remove(EiJob job) {
        this.allEiJobs.remove(job.id());
        multiMapRemove(this.jobsByType, job.typeId(), job);
    }

    public synchronized int size() {
        return allEiJobs.size();
    }

    public synchronized void clear() {
        this.allEiJobs.clear();
    }

    private void multiMapPut(Map<String, Map<String, EiJob>> multiMap, String key, EiJob value) {
        multiMap.computeIfAbsent(key, k -> new HashMap<>()).put(value.id(), value);
    }

    private void multiMapRemove(Map<String, Map<String, EiJob>> multiMap, String key, EiJob value) {
        Map<String, EiJob> map = multiMap.get(key);
        if (map != null) {
            map.remove(value.id());
            if (map.isEmpty()) {
                multiMap.remove(key);
            }
        }
    }

    private Collection<EiJob> multiMapGet(Map<String, Map<String, EiJob>> multiMap, String key) {
        Map<String, EiJob> map = multiMap.get(key);
        if (map == null) {
            return Collections.emptyList();
        }
        return new Vector<>(map.values());
    }

}
