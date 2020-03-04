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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.configuration.ImmutableRicConfig;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Rics;

@ExtendWith(MockitoExtension.class)
public class StartupServiceTest {
    private static final String FIRST_RIC_NAME = "first";
    private static final String SECOND_RIC_NAME = "second";

    @Mock
    ApplicationConfig appConfigMock;
    @Mock
    RefreshConfigTask refreshTaskMock;
    @Mock
    RicSynchronizationTask synchronizationTaskMock;

    @Test
    public void startup_thenServiceIsAddedAsObeserverAndRefreshIsStarted() {
        StartupService serviceUnderTest =
            new StartupService(appConfigMock, refreshTaskMock, null, null, null, null, null);

        serviceUnderTest.startup();

        verify(appConfigMock).addObserver(serviceUnderTest);
        verify(refreshTaskMock).start();
    }

    @Test
    public void twoNewRicsAddedToConfiguration_thenSynchronizationIsStartedAndTwoRicsAreAddedInRepository() {

        Rics rics = new Rics();
        StartupService serviceUnderTest =
            spy(new StartupService(appConfigMock, refreshTaskMock, rics, null, null, null, null));

        doReturn(synchronizationTaskMock).when(serviceUnderTest).createSynchronizationTask();

        serviceUnderTest.onRicConfigUpdate(getRicConfig(FIRST_RIC_NAME), ApplicationConfig.RicConfigUpdate.ADDED);
        serviceUnderTest.onRicConfigUpdate(getRicConfig(SECOND_RIC_NAME), ApplicationConfig.RicConfigUpdate.ADDED);

        Ric firstRic = rics.get(FIRST_RIC_NAME);
        assertEquals(FIRST_RIC_NAME, firstRic.name(), FIRST_RIC_NAME + " not added to Rics");
        verify(synchronizationTaskMock, times(1)).run(firstRic);

        Ric secondRic = rics.get(SECOND_RIC_NAME);
        assertEquals(SECOND_RIC_NAME, secondRic.name(), SECOND_RIC_NAME + " not added to Rics");
        verify(synchronizationTaskMock).run(secondRic);
    }

    @Test
    public void oneRicIsChanged_thenSynchronizationIsStartedAndRicIsUpdatedInRepository() {
        Rics rics = new Rics();
        Ric originalRic = new Ric(getRicConfig(FIRST_RIC_NAME, "managedElement1"));
        rics.put(originalRic);

        StartupService serviceUnderTest =
            spy(new StartupService(appConfigMock, refreshTaskMock, rics, null, null, null, null));

        doReturn(synchronizationTaskMock).when(serviceUnderTest).createSynchronizationTask();

        String updatedManagedElementName = "managedElement2";
        serviceUnderTest.onRicConfigUpdate(getRicConfig(FIRST_RIC_NAME, updatedManagedElementName),
            ApplicationConfig.RicConfigUpdate.CHANGED);

        Ric firstRic = rics.get(FIRST_RIC_NAME);
        assertEquals(FIRST_RIC_NAME, firstRic.name(), FIRST_RIC_NAME + " not added to Rics");
        assertTrue(firstRic.getManagedElementIds().contains(updatedManagedElementName), "Ric not updated");
        verify(synchronizationTaskMock).run(firstRic);
    }

    @Test
    public void oneRicIsRemoved_thenNoSynchronizationIsStartedAndRicIsDeletedFromRepository() {
        Rics rics = new Rics();
        RicConfig ricConfig = getRicConfig(FIRST_RIC_NAME);
        rics.put(new Ric(ricConfig));

        StartupService serviceUnderTest =
            new StartupService(appConfigMock, refreshTaskMock, rics, null, null, null, null);

        serviceUnderTest.onRicConfigUpdate(ricConfig, ApplicationConfig.RicConfigUpdate.REMOVED);

        assertEquals(0, rics.size(), "Ric not deleted");
    }

    private RicConfig getRicConfig(String name) {
        return getRicConfig(name, null);
    }

    private RicConfig getRicConfig(String name, String managedElementName) {
        List<String> managedElements = Collections.emptyList();
        if (managedElementName != null) {
            managedElements = Collections.singletonList(managedElementName);
        }
        ImmutableRicConfig ricConfig = ImmutableRicConfig.builder() //
            .name(name) //
            .managedElementIds(managedElements) //
            .baseUrl("baseUrl") //
            .build();
        return ricConfig;
    }
}
