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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oransc.policyagent.clients.A1Client;
import org.oransc.policyagent.clients.A1ClientFactory;
import org.oransc.policyagent.configuration.ImmutableRicConfig;
import org.oransc.policyagent.repository.ImmutablePolicy;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.Lock.LockType;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Ric.RicState;
import org.oransc.policyagent.repository.Rics;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class RicSupervisionTest {
    private static final String POLICY_TYPE_1_NAME = "type1";
    private static final PolicyType POLICY_TYPE_1 = ImmutablePolicyType.builder() //
        .name(POLICY_TYPE_1_NAME) //
        .schema("") //
        .build();

    private static final Ric RIC_1 = new Ric(ImmutableRicConfig.builder() //
        .name("RIC_1") //
        .baseUrl("baseUrl1") //
        .managedElementIds(new Vector<String>(Arrays.asList("kista_1", "kista_2"))) //
        .controllerName("controllerName") //
        .build());

    private static final String POLICY_1_ID = "policyId1";
    private static final Policy POLICY_1 = ImmutablePolicy.builder() //
        .id(POLICY_1_ID) //
        .json("") //
        .ownerServiceName("service") //
        .ric(RIC_1) //
        .type(POLICY_TYPE_1) //
        .lastModified("now") //
        .isTransient(false) //
        .build();

    private static final Policy POLICY_2 = ImmutablePolicy.builder() //
        .id("policyId2") //
        .json("") //
        .ownerServiceName("service") //
        .ric(RIC_1) //
        .type(POLICY_TYPE_1) //
        .lastModified("now") //
        .isTransient(false) //
        .build();

    @Mock
    private A1Client a1ClientMock;

    @Mock
    private A1ClientFactory a1ClientFactory;

    @Mock
    private RicSynchronizationTask synchronizationTaskMock;

    private final PolicyTypes types = new PolicyTypes();
    private Policies policies = new Policies();
    private Rics rics = new Rics();

    @BeforeEach
    void init() {
        types.clear();
        policies.clear();
        rics.clear();
        RIC_1.setState(RicState.UNAVAILABLE);
        RIC_1.clearSupportedPolicyTypes();
    }

    @AfterEach
    void verifyNoRicLocks() {
        for (Ric ric : this.rics.getRics()) {
            ric.getLock().lockBlocking(LockType.EXCLUSIVE);
            ric.getLock().unlockBlocking();
            assertThat(ric.getLock().getLockCounter()).isZero();
        }
    }

    @Test
    void whenRicIdleAndNoChangedPoliciesOrPolicyTypes_thenNoSynchronization() {
        doReturn(Mono.just(a1ClientMock)).when(a1ClientFactory).createA1Client(any(Ric.class));
        RIC_1.setState(RicState.AVAILABLE);
        RIC_1.addSupportedPolicyType(POLICY_TYPE_1);
        rics.put(RIC_1);

        types.put(POLICY_TYPE_1);

        policies.put(POLICY_1);

        setUpGetPolicyIdentitiesToReturn(new ArrayList<>(Arrays.asList(POLICY_1_ID)));
        setUpGetPolicyTypeIdentitiesToReturn(new ArrayList<>(Arrays.asList(POLICY_TYPE_1_NAME)));

        RicSupervision supervisorUnderTest = spy(new RicSupervision(rics, policies, a1ClientFactory, types, null));

        supervisorUnderTest.checkAllRics();

        verify(supervisorUnderTest).checkAllRics();
        verifyNoMoreInteractions(supervisorUnderTest);
    }

    @Test
    void whenRicUndefined_thenSynchronization() {
        doReturn(Mono.just(a1ClientMock)).when(a1ClientFactory).createA1Client(any(Ric.class));
        RIC_1.setState(RicState.UNAVAILABLE);
        rics.put(RIC_1);

        RicSupervision supervisorUnderTest = spy(new RicSupervision(rics, policies, a1ClientFactory, types, null));

        doReturn(synchronizationTaskMock).when(supervisorUnderTest).createSynchronizationTask();

        supervisorUnderTest.checkAllRics();

        verify(supervisorUnderTest).checkAllRics();
        verify(supervisorUnderTest).createSynchronizationTask();
        verify(synchronizationTaskMock).run(RIC_1);
        verifyNoMoreInteractions(supervisorUnderTest);
    }

    @Test
    void whenRicSynchronizing_thenNoSynchronization() {
        doReturn(Mono.just(a1ClientMock)).when(a1ClientFactory).createA1Client(any(Ric.class));
        RIC_1.setState(RicState.SYNCHRONIZING);
        rics.put(RIC_1);

        RicSupervision supervisorUnderTest = spy(new RicSupervision(rics, policies, a1ClientFactory, types, null));

        supervisorUnderTest.checkAllRics();

        verify(supervisorUnderTest).checkAllRics();
        verifyNoMoreInteractions(supervisorUnderTest);
    }

    @Test
    void whenRicIdleAndErrorGettingPolicyIdentities_thenNoSynchronization() {
        doReturn(Mono.just(a1ClientMock)).when(a1ClientFactory).createA1Client(any(Ric.class));
        RIC_1.setState(RicState.AVAILABLE);
        RIC_1.addSupportedPolicyType(POLICY_TYPE_1);
        rics.put(RIC_1);

        setUpGetPolicyIdentitiesToReturn(new Exception("Failed"));

        RicSupervision supervisorUnderTest = spy(new RicSupervision(rics, policies, a1ClientFactory, types, null));
        supervisorUnderTest.checkAllRics();

        verify(supervisorUnderTest).checkAllRics();
        verifyNoMoreInteractions(supervisorUnderTest);
        assertThat(RIC_1.getState()).isEqualTo(RicState.UNAVAILABLE);
    }

    @Test
    void whenRicIdleAndNotSameAmountOfPolicies_thenSynchronization() {
        doReturn(Mono.just(a1ClientMock)).when(a1ClientFactory).createA1Client(any(Ric.class));
        RIC_1.setState(RicState.AVAILABLE);
        rics.put(RIC_1);

        policies.put(POLICY_1);
        policies.put(POLICY_2);

        setUpGetPolicyIdentitiesToReturn(new ArrayList<>(Arrays.asList(POLICY_1_ID)));

        RicSupervision supervisorUnderTest = spy(new RicSupervision(rics, policies, a1ClientFactory, types, null));

        doReturn(synchronizationTaskMock).when(supervisorUnderTest).createSynchronizationTask();

        supervisorUnderTest.checkAllRics();

        verify(supervisorUnderTest).checkAllRics();
        verify(supervisorUnderTest).createSynchronizationTask();
        verify(synchronizationTaskMock).run(RIC_1);
        verifyNoMoreInteractions(supervisorUnderTest);
    }

    @Test
    void whenRicIdleAndSameAmountOfPoliciesButNotSamePolicies_thenSynchronization() {
        doReturn(Mono.just(a1ClientMock)).when(a1ClientFactory).createA1Client(any(Ric.class));
        RIC_1.setState(RicState.AVAILABLE);
        rics.put(RIC_1);

        policies.put(POLICY_1);
        policies.put(POLICY_2);

        setUpGetPolicyIdentitiesToReturn(new ArrayList<>(Arrays.asList(POLICY_1_ID, "Another_policy")));

        RicSupervision supervisorUnderTest = spy(new RicSupervision(rics, policies, a1ClientFactory, types, null));

        doReturn(synchronizationTaskMock).when(supervisorUnderTest).createSynchronizationTask();

        supervisorUnderTest.checkAllRics();

        verify(supervisorUnderTest).checkAllRics();
        verify(supervisorUnderTest).createSynchronizationTask();
        verify(synchronizationTaskMock).run(RIC_1);
        verifyNoMoreInteractions(supervisorUnderTest);
    }

    @Test
    void whenRicIdleAndErrorGettingPolicyTypes_thenNoSynchronization() {
        doReturn(Mono.just(a1ClientMock)).when(a1ClientFactory).createA1Client(any(Ric.class));
        RIC_1.setState(RicState.AVAILABLE);
        RIC_1.addSupportedPolicyType(POLICY_TYPE_1);
        rics.put(RIC_1);

        setUpGetPolicyIdentitiesToReturn(Collections.emptyList());
        setUpGetPolicyTypeIdentitiesToReturn(new Exception("Failed"));

        RicSupervision supervisorUnderTest = spy(new RicSupervision(rics, policies, a1ClientFactory, types, null));
        supervisorUnderTest.checkAllRics();

        verify(supervisorUnderTest).checkAllRics();
        verifyNoMoreInteractions(supervisorUnderTest);
    }

    @Test
    void whenRicIdleAndNotSameAmountOfPolicyTypes_thenSynchronization() {
        doReturn(Mono.just(a1ClientMock)).when(a1ClientFactory).createA1Client(any(Ric.class));
        RIC_1.setState(RicState.AVAILABLE);
        RIC_1.addSupportedPolicyType(POLICY_TYPE_1);
        rics.put(RIC_1);

        types.put(POLICY_TYPE_1);

        setUpGetPolicyIdentitiesToReturn(Collections.emptyList());
        setUpGetPolicyTypeIdentitiesToReturn(new ArrayList<>(Arrays.asList(POLICY_TYPE_1_NAME, "another_policy_type")));

        RicSupervision supervisorUnderTest = spy(new RicSupervision(rics, policies, a1ClientFactory, types, null));

        doReturn(synchronizationTaskMock).when(supervisorUnderTest).createSynchronizationTask();

        supervisorUnderTest.checkAllRics();

        verify(supervisorUnderTest).checkAllRics();
        verify(supervisorUnderTest).createSynchronizationTask();
        verify(synchronizationTaskMock).run(RIC_1);
        verifyNoMoreInteractions(supervisorUnderTest);
    }

    @Test
    void whenRicIdleAndSameAmountOfPolicyTypesButNotSameTypes_thenSynchronization() {
        doReturn(Mono.just(a1ClientMock)).when(a1ClientFactory).createA1Client(any(Ric.class));
        PolicyType policyType2 = ImmutablePolicyType.builder() //
            .name("policyType2") //
            .schema("") //
            .build();

        RIC_1.setState(RicState.AVAILABLE);
        RIC_1.addSupportedPolicyType(POLICY_TYPE_1);
        RIC_1.addSupportedPolicyType(policyType2);
        rics.put(RIC_1);

        setUpGetPolicyIdentitiesToReturn(Collections.emptyList());
        setUpGetPolicyTypeIdentitiesToReturn(new ArrayList<>(Arrays.asList(POLICY_TYPE_1_NAME, "another_policy_type")));

        RicSupervision supervisorUnderTest = spy(new RicSupervision(rics, policies, a1ClientFactory, types, null));

        doReturn(synchronizationTaskMock).when(supervisorUnderTest).createSynchronizationTask();

        supervisorUnderTest.checkAllRics();

        verify(supervisorUnderTest).checkAllRics();
        verify(supervisorUnderTest).createSynchronizationTask();
        verify(synchronizationTaskMock).run(RIC_1);
        verifyNoMoreInteractions(supervisorUnderTest);
    }

    @SuppressWarnings("unchecked")
    private void setUpGetPolicyIdentitiesToReturn(Object returnValue) {
        if (returnValue instanceof List<?>) {
            when(a1ClientMock.getPolicyIdentities()).thenReturn(Mono.just((List<String>) returnValue));
        } else if (returnValue instanceof Exception) {
            when(a1ClientMock.getPolicyIdentities()).thenReturn(Mono.error((Exception) returnValue));
        }
    }

    @SuppressWarnings("unchecked")
    private void setUpGetPolicyTypeIdentitiesToReturn(Object returnValue) {
        if (returnValue instanceof List<?>) {
            when(a1ClientMock.getPolicyTypeIdentities()).thenReturn(Mono.just((List<String>) returnValue));
        } else if (returnValue instanceof Exception) {
            when(a1ClientMock.getPolicyTypeIdentities()).thenReturn(Mono.error((Exception) returnValue));
        }
    }
}
