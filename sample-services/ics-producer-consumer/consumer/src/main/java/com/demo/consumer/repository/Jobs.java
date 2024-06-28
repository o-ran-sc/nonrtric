/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 *
 * Copyright (C) 2024: OpenInfra Foundation Europe
 *
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

package com.demo.consumer.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.demo.consumer.messages.PropertiesHelper;
import com.demo.consumer.repository.Job.Parameters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Component
public class Jobs {
    private static final Logger logger = LoggerFactory.getLogger(Jobs.class);

    private Map<String, Job> allJobs = new HashMap<>();

    public Jobs() {
    }

    public synchronized Job getJob(String id) throws Exception {
        Job job = allJobs.get(id);
        if (job == null) {
            throw new Exception("Could not find job: " + id);
        }
        return job;
    }

    public synchronized Job get(String id) {
        return allJobs.get(id);
    }

    public void addJob(String id, InfoType type, String owner, Parameters parameters) {
        Job job = new Job(id, type, owner, parameters);
        setKafkaServersEnvironment(job);
        this.put(job);
    }

    private void setKafkaServersEnvironment(Job job) {
        String kafkaServers = job.getParameters().getDeliveryInfo().getBootStrapServers();
        if (kafkaServers != null && !kafkaServers.isEmpty()) {
            PropertiesHelper.setKafkaServers(kafkaServers);
            logger.info("Setting variable bootStrapServers: {}", kafkaServers);
        } else {
            logger.warn("bootStrapServers is not set for job: {}", job.getId());
        }
    }

    private synchronized void put(Job job) {
        logger.debug("Put job: {}", job.getId());
        allJobs.put(job.getId(), job);
    }

    public synchronized Iterable<Job> getAll() {
        return new Vector<>(allJobs.values());
    }

    public synchronized int size() {
        return allJobs.size();
    }

    public synchronized Job delete(String id) {
        return allJobs.remove(id);
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(allJobs);
    }
}
