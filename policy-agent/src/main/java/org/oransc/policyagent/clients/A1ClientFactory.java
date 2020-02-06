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
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.Ric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

public class A1ClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(A1ClientFactory.class);

    private final ApplicationConfig appConfig;

    @Autowired
    public A1ClientFactory(ApplicationConfig appConfig) {
        this.appConfig = appConfig;
    }

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
        } else if (version == A1ProtocolType.SDNR_ONAP) {
            return Mono.just(createSdnrOnapA1Client(ric));
        } else {
            return Mono.error(new ServiceException("Not supported protocol type: " + version));
        }
    }

    private Mono<A1Client.A1ProtocolType> getProtocolVersion(Ric ric) {
        if (ric.getProtocolVersion() == A1ProtocolType.UNKNOWN) {
            return fetchVersion(ric, createSdnrOnapA1Client(ric)) //
                .onErrorResume(err -> fetchVersion(ric, createSdncOscA1Client(ric)))
                .onErrorResume(err -> fetchVersion(ric, createOscA1Client(ric)))
                .onErrorResume(err -> fetchVersion(ric, createStdA1ClientImpl(ric)))
                .onErrorResume(err -> handleNoProtocol(ric))
                .doOnNext(version -> setProtoclVersion(ric, version))
                .doOnNext(version -> logger.debug("Recover ric: {}, protocol version: {}", ric.name(), version));
        } else {
            return Mono.just(ric.getProtocolVersion());
        }
    }

    private Mono<A1ProtocolType> handleNoProtocol(Ric ric) {
        logger.warn("Could not get protocol version from RIC: {}", ric.name());
        return Mono.just(A1ProtocolType.UNKNOWN);
    }

    private void setProtoclVersion(Ric ric, A1ProtocolType version) {
        ric.setProtocolVersion(version);
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

    protected A1Client createSdnrOnapA1Client(Ric ric) {
        return new SdnrOnapA1Client(ric.getConfig(), appConfig.getA1ControllerBaseUrl(),
            appConfig.getA1ControllerUsername(), appConfig.getA1ControllerPassword());
    }

    private Mono<A1ProtocolType> fetchVersion(Ric ric, A1Client a1Client) {
        return Mono.just(a1Client) //
            .flatMap(client -> a1Client.getProtocolVersion());
    }

}
