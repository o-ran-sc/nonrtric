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

import static org.oransc.policyagent.repository.Ric.RicState;

import java.util.Vector;

import org.oransc.policyagent.clients.A1Client;
import org.oransc.policyagent.clients.A1ClientFactory;
import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.Lock;
import org.oransc.policyagent.repository.Lock.LockType;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Service;
import org.oransc.policyagent.repository.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Synchronizes the content of a RIC with the content in the repository. This
 * means:
 * <p>
 * load all policy types
 * <p>
 * send all policy instances to the RIC
 * <p>
 * if that fails remove all policy instances
 * <p>
 * Notify subscribing services
 */
public class RicSynchronizationTask {

    private static final Logger logger = LoggerFactory.getLogger(RicSynchronizationTask.class);

    private final A1ClientFactory a1ClientFactory;
    private final PolicyTypes policyTypes;
    private final Policies policies;
    private final Services services;

    public RicSynchronizationTask(A1ClientFactory a1ClientFactory, PolicyTypes policyTypes, Policies policies,
        Services services) {
        this.a1ClientFactory = a1ClientFactory;
        this.policyTypes = policyTypes;
        this.policies = policies;
        this.services = services;
    }

    @SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
    public void run(Ric ric) {
        logger.debug("Handling ric: {}", ric.getConfig().name());

        synchronized (ric) {
            if (ric.getState() == RicState.SYNCHRONIZING) {
                logger.debug("Ric: {} is already being synchronized", ric.getConfig().name());
                return;
            }
            ric.setState(RicState.SYNCHRONIZING);
        }

        ric.getLock().lock(LockType.EXCLUSIVE) // Make sure no NBI updates are running
            .flatMap(Lock::unlock) //
            .flatMap(lock -> this.a1ClientFactory.createA1Client(ric)) //
            .flatMapMany(client -> startSynchronization(ric, client)) //
            .subscribe(x -> logger.debug("Synchronize: {}", x), //
                throwable -> onSynchronizationError(ric, throwable), //
                () -> onSynchronizationComplete(ric));
    }

    private Flux<Object> startSynchronization(Ric ric, A1Client a1Client) {
        Flux<PolicyType> recoverTypes = synchronizePolicyTypes(ric, a1Client);
        Flux<?> policiesDeletedInRic = a1Client.deleteAllPolicies();
        Flux<?> policiesRecreatedInRic = recreateAllPoliciesInRic(ric, a1Client);

        return Flux.concat(recoverTypes, policiesDeletedInRic, policiesRecreatedInRic);
    }

    @SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
    private void onSynchronizationComplete(Ric ric) {
        logger.info("Synchronization completed for: {}", ric.name());
        ric.setState(RicState.IDLE);
        notifyAllServices("Synchronization completed for:" + ric.name());
    }

    private void notifyAllServices(String body) {
        synchronized (services) {
            for (Service service : services.getAll()) {
                String url = service.getCallbackUrl();
                if (service.getCallbackUrl().length() > 0) {
                    createNotificationClient(url) //
                        .put("", body) //
                        .subscribe(
                            notUsed -> logger.debug("Service {} notified", service.getName()), throwable -> logger
                                .warn("Service notification failed for service: {}", service.getName(), throwable),
                            () -> logger.debug("All services notified"));
                }
            }
        }
    }

    @SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
    private void onSynchronizationError(Ric ric, Throwable t) {
        logger.warn("Synchronization failed for ric: {}, reason: {}", ric.name(), t.getMessage());
        // If synchronization fails, try to remove all instances
        deleteAllPoliciesInRepository(ric);

        Flux<PolicyType> recoverTypes = this.a1ClientFactory.createA1Client(ric) //
            .flatMapMany(a1Client -> synchronizePolicyTypes(ric, a1Client));
        Flux<?> deletePoliciesInRic = this.a1ClientFactory.createA1Client(ric) //
            .flatMapMany(A1Client::deleteAllPolicies) //
            .doOnComplete(() -> deleteAllPoliciesInRepository(ric));

        Flux.concat(recoverTypes, deletePoliciesInRic) //
            .subscribe(x -> logger.debug("Brute recover: {}", x), //
                throwable -> onRecoveryError(ric, throwable), //
                () -> onSynchronizationComplete(ric));
    }

    @SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
    private void onRecoveryError(Ric ric, Throwable t) {
        logger.warn("Synchronization failure recovery failed for ric: {}, reason: {}", ric.name(), t.getMessage());
        ric.setState(RicState.UNDEFINED);
    }

    AsyncRestClient createNotificationClient(final String url) {
        return new AsyncRestClient(url);
    }

    private Flux<PolicyType> synchronizePolicyTypes(Ric ric, A1Client a1Client) {
        return a1Client.getPolicyTypeIdentities() //
            .doOnNext(x -> ric.clearSupportedPolicyTypes()) //
            .flatMapMany(Flux::fromIterable) //
            .doOnNext(typeId -> logger.debug("For ric: {}, handling type: {}", ric.getConfig().name(), typeId)) //
            .flatMap(policyTypeId -> getPolicyType(policyTypeId, a1Client)) //
            .doOnNext(ric::addSupportedPolicyType); //
    }

    private Mono<PolicyType> getPolicyType(String policyTypeId, A1Client a1Client) {
        if (policyTypes.contains(policyTypeId)) {
            return Mono.just(policyTypes.get(policyTypeId));
        }
        return a1Client.getPolicyTypeSchema(policyTypeId) //
            .flatMap(schema -> createPolicyType(policyTypeId, schema));
    }

    private Mono<PolicyType> createPolicyType(String policyTypeId, String schema) {
        PolicyType pt = ImmutablePolicyType.builder().name(policyTypeId).schema(schema).build();
        policyTypes.put(pt);
        return Mono.just(pt);
    }

    private void deleteAllPoliciesInRepository(Ric ric) {
        synchronized (policies) {
            for (Policy policy : policies.getForRic(ric.name())) {
                this.policies.remove(policy);
            }
        }
    }

    private Flux<String> recreateAllPoliciesInRic(Ric ric, A1Client a1Client) {
        synchronized (policies) {
            return Flux.fromIterable(new Vector<>(policies.getForRic(ric.name()))) //
                .doOnNext(
                    policy -> logger.debug("Recreating policy: {}, for ric: {}", policy.id(), ric.getConfig().name()))
                .flatMap(a1Client::putPolicy);
        }
    }

}
