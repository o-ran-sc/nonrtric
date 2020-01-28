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

import java.io.IOException;
import java.util.Properties;
import org.onap.dmaap.mr.client.MRClientFactory;
import org.onap.dmaap.mr.client.MRConsumer;
import org.onap.dmaap.mr.client.response.MRConsumerResponse;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private MRConsumerResponse response = null;
    @Autowired
    private DmaapMessageHandler dmaapMessageHandler;

    @Autowired
    public DmaapMessageConsumerImpl(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    @Scheduled(fixedRate = 1000 * 10) // , initialDelay=60000)
    @Override
    public void run() {
        if (!alive) {
            init();
        }
        if (this.alive) {
            try {
                Iterable<String> dmaapMsgs = fetchAllMessages();
                logger.debug("Fetched all the messages from DMAAP and will start to process the messages");
                for (String msg : dmaapMsgs) {
                    processMsg(msg);
                }
            } catch (Exception e) {
                logger.error("{}: cannot fetch because of ", this, e.getMessage(), e);
            }
        }
    }

    private Iterable<String> fetchAllMessages() {
        response = consumer.fetchWithReturnConsumerResponse();
        if (response == null) {
            logger.warn("{}: DMaaP NULL response received", this);
        } else {
            logger.debug("DMaaP consumer received {} : {}", response.getResponseCode(), response.getResponseMessage());
            if (!"200".equals(response.getResponseCode())) {
                logger.error("DMaaP consumer received: {} : {}", response.getResponseCode(),
                        response.getResponseMessage());
            }
        }
        return response.getActualMessages();
    }

    // Properties are not loaded in first atempt. Need to fix this and then uncomment the post construct annotation
    // @PostConstruct
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
            System.out.println("dmaapConsumerProperties--->"+dmaapConsumerProperties.getProperty("topic"));
            System.out.println("dmaapPublisherProperties--->"+dmaapPublisherProperties.getProperty("topic"));
            consumer = MRClientFactory.createConsumer(dmaapConsumerProperties);
            this.alive = true;
        } catch (IOException e) {
            logger.error("Exception occurred while creating Dmaap Consumer", e);
        }
    }

    @Override
    public void processMsg(String msg) throws Exception {
        logger.debug("Message Reveived from DMAAP : {}", msg);
        // Call the concurrent Task executor to handle the incoming request
        dmaapMessageHandler.handleDmaapMsg(msg);
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public void stopConsumer() {
        alive = false;
    }
}
