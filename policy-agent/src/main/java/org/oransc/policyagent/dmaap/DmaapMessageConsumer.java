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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.onap.dmaap.mr.client.MRBatchingPublisher;
import org.onap.dmaap.mr.client.MRClientFactory;
import org.onap.dmaap.mr.client.MRConsumer;
import org.onap.dmaap.mr.client.response.MRConsumerResponse;
import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DmaapMessageConsumer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DmaapMessageConsumer.class);

    final Duration ERROR_TIMEOUT = Duration.ofSeconds(30);
    final Duration TIME_BETWEEN_DMAAP_POLLS = Duration.ofSeconds(10);
    private final ApplicationConfig applicationConfig;

    @Value("${server.port}")
    private int localServerPort;

    @Autowired
    public DmaapMessageConsumer(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;

        Thread thread = new Thread(this);
        thread.start();
    }

    private boolean isDmaapConfigured() {
        Properties consumerCfg = applicationConfig.getDmaapConsumerConfig();
        Properties producerCfg = applicationConfig.getDmaapPublisherConfig();
        return (consumerCfg != null && consumerCfg.size() > 0 && producerCfg != null && producerCfg.size() > 0);
    }

    @Override
    public void run() {
        while (sleep(TIME_BETWEEN_DMAAP_POLLS) && isDmaapConfigured()) {
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
                sleep(ERROR_TIMEOUT);
            }
        }
    }

    private Iterable<String> fetchAllMessages() throws ServiceException, FileNotFoundException, IOException {
        Properties dmaapConsumerProperties = this.applicationConfig.getDmaapConsumerConfig();
        MRConsumer consumer = MRClientFactory.createConsumer(dmaapConsumerProperties);
        MRConsumerResponse response = consumer.fetchWithReturnConsumerResponse();
        if (response == null || !"200".equals(response.getResponseCode())) {
            throw new ServiceException("DMaaP NULL response received");
        } else {
            logger.debug("DMaaP consumer received {} : {}", response.getResponseCode(), response.getResponseMessage());
            return response.getActualMessages();
        }
    }

    private void processMsg(String msg) throws Exception {
        logger.debug("Message Reveived from DMAAP : {}", msg);
        createDmaapMessageHandler().handleDmaapMsg(msg);
    }

    private DmaapMessageHandler createDmaapMessageHandler() throws FileNotFoundException, IOException {
        String agentBaseUrl = "http://localhost:" + this.localServerPort;
        AsyncRestClient agentClient = new AsyncRestClient(agentBaseUrl);
        Properties dmaapPublisherProperties = applicationConfig.getDmaapPublisherConfig();
        MRBatchingPublisher producer = MRClientFactory.createBatchingPublisher(dmaapPublisherProperties);

        return new DmaapMessageHandler(producer, this.applicationConfig, agentClient);
    }

    private boolean sleep(Duration duration) {
        CountDownLatch sleep = new CountDownLatch(1);
        try {
            sleep.await(duration.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception e) {
            logger.error("msg", e);
            return false;
        }
    }
}
