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
    protected MRConsumer consumer;
    private MRConsumerResponse response = null;

    @Autowired
    public DmaapMessageConsumerImpl(ApplicationConfig applicationConfig) {
        Properties dmaapConsumerConfig = applicationConfig.getDmaapConsumerConfig();
        init(dmaapConsumerConfig);
    }

    @Scheduled(fixedRate = 1000 * 60)
    @Override
    public void run() {
        while (this.alive) {
            try {
                Iterable<String> dmaapMsgs = fetchAllMessages();
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

    @Override
    public void init(Properties properties) {
        // Initialize the DMAAP with the properties
        // Do we need to do any validation of properties before calling the factory?
        Properties prop = new Properties();
        prop.setProperty("ServiceName", "localhost:6845/events");
        prop.setProperty("topic", "A1-P");
        prop.setProperty("host", "localhost:6845");
        prop.setProperty("contenttype", "application/json");
        prop.setProperty("username", "admin");
        prop.setProperty("password", "admin");
        prop.setProperty("group", "users");
        prop.setProperty("id", "policy-agent");
        prop.setProperty("TransportType", "HTTPNOAUTH");
        prop.setProperty("timeout", "15000");
        prop.setProperty("limit", "1000");
        try {
            consumer = MRClientFactory.createConsumer(prop);
            this.alive = true;
        } catch (IOException e) {
            logger.error("Exception occurred while creating Dmaap Consumer", e);
        }
    }

    @Override
    public void processMsg(String msg) throws Exception {
        logger.info("info", msg);
        System.out.println("sysout" + msg);
        logger.trace("trace", msg);
        logger.debug("debug", msg);
        // Call the concurrent Task executor to handle the incoming request
        // Validate the Input & if its valid, post the ACCEPTED Response back to DMAAP
        // through REST CLIENT
        // Call the Controller with the extracted payload

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
