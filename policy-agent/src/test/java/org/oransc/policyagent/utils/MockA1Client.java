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

package org.oransc.policyagent.utils;

import java.util.Collection;
import java.util.Vector;

import org.oransc.policyagent.clients.A1Client;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.PolicyTypes;
import reactor.core.publisher.Mono;

public class MockA1Client implements A1Client {
    Policies policies = new Policies();
    private final PolicyTypes policyTypes;

    public MockA1Client(PolicyTypes policyTypes) {
        this.policyTypes = policyTypes;
    }

    @Override
    public Mono<Collection<String>> getPolicyTypeIdentities() {
        synchronized (this.policyTypes) {
            Vector<String> result = new Vector<>();
            for (PolicyType p : this.policyTypes.getAll()) {
                result.add(p.name());
            }
            return Mono.just(result);
        }
    }

    @Override
    public Mono<Collection<String>> getPolicyIdentities() {
        synchronized (this.policies) {
            Vector<String> result = new Vector<>();
            for (Policy policy : policies.getAll()) {
                result.add(policy.id());
            }

            return Mono.just(result);
        }
    }

    @Override
    public Mono<String> getPolicyTypeSchema(String policyTypeId) {
        try {
            return Mono.just(this.policyTypes.getType(policyTypeId).schema());
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @Override
    public Mono<String> putPolicy(Policy p) {
        this.policies.put(p);
        return Mono.just("OK");
    }

    @Override
    public Mono<String> deletePolicy(String policyId) {
        this.policies.removeId(policyId);
        return Mono.just("OK");
    }

    public Policies getPolicies() {
        return this.policies;
    }

    @Override
    public Mono<A1ProtocolType> getProtocolVersion() {
        return Mono.just(A1ProtocolType.STD_V1);
    }

}
