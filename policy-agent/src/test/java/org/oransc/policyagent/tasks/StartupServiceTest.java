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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.oransc.policyagent.repository.Ric.RicState.ACTIVE;

import java.util.Vector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oransc.policyagent.clients.RicClient;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.configuration.ImmutableRicConfig;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Rics;

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

    @Mock
    ApplicationConfig appConfigMock;

    @Mock
    RicClient ricClientMock;

    @Test
    public void startup_allOk() throws ServiceException {
        Vector<RicConfig> ricConfigs = new Vector<>(2);
        ricConfigs.add(getRicConfig(FIRST_RIC_NAME, FIRST_RIC_URL, MANAGED_NODE_A));
        ricConfigs.add(getRicConfig(SECOND_RIC_NAME, SECOND_RIC_URL, MANAGED_NODE_B, MANAGED_NODE_C));
        when(appConfigMock.getRicConfigs()).thenReturn(ricConfigs);

        Vector<PolicyType> firstTypes = new Vector<>();
        PolicyType type1 = ImmutablePolicyType.builder().name(POLICY_TYPE_1_NAME).jsonSchema("{}").build();
        firstTypes.add(type1);
        Vector<PolicyType> secondTypes = new Vector<>();
        secondTypes.add(type1);
        PolicyType type2 = ImmutablePolicyType.builder().name(POLICY_TYPE_2_NAME).jsonSchema("{}").build();
        secondTypes.add(type2);
        when(ricClientMock.getPolicyTypes(anyString())).thenReturn(firstTypes, secondTypes);

        Rics rics = new Rics();
        PolicyTypes policyTypes = new PolicyTypes();
        StartupService serviceUnderTest = new StartupService(appConfigMock, rics, policyTypes, ricClientMock);

        serviceUnderTest.startup();

        verify(ricClientMock).deleteAllPolicies(FIRST_RIC_URL);
        verify(ricClientMock).getPolicyTypes(FIRST_RIC_URL);
        verify(ricClientMock).deleteAllPolicies(SECOND_RIC_URL);
        verify(ricClientMock).getPolicyTypes(SECOND_RIC_URL);
        verifyNoMoreInteractions(ricClientMock);

        assertEquals(2, policyTypes.size(), "Not correct number of policy types added.");
        assertEquals(type1, policyTypes.getType(POLICY_TYPE_1_NAME), "Not correct type added.");
        assertEquals(type2, policyTypes.getType(POLICY_TYPE_2_NAME), "Not correct type added.");
        assertEquals(2, rics.size(), "Correct nymber of Rics not added to Rics");

        Ric firstRic = rics.getRic(FIRST_RIC_NAME);
        assertNotNull(firstRic, "Ric \"" + FIRST_RIC_NAME + "\" not added to repositpry");
        assertEquals(FIRST_RIC_NAME, firstRic.name(), "Not correct Ric \"" + FIRST_RIC_NAME + "\" added to Rics");
        assertEquals(ACTIVE, firstRic.state(), "Not correct state for \"" + FIRST_RIC_NAME + "\"");
        assertEquals(1, firstRic.getSupportedPolicyTypes().size(), "Not correct no of types supported");
        assertTrue(firstRic.isSupportingType(type1), "Not correct type supported");
        assertEquals(1, firstRic.getManagedNodes().size(), "Not correct no of managed nodes");
        assertTrue(firstRic.isManaging(MANAGED_NODE_A), "Not managed by node");

        Ric secondRic = rics.getRic(SECOND_RIC_NAME);
        assertNotNull(secondRic, "Ric \"" + SECOND_RIC_NAME + "\" not added to repositpry");
        assertEquals(SECOND_RIC_NAME, secondRic.name(), "Not correct Ric \"" + SECOND_RIC_NAME + "\" added to Rics");
        assertEquals(ACTIVE, secondRic.state(), "Not correct state for \"" + SECOND_RIC_NAME + "\"");
        assertEquals(2, secondRic.getSupportedPolicyTypes().size(), "Not correct no of types supported");
        assertTrue(secondRic.isSupportingType(type1), "Not correct type supported");
        assertTrue(secondRic.isSupportingType(type2), "Not correct type supported");
        assertEquals(2, secondRic.getManagedNodes().size(), "Not correct no of managed nodes");
        assertTrue(secondRic.isManaging(MANAGED_NODE_B), "Not correct managed node");
        assertTrue(secondRic.isManaging(MANAGED_NODE_C), "Not correct managed node");
    }

    private RicConfig getRicConfig(String name, String baseUrl, String... nodeNames) {
        Vector<String> managedNodes = new Vector<String>(1);
        for (String nodeName : nodeNames) {
            managedNodes.add(nodeName);
        }
        ImmutableRicConfig ricConfig = ImmutableRicConfig.builder() //
            .name(name) //
            .managedElementIds(managedNodes) //
            .baseUrl(baseUrl) //
            .build();
        return ricConfig;
    }
}
