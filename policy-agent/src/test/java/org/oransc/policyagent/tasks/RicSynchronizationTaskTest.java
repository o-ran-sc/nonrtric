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

import static ch.qos.logback.classic.Level.WARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oransc.policyagent.clients.A1Client;
import org.oransc.policyagent.clients.A1ClientFactory;
import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.configuration.ImmutableRicConfig;
import org.oransc.policyagent.repository.ImmutablePolicy;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Ric.RicState;
import org.oransc.policyagent.repository.Service;
import org.oransc.policyagent.repository.Services;
import org.oransc.policyagent.utils.LoggingUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class RicSynchronizationTaskTest {
    private static final String POLICY_TYPE_1_NAME = "type1";
    private static final PolicyType POLICY_TYPE_1 = ImmutablePolicyType.builder() //
        .name(POLICY_TYPE_1_NAME) //
        .schema("") //
        .build();

    private static final String RIC_1_NAME = "ric1";
    private static final Ric RIC_1 = new Ric(ImmutableRicConfig.builder() //
        .name(RIC_1_NAME) //
        .baseUrl("baseUrl1") //
        .managedElementIds(Collections.emptyList()) //
        .controllerName("controllerName") //
        .build());

    private static Policy createPolicy(boolean isTransient) {
        return ImmutablePolicy.builder() //
            .id("policyId1") //
            .json("") //
            .ownerServiceName("service") //
            .ric(RIC_1) //
            .type(POLICY_TYPE_1) //
            .lastModified("now") //
            .isTransient(isTransient) //
            .build();
    }

    private static final Policy POLICY_1 = createPolicy(false);

    private static final String SERVICE_1_NAME = "service1";
    private static final String SERVICE_1_CALLBACK_URL = "callbackUrl";
    private static final Service SERVICE_1 = new Service(SERVICE_1_NAME, Duration.ofSeconds(1), SERVICE_1_CALLBACK_URL);

    @Mock
    private A1Client a1ClientMock;

    @Mock
    private A1ClientFactory a1ClientFactoryMock;

    private PolicyTypes policyTypes;
    private Policies policies;
    private Services services;

    @BeforeEach
    public void init() {
        policyTypes = new PolicyTypes();
        policies = new Policies();
        services = new Services();
        RIC_1.setState(RicState.UNAVAILABLE);
        RIC_1.clearSupportedPolicyTypes();
    }

    @Test
    public void ricAlreadySynchronizing_thenNoSynchronization() {
        RIC_1.setState(RicState.SYNCHRONIZING);
        RIC_1.addSupportedPolicyType(POLICY_TYPE_1);

        policyTypes.put(POLICY_TYPE_1);
        policies.put(POLICY_1);

        RicSynchronizationTask synchronizerUnderTest =
            new RicSynchronizationTask(a1ClientFactoryMock, policyTypes, policies, services);

        synchronizerUnderTest.run(RIC_1);

        verifyNoInteractions(a1ClientMock);

        assertThat(policyTypes.size()).isEqualTo(1);
        assertThat(policies.size()).isEqualTo(1);
        assertThat(RIC_1.getState()).isEqualTo(RicState.SYNCHRONIZING);
        assertThat(RIC_1.getSupportedPolicyTypeNames().size()).isEqualTo(1);
    }

    @Test
    public void ricIdlePolicyTypeInRepo_thenSynchronizationWithReuseOfTypeFromRepoAndCorrectServiceNotified() {
        RIC_1.setState(RicState.AVAILABLE);

        policyTypes.put(POLICY_TYPE_1);

        services.put(SERVICE_1);
        Service serviceWithoutCallbackUrlShouldNotBeNotified = new Service("service2", Duration.ofSeconds(1), "");
        services.put(serviceWithoutCallbackUrlShouldNotBeNotified);

        setUpCreationOfA1Client();
        simulateRicWithOnePolicyType();

        RicSynchronizationTask synchronizerUnderTest =
            spy(new RicSynchronizationTask(a1ClientFactoryMock, policyTypes, policies, services));

        AsyncRestClient restClientMock = setUpCreationOfAsyncRestClient(synchronizerUnderTest);
        when(restClientMock.put(anyString(), anyString())).thenReturn(Mono.just("Ok"));

        synchronizerUnderTest.run(RIC_1);

        verify(a1ClientMock, times(1)).getPolicyTypeIdentities();
        verifyNoMoreInteractions(a1ClientMock);

        verify(synchronizerUnderTest).run(RIC_1);
        verify(synchronizerUnderTest).createNotificationClient(SERVICE_1_CALLBACK_URL);
        verifyNoMoreInteractions(synchronizerUnderTest);

        verify(restClientMock).put("", "Synchronization completed for:" + RIC_1_NAME);
        verifyNoMoreInteractions(restClientMock);

        assertThat(policyTypes.size()).isEqualTo(1);
        assertThat(policies.size()).isEqualTo(0);
        assertThat(RIC_1.getState()).isEqualTo(RicState.AVAILABLE);
    }

    @Test
    public void ricIdlePolicyTypeNotInRepo_thenSynchronizationWithTypeFromRic() throws Exception {
        RIC_1.setState(RicState.AVAILABLE);

        setUpCreationOfA1Client();
        simulateRicWithOnePolicyType();
        String typeSchema = "schema";
        when(a1ClientMock.getPolicyTypeSchema(POLICY_TYPE_1_NAME)).thenReturn(Mono.just(typeSchema));

        RicSynchronizationTask synchronizerUnderTest =
            new RicSynchronizationTask(a1ClientFactoryMock, policyTypes, policies, services);

        synchronizerUnderTest.run(RIC_1);

        verify(a1ClientMock).getPolicyTypeIdentities();
        verifyNoMoreInteractions(a1ClientMock);

        assertThat(policyTypes.size()).isEqualTo(1);
        assertThat(policyTypes.getType(POLICY_TYPE_1_NAME).schema()).isEqualTo(typeSchema);
        assertThat(policies.size()).isEqualTo(0);
        assertThat(RIC_1.getState()).isEqualTo(RicState.AVAILABLE);
    }

    @Test
    public void ricIdleAndHavePolicies_thenSynchronizationWithRecreationOfPolicies() {
        RIC_1.setState(RicState.AVAILABLE);

        Policy transientPolicy = createPolicy(true);

        policies.put(transientPolicy);
        policies.put(POLICY_1);

        setUpCreationOfA1Client();
        simulateRicWithNoPolicyTypes();

        when(a1ClientMock.deleteAllPolicies()).thenReturn(Flux.just("OK"));
        when(a1ClientMock.putPolicy(any(Policy.class))).thenReturn(Mono.just("OK"));

        RicSynchronizationTask synchronizerUnderTest =
            new RicSynchronizationTask(a1ClientFactoryMock, policyTypes, policies, services);

        synchronizerUnderTest.run(RIC_1);

        verify(a1ClientMock).deleteAllPolicies();
        verify(a1ClientMock).putPolicy(POLICY_1);
        verifyNoMoreInteractions(a1ClientMock);

        assertThat(policyTypes.size()).isEqualTo(0);
        assertThat(policies.size()).isEqualTo(1); // The transient policy shall be deleted
        assertThat(RIC_1.getState()).isEqualTo(RicState.AVAILABLE);
    }

    @Test
    public void ricIdleAndErrorDeletingPoliciesFirstTime_thenSynchronizationWithDeletionOfPolicies() {
        RIC_1.setState(RicState.AVAILABLE);

        policies.put(POLICY_1);

        setUpCreationOfA1Client();
        simulateRicWithNoPolicyTypes();

        when(a1ClientMock.deleteAllPolicies()) //
            .thenReturn(Flux.error(new Exception("Exception"))) //
            .thenReturn(Flux.just("OK"));

        RicSynchronizationTask synchronizerUnderTest =
            new RicSynchronizationTask(a1ClientFactoryMock, policyTypes, policies, services);

        synchronizerUnderTest.run(RIC_1);

        verify(a1ClientMock, times(2)).deleteAllPolicies();
        verifyNoMoreInteractions(a1ClientMock);

        assertThat(policyTypes.size()).isEqualTo(0);
        assertThat(policies.size()).isEqualTo(0);
        assertThat(RIC_1.getState()).isEqualTo(RicState.AVAILABLE);
    }

    @Test
    public void ricIdleAndErrorDeletingPoliciesAllTheTime_thenSynchronizationWithFailedRecovery() {
        RIC_1.setState(RicState.AVAILABLE);

        policies.put(POLICY_1);

        setUpCreationOfA1Client();
        simulateRicWithNoPolicyTypes();

        String originalErrorMessage = "Exception";
        when(a1ClientMock.deleteAllPolicies()).thenReturn(Flux.error(new Exception(originalErrorMessage)));

        RicSynchronizationTask synchronizerUnderTest =
            new RicSynchronizationTask(a1ClientFactoryMock, policyTypes, policies, services);

        final ListAppender<ILoggingEvent> logAppender =
            LoggingUtils.getLogListAppender(RicSynchronizationTask.class, WARN);

        synchronizerUnderTest.run(RIC_1);

        verifyCorrectLogMessage(0, logAppender,
            "Synchronization failure for ric: " + RIC_1_NAME + ", reason: " + originalErrorMessage);

        verify(a1ClientMock, times(2)).deleteAllPolicies();
        verifyNoMoreInteractions(a1ClientMock);

        assertThat(policyTypes.size()).isEqualTo(0);
        assertThat(policies.size()).isEqualTo(0);
        assertThat(RIC_1.getState()).isEqualTo(RicState.UNAVAILABLE);
    }

    @Test
    public void ricIdlePolicyTypeInRepo_thenSynchronizationWithErrorOnServiceNotificationErrorLogged() {
        RIC_1.setState(RicState.AVAILABLE);

        policyTypes.put(POLICY_TYPE_1);

        services.put(SERVICE_1);

        setUpCreationOfA1Client();
        simulateRicWithOnePolicyType();

        final ListAppender<ILoggingEvent> logAppender =
            LoggingUtils.getLogListAppender(RicSynchronizationTask.class, WARN);

        RicSynchronizationTask synchronizerUnderTest =
            spy(new RicSynchronizationTask(a1ClientFactoryMock, policyTypes, policies, services));

        AsyncRestClient restClientMock = setUpCreationOfAsyncRestClient(synchronizerUnderTest);
        String originalErrorMessage = "Exception";
        when(restClientMock.put(anyString(), anyString())).thenReturn(Mono.error(new Exception(originalErrorMessage)));

        synchronizerUnderTest.run(RIC_1);

        ILoggingEvent loggingEvent = logAppender.list.get(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(WARN);
        verifyCorrectLogMessage(0, logAppender, "Service notification failed for service: " + SERVICE_1_NAME);
    }

    private void setUpCreationOfA1Client() {
        when(a1ClientFactoryMock.createA1Client(any(Ric.class))).thenReturn(Mono.just(a1ClientMock));
        doReturn(Flux.empty()).when(a1ClientMock).deleteAllPolicies();
    }

    private AsyncRestClient setUpCreationOfAsyncRestClient(RicSynchronizationTask synchronizerUnderTest) {
        AsyncRestClient restClientMock = mock(AsyncRestClient.class);
        doReturn(restClientMock).when(synchronizerUnderTest).createNotificationClient(anyString());
        return restClientMock;
    }

    private void simulateRicWithOnePolicyType() {
        when(a1ClientMock.getPolicyTypeIdentities()).thenReturn(Mono.just(Arrays.asList(POLICY_TYPE_1_NAME)));
    }

    private void simulateRicWithNoPolicyTypes() {
        when(a1ClientMock.getPolicyTypeIdentities()).thenReturn(Mono.just(Collections.emptyList()));
    }

    private void verifyCorrectLogMessage(int messageIndex, ListAppender<ILoggingEvent> logAppender,
        String expectedMessage) {
        ILoggingEvent loggingEvent = logAppender.list.get(messageIndex);
        assertThat(loggingEvent.toString().contains(expectedMessage)).isTrue();
    }
}
