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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.demo.consumer.consumer.SimpleConsumer;

@Component
public class PropertiesHelper {
    private static final Logger log = LoggerFactory.getLogger(PropertiesHelper.class);

    public static Properties getProperties() throws Exception {
        Properties props = null;
        try (InputStream input = SimpleConsumer.class.getClassLoader().getResourceAsStream("config.properties")) {
            props = new Properties();
            if (input == null) {
                log.error("Found no configuration file in resources");
                throw new Exception("Sorry, unable to find config.properties");
            }
            props.load(input);
            String kafkaServers = System.getenv("KAFKA_SERVERS");
            if (kafkaServers != null) {
                props.setProperty("bootstrap.servers", kafkaServers);
                log.info("Env variable KAFKA_SERVERS found, adding: " + kafkaServers);
            } else {
                log.info("Env variable KAFKA_SERVERS not found, defaulting to config file");
            }
        } catch (IOException e) {
            log.error("Error reading configuration file: ", e.getMessage());
        }
        return props;
    }
}
