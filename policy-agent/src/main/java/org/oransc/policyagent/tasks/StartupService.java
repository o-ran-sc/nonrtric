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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.oransc.policyagent.clients.A1Client;
import org.oransc.policyagent.configuration.ApplicationConfig;
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

    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

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
            .doOnNext(ric -> logger.debug("Handling ric: {}", ric.getConfig().name())).flatMap(this::handlePolicyTypes)
            .flatMap(this::setRicToActive) //
            .flatMap(this::addRicToRepo) //
            .subscribe();
    }

    private Mono<Ric> handlePolicyTypes(Ric ric) {
        a1Client.getAllPolicyTypes(ric.getConfig().baseUrl()) //
            .map(policyTypeString -> gson.fromJson(policyTypeString, ImmutablePolicyType.class)) //
            .doOnNext(type -> logger.debug("For ric: {}, handling type: {}", ric.getConfig().name(), type.name()))
            .flatMap(this::addTypeToRepo) //
            .flatMap(type -> addTypeToRic(ric, type)) //
            .flatMap(type -> deletePoliciesForType(ric, type)) //
            .subscribe();
        return Mono.just(ric);
    }

    private Mono<PolicyType> addTypeToRepo(PolicyType policyType) {
        if (!policyTypes.contains(policyType)) {
            policyTypes.put(policyType);
        }
        return Mono.just(policyType);
    }

    private Mono<PolicyType> addTypeToRic(Ric ric, PolicyType policyType) {
        ric.addSupportedPolicyType(policyType);
        return Mono.just(policyType);
    }

    private Mono<Void> deletePoliciesForType(Ric ric, PolicyType policyType) {
        a1Client.getPoliciesForType(ric.getConfig().baseUrl(), policyType.name()) //
            .doOnNext(policyId -> logger.debug("deleting policy: {}, for ric: {}", policyId, ric.getConfig().name())) //
            .flatMap(policyId -> a1Client.deletePolicy(ric.getConfig().baseUrl(), policyId)) //
            .subscribe();

        return Mono.empty();
    }

    private Mono<Ric> setRicToActive(Ric ric) {
        ric.setState(RicState.ACTIVE);

        return Mono.just(ric);
    }

    private Mono<Void> addRicToRepo(Ric ric) {
        rics.put(ric);

        return Mono.empty();
    }
}
