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

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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

  private final Logger log = LoggerFactory.getLogger(RestAdapterImpl.class);

  private RestTemplate restTemplate;

  public RestAdapterImpl() {
    restTemplate = new RestTemplate();
  }

  private HttpEntity<?> getHttpEntity(final Object object) {
    return new HttpEntity<>(object);
  }

  @Override
  public <T> Optional<T> get(String uri, Class<?> clazz) {
    HttpEntity<?> entity = getHttpEntity(null);
    final ResponseEntity<T> response = invokeHttpRequest(uri, HttpMethod.GET, clazz, entity);
    return buildOptional(response);
  }

  @Override
  public <T> Optional<T> put(String uri, String body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<String>(body, headers);
    final ResponseEntity<T> response = invokeHttpRequest(uri, HttpMethod.PUT, null, entity);
    return buildOptional(response);
  }

  @Override
  public <T> Optional<T> delete(String uri) {
    HttpEntity<?> entity = getHttpEntity(null);
    final ResponseEntity<T> response = invokeHttpRequest(uri, HttpMethod.DELETE, null, entity);
    return buildOptional(response);
  }

  @SuppressWarnings("unchecked")
  private <T> ResponseEntity<T> invokeHttpRequest(String uri, HttpMethod httpMethod, Class<?> clazz,
      HttpEntity<?> entity) {
    return (ResponseEntity<T>) restTemplate.exchange(uri, httpMethod, entity, clazz);
  }

  private <T> Optional<T> buildOptional(ResponseEntity<T> response) {
    if (!response.getStatusCode().equals(HttpStatus.OK)
        & !response.getStatusCode().equals(HttpStatus.CREATED)
        & !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
      log.error("Failed to get the Response, Status Code = {}", response.getStatusCode());
      return Optional.absent();
    }
    if (response.hasBody()) {
      return Optional.of(response.getBody());
    }
    return Optional.absent();
  }
}
