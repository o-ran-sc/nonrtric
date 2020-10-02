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

import lombok.Getter;

public class EiType {
    @Getter
    private final String id;

    @Getter
    private final Object jobDataSchema;

    private final Map<String, EiProducer> producers = new HashMap<>();

    public EiType(String id, Object jobDataSchema) {
        this.id = id;
        this.jobDataSchema = jobDataSchema;
    }

    public synchronized Collection<EiProducer> getProducers() {
        return Collections.unmodifiableCollection(producers.values());
    }

    public synchronized Collection<String> getProducerIds() {
        return Collections.unmodifiableCollection(producers.keySet());
    }

    public synchronized void addProducer(EiProducer producer) {
        this.producers.put(producer.getId(), producer);
    }

    public synchronized EiProducer removeProducer(EiProducer producer) {
        return this.producers.remove(producer.getId());
    }
}
