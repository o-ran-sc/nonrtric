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

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
import org.oransc.policyagent.configuration.ImmutableRicConfig;
import org.oransc.policyagent.repository.ImmutablePolicy;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Ric.RicState;
import org.oransc.policyagent.repository.Rics;
import org.oransc.policyagent.repository.Services;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class RepositorySupervisionTest {
    @Mock
    A1Client a1ClientMock;

    @Test
    public void test() {
        Ric ric1 = new Ric(ImmutableRicConfig.builder() //
            .name("ric1") //
            .baseUrl("baseUrl1") //
            .managedElementIds(new Vector<String>(Arrays.asList("kista_1", "kista_2"))) //
            .build());
        ric1.setState(Ric.RicState.ACTIVE);
        Ric ric2 = new Ric(ImmutableRicConfig.builder() //
            .name("ric2") //
            .baseUrl("baseUrl2") //
            .managedElementIds(new Vector<String>(Arrays.asList("kista_3", "kista_4"))) //
            .build());
        ric2.setState(Ric.RicState.NOT_REACHABLE);
        Ric ric3 = new Ric(ImmutableRicConfig.builder() //
            .name("ric3") //
            .baseUrl("baseUrl3") //
            .managedElementIds(new Vector<String>(Arrays.asList("kista_5"))) //
            .build());
        Rics rics = new Rics();
        rics.put(ric1);
        rics.put(ric2);
        rics.put(ric3);

        PolicyType policyType = ImmutablePolicyType.builder() //
            .name("type") //
            .schema("") //
            .build();
        Policy policy1 = ImmutablePolicy.builder() //
            .id("policyId1") //
            .json("") //
            .ownerServiceName("service") //
            .ric(ric1) //
            .type(policyType) //
            .lastModified("now") //
            .build();
        Policies policies = new Policies();
        policies.put(policy1);
        PolicyTypes types = new PolicyTypes();
        Services services = new Services();

        RepositorySupervision supervisorUnderTest =
            new RepositorySupervision(rics, policies, a1ClientMock, types, services);

        Mono<Collection<String>> policyIds = Mono.just(Arrays.asList("policyId1", "policyId2"));
        when(a1ClientMock.getPolicyIdentities(anyString())).thenReturn(policyIds);
        when(a1ClientMock.deletePolicy(anyString(), anyString())).thenReturn(Mono.empty());
        when(a1ClientMock.getPolicyTypeIdentities(anyString())).thenReturn(policyIds);
        when(a1ClientMock.getPolicyType(anyString(), anyString())).thenReturn(Mono.just("schema"));

        supervisorUnderTest.checkAllRics();

        await().untilAsserted(() -> RicState.ACTIVE.equals(ric1.state()));
        await().untilAsserted(() -> RicState.ACTIVE.equals(ric2.state()));
        await().untilAsserted(() -> RicState.ACTIVE.equals(ric3.state()));

        verify(a1ClientMock).deletePolicy("baseUrl1", "policyId2");
        verify(a1ClientMock).deletePolicy("baseUrl2", "policyId2");
        verifyNoMoreInteractions(a1ClientMock);
    }
}
