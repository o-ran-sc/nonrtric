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

package org.oransc.policyagent.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oransc.policyagent.configuration.ApplicationConfig.RicConfigUpdate;
import org.oransc.policyagent.configuration.ApplicationConfigParser.ConfigParserResult;
import org.oransc.policyagent.exceptions.ServiceException;

@ExtendWith(MockitoExtension.class)
public class ApplicationConfigTest {

    private static final ImmutableRicConfig RIC_CONFIG_1 = ImmutableRicConfig.builder() //
        .name("ric1") //
        .baseUrl("ric1_url") //
        .managedElementIds(new Vector<>()) //
        .controllerName("") //
        .build();

    ConfigParserResult configParserResult(RicConfig... rics) {
        return ImmutableConfigParserResult.builder() //
            .ricConfigs(Arrays.asList(rics)) //
            .dmaapConsumerConfig(new Properties()) //
            .dmaapPublisherConfig(new Properties()) //
            .controllerConfigs(new HashMap<>()) //
            .build();
    }

    @Test
    void gettingNotAddedRicShouldThrowException() {
        ApplicationConfig appConfigUnderTest = new ApplicationConfig();

        appConfigUnderTest.setConfiguration(configParserResult(RIC_CONFIG_1));

        Exception exception = assertThrows(ServiceException.class, () -> {
            appConfigUnderTest.getRic("name");
        });

        assertEquals("Could not find ric configuration: name", exception.getMessage());
    }

    @Test
    void addRicShouldNotifyAllObserversOfRicAdded() throws Exception {
        ApplicationConfig appConfigUnderTest = new ApplicationConfig();

        RicConfigUpdate update = appConfigUnderTest.setConfiguration(configParserResult(RIC_CONFIG_1)).blockFirst();
        assertEquals(RicConfigUpdate.Type.ADDED, update.getType());
        assertTrue(appConfigUnderTest.getRicConfigs().contains(RIC_CONFIG_1), "Ric not added to configurations.");

        assertEquals(RIC_CONFIG_1, appConfigUnderTest.getRic(RIC_CONFIG_1.name()),
            "Not correct Ric retrieved from configurations.");

        update = appConfigUnderTest.setConfiguration(configParserResult(RIC_CONFIG_1)).blockFirst();
        assertNull(update, "Nothing should be updated");
        assertTrue(appConfigUnderTest.getRicConfigs().contains(RIC_CONFIG_1), "Ric should remain.");

    }

    @Test
    void changedRicShouldNotifyAllObserversOfRicChanged() throws Exception {
        ApplicationConfig appConfigUnderTest = new ApplicationConfig();

        appConfigUnderTest.setConfiguration(configParserResult(RIC_CONFIG_1));

        ImmutableRicConfig changedRicConfig = ImmutableRicConfig.builder() //
            .name("ric1") //
            .baseUrl("changed_ric1_url") //
            .managedElementIds(new Vector<>()) //
            .controllerName("") //
            .build();

        RicConfigUpdate update = appConfigUnderTest.setConfiguration(configParserResult(changedRicConfig)).blockFirst();

        assertEquals(RicConfigUpdate.Type.CHANGED, update.getType());
        assertEquals(changedRicConfig, appConfigUnderTest.getRic(RIC_CONFIG_1.name()),
            "Changed Ric not retrieved from configurations.");
    }

    @Test
    void removedRicShouldNotifyAllObserversOfRicRemoved() {
        ApplicationConfig appConfigUnderTest = new ApplicationConfig();

        ImmutableRicConfig ricConfig2 = ImmutableRicConfig.builder() //
            .name("ric2") //
            .baseUrl("ric2_url") //
            .managedElementIds(new Vector<>()) //
            .controllerName("") //
            .build();

        appConfigUnderTest.setConfiguration(configParserResult(RIC_CONFIG_1, ricConfig2));

        RicConfigUpdate update = appConfigUnderTest.setConfiguration(configParserResult(ricConfig2)).blockFirst();

        assertEquals(RicConfigUpdate.Type.REMOVED, update.getType());
        assertEquals(1, appConfigUnderTest.getRicConfigs().size(), "Ric not deleted from configurations.");
    }

}
