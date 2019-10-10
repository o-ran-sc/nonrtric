/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 * ================================================================================
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.nearric.simulator.service;

import org.onap.nearric.simulator.model.PolicyType;
import org.oransc.ric.a1med.api.model.PolicyTypeSchema;

/**
 * @author lathishbabu.ganesan@est.tech
 *
 */

public interface A1PApiService {

  public void getHealthCheck();

  public void getPolicyTypes();

  public PolicyType getPolicyType(Integer policyTypeId);

  public void deletePolicyTypeId();

  public void putPolicyType(Integer policyTypeId, PolicyTypeSchema policyTypeSchema);

  public void getPolicyInstances();

  public void getPolicyInstanceId();

  public void deletePolicyInstanceId();

  public void putPolicyInstance(Integer policyTypeId, String policyInstanceId, Object body);

  public void getPolicyInstanceIdStatus();

}
