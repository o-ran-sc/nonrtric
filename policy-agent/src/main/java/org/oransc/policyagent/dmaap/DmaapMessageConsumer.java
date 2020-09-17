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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * The class fetches incoming requests from DMAAP. It uses the timeout parameter
 * that lets the MessageRouter keep the connection with the Kafka open until
 * requests are sent in.
 *
 * <p>
 * this service will regularly check the configuration and start polling DMaaP
 * if the configuration is added. If the DMaaP configuration is removed, then
 * the service will stop polling and resume checking for configuration.
 *
 * <p>
 * Each received request is processed by {@link DmaapMessageHandler}.
 */
@Component
public class DmaapMessageConsumer {

    protected static final Duration TIME_BETWEEN_DMAAP_RETRIES = Duration.ofSeconds(10);

    private static final Logger logger = LoggerFactory.getLogger(DmaapMessageConsumer.class);

    private final ApplicationConfig applicationConfig;

    private DmaapMessageHandler dmaapMessageHandler = null;

    @Value("${server.http-port}")
    private int localServerHttpPort;

    @Autowired
    public DmaapMessageConsumer(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    /**
     * Starts the consumer. If there is a DMaaP configuration, it will start polling
     * for messages. Otherwise it will check regularly for the configuration.
     *
     * @return the running thread, for test purposes.
     */
    public Thread start() {
        Thread thread = new Thread(this::messageHandlingLoop);
        thread.start();
        return thread;
    }

    private void messageHandlingLoop() {
        while (!isStopped()) {
            try {
                if (isDmaapConfigured()) {
                    Iterable<String> dmaapMsgs = fetchAllMessages();
                    if (dmaapMsgs != null && Iterables.size(dmaapMsgs) > 0) {
                        logger.debug("Fetched all the messages from DMAAP and will start to process the messages");
                        for (String msg : dmaapMsgs) {
                            processMsg(msg);
                        }
                    }
                } else {
                    sleep(TIME_BETWEEN_DMAAP_RETRIES); // wait for configuration
                }
            } catch (Exception e) {
                logger.warn("{}", e.getMessage());
                sleep(TIME_BETWEEN_DMAAP_RETRIES);
            }
        }
    }

    protected boolean isStopped() {
        return false;
    }

    protected boolean isDmaapConfigured() {
        String producerTopicUrl = applicationConfig.getDmaapProducerTopicUrl();
        String consumerTopicUrl = applicationConfig.getDmaapConsumerTopicUrl();
        return (!producerTopicUrl.isEmpty() && !consumerTopicUrl.isEmpty());
    }

    private static List<String> parseMessages(String jsonString) {
        JsonArray arrayOfMessages = JsonParser.parseString(jsonString).getAsJsonArray();
        List<String> result = new ArrayList<>();
        for (JsonElement element : arrayOfMessages) {
            if (element.isJsonPrimitive()) {
                result.add(element.getAsString());
            } else {
                String messageAsString = element.toString();
                result.add(messageAsString);
            }
        }
        return result;
    }

    protected Iterable<String> fetchAllMessages() throws ServiceException {
        String topicUrl = this.applicationConfig.getDmaapConsumerTopicUrl();
        AsyncRestClient consumer = getMessageRouterConsumer();
        ResponseEntity<String> response = consumer.getForEntity(topicUrl).block();
        logger.debug("DMaaP consumer received {} : {}", response.getStatusCode(), response.getBody());
        if (response.getStatusCode().is2xxSuccessful()) {
            return parseMessages(response.getBody());
        } else {
            throw new ServiceException("Cannot fetch because of Error respons: " + response.getStatusCode().toString()
                + " " + response.getBody());
        }
    }

    private void processMsg(String msg) {
        logger.debug("Message Reveived from DMAAP : {}", msg);
        getDmaapMessageHandler().handleDmaapMsg(msg);
    }

    protected DmaapMessageHandler getDmaapMessageHandler() {
        if (this.dmaapMessageHandler == null) {
            String agentBaseUrl = "http://localhost:" + this.localServerHttpPort;
            AsyncRestClient agentClient =
                new AsyncRestClient(agentBaseUrl, this.applicationConfig.getWebClientConfig());
            AsyncRestClient producer = new AsyncRestClient(this.applicationConfig.getDmaapProducerTopicUrl(),
                this.applicationConfig.getWebClientConfig());
            this.dmaapMessageHandler = new DmaapMessageHandler(producer, agentClient);
        }
        return this.dmaapMessageHandler;
    }

    protected void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (Exception e) {
            logger.error("Failed to put the thread to sleep", e);
        }
    }

    protected AsyncRestClient getMessageRouterConsumer() {
        return new AsyncRestClient("", this.applicationConfig.getWebClientConfig());
    }

}
