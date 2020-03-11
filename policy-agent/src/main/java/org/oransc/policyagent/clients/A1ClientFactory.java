/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2020 Nordix Foundation
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
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.repository.Ric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

/**
 * Factory for A1 clients that supports four different protocol versions of the
 * A1 api.
 */
public class A1ClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(A1ClientFactory.class);

    private final ApplicationConfig appConfig;

    @Autowired
    public A1ClientFactory(ApplicationConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * Creates an A1 client with the correct A1 protocol for the provided Ric.
     *
     * <p>
     * It detects the protocol version by trial and error, since there is no
     * getVersion method specified in the A1 api yet.
     *
     * <p>
     * As a side effect it also sets the protocol version in the provided Ric. This
     * means that after the first successful creation it won't have to try which
     * protocol to use, but can create the client directly.
     *
     * @param ric The RIC to get a client for.
     * @return a client with the correct protocol, or a ServiceException if none of
     *         the protocols are supported by the Ric.
     */
    public Mono<A1Client> createA1Client(Ric ric) {
        return getProtocolVersion(ric) //
            .flatMap(version -> createA1Client(ric, version));
    }

    private Mono<A1Client> createA1Client(Ric ric, A1ProtocolType version) {
        if (version == A1ProtocolType.STD_V1) {
            return Mono.just(createStdA1ClientImpl(ric));
        } else if (version == A1ProtocolType.OSC_V1) {
            return Mono.just(createOscA1Client(ric));
        } else if (version == A1ProtocolType.SDNC_OSC) {
            return Mono.just(createSdncOscA1Client(ric));
        } else { // A1ProtocolType.SDNC_ONAP
            return Mono.just(createSdncOnapA1Client(ric));
        }
    }

    private Mono<A1Client.A1ProtocolType> getProtocolVersion(Ric ric) {
        if (ric.getProtocolVersion() == A1ProtocolType.UNKNOWN) {
            return fetchVersion(createSdncOnapA1Client(ric)) //
                .onErrorResume(notUsed -> fetchVersion(createSdncOscA1Client(ric))) //
                .onErrorResume(notUsed -> fetchVersion(createOscA1Client(ric))) //
                .onErrorResume(notUsed -> fetchVersion(createStdA1ClientImpl(ric))) //
                .doOnNext(ric::setProtocolVersion)
                .doOnNext(version -> logger.debug("Recover ric: {}, protocol version:{}", ric.name(), version)) //
                .doOnError(notUsed -> logger.warn("Could not get protocol version from RIC: {}", ric.name())); //
        } else {
            return Mono.just(ric.getProtocolVersion());
        }
    }

    protected A1Client createOscA1Client(Ric ric) {
        return new OscA1Client(ric.getConfig());
    }

    protected A1Client createStdA1ClientImpl(Ric ric) {
        return new StdA1Client(ric.getConfig());
    }

    protected A1Client createSdncOscA1Client(Ric ric) {
        return new SdncOscA1Client(ric.getConfig(), appConfig.getA1ControllerBaseUrl(),
            appConfig.getA1ControllerUsername(), appConfig.getA1ControllerPassword());
    }

    protected A1Client createSdncOnapA1Client(Ric ric) {
        return new SdncOnapA1Client(ric.getConfig(), appConfig.getA1ControllerBaseUrl(),
            appConfig.getA1ControllerUsername(), appConfig.getA1ControllerPassword());
    }

    private Mono<A1ProtocolType> fetchVersion(A1Client a1Client) {
        return Mono.just(a1Client) //
            .flatMap(client -> a1Client.getProtocolVersion());
    }
}
