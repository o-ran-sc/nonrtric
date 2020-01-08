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
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Ric.RicState;
import org.oransc.policyagent.repository.Rics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Loads information about RealTime-RICs at startup.
 */
@Service("startupService")
public class StartupService {

    private static final Logger logger = LoggerFactory.getLogger(StartupService.class);

    @Autowired
    ApplicationConfig applicationConfig;

    @Autowired
    private Rics rics;

    @Autowired
    PolicyTypes policyTypes;

    @Autowired
    private A1Client a1Client;

    StartupService(ApplicationConfig appConfig, Rics rics, PolicyTypes policyTypes, A1Client a1Client) {
        this.applicationConfig = appConfig;
        this.rics = rics;
        this.policyTypes = policyTypes;
        this.a1Client = a1Client;
    }

    /**
     * Reads the configured Rics and performs the service discovery. The result is put into the repository.
     */
    public void startup() {
        applicationConfig.initialize();
        Flux.fromIterable(applicationConfig.getRicConfigs()) //
            .map(ricConfig -> new Ric(ricConfig)) //
            .doOnNext(ric -> logger.debug("Handling ric: {}", ric.getConfig().name())) //
            .flatMap(this::addPolicyTypesForRic) //
            .flatMap(this::deletePoliciesForRic) //
            .doOnNext(rics::put) //
            .subscribe();
    }

    private Mono<Ric> addPolicyTypesForRic(Ric ric) {
        a1Client.getPolicyTypeIdentities(ric.getConfig().baseUrl()) //
            .doOnNext(typeId -> logger.debug("For ric: {}, handling type: {}", ric.getConfig().name(), typeId))
            .flatMap((policyTypeId) -> addTypeToRepo(ric, policyTypeId)) //
            .flatMap(type -> addTypeToRic(ric, type)) //
            .subscribe(null, cause -> setRicToNotReachable(ric, cause), () -> setRicToActive(ric));
        return Mono.just(ric);
    }

    private Mono<PolicyType> addTypeToRepo(Ric ric, String policyTypeId) {
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

    private Mono<PolicyType> addTypeToRic(Ric ric, PolicyType policyType) {
        ric.addSupportedPolicyType(policyType);
        return Mono.just(policyType);
    }

    private Mono<Ric> deletePoliciesForRic(Ric ric) {
        if (!Ric.RicState.NOT_REACHABLE.equals(ric.state())) {
            a1Client.getPolicyIdentities(ric.getConfig().baseUrl()) //
                .doOnNext(
                    policyId -> logger.debug("Deleting policy: {}, for ric: {}", policyId, ric.getConfig().name()))
                .flatMap(policyId -> a1Client.deletePolicy(ric.getConfig().baseUrl(), policyId)) //
                .subscribe(null, cause -> setRicToNotReachable(ric, cause), null);
        }

        return Mono.just(ric);
    }

    private void setRicToNotReachable(Ric ric, Throwable t) {
        ric.setState(Ric.RicState.NOT_REACHABLE);
        logger.info("Unable to connect to ric {}. Cause: {}", ric.name(), t.getMessage());
    }

    private void setRicToActive(Ric ric) {
        ric.setState(RicState.ACTIVE);
    }

}
