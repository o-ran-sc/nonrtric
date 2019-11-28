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

package org.oransc.policyagent.configuration;

import java.util.Optional;
import java.util.Properties;

import org.oransc.policyagent.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

class Environment {

    public static class Variables {

        public final String consulHost;
        public final Integer consulPort;
        public final String cbsName;
        public final String appName;

        public Variables(String consulHost, Integer consulPort, String cbsName, String appName) {
            this.consulHost = consulHost;
            this.consulPort = consulPort;
            this.cbsName = cbsName;
            this.appName = appName;
        }
    }

    private static final int DEFAULT_CONSUL_PORT = 8500;
    private static final Logger logger = LoggerFactory.getLogger(Environment.class);

    private Environment() {
    }

    static Mono<Variables> readEnvironmentVariables(Properties systemEnvironment) {
        logger.trace("Loading system environment variables");

        try {
            Variables envProperties = new Variables(getConsulHost(systemEnvironment) //
                , getConsultPort(systemEnvironment) //
                , getConfigBindingService(systemEnvironment) //
                , getService(systemEnvironment)); //

            logger.trace("Evaluated environment system variables {}", envProperties);
            return Mono.just(envProperties);
        } catch (ServiceException e) {
            return Mono.error(e);
        }
    }

    private static String getConsulHost(Properties systemEnvironments) throws ServiceException {
        return Optional.ofNullable(systemEnvironments.getProperty("CONSUL_HOST"))
            .orElseThrow(() -> new ServiceException("$CONSUL_HOST environment has not been defined"));
    }

    private static Integer getConsultPort(Properties systemEnvironments) {
        return Optional.ofNullable(systemEnvironments.getProperty("CONSUL_PORT")) //
            .map(Integer::valueOf) //
            .orElseGet(Environment::getDefaultPortOfConsul);
    }

    private static String getConfigBindingService(Properties systemEnvironments) throws ServiceException {
        return Optional.ofNullable(systemEnvironments.getProperty("CONFIG_BINDING_SERVICE")) //
            .orElseThrow(() -> new ServiceException("$CONFIG_BINDING_SERVICE environment has not been defined"));
    }

    private static String getService(Properties systemEnvironments) throws ServiceException {
        return Optional
            .ofNullable(Optional.ofNullable(systemEnvironments.getProperty("HOSTNAME"))
                .orElse(systemEnvironments.getProperty("SERVICE_NAME")))
            .orElseThrow(() -> new ServiceException(
                "Neither $HOSTNAME/$SERVICE_NAME have not been defined as system environment"));
    }

    private static Integer getDefaultPortOfConsul() {
        logger.warn("$CONSUL_PORT variable will be set to default port {}", DEFAULT_CONSUL_PORT);
        return DEFAULT_CONSUL_PORT;
    }
}
