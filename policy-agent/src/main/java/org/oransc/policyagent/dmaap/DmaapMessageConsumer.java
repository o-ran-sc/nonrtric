/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2020 Nordix Foundation
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
import java.time.Duration;
import java.util.Properties;

import org.onap.dmaap.mr.client.MRBatchingPublisher;
import org.onap.dmaap.mr.client.MRClientFactory;
import org.onap.dmaap.mr.client.MRConsumer;
import org.onap.dmaap.mr.client.response.MRConsumerResponse;
import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.tasks.RefreshConfigTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The class fetches incoming requests from DMAAP. It uses the timeout parameter that lets the MessageRouter keep the
 * connection with the Kafka open until requests are sent in.
 *
 * If there is no DMaaP configuration in the application configuration, then this service will regularly check the
 * configuration and start polling DMaaP if the configuration is added. If the DMaaP configuration is removed, then the
 * service will stop polling and resume checking for configuration.
 *
 * Each received request is processed by {@link DmaapMessageHandler}.
 */
@Component
public class DmaapMessageConsumer {

    protected static final Duration TIME_BETWEEN_DMAAP_RETRIES = Duration.ofSeconds(10);

    private static final Logger logger = LoggerFactory.getLogger(DmaapMessageConsumer.class);

    private final ApplicationConfig applicationConfig;

    @Value("${server.port}")
    private int localServerPort;

    @Autowired
    public DmaapMessageConsumer(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    public Thread start() {
        Thread thread = new Thread(() -> this.checkConfigLoop());
        thread.start();
        return thread;
    }

    private void checkConfigLoop() {
        while (!isStopped()) {
            if (isDmaapConfigured()) {
                messageHandlingLoop();
            } else {
                sleep(RefreshConfigTask.CONFIG_REFRESH_INTERVAL);
            }
        }
    }

    private void messageHandlingLoop() {
        while (!isStopped() && isDmaapConfigured()) {
            try {
                Iterable<String> dmaapMsgs = fetchAllMessages();
                if (dmaapMsgs != null && Iterables.size(dmaapMsgs) > 0) {
                    logger.debug("Fetched all the messages from DMAAP and will start to process the messages");
                    for (String msg : dmaapMsgs) {
                        processMsg(msg);
                    }
                }
            } catch (Exception e) {
                logger.warn("Cannot fetch because of {}", e.getMessage());
                sleep(TIME_BETWEEN_DMAAP_RETRIES);
            }
        }
    }

    protected boolean isStopped() {
        return false;
    }

    protected boolean isDmaapConfigured() {
        Properties consumerCfg = applicationConfig.getDmaapConsumerConfig();
        Properties producerCfg = applicationConfig.getDmaapPublisherConfig();
        return (consumerCfg != null && consumerCfg.size() > 0 && producerCfg != null && producerCfg.size() > 0);
    }

    protected Iterable<String> fetchAllMessages() throws ServiceException, IOException {
        Properties dmaapConsumerProperties = this.applicationConfig.getDmaapConsumerConfig();
        MRConsumer consumer = getMessageRouterConsumer(dmaapConsumerProperties);
        MRConsumerResponse response = consumer.fetchWithReturnConsumerResponse();
        if (response == null || !"200".equals(response.getResponseCode())) {
            String errorMessage = "DMaaP NULL response received";
            if (response != null) {
                errorMessage = "Error respons " + response.getResponseCode() + " " + response.getResponseMessage()
                    + " from DMaaP.";
            }
            throw new ServiceException(errorMessage);
        } else {
            logger.debug("DMaaP consumer received {} : {}", response.getResponseCode(), response.getResponseMessage());
            return response.getActualMessages();
        }
    }

    private void processMsg(String msg) throws IOException {
        logger.debug("Message Reveived from DMAAP : {}", msg);
        getDmaapMessageHandler().handleDmaapMsg(msg);
    }

    private DmaapMessageHandler getDmaapMessageHandler() throws IOException {
        String agentBaseUrl = "https://localhost:" + this.localServerPort;
        AsyncRestClient agentClient = createRestClient(agentBaseUrl);
        Properties dmaapPublisherProperties = applicationConfig.getDmaapPublisherConfig();
        MRBatchingPublisher producer = getMessageRouterPublisher(dmaapPublisherProperties);

        return createDmaapMessageHandler(agentClient, producer);
    }

    protected void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (Exception e) {
            logger.error("Failed to put the thread to sleep", e);
        }
    }

    protected MRConsumer getMessageRouterConsumer(Properties dmaapConsumerProperties) throws IOException {
        return MRClientFactory.createConsumer(dmaapConsumerProperties);
    }

    protected DmaapMessageHandler createDmaapMessageHandler(AsyncRestClient agentClient, MRBatchingPublisher producer) {
        return new DmaapMessageHandler(producer, agentClient);
    }

    protected AsyncRestClient createRestClient(String agentBaseUrl) {
        return new AsyncRestClient(agentBaseUrl);
    }

    protected MRBatchingPublisher getMessageRouterPublisher(Properties dmaapPublisherProperties) throws IOException {
        return MRClientFactory.createBatchingPublisher(dmaapPublisherProperties);
    }
}
