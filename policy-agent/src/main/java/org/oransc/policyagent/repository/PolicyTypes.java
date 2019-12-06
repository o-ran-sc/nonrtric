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

package org.oransc.policyagent.repository;

import java.util.HashMap;
import java.util.Map;

import org.oransc.policyagent.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class PolicyTypes {
    private static final Logger logger = LoggerFactory.getLogger(PolicyTypes.class);

    private Map<String, PolicyType> types = new HashMap<String, PolicyType>();

    @Autowired
    public PolicyTypes() {
    }

    public synchronized PolicyType getType(String name) throws ServiceException {
        PolicyType t = types.get(name);
        if (t == null) {
            throw new ServiceException("Could not find type: " + name);
        }
        return t;
    }

    public synchronized void putType(String name, PolicyType type) {
        types.put(name, type);
    }

}
