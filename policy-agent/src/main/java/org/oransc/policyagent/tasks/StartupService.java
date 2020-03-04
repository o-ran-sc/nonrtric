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
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.configuration.ApplicationConfig.RicConfigUpdate;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Rics;
import org.oransc.policyagent.repository.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Loads information about RealTime-RICs at startup.
 */
@Service("startupService")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StartupService implements ApplicationConfig.Observer {

    private static final Logger logger = LoggerFactory.getLogger(StartupService.class);

    @Autowired
    ApplicationConfig applicationConfig;

    @Autowired
    RefreshConfigTask refreshConfigTask;

    @Autowired
    private Rics rics;

    @Autowired
    PolicyTypes policyTypes;

    @Autowired
    private A1ClientFactory a1ClientFactory;

    @Autowired
    private Policies policies;

    @Autowired
    private Services services;

    // Only for unit testing
    StartupService(ApplicationConfig appConfig, RefreshConfigTask refreshTask, Rics rics, PolicyTypes policyTypes,
        A1ClientFactory a1ClientFactory, Policies policies, Services services) {
        this.applicationConfig = appConfig;
        this.refreshConfigTask = refreshTask;
        this.rics = rics;
        this.policyTypes = policyTypes;
        this.a1ClientFactory = a1ClientFactory;
        this.policies = policies;
        this.services = services;
    }

    @Override
    public void onRicConfigUpdate(RicConfig ricConfig, RicConfigUpdate event) {
        synchronized (this.rics) {
            switch (event) {
                case ADDED:
                case CHANGED:
                    Ric ric = new Ric(ricConfig);
                    rics.put(ric);
                    RicSynchronizationTask synchronizationTask = createSynchronizationTask();
                    synchronizationTask.run(ric);
                    break;

                case REMOVED:
                    rics.remove(ricConfig.name());
                    policies.removePoliciesForRic(ricConfig.name());
                    break;

                default:
                    logger.error("Unhandled ric event: {}", event);
            }
        }
    }

    /**
     * Reads the configured Rics and performs the service discovery. The result is put into the repository.
     */
    public void startup() {
        logger.debug("Starting up");
        applicationConfig.addObserver(this);
        refreshConfigTask.start();
    }

    RicSynchronizationTask createSynchronizationTask() {
        return new RicSynchronizationTask(a1ClientFactory, policyTypes, policies, services);
    }
}
