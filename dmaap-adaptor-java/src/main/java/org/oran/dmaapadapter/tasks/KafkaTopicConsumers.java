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

import lombok.Getter;

import org.oran.dmaapadapter.configuration.ApplicationConfig;
import org.oran.dmaapadapter.repository.InfoType;
import org.oran.dmaapadapter.repository.InfoTypes;
import org.oran.dmaapadapter.repository.Job;
import org.oran.dmaapadapter.repository.Jobs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
@Component
@EnableScheduling
public class KafkaTopicConsumers {
    private static final Logger logger = LoggerFactory.getLogger(KafkaTopicConsumers.class);

    private final Map<String, KafkaTopicListener> topicListeners = new HashMap<>(); // Key is typeId

    @Getter
    private final Map<String, KafkaJobDataConsumer> activeSubscriptions = new HashMap<>(); // Key is jobId

    private static final int CONSUMER_SUPERVISION_INTERVAL_MS = 1000 * 60 * 3;

    public KafkaTopicConsumers(@Autowired ApplicationConfig appConfig, @Autowired InfoTypes types,
            @Autowired Jobs jobs) {

        for (InfoType type : types.getAll()) {
            if (type.isKafkaTopicDefined()) {
                KafkaTopicListener topicConsumer = new KafkaTopicListener(appConfig, type);
                topicListeners.put(type.getId(), topicConsumer);
            }
        }

        jobs.addObserver(new Jobs.Observer() {
            @Override
            public void onJobbAdded(Job job) {
                addJob(job);
            }

            @Override
            public void onJobRemoved(Job job) {
                removeJob(job);
            }

        });
    }

    public synchronized void addJob(Job job) {
        if (this.activeSubscriptions.get(job.getId()) == null && job.getType().isKafkaTopicDefined()) {
            logger.debug("Kafka job added {}", job.getId());
            KafkaTopicListener topicConsumer = topicListeners.get(job.getType().getId());
            KafkaJobDataConsumer subscription = new KafkaJobDataConsumer(job);
            subscription.start(topicConsumer.getOutput());
            activeSubscriptions.put(job.getId(), subscription);
        }
    }

    public synchronized void removeJob(Job job) {
        KafkaJobDataConsumer d = activeSubscriptions.remove(job.getId());
        if (d != null) {
            logger.debug("Kafka job removed {}", job.getId());
            d.stop();
        }
    }

    @Scheduled(fixedRate = CONSUMER_SUPERVISION_INTERVAL_MS)
    public synchronized void restartNonRunningTasks() {

        for (KafkaJobDataConsumer consumer : activeSubscriptions.values()) {
            if (!consumer.isRunning()) {
                restartTopic(consumer);
            }
        }
    }

    private void restartTopic(KafkaJobDataConsumer consumer) {
        KafkaTopicListener topic = this.topicListeners.get(consumer.getJob().getType().getId());
        topic.start();
        this.activeSubscriptions.forEach((jobId, aConsumer) -> aConsumer.start(topic.getOutput()));
    }

}
