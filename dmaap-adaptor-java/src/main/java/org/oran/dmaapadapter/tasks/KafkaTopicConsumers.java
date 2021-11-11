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

package org.oran.dmaapadapter.tasks;

import java.util.HashMap;
import java.util.Map;

import org.oran.dmaapadapter.configuration.ApplicationConfig;
import org.oran.dmaapadapter.repository.InfoType;
import org.oran.dmaapadapter.repository.InfoTypes;
import org.oran.dmaapadapter.repository.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

/**
 * The class fetches incoming requests from DMAAP and sends them further to the
 * consumers that has a job for this InformationType.
 */
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
@Component
public class KafkaTopicConsumers {
    private static final Logger logger = LoggerFactory.getLogger(KafkaTopicConsumers.class);

    private final Map<String, KafkaTopicConsumer> topicConsumers = new HashMap<>();
    private final Map<String, Disposable> activeSubscriptions = new HashMap<>();
    private final ApplicationConfig appConfig;

    public KafkaTopicConsumers(@Autowired ApplicationConfig appConfig) {
        this.appConfig = appConfig;
    }

    public void start(InfoTypes types) {
        for (InfoType type : types.getAll()) {
            if (type.isKafkaTopicDefined()) {
                KafkaTopicConsumer topicConsumer = new KafkaTopicConsumer(appConfig, type);
                topicConsumers.put(type.getId(), topicConsumer);
            }
        }
    }

    public synchronized void addJob(Job job) {
        if (this.activeSubscriptions.get(job.getId()) == null && job.getType().isKafkaTopicDefined()) {
            logger.debug("Kafka job added {}", job.getId());
            KafkaTopicConsumer topicConsumer = topicConsumers.get(job.getType().getId());
            Disposable subscription = topicConsumer.startDistributeToConsumer(job);
            activeSubscriptions.put(job.getId(), subscription);
        }
    }

    public synchronized void removeJob(Job job) {
        Disposable d = activeSubscriptions.remove(job.getId());
        if (d != null) {
            logger.debug("Kafka job removed {}", job.getId());
            d.dispose();
        }
    }

}
