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

package com.demo.consumer.messages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSimpleKafka {
    private static final Logger log = LoggerFactory.getLogger(AbstractSimpleKafka.class);

    public AbstractSimpleKafka() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    shutdown();
                } catch (Exception e) {
                    log.error("Shutdown Exception: ", e);
                }
            }
        });

        log.info(MessageHelper.getSimpleJSONObject("Created the Shutdown Hook").toJSONString());
    }
    public abstract void shutdown() throws Exception;
    public abstract void runAlways(String topicName, KafkaMessageHandler callback) throws Exception;
    public abstract void run(String topicName, KafkaMessageHandler callback, Integer numberOfMessages) throws Exception;
}
