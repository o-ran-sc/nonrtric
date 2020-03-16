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

import java.util.Arrays;
import java.util.List;

import org.oransc.policyagent.configuration.RicConfig;
import reactor.core.publisher.Mono;

public class StdA1ClientVersion2 extends StdA1ClientVersion1 {

    public StdA1ClientVersion2(RicConfig ricConfig) {
        super(ricConfig);
    }

    public StdA1ClientVersion2(AsyncRestClient restClient) {
        super(restClient);
    }

    @Override
    public Mono<List<String>> getPolicyTypeIdentities() {
        return Mono.just(Arrays.asList(""));
    }

    @Override
    public Mono<String> getPolicyTypeSchema(String policyTypeId) {
        return Mono.just("{}");
    }

    @Override
    public Mono<A1ProtocolType> getProtocolVersion() {
        return getPolicyIdentities() //
            .flatMap(x -> Mono.just(A1ProtocolType.STD_V1_1));
    }
}
