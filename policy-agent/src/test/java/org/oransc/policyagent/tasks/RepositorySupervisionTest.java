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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Arrays;
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
import org.oransc.policyagent.configuration.RicConfig;
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
import org.oransc.policyagent.repository.Services;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class RepositorySupervisionTest {
    @Mock
    A1Client a1ClientMock;

    @Mock
    A1ClientFactory a1ClientFactory;

    final Rics rics = new Rics();
    final Policies policies = new Policies();
    final PolicyTypes types = new PolicyTypes();
    final Services services = new Services();

    @BeforeEach
    public void init() {
        policies.clear();
        types.clear();
        doReturn(Mono.just(a1ClientMock)).when(a1ClientFactory).createA1Client(any());
    }

    @AfterEach
    public void verifyNoRicLocks() {
        for (Ric ric : this.rics.getRics()) {
            ric.getLock().lockBlocking(LockType.EXCLUSIVE);
            assertThat(ric.getLock().getLockCounter()).isEqualTo(1);
            ric.getLock().unlock();
        }
    }

    private Ric addRic(String ricName, Ric.RicState initialState) {
        if (rics.get(ricName) != null) {
            return rics.get(ricName);
        }
        Vector<String> mes = new Vector<>();
        RicConfig conf = ImmutableRicConfig.builder() //
            .name(ricName) //
            .baseUrl(ricName) //
            .managedElementIds(mes) //
            .build();
        Ric ric = new Ric(conf);
        ric.setState(initialState);
        this.rics.put(ric);
        return ric;
    }

    private Policy createPolicy(String id, String typeName, Ric ric) {

        PolicyType policyType = ImmutablePolicyType.builder() //
            .name(typeName) //
            .schema("") //
            .build();
        ric.addSupportedPolicyType(policyType);
        Policy policy = ImmutablePolicy.builder() //
            .id("policyId1") //
            .json("") //
            .ownerServiceName("service") //
            .ric(ric) //
            .type(policyType) //
            .lastModified("now") //
            .build();

        policies.put(policy);
        return policy;
    }

    @Test
    public void testRecoveryOk() {
        {
            Mono<List<String>> policyIds = Mono.just(Arrays.asList("policyId1", "policyId2"));
            Mono<List<String>> policyTypeIds = Mono.just(Arrays.asList("type1", "type2"));
            doReturn(policyTypeIds).when(a1ClientMock).getPolicyTypeIdentities();
            doReturn(policyIds).when(a1ClientMock).getPolicyIdentities();
            doReturn(Mono.just("schema")).when(a1ClientMock).getPolicyTypeSchema(anyString());
            doReturn(Mono.just("OK")).when(a1ClientMock).putPolicy(any());
            doReturn(Flux.just("policyId1")).when(a1ClientMock).deleteAllPolicies();
        }

        Ric ric1 = addRic("ric1", Ric.RicState.IDLE);
        Ric ric2 = addRic("ric2", Ric.RicState.UNDEFINED);

        Policy policyRic1 = createPolicy("id1", "type1", ric1);
        Policy policyRic2 = createPolicy("id2", "type1", ric2);

        RepositorySupervision supervisorUnderTest =
            new RepositorySupervision(rics, policies, a1ClientFactory, types, services);

        supervisorUnderTest.checkAllRics();

        await().untilAsserted(() -> RicState.IDLE.equals(ric1.getState()));
        await().untilAsserted(() -> RicState.IDLE.equals(ric2.getState()));

        verify(a1ClientMock, atLeastOnce()).deleteAllPolicies();
        verify(a1ClientMock).putPolicy(policyRic1);
        verify(a1ClientMock).putPolicy(policyRic2);
        verifyNoMoreInteractions(a1ClientMock);
        assertThat(ric1.getSupportedPolicyTypeNames()).contains("type1", "type2");
    }

    @Test
    public void testRecoveryFailedPut() {
        {
            Mono<List<String>> ids = Mono.just(Arrays.asList("policyId1", "policyId2"));
            doReturn(ids).when(a1ClientMock).getPolicyTypeIdentities();
            doReturn(ids).when(a1ClientMock).getPolicyIdentities();
            doReturn(Mono.just("schema")).when(a1ClientMock).getPolicyTypeSchema(anyString());
            doReturn(Flux.just("policyId1")).when(a1ClientMock).deleteAllPolicies();

            // Fails
            doReturn(Mono.error(new Exception("Nope"))).when(a1ClientMock).putPolicy(any());
        }

        Ric ric = addRic("ric1", Ric.RicState.IDLE);
        createPolicy("id1", "type", ric);
        assertThat(ric.getSupportedPolicyTypeNames()).contains("type");

        RepositorySupervision supervisorUnderTest =
            new RepositorySupervision(rics, policies, a1ClientFactory, types, services);
        supervisorUnderTest.checkAllRics();

        await().untilAsserted(() -> RicState.RECOVERING.equals(ric.getState()));
        await().untilAsserted(() -> RicState.IDLE.equals(ric.getState()));

        verify(a1ClientMock, atLeastOnce()).deleteAllPolicies();
        verifyNoMoreInteractions(a1ClientMock);
        // All policies are removed
        assertThat(policies.size()).isEqualTo(0);
        // Supported types are not affected
        assertThat(this.types.size()).isEqualTo(2);
        assertThat(ric.getSupportedPolicyTypes().size()).isEqualTo(2);
        assertThat(ric.getSupportedPolicyTypeNames()).contains("policyId1", "policyId2");
    }

    @Test
    public void testFailedRecovery() {
        {
            doReturn(Mono.error(new Exception("Nope"))).when(a1ClientMock).getPolicyTypeIdentities();
        }
        Ric ric = addRic("ric1", Ric.RicState.IDLE);
        createPolicy("id1", "type", ric);

        RicRecoveryTask recovery = new RicRecoveryTask(a1ClientFactory, types, policies, services);
        recovery.run(ric);

        // State is set to UNDEFINED but cached info in not affected
        await().untilAsserted(() -> RicState.UNDEFINED.equals(ric.getState()));
        assertThat(ric.getSupportedPolicyTypeNames()).contains("type");
        assertThat(policies.size()).isEqualTo(1);
    }
}
