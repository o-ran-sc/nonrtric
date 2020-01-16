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
    private Rics rics;

    @Autowired
    PolicyTypes policyTypes;

    @Autowired
    private A1Client a1Client;

    @Autowired
    private Policies policies;

    @Autowired
    private Services services;

    // Only for unittesting
    StartupService(ApplicationConfig appConfig, Rics rics, PolicyTypes policyTypes, A1Client a1Client,
        Policies policies, Services services) {
        this.applicationConfig = appConfig;
        this.rics = rics;
        this.policyTypes = policyTypes;
        this.a1Client = a1Client;
        this.policies = policies;
        this.services = services;
    }

    @Override
    public void onRicConfigUpdate(RicConfig ricConfig, ApplicationConfig.RicConfigUpdate event) {
        synchronized (this.rics) {
            if (event.equals(ApplicationConfig.RicConfigUpdate.ADDED)
                || event.equals(ApplicationConfig.RicConfigUpdate.CHANGED)) {
                Ric ric = new Ric(ricConfig);
                rics.put(ric);
                RicRecoveryTask recoveryTask = new RicRecoveryTask(a1Client, policyTypes, policies, services);
                recoveryTask.run(ric);
            } else if (event.equals(ApplicationConfig.RicConfigUpdate.REMOVED)) {
                rics.remove(ricConfig.name());
            } else {
                logger.debug("Unhandled event :" + event);
            }
        }
    }

    /**
     * Reads the configured Rics and performs the service discovery. The result is put into the repository.
     */
    public void startup() {
        logger.debug("Starting up");
        applicationConfig.addObserver(this);
        applicationConfig.initialize();
    }

}
