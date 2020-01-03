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

import org.oransc.policyagent.clients.A1Client;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Rics;
import org.oransc.policyagent.repository.Ric.RicState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Regularly checks the exisiting rics towards the local repository to keep it consistent.
 */
@Component
@EnableScheduling
public class RepositorySupervision {
    private static final Logger logger = LoggerFactory.getLogger(RepositorySupervision.class);

    private final Rics rics;
    private final Policies policies;
    private final A1Client a1Client;

    @Autowired
    public RepositorySupervision(Rics rics, Policies policies, A1Client a1Client) {
        this.rics = rics;
        this.policies = policies;
        this.a1Client = a1Client;
    }

    /**
     * Regularly contacts all Rics to check if they are alive.
     */
    @Scheduled(fixedRate = 1000 * 60)
    public void checkAllRics() {
        logger.debug("Checking Rics starting");
        createTask().subscribe(this::onRicChecked, this::onError, this::onComplete);

    }

    private Flux<Ric> createTask() {
        return Flux.fromIterable(rics.getRics()) //
            .groupBy(ric -> ric.state()) //
            .flatMap(fluxGroup -> handleGroup(fluxGroup.key(), fluxGroup));
    }

    private Flux<Ric> handleGroup(Ric.RicState key, Flux<Ric> fluxGroup) {
        logger.debug("Handling group {}", key);
        switch (key) {
            case ACTIVE:
                return fluxGroup.flatMap(this::checkActive);

            case NOT_REACHABLE:
                return fluxGroup.flatMap(this::checkNotReachable);

            default:
                // If not initiated, leave it to the StartupService.
                return Flux.empty();
        }
    }

    private Mono<Ric> checkActive(Ric ric) {
        logger.debug("Handling active ric {}", ric);
        a1Client.getPolicyIdentities(ric.getConfig().baseUrl()) //
        .filter(policyId -> !policies.containsPolicy(policyId)) //
        .doOnNext(policyId -> logger.debug("Deleting policy {}", policyId))
        .flatMap(policyId -> a1Client.deletePolicy(ric.getConfig().baseUrl(), policyId)) //
        .subscribe();
        return Mono.just(ric);
    }

    private Mono<Ric> checkNotReachable(Ric ric) {
        logger.debug("Handling not reachable ric {}", ric);
        a1Client.getPolicyIdentities(ric.getConfig().baseUrl()) //
        .filter(policyId -> !policies.containsPolicy(policyId)) //
        .doOnNext(policyId -> logger.debug("Deleting policy {}", policyId))
        .flatMap(policyId -> a1Client.deletePolicy(ric.getConfig().baseUrl(), policyId)) //
        .subscribe(null, null, () -> ric.setState(RicState.ACTIVE));
        return Mono.just(ric);
    }

    private void onRicChecked(Ric ric) {
        logger.info("Ric: " + ric.name() + " checked");
    }

    private void onError(Throwable t) {
        logger.error("Rics supervision failed", t);
    }

    private void onComplete() {
        logger.debug("Checking Rics completed");
    }

}
