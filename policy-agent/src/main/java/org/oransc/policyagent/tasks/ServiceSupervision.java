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

@Component
@EnableScheduling
public class ServiceSupervision {
    private static final Logger logger = LoggerFactory.getLogger(ServiceSupervision.class);
    private final Services services;
    private final Policies policies;

    @Autowired
    public ServiceSupervision(Services services, Policies policies) {
        this.services = services;
        this.policies = policies;
    }

    @Scheduled(fixedRate = 1000 * 60)
    public void checkAllServices() {
        logger.debug("Checking services starting");
        createTask().subscribe(this::onPolicyDeleted, this::onError, this::onComplete);
    }

    private void onPolicyDeleted(Policy policy) {
        logger.info("Policy deleted due to inactivity: " + policy.id() + ", service: " + policy.ownerServiceName());
    }

    private void onError(Throwable t) {
        logger.error("Service supervision failed", t);
    }

    private void onComplete() {
        logger.debug("Checking services completed");
    }

    Flux<Policy> createTask() {
        return Flux.fromIterable(services.getAll()) //
            .filter(service -> service.isExpired()) //
            .doOnNext(service -> logger.info("Service is expired:" + service.getName()))
            .flatMap(service -> getAllPolicies(service)) //
            .flatMap(policy -> deletePolicy(policy));
    }

    Flux<Policy> getAllPolicies(Service service) {
        return Flux.fromIterable(policies.getForService(service.getName()));
    }

    Flux<Policy> deletePolicy(Policy policy) {
        this.policies.remove(policy);
        return Flux.just(policy);
    }

}
