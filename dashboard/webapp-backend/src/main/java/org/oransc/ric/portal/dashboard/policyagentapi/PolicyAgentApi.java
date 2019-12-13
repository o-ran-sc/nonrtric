/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2019 AT&T Intellectual Property
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
package org.oransc.ric.portal.dashboard.policyagentapi;

import org.oransc.ric.portal.dashboard.model.PolicyInstances;
import org.oransc.ric.portal.dashboard.model.PolicyTypes;
import org.springframework.web.client.RestClientException;

public interface PolicyAgentApi {

    public PolicyTypes getAllPolicyTypes() throws RestClientException;

    public PolicyInstances getPolicyInstancesForType(String type);

    public String getPolicyInstance(String id) throws RestClientException;

    public void putPolicy(String policyTypeIdString, String policyInstanceId, String json) throws RestClientException;

    public void deletePolicy(String policyInstanceId) throws RestClientException;
}
