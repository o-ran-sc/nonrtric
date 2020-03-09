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

import org.oransc.policyagent.clients.A1ClientFactory;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.Service;
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
 * Periodically checks that services with a keepAliveInterval set are alive. If a service is deemed not alive,
 * all the service's policies are deleted, both in the repository and in the affected Rics, and the service is
 * removed from the repository. This means that the service needs to register again after this.
 */
@Component
@EnableScheduling
public class ServiceSupervision {
    private static final Logger logger = LoggerFactory.getLogger(ServiceSupervision.class);
    private final Services services;
    private final Policies policies;
    private A1ClientFactory a1ClientFactory;

    @Autowired
    public ServiceSupervision(Services services, Policies policies, A1ClientFactory a1ClientFactory) {
        this.services = services;
        this.policies = policies;
        this.a1ClientFactory = a1ClientFactory;
    }

    @Scheduled(fixedRate = 1000 * 60)
    public void checkAllServices() {
        logger.debug("Checking services starting");
        createTask().subscribe(this::onPolicyDeleted, null, this::onComplete);
    }

    @SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
    private void onPolicyDeleted(Policy policy) {
        logger.debug("Policy deleted due to inactivity: {}, service: {}", policy.id(), policy.ownerServiceName());
    }

    private void onComplete() {
        logger.debug("Checking services completed");
    }

    private Flux<Policy> createTask() {
        synchronized (services) {
            return Flux.fromIterable(services.getAll()) //
                .filter(Service::isExpired) //
                .doOnNext(service -> logger.info("Service is expired: {}", service.getName())) //
                .doOnNext(service -> services.remove(service.getName())) //
                .flatMap(this::getAllPoliciesForService) //
                .doOnNext(policies::remove) //
                .flatMap(this::deletePolicyInRic);
        }
    }

    private Flux<Policy> getAllPoliciesForService(Service service) {
        synchronized (policies) {
            return Flux.fromIterable(policies.getForService(service.getName()));
        }
    }

    private Mono<Policy> deletePolicyInRic(Policy policy) {
        return a1ClientFactory.createA1Client(policy.ric()) //
            .flatMap(client -> client.deletePolicy(policy) //
                .onErrorResume(exception -> handleDeleteFromRicFailure(policy, exception)) //
                .map(nothing -> policy));
    }

    @SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
    private Mono<String> handleDeleteFromRicFailure(Policy policy, Throwable e) {
        logger.warn("Could not delete policy: {} from ric: {}", policy.id(), policy.ric().name(), e);
        return Mono.empty();
    }
}
