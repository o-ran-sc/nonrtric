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

import java.util.Collection;
import java.util.Vector;

import org.oransc.policyagent.clients.A1Client;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Loads information about RealTime-RICs at startup.
 */
public class RicRecoveryTask {

    private static final Logger logger = LoggerFactory.getLogger(RicRecoveryTask.class);

    private final A1Client a1Client;
    private final PolicyTypes policyTypes;
    private final Policies policies;

    public RicRecoveryTask(A1Client a1Client, PolicyTypes policyTypes, Policies policies) {
        this.a1Client = a1Client;
        this.policyTypes = policyTypes;
        this.policies = policies;
    }

    public void run(Collection<Ric> rics) {
        for (Ric ric : rics) {
            run(ric);
        }
    }

    public void run(Ric ric) {
        logger.debug("Handling ric: {}", ric.getConfig().name());

        synchronized (ric) {
            if (ric.state().equals(Ric.RicState.RECOVERING)) {
                return; // Already running
            }
            ric.setState(Ric.RicState.RECOVERING);
        }
        Flux<PolicyType> recoveredTypes = recoverPolicyTypes(ric);
        Flux<?> deletedPolicies = deletePolicies(ric);

        Flux.merge(recoveredTypes, deletedPolicies) //
            .subscribe(x -> logger.debug("Recover: " + x), //
                throwable -> onError(ric, throwable), //
                () -> onComplete(ric));
    }

    private void onComplete(Ric ric) {
        logger.debug("Recovery completed for:" + ric.name());
        ric.setState(Ric.RicState.ACTIVE);

    }

    private void onError(Ric ric, Throwable t) {
        logger.debug("Recovery failed for: {}, reason: {}", ric.name(), t.getMessage());
        ric.setState(Ric.RicState.NOT_REACHABLE);
    }

    private Flux<PolicyType> recoverPolicyTypes(Ric ric) {
        ric.clearSupportedPolicyTypes();
        return a1Client.getPolicyTypeIdentities(ric.getConfig().baseUrl()) //
            .flatMapMany(types -> Flux.fromIterable(types)) //
            .doOnNext(typeId -> logger.debug("For ric: {}, handling type: {}", ric.getConfig().name(), typeId))
            .flatMap((policyTypeId) -> getPolicyType(ric, policyTypeId)) //
            .doOnNext(policyType -> ric.addSupportedPolicyType(policyType)); //
    }

    private Mono<PolicyType> getPolicyType(Ric ric, String policyTypeId) {
        if (policyTypes.contains(policyTypeId)) {
            try {
                return Mono.just(policyTypes.getType(policyTypeId));
            } catch (ServiceException e) {
                return Mono.error(e);
            }
        }
        return a1Client.getPolicyType(ric.getConfig().baseUrl(), policyTypeId) //
            .flatMap(schema -> createPolicyType(policyTypeId, schema));
    }

    private Mono<PolicyType> createPolicyType(String policyTypeId, String schema) {
        PolicyType pt = ImmutablePolicyType.builder().name(policyTypeId).schema(schema).build();
        policyTypes.put(pt);
        return Mono.just(pt);
    }

    private Flux<String> deletePolicies(Ric ric) {
        Collection<Policy> ricPolicies = new Vector<>(policies.getForRic(ric.name()));
        for (Policy policy : ricPolicies) {
            this.policies.remove(policy);
        }

        return a1Client.getPolicyIdentities(ric.getConfig().baseUrl()) //
            .flatMapMany(policyIds -> Flux.fromIterable(policyIds)) //
            .doOnNext(policyId -> logger.debug("Deleting policy: {}, for ric: {}", policyId, ric.getConfig().name()))
            .flatMap(policyId -> a1Client.deletePolicy(ric.getConfig().baseUrl(), policyId)); //
    }
}
