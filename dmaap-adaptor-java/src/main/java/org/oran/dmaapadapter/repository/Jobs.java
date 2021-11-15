/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2021 Nordix Foundation
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

package org.oran.dmaapadapter.repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.oran.dmaapadapter.clients.AsyncRestClient;
import org.oran.dmaapadapter.clients.AsyncRestClientFactory;
import org.oran.dmaapadapter.configuration.ApplicationConfig;
import org.oran.dmaapadapter.exceptions.ServiceException;
import org.oran.dmaapadapter.repository.Job.Parameters;
import org.oran.dmaapadapter.tasks.KafkaTopicConsumers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Jobs {
    private static final Logger logger = LoggerFactory.getLogger(Jobs.class);

    private Map<String, Job> allJobs = new HashMap<>();
    private MultiMap<Job> jobsByType = new MultiMap<>();
    private final KafkaTopicConsumers kafkaConsumers;
    private final AsyncRestClientFactory restclientFactory;

    public Jobs(@Autowired KafkaTopicConsumers kafkaConsumers, @Autowired ApplicationConfig applicationConfig) {
        this.kafkaConsumers = kafkaConsumers;

        restclientFactory = new AsyncRestClientFactory(applicationConfig.getWebClientConfig());
    }

    public synchronized Job getJob(String id) throws ServiceException {
        Job job = allJobs.get(id);
        if (job == null) {
            throw new ServiceException("Could not find job: " + id);
        }
        return job;
    }

    public synchronized Job get(String id) {
        return allJobs.get(id);
    }

    public void addJob(String id, String callbackUrl, InfoType type, String owner, String lastUpdated,
            Parameters parameters) {
        AsyncRestClient consumerRestClient = type.isUseHttpProxy() //
                ? restclientFactory.createRestClientUseHttpProxy(callbackUrl) //
                : restclientFactory.createRestClientNoHttpProxy(callbackUrl);
        Job job = new Job(id, callbackUrl, type, owner, lastUpdated, parameters, consumerRestClient);
        this.put(job);
    }

    private synchronized void put(Job job) {
        logger.debug("Put job: {}", job.getId());
        allJobs.put(job.getId(), job);
        jobsByType.put(job.getType().getId(), job.getId(), job);
        kafkaConsumers.addJob(job);
    }

    public synchronized Iterable<Job> getAll() {
        return new Vector<>(allJobs.values());
    }

    public synchronized Job remove(String id) {
        Job job = allJobs.get(id);
        if (job != null) {
            remove(job);
        }
        return job;
    }

    public synchronized void remove(Job job) {
        this.allJobs.remove(job.getId());
        jobsByType.remove(job.getType().getId(), job.getId());
        kafkaConsumers.removeJob(job);
    }

    public synchronized int size() {
        return allJobs.size();
    }

    public synchronized Collection<Job> getJobsForType(InfoType type) {
        return jobsByType.get(type.getId());
    }

    public synchronized void clear() {
        allJobs.clear();
        jobsByType.clear();
    }
}
