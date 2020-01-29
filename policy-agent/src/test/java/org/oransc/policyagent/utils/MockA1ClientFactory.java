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

package org.oransc.policyagent.utils;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.oransc.policyagent.clients.A1Client;
import org.oransc.policyagent.clients.A1ClientFactory;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockA1ClientFactory extends A1ClientFactory {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Map<String, MockA1Client> clients = new HashMap<>();
    private final PolicyTypes policyTypes;

    public MockA1ClientFactory(PolicyTypes policyTypes) {
        this.policyTypes = policyTypes;
    }

    @Override
    protected A1Client createStdA1ClientImpl(Ric ric) {
        return getOrCreateA1Client(ric.name());
    }

    @Override
    protected A1Client createSdncOscA1Client(Ric ric) {
        return getOrCreateA1Client(ric.name());
    }

    @Override
    protected A1Client createSdnrOnapA1Client(Ric ric) {
        return getOrCreateA1Client(ric.name());
    }

    public MockA1Client getOrCreateA1Client(String ricName) {
        if (!clients.containsKey(ricName)) {
            logger.debug("Creating client for RIC: {}", ricName);
            MockA1Client client = new MockA1Client(policyTypes);
            clients.put(ricName, client);
        }
        return clients.get(ricName);
    }

}
