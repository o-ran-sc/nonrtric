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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Autowired
    private Policies policies;

    StartupService(ApplicationConfig appConfig, Rics rics, PolicyTypes policyTypes, A1Client a1Client,
        Policies policies) {
        this.applicationConfig = appConfig;
        this.rics = rics;
        this.policyTypes = policyTypes;
        this.a1Client = a1Client;
        this.policies = policies;
    }

    /**
     * Reads the configured Rics and performs the service discovery. The result is put into the repository.
     */
    public void startup() {
        logger.debug("Starting up");
        applicationConfig.initialize();
        for (RicConfig ricConfig : applicationConfig.getRicConfigs()) {
            rics.put(new Ric(ricConfig));
        }
        RicRecoveryTask recoveryTask = new RicRecoveryTask(a1Client, policyTypes, policies);
        recoveryTask.run(rics.getRics()); // recover all Rics
    }

}
