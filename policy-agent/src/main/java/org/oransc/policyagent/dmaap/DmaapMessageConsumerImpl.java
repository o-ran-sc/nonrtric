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

package org.oransc.policyagent.dmaap;

import com.google.common.collect.Iterables;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.onap.dmaap.mr.client.MRBatchingPublisher;
import org.onap.dmaap.mr.client.MRClientFactory;
import org.onap.dmaap.mr.client.MRConsumer;
import org.onap.dmaap.mr.client.response.MRConsumerResponse;
import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class DmaapMessageConsumerImpl implements DmaapMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DmaapMessageConsumerImpl.class);

    private boolean alive = false;
    private final ApplicationConfig applicationConfig;
    protected MRConsumer consumer;
    private MRBatchingPublisher producer;
    private final long FETCHTIMEOUT = 30000;

    private CountDownLatch sleep = new CountDownLatch(1);

    @Value("${server.port}")
    private int localServerPort;

    @Autowired
    public DmaapMessageConsumerImpl(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    @Scheduled(fixedRate = 1000 * 10)
    @Override
    public void run() {
        if (!alive) {
            init();
        }
        if (this.alive) {
            try {
                Iterable<String> dmaapMsgs = fetchAllMessages();
                if (dmaapMsgs != null && Iterables.size(dmaapMsgs) > 0) {
                    logger.debug("Fetched all the messages from DMAAP and will start to process the messages");
                    for (String msg : dmaapMsgs) {
                        processMsg(msg);
                    }
                }
            } catch (Exception e) {
                logger.error("{}: cannot fetch because of ", this, e.getMessage(), e);
            }
        }
    }

    private Iterable<String> fetchAllMessages() {
        MRConsumerResponse response = null;
        try {
            response = consumer.fetchWithReturnConsumerResponse();
        } catch (Exception e) {
            logger.error("Failed to get message from DMAAP", e);
        }
        if (response == null || !"200".equals(response.getResponseCode())) {
            logger.warn("{}: DMaaP NULL response received", this);
            sleepAfterFailure();
        } else {
            logger.debug("DMaaP consumer received {} : {}", response.getResponseCode(), response.getResponseMessage());
        }
        return response.getActualMessages();
    }

    @Override
    public void init() {
        Properties dmaapConsumerProperties = applicationConfig.getDmaapConsumerConfig();
        Properties dmaapPublisherProperties = applicationConfig.getDmaapPublisherConfig();
        // No need to start if there is no configuration.
        if (dmaapConsumerProperties == null || dmaapPublisherProperties == null || dmaapConsumerProperties.size() == 0
            || dmaapPublisherProperties.size() == 0) {
            logger.error("DMaaP properties Failed to Load");
            return;
        }
        try {
            logger.debug("Creating DMAAP Client");
            logger.debug("dmaapConsumerProperties---> {}", dmaapConsumerProperties.getProperty("topic"));
            logger.debug("dmaapPublisherProperties---> {}", dmaapPublisherProperties.getProperty("topic"));
            consumer = MRClientFactory.createConsumer(dmaapConsumerProperties);
            producer = MRClientFactory.createBatchingPublisher(dmaapConsumerProperties);
            this.alive = true;
        } catch (IOException e) {
            logger.error("Exception occurred while creating Dmaap Consumer", e);
        }
    }

    @Override
    public void processMsg(String msg) throws Exception {
        logger.debug("Message Reveived from DMAAP : {}", msg);
        createDmaapMessageHandler().handleDmaapMsg(msg);
    }

    protected DmaapMessageHandler createDmaapMessageHandler() {
        String agentBaseUrl = "http://localhost:" + this.localServerPort;
        AsyncRestClient agentClient = new AsyncRestClient(agentBaseUrl);
        return new DmaapMessageHandler(this.producer, this.applicationConfig, agentClient);
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    private void sleepAfterFailure() {
        logger.warn("DMAAP message Consumer is put to Sleep for {} milliseconds", FETCHTIMEOUT);
        try {
            sleep.await(FETCHTIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("Failed to put the thread to sleep: {}", e);
        }
    }
}
