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

import org.oransc.policyagent.clients.A1Client;
import org.oransc.policyagent.clients.A1ClientFactory;
import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.repository.ImmutablePolicyType;
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

import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

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
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
public class RicSynchronizationTask {

    private static final Logger logger = LoggerFactory.getLogger(RicSynchronizationTask.class);
    static final int CONCURRENCY_RIC = 1; // How may paralell requests that is sent to one NearRT RIC

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

    public void run(Ric ric) {
        logger.debug("Handling ric: {}", ric.getConfig().name());

        if (ric.getState() == RicState.SYNCHRONIZING) {
            logger.debug("Ric: {} is already being synchronized", ric.getConfig().name());
            return;
        }

        ric.getLock().lock(LockType.EXCLUSIVE) //
            .flatMap(notUsed -> setRicState(ric)) //
            .flatMap(lock -> this.a1ClientFactory.createA1Client(ric)) //
            .flatMapMany(client -> runSynchronization(ric, client)) //
            .onErrorResume(throwable -> deleteAllPolicyInstances(ric, throwable))
            .subscribe(new BaseSubscriber<Object>() {
                @Override
                protected void hookOnError(Throwable throwable) {
                    logger.warn("Synchronization failure for ric: {}, reason: {}", ric.name(), throwable.getMessage());
                    ric.setState(RicState.UNAVAILABLE);
                }

                @Override
                protected void hookOnComplete() {
                    onSynchronizationComplete(ric);
                }

                @Override
                protected void hookFinally(SignalType type) {
                    ric.getLock().unlockBlocking();
                }
            });
    }

    @SuppressWarnings("squid:S2445") // Blocks should be synchronized on "private final" fields
    private Mono<Ric> setRicState(Ric ric) {
        synchronized (ric) {
            if (ric.getState() == RicState.SYNCHRONIZING) {
                logger.debug("Ric: {} is already being synchronized", ric.getConfig().name());
                return Mono.empty();
            }
            ric.setState(RicState.SYNCHRONIZING);
            return Mono.just(ric);
        }
    }

    private Flux<Object> runSynchronization(Ric ric, A1Client a1Client) {
        Flux<PolicyType> synchronizedTypes = synchronizePolicyTypes(ric, a1Client);
        Flux<?> policiesDeletedInRic = a1Client.deleteAllPolicies();
        Flux<Policy> policiesRecreatedInRic = recreateAllPoliciesInRic(ric, a1Client);

        return Flux.concat(synchronizedTypes, policiesDeletedInRic, policiesRecreatedInRic);
    }

    private void onSynchronizationComplete(Ric ric) {
        logger.debug("Synchronization completed for: {}", ric.name());
        ric.setState(RicState.AVAILABLE);
        notifyAllServices("Synchronization completed for:" + ric.name());
    }

    private void notifyAllServices(String body) {
        for (Service service : services.getAll()) {
            String url = service.getCallbackUrl();
            if (service.getCallbackUrl().length() > 0) {
                createNotificationClient(url) //
                    .put("", body) //
                    .subscribe( //
                        notUsed -> logger.debug("Service {} notified", service.getName()),
                        throwable -> logger.warn("Service notification failed for service: {}. Cause: {}",
                            service.getName(), throwable.getMessage()),
                        () -> logger.debug("All services notified"));
            }
        }
    }

    private Flux<Object> deleteAllPolicyInstances(Ric ric, Throwable t) {
        logger.debug("Recreation of policies failed for ric: {}, reason: {}", ric.name(), t.getMessage());
        deleteAllPoliciesInRepository(ric);

        Flux<PolicyType> synchronizedTypes = this.a1ClientFactory.createA1Client(ric) //
            .flatMapMany(a1Client -> synchronizePolicyTypes(ric, a1Client));
        Flux<?> deletePoliciesInRic = this.a1ClientFactory.createA1Client(ric) //
            .flatMapMany(A1Client::deleteAllPolicies) //
            .doOnComplete(() -> deleteAllPoliciesInRepository(ric));

        return Flux.concat(synchronizedTypes, deletePoliciesInRic);
    }

    AsyncRestClient createNotificationClient(final String url) {
        return new AsyncRestClient(url);
    }

    private Flux<PolicyType> synchronizePolicyTypes(Ric ric, A1Client a1Client) {
        return a1Client.getPolicyTypeIdentities() //
            .doOnNext(x -> ric.clearSupportedPolicyTypes()) //
            .flatMapMany(Flux::fromIterable) //
            .doOnNext(typeId -> logger.debug("For ric: {}, handling type: {}", ric.getConfig().name(), typeId)) //
            .flatMap(policyTypeId -> getPolicyType(policyTypeId, a1Client), CONCURRENCY_RIC) //
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
        for (Policy policy : policies.getForRic(ric.name())) {
            this.policies.remove(policy);
        }
    }

    private Flux<Policy> putPolicy(Policy policy, Ric ric, A1Client a1Client) {
        logger.debug("Recreating policy: {}, for ric: {}", policy.id(), ric.getConfig().name());
        return a1Client.putPolicy(policy) //
            .flatMapMany(notUsed -> Flux.just(policy));
    }

    private boolean checkTransient(Policy policy) {
        if (policy.isTransient()) {
            this.policies.remove(policy);
        }
        return policy.isTransient();
    }

    private Flux<Policy> recreateAllPoliciesInRic(Ric ric, A1Client a1Client) {
        return Flux.fromIterable(policies.getForRic(ric.name())) //
            .filter(policy -> !checkTransient(policy)) //
            .flatMap(policy -> putPolicy(policy, ric, a1Client), CONCURRENCY_RIC);
    }

}
