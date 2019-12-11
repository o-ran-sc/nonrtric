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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.oransc.policyagent.repository.Ric.RicState.ACTIVE;

import java.util.Vector;

import org.junit.Test;
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

public class StartupServiceTest {
    private static final String FIRST_RIC_NAME = "first";
    private static final String FIRST_RIC_URL = "firstUrl";
    private static final String SECOND_RIC_NAME = "second";
    private static final String SECOND_RIC_URL = "secondUrl";

    private static final String PLOCY_TYPE_1_NAME = "type1";
    private static final String POLICY_TYPE_2_NAME = "type2";

    ApplicationConfig appConfigMock;

    @Test
    public void startup_allOk() throws ServiceException {
        ApplicationConfig appConfigMock = mock(ApplicationConfig.class);
        Vector<RicConfig> ricConfigs = new Vector<>(2);
        ricConfigs.add(getRicConfig(FIRST_RIC_NAME, FIRST_RIC_URL, "nodeA"));
        ricConfigs.add(getRicConfig(SECOND_RIC_NAME, SECOND_RIC_URL, "nodeB", "nodeC"));
        when(appConfigMock.getRicConfigs()).thenReturn(ricConfigs);

        Vector<PolicyType> firstTypes = new Vector<>();
        PolicyType type1 = ImmutablePolicyType.builder().name(PLOCY_TYPE_1_NAME).jsonSchema("{}").build();
        firstTypes.add(type1);
        Vector<PolicyType> secondTypes = new Vector<>();
        secondTypes.add(type1);
        PolicyType type2 = ImmutablePolicyType.builder().name(POLICY_TYPE_2_NAME).jsonSchema("{}").build();
        secondTypes.add(type2);
        RicClient ricClientMock = mock(RicClient.class);
        when(ricClientMock.getPolicyTypes(FIRST_RIC_URL)).thenReturn(firstTypes);
        when(ricClientMock.getPolicyTypes("secondUrl")).thenReturn(secondTypes);

        Rics rics = new Rics();
        PolicyTypes policyTypes = new PolicyTypes();
        StartupService serviceUnderTest = new StartupService(appConfigMock, rics, policyTypes, ricClientMock);

        serviceUnderTest.startup();

        verify(ricClientMock).deleteAllPolicies(FIRST_RIC_URL);
        verify(ricClientMock).getPolicyTypes(FIRST_RIC_URL);
        verify(ricClientMock).deleteAllPolicies(SECOND_RIC_URL);
        verify(ricClientMock).getPolicyTypes(SECOND_RIC_URL);
        verifyNoMoreInteractions(ricClientMock);

        assertEquals("Correct nymber of Rics not added to Rics", 2, rics.size());
        Ric firstRic = rics.getRic(FIRST_RIC_NAME);
        assertEquals("Not correct Ric \"" + FIRST_RIC_NAME + "\" added to Rics", FIRST_RIC_NAME, firstRic.name());
        assertEquals("Not correct state for \"" + FIRST_RIC_NAME + "\"", ACTIVE, firstRic.state());
        Ric secondRic = rics.getRic(SECOND_RIC_NAME);
        assertEquals("Not correct Ric \"" + SECOND_RIC_NAME + "\" added to Rics", SECOND_RIC_NAME, secondRic.name());
        assertEquals("Not correct state for \"" + SECOND_RIC_NAME + "\"", ACTIVE, secondRic.state());

        assertEquals("Not correct number of policy types added.", 2, policyTypes.size());
        assertEquals("Not correct type added.", type1, policyTypes.getType(PLOCY_TYPE_1_NAME));
        assertEquals("Not correct type added.", type2, policyTypes.getType(POLICY_TYPE_2_NAME));
    }

    private RicConfig getRicConfig(String name, String baseUrl, String... nodeNames) {
        Vector<String> managedNodes = new Vector<String>(1);
        for (String nodeName : managedNodes) {
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
