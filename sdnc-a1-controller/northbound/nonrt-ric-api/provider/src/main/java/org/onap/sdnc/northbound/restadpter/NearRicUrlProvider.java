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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This class provides Near-RIC api to invoke the A1 interface
 *
 * @author lathishbabu.ganesan@est.tech
 *
 */

public class NearRicUrlProvider {

  // nearRicMap provides mapping from nearRtRicId to domainname:port of nearRTRics
  private HashMap<String, String> nearRicMap = new HashMap<>();
  private static final String NEAR_RIC_LIST_FILE = "NearRtRicList.properties";
  private final Logger log = LoggerFactory.getLogger(NearRicUrlProvider.class);

  public NearRicUrlProvider() {
      try {
        readNearRtRicConfigFile();
      } catch (IOException ex) {
        log.error("Exception while reading nearRtRicConfigFile: {}", ex);
      }
  }

  private void readNearRtRicConfigFile() throws IOException {
      InputStream inputStream = NearRicUrlProvider.class.getClassLoader().getResourceAsStream(NEAR_RIC_LIST_FILE);
      if (inputStream == null) {
          log.error("The file {} not found in classpath", NEAR_RIC_LIST_FILE);
      } else {
          Properties properties = new Properties();
          properties.load(inputStream);
          Enumeration<?> keys = properties.propertyNames();
          while (keys.hasMoreElements()) {
              String key = (String) keys.nextElement();
              nearRicMap.put(key, properties.getProperty(key));
          }
          inputStream.close();
      }
  }

  /**
   * Retrieve the list of Near-RICs
   *
   * @return the list of Near-RICs
   */
  public List<String> getNearRTRicIdsList () {
      return new ArrayList<>(nearRicMap.keySet());
  }

  /**
   * Retrieve the base url of the Near-RIC
   *
   * @return the base url
   */
  public String getBaseUrl(final String nearRtRicId) {
    String baseUrl = "http://" + nearRicMap.get(nearRtRicId) + "/a1-p/";
    return UriComponentsBuilder.fromUriString(baseUrl).build().toString();
  }

  /**
   * Retrieve the url of A1 healthcheck
   *
   * @return the health check url
   */
  public String getHealthCheck(final String nearRtRicId) {
    return UriComponentsBuilder.fromUriString(getBaseUrl(nearRtRicId)).pathSegment("healthcheck").build()
        .toString();
  }

  /**
   * Retrieve the policy type url
   *
   * @return the base url with the policytypes
   */
  public String getPolicyTypes(final String nearRtRicId) {
    return UriComponentsBuilder.fromUriString(getBaseUrl(nearRtRicId)).pathSegment("policytypes/").build()
        .toString();
  }

  /**
   * Retrieve the url of policy type id
   *
   * @param policyTypeId Policy Type Id
   * @return the policy type id url
   */
  public String getPolicyTypeId(final String nearRtRicId, final String policyTypeId) {
    return UriComponentsBuilder.fromUriString(getBaseUrl(nearRtRicId)).pathSegment("policytypes")
        .pathSegment(policyTypeId).build().toString();
  }

  /**
   * Retrieve the url of the policy instances
   *
   * @param policyTypeId Policy Type Id
   * @return the policy instances for the given policy type
   */
  public String getPolicyInstances(final String nearRtRicId, final String policyTypeId) {
    return UriComponentsBuilder.fromUriString(getPolicyTypeId(nearRtRicId, policyTypeId)).pathSegment("policies")
        .build().toString();
  }

  /**
   * Retrieve the url of the policy instance id
   *
   * @param policyTypeId Policy Type Id
   * @param policyInstanceId Policy Instance Id
   * @return the policy instance id for the given policy type
   */
  public String getPolicyInstanceId(final String nearRtRicId, final String policyTypeId, final String policyInstanceId) {
    return UriComponentsBuilder.fromUriString(getPolicyTypeId(nearRtRicId, policyTypeId)).pathSegment("policies")
        .pathSegment(policyInstanceId).build().toString();
  }

  /**
   * Retrieve the url of the policy instance id status
   *
   * @param policyTypeId Policy Type Id
   * @param policyInstanceId Policy Instance Id
   * @return the policy instance id status for the given policy type
   */
  public String getPolicyInstanceIdStatus(final String nearRtRicId, final String policyTypeId,
      final String policyInstanceId) {
    return UriComponentsBuilder.fromUriString(getPolicyInstanceId(nearRtRicId, policyTypeId, policyInstanceId))
        .pathSegment("status").build().toString();
  }
}
