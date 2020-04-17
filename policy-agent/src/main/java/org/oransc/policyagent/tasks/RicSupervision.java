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

package org.oransc.policyagent.tasks;

import java.util.Collection;

import org.oransc.policyagent.clients.A1Client;
import org.oransc.policyagent.clients.A1ClientFactory;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.Lock.LockType;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Ric.RicState;
import org.oransc.policyagent.repository.Rics;
import org.oransc.policyagent.repository.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Regularly checks the existing rics towards the local repository to keep it
 * consistent. When the policy types or instances in the Near-RT RIC is not
 * consistent, a synchronization is performed.
 */
@Component
@EnableScheduling
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
public class RicSupervision {
    private static final Logger logger = LoggerFactory.getLogger(RicSupervision.class);

    private final Rics rics;
    private final Policies policies;
    private final PolicyTypes policyTypes;
    private final A1ClientFactory a1ClientFactory;
    private final Services services;

    private static class SynchStartedException extends ServiceException {
        private static final long serialVersionUID = 1L;

        public SynchStartedException(String message) {
            super(message);
        }
    }

    @Autowired
    public RicSupervision(Rics rics, Policies policies, A1ClientFactory a1ClientFactory, PolicyTypes policyTypes,
        Services services) {
        this.rics = rics;
        this.policies = policies;
        this.a1ClientFactory = a1ClientFactory;
        this.policyTypes = policyTypes;
        this.services = services;
    }

    /**
     * Regularly contacts all Rics to check if they are alive and synchronized.
     */
    @Scheduled(fixedRate = 1000 * 60)
    public void checkAllRics() {
        logger.debug("Checking Rics starting");
        createTask().subscribe(null, null, () -> logger.debug("Checking all RICs completed"));
    }

    private Flux<RicData> createTask() {
        return Flux.fromIterable(rics.getRics()) //
            .flatMap(this::createRicData) //
            .flatMap(this::checkOneRic) //
            .onErrorResume(throwable -> Mono.empty());

    }

    private Mono<RicData> checkOneRic(RicData ricData) {
        return checkRicState(ricData) //
            .flatMap(x -> ricData.ric.getLock().lock(LockType.EXCLUSIVE)) //
            .flatMap(notUsed -> setRicState(ricData)) //
            .flatMap(x -> checkRicPolicies(ricData)) //
            .flatMap(x -> checkRicPolicyTypes(ricData)) //
            .doOnNext(x -> onRicCheckedOk(ricData)) //
            .doOnError(t -> onRicCheckedError(t, ricData)) //
            .doFinally(r -> onRicCheckedFinally(ricData));
    }

    private void onRicCheckedError(Throwable t, RicData ricData) {
        logger.debug("Ric: {} check stopped, exception: {}", ricData.ric.name(), t.getMessage());
        if (t instanceof SynchStartedException) {
            // this is just a temporary state,
            ricData.ric.setState(RicState.AVAILABLE);
        } else {
            ricData.ric.setState(RicState.UNAVAILABLE);
        }
        ricData.ric.getLock().unlockBlocking();
    }

    private void onRicCheckedOk(RicData ricData) {
        logger.debug("Ric: {} checked OK", ricData.ric.name());
        ricData.ric.setState(RicState.AVAILABLE);
        ricData.ric.getLock().unlockBlocking();
    }

    private void onRicCheckedFinally(RicData ricData) {
        logger.debug("Ric: {} checke fithis.a1ClientFactory.createA1Client(ric)nalized", ricData.ric.name());
    }

    @SuppressWarnings("squid:S2445") // Blocks should be synchronized on "private final" fields
    private Mono<RicData> setRicState(RicData ric) {
        synchronized (ric) {
            if (ric.ric.getState() == RicState.CONSISTENCY_CHECK) {
                logger.debug("Ric: {} is already being checked", ric.ric.getConfig().name());
                return Mono.empty();
            }
            ric.ric.setState(RicState.CONSISTENCY_CHECK);
            return Mono.just(ric);
        }
    }

    private static class RicData {
        RicData(Ric ric, A1ClientFactory a1ClientFactory) {
            this.ric = ric;
            this.a1ClientFactory = a1ClientFactory;

        }

        Mono<A1Client> getClient() {
            if (a1Client == null) {
                a1Client = this.a1ClientFactory.createA1Client(ric);
            }
            return a1Client;
        }

        final Ric ric;
        private Mono<A1Client> a1Client;
        private final A1ClientFactory a1ClientFactory;
    }

    private Mono<RicData> createRicData(Ric ric) {
        return Mono.just(new RicData(ric, a1ClientFactory));
    }

    private Mono<RicData> checkRicState(RicData ric) {
        if (ric.ric.getState() == RicState.UNAVAILABLE) {
            return startSynchronization(ric) //
                .onErrorResume(t -> Mono.empty());
        } else if (ric.ric.getState() == RicState.SYNCHRONIZING || ric.ric.getState() == RicState.CONSISTENCY_CHECK) {
            return Mono.empty();
        } else {
            return Mono.just(ric);
        }
    }

    private Mono<RicData> checkRicPolicies(RicData ric) {
        return ric.getClient() //
            .flatMap(client -> client.getPolicyIdentities()) //
            .flatMap(ricP -> validateInstances(ricP, ric));
    }

    private Mono<RicData> validateInstances(Collection<String> ricPolicies, RicData ric) {
        synchronized (this.policies) {
            if (ricPolicies.size() != policies.getForRic(ric.ric.name()).size()) {
                return startSynchronization(ric);
            }

            for (String policyId : ricPolicies) {
                if (!policies.containsPolicy(policyId)) {
                    return startSynchronization(ric);
                }
            }
            return Mono.just(ric);
        }
    }

    private Mono<RicData> checkRicPolicyTypes(RicData ric) {
        return ric.getClient() //
            .flatMap(client -> client.getPolicyTypeIdentities()) //
            .flatMap(ricTypes -> validateTypes(ricTypes, ric));
    }

    private Mono<RicData> validateTypes(Collection<String> ricTypes, RicData ric) {
        if (ricTypes.size() != ric.ric.getSupportedPolicyTypes().size()) {
            return startSynchronization(ric);
        }
        for (String typeName : ricTypes) {
            if (!ric.ric.isSupportingType(typeName)) {
                return startSynchronization(ric);
            }
        }
        return Mono.just(ric);
    }

    private Mono<RicData> startSynchronization(RicData ric) {
        RicSynchronizationTask synchronizationTask = createSynchronizationTask();
        synchronizationTask.run(ric.ric);
        return Mono.error(new SynchStartedException("Syncronization started"));
    }

    RicSynchronizationTask createSynchronizationTask() {
        return new RicSynchronizationTask(a1ClientFactory, policyTypes, policies, services);
    }
}
