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

package org.onap.sdnc.northbound.restadpter;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * This class provides Near-RIC api to invoke the A1 interface
 * 
 * @author lathishbabu.ganesan@est.tech
 *
 */

public class NearRicUrlProvider {

  private String baseUrl;

  public NearRicUrlProvider() {
    // Near ric ip is passed in payload
    baseUrl = "http://localhost:8080/a1-p/";
  }

  /**
   * Retrieve the base url of the Near-RIC
   * 
   * @return the base url
   */
  public String getBaseUrl() {
    return UriComponentsBuilder.fromUriString(baseUrl).build().toString();
  }

  /**
   * Retrieve the url of A1 healthcheck
   * 
   * @return the health check url
   */
  public String getHealthCheck() {
    return UriComponentsBuilder.fromUriString(getBaseUrl()).pathSegment("healthcheck").build()
        .toString();
  }

  /**
   * Retrieve the policy type url
   * 
   * @return the base url with the policytypes
   */
  public String getPolicyTypes() {
    return UriComponentsBuilder.fromUriString(getBaseUrl()).pathSegment("policytypes").build()
        .toString();
  }

  /**
   * Retrieve the url of policy type id
   * 
   * @param policyTypeId Policy Type Id
   * @return the policy type id url
   */
  public String getPolicyTypeId(final String policyTypeId) {
    return UriComponentsBuilder.fromUriString(getBaseUrl()).pathSegment("policytypes")
        .pathSegment(policyTypeId).build().toString();
  }

  /**
   * Retrieve the url of the policy instances
   * 
   * @param policyTypeId Policy Type Id
   * @return the policy instances for the given policy type
   */
  public String getPolicyInstances(final String policyTypeId) {
    return UriComponentsBuilder.fromUriString(getPolicyTypeId(policyTypeId)).pathSegment("policies")
        .build().toString();
  }

  /**
   * Retrieve the url of the policy instance id
   * 
   * @param policyTypeId Policy Type Id
   * @param policyInstanceId Policy Instance Id
   * @return the policy instance id for the given policy type
   */
  public String getPolicyInstanceId(final String policyTypeId, final String policyInstanceId) {
    return UriComponentsBuilder.fromUriString(getPolicyTypeId(policyTypeId)).pathSegment("policies")
        .pathSegment(policyInstanceId).build().toString();
  }

  /**
   * Retrieve the url of the policy instance id status
   * 
   * @param policyTypeId Policy Type Id
   * @param policyInstanceId Policy Instance Id
   * @return the policy instance id status for the given policy type
   */
  public String getPolicyInstanceIdStatus(final String policyTypeId,
      final String policyInstanceId) {
    return UriComponentsBuilder.fromUriString(getPolicyInstanceId(policyTypeId, policyInstanceId))
        .pathSegment("status").build().toString();
  }
}
