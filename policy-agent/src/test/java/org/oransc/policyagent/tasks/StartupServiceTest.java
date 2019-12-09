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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
import org.oransc.policyagent.repository.Rics;

public class StartupServiceTest {
    ApplicationConfig appConfigMock;

    @Test
    public void startup_allOk() throws ServiceException {
        ApplicationConfig appConfigMock = mock(ApplicationConfig.class);
        Vector<RicConfig> ricConfigs = new Vector<>(2);
        Vector<String> firstNodes = new Vector<String>(1);
        firstNodes.add("nodeA");
        ricConfigs.add(ImmutableRicConfig.builder().name("first").managedElementIds(firstNodes).baseUrl("url").build());
        when(appConfigMock.getRicConfigs()).thenReturn(ricConfigs);

        Rics rics = new Rics();
        PolicyTypes policyTypes = new PolicyTypes();

        Vector<PolicyType> types = new Vector<>();
        PolicyType type = ImmutablePolicyType.builder().name("type1").jsonSchema("{}").build();
        types.add(type);
        RicClient ricClientMock = mock(RicClient.class);
        when(ricClientMock.getPolicyTypes(anyString())).thenReturn(types);

        StartupService serviceUnderTest = new StartupService(appConfigMock, rics, policyTypes, ricClientMock);

        serviceUnderTest.startup();

        verify(ricClientMock).deleteAllPolicies("url");
        verify(ricClientMock).getPolicyTypes("url");
        verifyNoMoreInteractions(ricClientMock);

        assertEquals("Correct nymber of Rics not added to Rics", 1, rics.size());
        assertEquals("Not correct Ric added to Rics", "first", rics.getRic("first").name());

        assertEquals("Not correct number of policy types added.", 1, policyTypes.size());
        assertEquals("Not correct type added.", type, policyTypes.getType(type.name()));
    }
}
