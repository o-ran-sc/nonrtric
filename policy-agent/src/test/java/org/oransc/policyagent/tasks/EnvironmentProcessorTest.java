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

package org.oransc.policyagent.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.EnvProperties;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.ImmutableEnvProperties;
import org.oransc.policyagent.exceptions.EnvironmentLoaderException;
import org.oransc.policyagent.utils.LoggingUtils;
import reactor.test.StepVerifier;

class EnvironmentProcessorTest {
    private static final String CONSUL_HOST = "CONSUL_HOST";
    private static final String CONSUL_HOST_VALUE = "consulHost";

    private static final String CONFIG_BINDING_SERVICE = "CONFIG_BINDING_SERVICE";
    private static final String CONFIG_BINDING_SERVICE_VALUE = "configBindingService";

    private static final String HOSTNAME = "HOSTNAME";
    private static final String HOSTNAME_VALUE = "hostname";

    @Test
    void allPropertiesAvailableWithHostname_thenAllPropertiesAreReturnedWithGivenConsulPort() {
        Properties systemEnvironment = new Properties();
        String consulPort = "8080";
        systemEnvironment.put(CONSUL_HOST, CONSUL_HOST_VALUE);
        systemEnvironment.put("CONSUL_PORT", consulPort);
        systemEnvironment.put(CONFIG_BINDING_SERVICE, CONFIG_BINDING_SERVICE_VALUE);
        systemEnvironment.put(HOSTNAME, HOSTNAME_VALUE);

        EnvProperties expectedEnvProperties = ImmutableEnvProperties.builder() //
            .consulHost(CONSUL_HOST_VALUE) //
            .consulPort(Integer.valueOf(consulPort)) //
            .cbsName(CONFIG_BINDING_SERVICE_VALUE) //
            .appName(HOSTNAME_VALUE) //
            .build();

        StepVerifier.create(EnvironmentProcessor.readEnvironmentVariables(systemEnvironment))
            .expectNext(expectedEnvProperties).expectComplete();
    }

    @Test
    void consulHostMissing_thenExceptionReturned() {
        Properties systemEnvironment = new Properties();

        StepVerifier.create(EnvironmentProcessor.readEnvironmentVariables(systemEnvironment))
            .expectErrorMatches(throwable -> throwable instanceof EnvironmentLoaderException
                && throwable.getMessage().equals("$CONSUL_HOST environment has not been defined"))
            .verify();
    }

    @Test
    void withAllPropertiesExceptConsulPort_thenAllPropertiesAreReturnedWithDefaultConsulPortAndWarning() {
        Properties systemEnvironment = new Properties();
        systemEnvironment.put(CONSUL_HOST, CONSUL_HOST_VALUE);
        systemEnvironment.put(CONFIG_BINDING_SERVICE, CONFIG_BINDING_SERVICE_VALUE);
        systemEnvironment.put(HOSTNAME, HOSTNAME_VALUE);

        String defaultConsulPort = "8500";
        EnvProperties expectedEnvProperties = ImmutableEnvProperties.builder() //
            .consulHost(CONSUL_HOST_VALUE) //
            .consulPort(Integer.valueOf(defaultConsulPort)) //
            .cbsName(CONFIG_BINDING_SERVICE_VALUE) //
            .appName(HOSTNAME_VALUE) //
            .build();

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(EnvironmentProcessor.class);

        StepVerifier.create(EnvironmentProcessor.readEnvironmentVariables(systemEnvironment))
            .expectNext(expectedEnvProperties).expectComplete();

        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(logAppender.list.toString()
            .contains("$CONSUL_PORT variable will be set to default port " + defaultConsulPort)).isTrue();
    }

    @Test
    void configBindingServiceMissing_thenExceptionReturned() {
        Properties systemEnvironment = new Properties();
        systemEnvironment.put(CONSUL_HOST, CONSUL_HOST_VALUE);

        StepVerifier.create(EnvironmentProcessor.readEnvironmentVariables(systemEnvironment))
            .expectErrorMatches(throwable -> throwable instanceof EnvironmentLoaderException
                && throwable.getMessage().equals("$CONFIG_BINDING_SERVICE environment has not been defined"))
            .verify();
    }

    @Test
    void allPropertiesAvailableWithServiceName_thenAllPropertiesAreReturned() {
        Properties systemEnvironment = new Properties();
        String consulPort = "8080";
        systemEnvironment.put(CONSUL_HOST, CONSUL_HOST_VALUE);
        systemEnvironment.put("CONSUL_PORT", consulPort);
        systemEnvironment.put(CONFIG_BINDING_SERVICE, CONFIG_BINDING_SERVICE_VALUE);
        systemEnvironment.put("SERVICE_NAME", HOSTNAME_VALUE);

        EnvProperties expectedEnvProperties = ImmutableEnvProperties.builder() //
            .consulHost(CONSUL_HOST_VALUE) //
            .consulPort(Integer.valueOf(consulPort)) //
            .cbsName(CONFIG_BINDING_SERVICE_VALUE) //
            .appName(HOSTNAME_VALUE) //
            .build();

        StepVerifier.create(EnvironmentProcessor.readEnvironmentVariables(systemEnvironment))
            .expectNext(expectedEnvProperties).expectComplete();
    }

    @Test
    void serviceNameAndHostnameMissing_thenExceptionIsReturned() {
        Properties systemEnvironment = new Properties();
        systemEnvironment.put(CONSUL_HOST, CONSUL_HOST_VALUE);
        systemEnvironment.put(CONFIG_BINDING_SERVICE, CONFIG_BINDING_SERVICE_VALUE);

        StepVerifier.create(EnvironmentProcessor.readEnvironmentVariables(systemEnvironment))
            .expectErrorMatches(throwable -> throwable instanceof EnvironmentLoaderException && throwable.getMessage()
                .equals("Neither $HOSTNAME/$SERVICE_NAME have not been defined as system environment"))
            .verify();
    }
}
