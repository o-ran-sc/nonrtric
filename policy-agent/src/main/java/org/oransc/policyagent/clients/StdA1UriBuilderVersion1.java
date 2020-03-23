/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2020 Nordix Foundation
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

package org.oransc.policyagent.clients;

import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.Policy;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * URI builder for A1 STD version 1.1
 */
public class StdA1UriBuilderVersion1 implements A1UriBuilder {

    private final RicConfig ricConfig;

    @Autowired
    public StdA1UriBuilderVersion1(RicConfig ricConfig) {
        this.ricConfig = ricConfig;
    }

    @Override
    public String createPutPolicyUri(Policy policy) {
        return policiesBaseUri() + policy.id();
    }

    @Override
    public String createGetPolicyIdsUri() {
        return baseUri() + "/policies";
    }

    @Override
    public String createDeleteUri(String policyId) {
        return policiesBaseUri() + policyId;
    }

    @Override
    public String createGetPolicyStatusUri(String policyId) {
        return policiesBaseUri() + policyId + "/status";
    }

    private String baseUri() {
        return ricConfig.baseUrl() + "/A1-P/v1";
    }

    private String policiesBaseUri() {
        return createGetPolicyIdsUri() + "/";
    }
}
