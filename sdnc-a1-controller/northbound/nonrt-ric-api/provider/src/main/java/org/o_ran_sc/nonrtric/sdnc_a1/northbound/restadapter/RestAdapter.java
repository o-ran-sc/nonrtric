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

package org.o_ran_sc.nonrtric.sdnc_a1.northbound.restadapter;

import org.springframework.http.ResponseEntity;

/**
 * An interface to wrap the generic HTTP methods
 *
 * @author lathishbabu.ganesan@est.tech
 *
 */
public interface RestAdapter {

  /**
   * Retrieve a representation by doing a GET on the specified URL. The response (if any) is
   * converted and returned.
   *
   * @param url the URL
   * @param clazz responseType the type of the return value
   * @return the converted object
   */

  <T> ResponseEntity<T> get(final String url, final Class<?> clazz);

  /**
   * Create or update a resource by PUTting the given object to the URI.
   *
   * @param url the URL
   * @param body the String to be PUT (may be {@code null})
   * @param clazz responseType the type of the return value
   * @return the response code
   */
  <T> ResponseEntity<T> put(final String url, final String body, final Class<T> clazz);

  /**
   * Delete resource for the given object to the URI.
   *
   * @param url the URL
   * @return the response code
   */
  <T> ResponseEntity<T> delete(final String url);

}
