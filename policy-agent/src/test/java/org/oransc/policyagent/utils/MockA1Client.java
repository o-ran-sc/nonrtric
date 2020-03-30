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

package org.oransc.policyagent.utils;

import java.util.List;
import java.util.Vector;

import org.oransc.policyagent.clients.A1Client;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.PolicyTypes;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

public class MockA1Client implements A1Client {
    Policies policies = new Policies();
    private final PolicyTypes policyTypes;
    private final boolean generateAsynchResponses;

    public MockA1Client(PolicyTypes policyTypes, boolean asynchResponses) {
        this.policyTypes = policyTypes;
        this.generateAsynchResponses = asynchResponses;
    }

    @Override
    public Mono<List<String>> getPolicyTypeIdentities() {
        synchronized (this.policyTypes) {
            List<String> result = new Vector<>();
            for (PolicyType p : this.policyTypes.getAll()) {
                result.add(p.name());
            }
            return mono(result);
        }
    }

    @Override
    public Mono<List<String>> getPolicyIdentities() {
        synchronized (this.policies) {
            Vector<String> result = new Vector<>();
            for (Policy policy : policies.getAll()) {
                result.add(policy.id());
            }

            return mono(result);
        }
    }

    @Override
    public Mono<String> getPolicyTypeSchema(String policyTypeId) {
        try {
            return mono(this.policyTypes.getType(policyTypeId).schema());
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @Override
    public Mono<String> putPolicy(Policy p) {
        this.policies.put(p);
        return mono("OK");

    }

    @Override
    public Mono<String> deletePolicy(Policy policy) {
        this.policies.remove(policy);
        return mono("OK");
    }

    public Policies getPolicies() {
        return this.policies;
    }

    @Override
    public Mono<A1ProtocolType> getProtocolVersion() {
        return mono(A1ProtocolType.STD_V1_1);
    }

    @Override
    public Flux<String> deleteAllPolicies() {
        this.policies.clear();
        return Flux.empty();
    }

    @Override
    public Mono<String> getPolicyStatus(Policy policy) {
        return mono("OK");
    }

    private <T> Mono<T> mono(T value) {
        if (generateAsynchResponses) {
            return Mono.create(monoSink -> asynchResponse(monoSink, value));
        } else {
            return Mono.just(value);
        }
    }

    private <T> void asynchResponse(MonoSink<T> callback, T str) {
        Thread thread = new Thread(() -> {
            try {
                // Thread.sleep(10);
            } catch (Exception e) {
            }
            callback.success(str);
        });
        thread.start();

    }

}
