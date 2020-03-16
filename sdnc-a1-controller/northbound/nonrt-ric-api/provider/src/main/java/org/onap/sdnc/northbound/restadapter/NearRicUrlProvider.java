/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation.
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

  /**
   * Retrieve the base url of the Near-RIC
   *
   * @param nearRtRicUrl the near-rt-ric url
   * @return the base url
   */
  public String getBaseUrl(final String nearRtRicUrl) {
    String baseUrl = nearRtRicUrl + "/A1-P/v1";
    return UriComponentsBuilder.fromUriString(baseUrl).build().toString();
  }

  /**
   * Retrieve the policytypes url
   *
   * @param nearRtRicUrl the near-rt-ric url
   * @return the policytypes url
   */
  public String policyTypesUrl(final String nearRtRicUrl) {
    return UriComponentsBuilder.fromUriString(getBaseUrl(nearRtRicUrl)).pathSegment("policytypes")
            .build().toString();
  }

  /**
   * Retrieve the policies url
   *
   * @param nearRtRicUrl the near-rt-ric url
   * @return the policies url
   */
  public String policiesUrl(final String nearRtRicUrl) {
    return UriComponentsBuilder.fromUriString(getBaseUrl(nearRtRicUrl)).pathSegment("policies")
            .build().toString();
  }

  /**
   * Retrieve the url of policy type
   *
   * @param nearRtRicUrl the near-rt-ric url
   * @param policyTypeId Policy Type Id
   * @return the policy type url
   */
  public String getPolicyTypeUrl(final String nearRtRicUrl, final String policyTypeId) {
    return UriComponentsBuilder.fromUriString(policyTypesUrl(nearRtRicUrl)).pathSegment(policyTypeId)
        .build().toString();
  }

  /**
   * Retrieve the url of putPolicy
   *
   * @param nearRtRicUrl the near-rt-ric url
   * @param policyId Policy Id
   * @param policyTypeId Policy Type Id
   * @return the putPolicy url
   */
  public String putPolicyUrl(final String nearRtRicUrl, final String policyId, final String policyTypeId) {
    return UriComponentsBuilder.fromUriString(policiesUrl(nearRtRicUrl))
            .pathSegment(policyId + "?policyTypeId=" + policyTypeId).build().toString();
  }

  /**
   * Retrieve the url of deletePolicy
   *
   * @param nearRtRicUrl the near-rt-ric url
   * @param policyId Policy Id
   * @return the deletePolicy url
   */
  public String deletePolicyUrl(final String nearRtRicUrl, final String policyId) {
    return UriComponentsBuilder.fromUriString(policiesUrl(nearRtRicUrl)).pathSegment(policyId)
            .build().toString();
  }
}
