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

package org.oransc.policyagent.clients;

import java.util.List;

import org.oransc.policyagent.repository.Policy;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Common interface for 'A1' Policy access. Implementations of this interface
 * adapts to the different southbound REST APIs supported.
 */
public interface A1Client {

    public enum A1ProtocolType {
        UNKNOWN, //
        STD_V1_1, // STD A1 version 1.1
        OSC_V1, // OSC 'A1'
        SDNC_OSC_STD_V1_1, // SDNC_OSC with STD A1 version 1.1 southbound
        SDNC_OSC_OSC_V1, // SDNC_OSC with OSC 'A1' southbound
        SDNC_ONAP
    }

    public Mono<A1ProtocolType> getProtocolVersion();

    public Mono<List<String>> getPolicyTypeIdentities();

    public Mono<List<String>> getPolicyIdentities();

    public Mono<String> getPolicyTypeSchema(String policyTypeId);

    public Mono<String> putPolicy(Policy policy);

    public Mono<String> deletePolicy(Policy policy);

    public Flux<String> deleteAllPolicies();

    public Mono<String> getPolicyStatus(Policy policy);
}
