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

import org.oransc.policyagent.clients.A1Client.A1ProtocolType;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.Ric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class A1ClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(A1ClientFactory.class);

    public Mono<A1Client> createA1Client(Ric ric) {
        return getProtocolVersion(ric) //
            .flatMap(version -> createA1Client(ric, version));
    }

    private Mono<A1Client> createA1Client(Ric ric, A1ProtocolType version) {
        if (version == A1ProtocolType.STD_V1) {
            return Mono.just(createA1ClientImpl(ric));
        }
        return Mono.error(new ServiceException("Not supported protocoltype: " + version));
    }

    private Mono<A1Client.A1ProtocolType> getProtocolVersion(Ric ric) {
        if (ric.getProtocolVersion() == A1ProtocolType.UNKNOWN) {
            return fetchVersion(ric, new OscA1Client(ric.getConfig())) //
                .onErrorResume(err -> fetchVersion(ric, createA1ClientImpl(ric)))
                .doOnNext(version -> ric.setProtocolVersion(version))
                .doOnNext(version -> logger.debug("Recover ric: {}, protocol version:{}", ric.name(), version)) //
                .doOnError(t -> logger.warn("Could not get protocol version from tic: {}", ric.name())); //
        } else {
            return Mono.just(ric.getProtocolVersion());
        }
    }

    protected A1Client createA1ClientImpl(Ric ric) {
        return new A1ClientImpl(ric.getConfig());
    }

    private Mono<A1Client.A1ProtocolType> fetchVersion(Ric ric, A1Client a1Client) {
        return Mono.just(a1Client) //
            .flatMap(client -> a1Client.getProtocolVersion());
    }

}
