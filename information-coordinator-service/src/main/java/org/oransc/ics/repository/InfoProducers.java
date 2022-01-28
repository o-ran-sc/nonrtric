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

package org.oransc.ics.repository;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import lombok.Builder;
import lombok.Getter;

import org.oransc.ics.controllers.a1e.A1eCallbacks;
import org.oransc.ics.controllers.r1producer.ProducerCallbacks;
import org.oransc.ics.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Dynamic representation of all EiProducers.
 */
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
@Component
public class InfoProducers {
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Map<String, InfoProducer> allEiProducers = new HashMap<>();
    private final MultiMap<InfoProducer> producersByType = new MultiMap<>();

    @Autowired
    private ProducerCallbacks producerCallbacks;

    @Autowired
    private A1eCallbacks consumerCallbacks;

    @Autowired
    private InfoJobs infoJobs;

    @Builder
    @Getter
    public static class InfoProducerRegistrationInfo {
        String id;

        Collection<InfoType> supportedTypes;

        String jobCallbackUrl;

        String producerSupervisionCallbackUrl;
    }

    public InfoProducer registerProducer(InfoProducerRegistrationInfo producerInfo) {
        final String producerId = producerInfo.getId();
        InfoProducer previousDefinition = this.get(producerId);
        if (previousDefinition != null) {
            for (InfoType type : previousDefinition.getInfoTypes()) {
                producersByType.remove(type.getId(), producerId);
            }
            allEiProducers.remove(producerId);
        }

        InfoProducer producer = createProducer(producerInfo);
        allEiProducers.put(producer.getId(), producer);
        for (InfoType type : producer.getInfoTypes()) {
            producersByType.put(type.getId(), producer.getId(), producer);
        }

        Collection<InfoType> previousTypes =
            previousDefinition != null ? previousDefinition.getInfoTypes() : new ArrayList<>();

        producerCallbacks.startInfoJobs(producer, this.infoJobs) //
            .collectList() //
            .flatMapMany(list -> consumerCallbacks.notifyJobStatus(producer.getInfoTypes(), this)) //
            .collectList() //
            .flatMapMany(list -> consumerCallbacks.notifyJobStatus(previousTypes, this)) //
            .subscribe();

        return producer;
    }

    private InfoProducer createProducer(InfoProducerRegistrationInfo producerInfo) {
        return new InfoProducer(producerInfo.getId(), producerInfo.getSupportedTypes(),
            producerInfo.getJobCallbackUrl(), producerInfo.getProducerSupervisionCallbackUrl());
    }

    public synchronized Collection<InfoProducer> getAllProducers() {
        return new Vector<>(allEiProducers.values());
    }

    public synchronized InfoProducer getProducer(String id) throws ServiceException {
        InfoProducer p = allEiProducers.get(id);
        if (p == null) {
            throw new ServiceException("Could not find Information Producer: " + id, HttpStatus.NOT_FOUND);
        }
        return p;
    }

    public synchronized InfoProducer get(String id) {
        return allEiProducers.get(id);
    }

    public synchronized int size() {
        return allEiProducers.size();
    }

    public synchronized void clear() {
        this.allEiProducers.clear();
        this.producersByType.clear();
    }

    public void deregisterProducer(InfoProducer producer) {
        allEiProducers.remove(producer.getId());
        for (InfoType type : producer.getInfoTypes()) {
            if (producersByType.remove(type.getId(), producer.getId()) == null) {
                this.logger.error("Bug, no producer found");
            }
        }
        this.consumerCallbacks.notifyJobStatus(producer.getInfoTypes(), this) //
            .subscribe();
    }

    public synchronized Collection<InfoProducer> getProducersForType(InfoType type) {
        return this.producersByType.get(type.getId());
    }

    public synchronized Collection<InfoProducer> getProducersForType(String typeId) {
        return this.producersByType.get(typeId);
    }

    public synchronized Collection<String> getProducerIdsForType(String typeId) {
        Collection<String> producerIds = new ArrayList<>();
        for (InfoProducer p : this.getProducersForType(typeId)) {
            producerIds.add(p.getId());
        }
        return producerIds;
    }

    public synchronized boolean isJobEnabled(InfoJob job) {
        for (InfoProducer producer : this.producersByType.get(job.getTypeId())) {
            if (producer.isJobEnabled(job)) {
                return true;
            }
        }
        return false;
    }

}
