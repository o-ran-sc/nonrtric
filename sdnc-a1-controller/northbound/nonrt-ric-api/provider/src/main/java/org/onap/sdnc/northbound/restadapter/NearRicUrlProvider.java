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

package org.onap.sdnc.northbound.restadapter;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * This class provides Near-RIC api to invoke the A1 interface
 *
 * @author lathishbabu.ganesan@est.tech
 *
 */

public class NearRicUrlProvider {

  public NearRicUrlProvider() {
  }

  /**
   * Retrieve the base url of the Near-RIC
   *
   * @return the base url
   */
  public String getBaseUrl(final String nearRtRicUrl) {
    String baseUrl = nearRtRicUrl + "/A1-P/v1";
    return UriComponentsBuilder.fromUriString(baseUrl).build().toString();
  }

  /**
   * Retrieve the policy type ids url
   *
   * @return the policytype ids url
   */
  public String getPolicyTypeIdentitiesUrl(final String nearRtRicUrl) {
    return UriComponentsBuilder.fromUriString(getBaseUrl(nearRtRicUrl)).pathSegment("policytypes")
            .pathSegment("identities").build().toString();
  }

  /**
   * Retrieve the url of the policy instances
   *
   * @param policyTypeId Policy Type Id
   * @return the policy ids url
   */
  public String getPolicyIdentitiesUrl(final String nearRtRicUrl) {
    return UriComponentsBuilder.fromUriString(getBaseUrl(nearRtRicUrl)).pathSegment("policies")
            .pathSegment("identities").build().toString();
  }

  /**
   * Retrieve the url of policy type
   *
   * @param policyTypeId Policy Type Id
   * @return the policy type url
   */
  public String getPolicyTypeUrl(final String nearRtRicUrl, final String policyTypeId) {
    return UriComponentsBuilder.fromUriString(getBaseUrl(nearRtRicUrl)).pathSegment("policytypes")
        .pathSegment(policyTypeId).build().toString();
  }

  /**
   * Retrieve the url of the policy instance id
   *
   * @param policyId Policy Id
   * @return the policy id url
   */
  public String getPolicyUrl(final String nearRtRicUrl, final String policyId) {
    return UriComponentsBuilder.fromUriString(getBaseUrl(nearRtRicUrl)).pathSegment("policies")
            .pathSegment(policyId).build().toString();
  }
}
