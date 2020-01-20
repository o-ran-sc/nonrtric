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

import java.lang.invoke.MethodHandles;
import java.util.Collection;

import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class OscA1Client implements A1Client {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final RicConfig ricConfig;

    public OscA1Client(RicConfig ricConfig) {
        this.ricConfig = ricConfig;
        logger.debug("OscA1Client for ric: {}", this.ricConfig.name());
    }

    @Override
    public Mono<Collection<String>> getPolicyTypeIdentities() {
        return Mono.error(new Exception("Not impl"));
    }

    @Override
    public Mono<Collection<String>> getPolicyIdentities() {
        return Mono.error(new Exception("Not impl"));
    }

    @Override
    public Mono<String> getPolicyTypeSchema(String policyTypeId) {
        return Mono.error(new Exception("Not impl"));
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        return Mono.error(new Exception("Not impl"));
    }

    @Override
    public Mono<String> deletePolicy(String policyId) {
        return Mono.error(new Exception("Not impl"));
    }

    @Override
    public Mono<A1ProtocolType> getProtocolVersion() {
        return Mono.error(new Exception("Not impl"));
    }

}
