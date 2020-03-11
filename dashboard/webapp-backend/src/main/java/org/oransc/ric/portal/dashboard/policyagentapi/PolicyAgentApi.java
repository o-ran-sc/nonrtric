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
package org.oransc.ric.portal.dashboard.policyagentapi;

import org.springframework.http.ResponseEntity;

public interface PolicyAgentApi {

    public ResponseEntity<String> getAllPolicyTypes();

    public ResponseEntity<String> getPolicyInstancesForType(String type);

    public ResponseEntity<Object> getPolicyInstance(String id);

    public ResponseEntity<String> putPolicy(String policyTypeIdString, String policyInstanceId, Object json,
        String ric);

    public ResponseEntity<String> deletePolicy(String policyInstanceId);

    public ResponseEntity<String> getRicsSupportingType(String typeName);

}
