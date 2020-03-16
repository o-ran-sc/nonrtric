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

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * This class provides the Generic Rest Adapter interface to the RestTemplate
 *
 * @author lathishbabu.ganesan@est.tech
 *
 */

public class RestAdapterImpl implements RestAdapter {

  private RestTemplate restTemplate;

  public RestAdapterImpl() {
    restTemplate = new RestTemplate();
  }

  private HttpEntity<?> getHttpEntity(final Object object) {
    return new HttpEntity<>(object);
  }

  @Override
  public <T> ResponseEntity<T> get(String uri, Class<?> clazz) {
    HttpEntity<?> entity = getHttpEntity(null);
    return invokeHttpRequest(uri, HttpMethod.GET, clazz, entity);
  }

  @Override
  public <T> ResponseEntity<T> put(String uri, String body, Class<T> clazz) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(body, headers);
    return invokeHttpRequest(uri, HttpMethod.PUT, clazz, entity);
  }

  @Override
  public <T> ResponseEntity<T> delete(String uri) {
    HttpEntity<?> entity = getHttpEntity(null);
    return invokeHttpRequest(uri, HttpMethod.DELETE, null, entity);
  }

  @SuppressWarnings("unchecked")
  private <T> ResponseEntity<T> invokeHttpRequest(String uri, HttpMethod httpMethod, Class<?> clazz,
      HttpEntity<?> entity) {
    return (ResponseEntity<T>) restTemplate.exchange(uri, httpMethod, entity, clazz);
  }
}
