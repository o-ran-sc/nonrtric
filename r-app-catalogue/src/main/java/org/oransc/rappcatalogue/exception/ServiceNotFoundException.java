/*-
 * ========================LICENSE_START=================================
 * Copyright (C) 2020 Nordix Foundation. All rights reserved.
 * ======================================================================
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

package org.oransc.rappcatalogue.exception;

public class ServiceNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 6579271315716003988L;

    public ServiceNotFoundException(String serviceName) {
        super(String.format("Service %s not found", serviceName));
    }
}
