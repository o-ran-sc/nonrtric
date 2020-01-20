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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.oransc.policyagent.repository.Ric.RicState.IDLE;

import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oransc.policyagent.clients.A1Client;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.configuration.ImmutableRicConfig;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Ric.RicState;
import org.oransc.policyagent.repository.Rics;
import org.oransc.policyagent.repository.Services;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class StartupServiceTest {
    private static final String FIRST_RIC_NAME = "first";
    private static final String FIRST_RIC_URL = "firstUrl";
    private static final String SECOND_RIC_NAME = "second";
    private static final String SECOND_RIC_URL = "secondUrl";
    private static final String MANAGED_NODE_A = "nodeA";
    private static final String MANAGED_NODE_B = "nodeB";
    private static final String MANAGED_NODE_C = "nodeC";

    private static final String POLICY_TYPE_1_NAME = "type1";
    private static final String POLICY_TYPE_2_NAME = "type2";
    private static final String POLICY_ID_1 = "policy1";
    private static final String POLICY_ID_2 = "policy2";

    @Mock
    ApplicationConfig appConfigMock;

    @Mock
    RefreshConfigTask refreshTaskMock;

    @Mock
    A1Client a1ClientMock;

    @Test
    public void startup_allOk() {
        Mono<Collection<String>> policyTypes1 = Mono.just(Arrays.asList(POLICY_TYPE_1_NAME));
        Mono<Collection<String>> policyTypes2 = Mono.just(Arrays.asList(POLICY_TYPE_1_NAME, POLICY_TYPE_2_NAME));
        when(a1ClientMock.getPolicyTypeIdentities(anyString())).thenReturn(policyTypes1).thenReturn(policyTypes2);
        Mono<Collection<String>> policies = Mono.just(Arrays.asList(POLICY_ID_1, POLICY_ID_2));
        when(a1ClientMock.getPolicyIdentities(anyString())).thenReturn(policies);
        when(a1ClientMock.getPolicyType(anyString(), anyString())).thenReturn(Mono.just("Schema"));
        when(a1ClientMock.deletePolicy(anyString(), anyString())).thenReturn(Mono.just("OK"));

        Rics rics = new Rics();
        PolicyTypes policyTypes = new PolicyTypes();
        StartupService serviceUnderTest = new StartupService(appConfigMock, refreshTaskMock, rics, policyTypes,
            a1ClientMock, new Policies(), new Services());

        serviceUnderTest.startup();

        serviceUnderTest.onRicConfigUpdate(getRicConfig(FIRST_RIC_NAME, FIRST_RIC_URL, MANAGED_NODE_A),
            ApplicationConfig.RicConfigUpdate.ADDED);
        serviceUnderTest.onRicConfigUpdate(
            getRicConfig(SECOND_RIC_NAME, SECOND_RIC_URL, MANAGED_NODE_B, MANAGED_NODE_C),
            ApplicationConfig.RicConfigUpdate.ADDED);

        await().untilAsserted(() -> assertThat(policyTypes.size()).isEqualTo(2));

        verify(a1ClientMock).getPolicyTypeIdentities(FIRST_RIC_URL);
        verify(a1ClientMock).deletePolicy(FIRST_RIC_URL, POLICY_ID_1);
        verify(a1ClientMock).deletePolicy(FIRST_RIC_URL, POLICY_ID_2);

        verify(a1ClientMock).getPolicyTypeIdentities(SECOND_RIC_URL);
        verify(a1ClientMock).deletePolicy(SECOND_RIC_URL, POLICY_ID_1);
        verify(a1ClientMock).deletePolicy(SECOND_RIC_URL, POLICY_ID_2);

        assertTrue(policyTypes.contains(POLICY_TYPE_1_NAME), POLICY_TYPE_1_NAME + " not added to PolicyTypes.");
        assertTrue(policyTypes.contains(POLICY_TYPE_2_NAME), POLICY_TYPE_2_NAME + " not added to PolicyTypes.");
        assertEquals(2, rics.size(), "Correct number of Rics not added to Rics");

        Ric firstRic = rics.get(FIRST_RIC_NAME);
        assertNotNull(firstRic, "Ric " + FIRST_RIC_NAME + " not added to repository");
        assertEquals(FIRST_RIC_NAME, firstRic.name(), FIRST_RIC_NAME + " not added to Rics");
        assertEquals(IDLE, firstRic.state(), "Not correct state for ric " + FIRST_RIC_NAME);
        assertEquals(1, firstRic.getSupportedPolicyTypes().size(),
            "Not correct no of types supported for ric " + FIRST_RIC_NAME);
        assertTrue(firstRic.isSupportingType(POLICY_TYPE_1_NAME),
            POLICY_TYPE_1_NAME + " not supported by ric " + FIRST_RIC_NAME);
        assertEquals(1, firstRic.getManagedElementIds().size(),
            "Not correct no of managed nodes for ric " + FIRST_RIC_NAME);
        assertTrue(firstRic.isManaging(MANAGED_NODE_A), MANAGED_NODE_A + " not managed by ric " + FIRST_RIC_NAME);

        Ric secondRic = rics.get(SECOND_RIC_NAME);
        assertNotNull(secondRic, "Ric " + SECOND_RIC_NAME + " not added to repository");
        assertEquals(SECOND_RIC_NAME, secondRic.name(), SECOND_RIC_NAME + " not added to Rics");
        assertEquals(IDLE, secondRic.state(), "Not correct state for " + SECOND_RIC_NAME);
        assertEquals(2, secondRic.getSupportedPolicyTypes().size(),
            "Not correct no of types supported for ric " + SECOND_RIC_NAME);
        assertTrue(secondRic.isSupportingType(POLICY_TYPE_1_NAME),
            POLICY_TYPE_1_NAME + " not supported by ric " + SECOND_RIC_NAME);
        assertTrue(secondRic.isSupportingType(POLICY_TYPE_2_NAME),
            POLICY_TYPE_2_NAME + " not supported by ric " + SECOND_RIC_NAME);
        assertEquals(2, secondRic.getManagedElementIds().size(),
            "Not correct no of managed nodes for ric " + SECOND_RIC_NAME);
        assertTrue(secondRic.isManaging(MANAGED_NODE_B), MANAGED_NODE_B + " not managed by ric " + SECOND_RIC_NAME);
        assertTrue(secondRic.isManaging(MANAGED_NODE_C), MANAGED_NODE_C + " not managed by ric " + SECOND_RIC_NAME);
    }

    @Test
    public void startup_unableToConnectToGetTypes() {
        Mono<?> error = Mono.error(new Exception("Unable to contact ric."));
        doReturn(error, error).when(a1ClientMock).getPolicyTypeIdentities(anyString());
        doReturn(error).when(a1ClientMock).getPolicyIdentities(anyString());

        Rics rics = new Rics();
        PolicyTypes policyTypes = new PolicyTypes();
        StartupService serviceUnderTest = new StartupService(appConfigMock, refreshTaskMock, rics, policyTypes,
            a1ClientMock, new Policies(), new Services());

        serviceUnderTest.startup();
        serviceUnderTest.onRicConfigUpdate(getRicConfig(FIRST_RIC_NAME, FIRST_RIC_URL, MANAGED_NODE_A),
            ApplicationConfig.RicConfigUpdate.ADDED);

        assertEquals(RicState.UNDEFINED, rics.get(FIRST_RIC_NAME).state(), "Not correct state for " + FIRST_RIC_NAME);
    }

    @Test
    public void startup_unableToConnectToGetPolicies() {

        Mono<Collection<String>> policyTypes = Mono.just(Arrays.asList(POLICY_TYPE_1_NAME));
        when(a1ClientMock.getPolicyTypeIdentities(anyString())).thenReturn(policyTypes);
        when(a1ClientMock.getPolicyType(anyString(), anyString())).thenReturn(Mono.just("Schema"));
        Mono<?> error = Mono.error(new Exception("Unable to contact ric."));
        doReturn(error).when(a1ClientMock).getPolicyIdentities(anyString());

        Rics rics = new Rics();
        StartupService serviceUnderTest = new StartupService(appConfigMock, refreshTaskMock, rics, new PolicyTypes(),
            a1ClientMock, new Policies(), new Services());

        serviceUnderTest.startup();
        serviceUnderTest.onRicConfigUpdate(getRicConfig(FIRST_RIC_NAME, FIRST_RIC_URL, MANAGED_NODE_A),
            ApplicationConfig.RicConfigUpdate.ADDED);

        assertEquals(RicState.UNDEFINED, rics.get(FIRST_RIC_NAME).state(), "Not correct state for " + FIRST_RIC_NAME);
    }

    @SafeVarargs
    private <T> Vector<T> toVector(T... objs) {
        Vector<T> result = new Vector<>();
        for (T o : objs) {
            result.add(o);
        }
        return result;
    }

    private RicConfig getRicConfig(String name, String baseUrl, String... managedElementIds) {
        ImmutableRicConfig ricConfig = ImmutableRicConfig.builder() //
            .name(name) //
            .managedElementIds(toVector(managedElementIds)) //
            .baseUrl(baseUrl) //
            .build();
        return ricConfig;
    }
}
