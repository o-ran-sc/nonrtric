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

package org.oransc.policyagent.tasks;

import static ch.qos.logback.classic.Level.WARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.time.Duration;
import java.util.Collections;

import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oransc.policyagent.clients.A1Client;
import org.oransc.policyagent.clients.A1ClientFactory;
import org.oransc.policyagent.configuration.ImmutableRicConfig;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.ImmutablePolicy;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Service;
import org.oransc.policyagent.repository.Services;
import org.oransc.policyagent.utils.LoggingUtils;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ServiceSupervisionTest {

    private static final String SERVICE_NAME = "Service name";
    private static final String RIC_NAME = "name";
    private static final String POLICY_ID = "policy";

    @Mock
    A1ClientFactory a1ClientFactoryMock;
    @Mock
    A1Client a1ClientMock;

    private Services services;
    private Service service;
    private Policies policies;
    private RicConfig ricConfig = ImmutableRicConfig.builder() //
        .name(RIC_NAME) //
        .baseUrl("baseUrl") //
        .managedElementIds(Collections.emptyList()) //
        .controllerName("") //
        .build();
    private Ric ric = new Ric(ricConfig);
    private PolicyType policyType = ImmutablePolicyType.builder() //
        .name("plicyTypeName") //
        .schema("schema") //
        .build();
    private Policy policy = ImmutablePolicy.builder() //
        .id(POLICY_ID) //
        .json("json") //
        .ownerServiceName(SERVICE_NAME) //
        .ric(ric) //
        .type(policyType) //
        .lastModified("lastModified") //
        .isTransient(false) //
        .build();

    @Test
    void serviceExpired_policyAndServiceAreDeletedInRepoAndPolicyIsDeletedInRic() {
        setUpRepositoryWithKeepAliveInterval(Duration.ofSeconds(2));

        setUpCreationOfA1Client();
        when(a1ClientMock.deletePolicy(any(Policy.class))).thenReturn(Mono.just("Policy deleted"));

        ServiceSupervision serviceSupervisionUnderTest =
            new ServiceSupervision(services, policies, a1ClientFactoryMock);

        await().atMost(Durations.FIVE_SECONDS).with().pollInterval(Durations.ONE_SECOND).until(service::isExpired);

        serviceSupervisionUnderTest.checkAllServices().blockLast();

        assertThat(policies.size()).isEqualTo(0);
        assertThat(services.size()).isEqualTo(0);

        verify(a1ClientMock).deletePolicy(policy);
        verifyNoMoreInteractions(a1ClientMock);
    }

    @Test
    void serviceExpiredButDeleteInRicFails_policyAndServiceAreDeletedInRepoAndErrorLoggedForRic() {
        setUpRepositoryWithKeepAliveInterval(Duration.ofSeconds(2));

        setUpCreationOfA1Client();
        String originalErrorMessage = "Failed";
        when(a1ClientMock.deletePolicy(any(Policy.class))).thenReturn(Mono.error(new Exception(originalErrorMessage)));

        ServiceSupervision serviceSupervisionUnderTest =
            new ServiceSupervision(services, policies, a1ClientFactoryMock);

        await().atMost(Durations.FIVE_SECONDS).with().pollInterval(Durations.ONE_SECOND).until(service::isExpired);

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(ServiceSupervision.class, WARN);

        serviceSupervisionUnderTest.checkAllServices().blockLast();

        assertThat(policies.size()).isEqualTo(0);
        assertThat(services.size()).isEqualTo(0);

        ILoggingEvent loggingEvent = logAppender.list.get(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(WARN);
        String expectedLogMessage =
            "Could not delete policy: " + POLICY_ID + " from ric: " + RIC_NAME + ". Cause: " + originalErrorMessage;
        assertThat(loggingEvent.getFormattedMessage()).isEqualTo(expectedLogMessage);
    }

    @Test
    void serviceNotExpired_shouldNotBeChecked() {
        setUpRepositoryWithKeepAliveInterval(Duration.ofSeconds(2));

        ServiceSupervision serviceSupervisionUnderTest =
            new ServiceSupervision(services, policies, a1ClientFactoryMock);

        serviceSupervisionUnderTest.checkAllServices().blockLast();

        assertThat(policies.size()).isEqualTo(1);
        assertThat(services.size()).isEqualTo(1);

        verifyNoInteractions(a1ClientFactoryMock);
        verifyNoInteractions(a1ClientMock);
    }

    @Test
    void serviceWithoutKeepAliveInterval_shouldNotBeChecked() {
        setUpRepositoryWithKeepAliveInterval(Duration.ofSeconds(0));

        ServiceSupervision serviceSupervisionUnderTest =
            new ServiceSupervision(services, policies, a1ClientFactoryMock);

        serviceSupervisionUnderTest.checkAllServices().blockLast();

        assertThat(policies.size()).isEqualTo(1);
        assertThat(services.size()).isEqualTo(1);

        verifyNoInteractions(a1ClientFactoryMock);
        verifyNoInteractions(a1ClientMock);
    }

    private void setUpCreationOfA1Client() {
        when(a1ClientFactoryMock.createA1Client(any(Ric.class))).thenReturn(Mono.just(a1ClientMock));
    }

    private void setUpRepositoryWithKeepAliveInterval(Duration keepAliveInterval) {
        services = new Services();
        service = new Service(SERVICE_NAME, keepAliveInterval, "callbackUrl");
        services.put(service);

        policies = new Policies();
        policies.put(policy);
    }
}
