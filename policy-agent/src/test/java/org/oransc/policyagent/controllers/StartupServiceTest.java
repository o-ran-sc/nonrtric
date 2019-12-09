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

package org.oransc.policyagent.controllers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Vector;

import org.junit.Test;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.configuration.ImmutableRicConfig;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.Rics;

public class StartupServiceTest {
    ApplicationConfig appConfigMock;

    @Test
    public void startup_allOk() {
        ApplicationConfig appConfigMock = mock(ApplicationConfig.class);
        Vector<RicConfig> ricConfigs = new Vector<>(2);
        Vector<String> firstNodes = new Vector<String>(1);
        firstNodes.add("nodeA");
        ricConfigs.add(ImmutableRicConfig.builder().name("first").managedElementIds(firstNodes).baseUrl("url").build());
        when(appConfigMock.getRicConfigs()).thenReturn(ricConfigs);

        Rics rics = new Rics();

        StartupService serviceUnderTest = new StartupService(appConfigMock, rics);

        serviceUnderTest.startup();

        assertEquals("Correct nymber of Rics not added to Rics", 1, rics.size());
        assertEquals("Not correct Ric added to Rics", "first", rics.getRic("first").name());
    }
}
