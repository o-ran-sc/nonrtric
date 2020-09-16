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
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.oransc.enrichment.exceptions.ServiceException;

/**
 * Dynamic representation of all Rics in the system.
 */
public class EiProducers {
    private Map<String, EiProducer> allEiProducers = new HashMap<>();

    public synchronized void put(EiProducer producer) {
        allEiProducers.put(producer.id(), producer);

    }

    public synchronized Collection<EiProducer> getAllProducers() {
        return new Vector<>(allEiProducers.values());
    }

    public synchronized EiProducer getProducer(String id) throws ServiceException {
        EiProducer p = allEiProducers.get(id);
        if (p == null) {
            throw new ServiceException("Could not find EI producer: " + id);
        }
        return p;
    }

    public synchronized EiProducer get(String id) {
        return allEiProducers.get(id);
    }

    public synchronized void remove(String id) {
        this.allEiProducers.remove(id);
    }

    public synchronized void remove(EiProducer producer) {
        this.allEiProducers.remove(producer.id());
    }

    public synchronized int size() {
        return allEiProducers.size();
    }

    public synchronized void clear() {
        this.allEiProducers.clear();
    }
}
