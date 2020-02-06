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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oransc.policyagent.clients.A1Client.A1ProtocolType;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.configuration.ImmutableRicConfig;
import org.oransc.policyagent.repository.Ric;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class A1ClientFactoryTest {
    @Mock
    private ApplicationConfig applicationConfigMock;

    @Mock
    A1Client stdA1ClientMock;

    @Mock
    A1Client oscA1ClientMock;

    @Mock
    A1Client sdncOscA1ClientMock;

    @Mock
    A1Client sdnrOnapA1ClientMock;

    private ImmutableRicConfig ricConfig =
        ImmutableRicConfig.builder().name("Name").baseUrl("baseUrl").managedElementIds(new Vector<>()).build();
    private Ric ric = new Ric(ricConfig);

    private A1ClientFactory factoryUnderTest;

    @BeforeEach
    public void createFactoryUnderTest() {
        factoryUnderTest = spy(new A1ClientFactory(applicationConfigMock));
    }

    @Test
    public void createStd_ok() {
        whenGetProtocolVersionSdnrOnapA1ClientThrowException();
        whenGetProtocolVersionSdncOscA1ClientThrowException();
        whenGetProtocolVersionOscA1ClientThrowException();
        whenGetProtocolVersionStdA1ClientReturnCorrectProtocol();

        StepVerifier.create(factoryUnderTest.createA1Client(ric)) //
            .expectSubscription() //
            .expectNext(stdA1ClientMock) //
            .verifyComplete();

        assertEquals(A1ProtocolType.STD_V1, ric.getProtocolVersion(), "Not correct protocol");
    }

    @Test
    public void createOsc_ok() {
        whenGetProtocolVersionSdnrOnapA1ClientThrowException();
        whenGetProtocolVersionSdncOscA1ClientThrowException();
        whenGetProtocolVersionOscA1ClientReturnCorrectProtocol();

        StepVerifier.create(factoryUnderTest.createA1Client(ric)) //
            .expectSubscription() //
            .expectNext(oscA1ClientMock) //
            .verifyComplete();

        assertEquals(A1ProtocolType.OSC_V1, ric.getProtocolVersion(), "Not correct protocol");
    }

    @Test
    public void createSdncOsc_ok() {
        whenGetProtocolVersionSdnrOnapA1ClientThrowException();
        whenGetProtocolVersionSdncOscA1ClientReturnCorrectProtocol();

        StepVerifier.create(factoryUnderTest.createA1Client(ric)) //
            .expectSubscription() //
            .expectNext(sdncOscA1ClientMock) //
            .verifyComplete();

        assertEquals(A1ProtocolType.SDNC_OSC, ric.getProtocolVersion(), "Not correct protocol");
    }

    @Test
    public void createSdnrOnap_ok() {
        whenGetProtocolVersionSdnrOnapA1ClientReturnCorrectProtocol();

        StepVerifier.create(factoryUnderTest.createA1Client(ric)) //
            .expectSubscription() //
            .expectNext(sdnrOnapA1ClientMock) //
            .verifyComplete();

        assertEquals(A1ProtocolType.SDNR_ONAP, ric.getProtocolVersion(), "Not correct protocol");
    }

    @Test
    public void createWithNoProtocol_error() {
        whenGetProtocolVersionSdnrOnapA1ClientThrowException();
        whenGetProtocolVersionSdncOscA1ClientThrowException();
        whenGetProtocolVersionOscA1ClientThrowException();
        whenGetProtocolVersionStdA1ClientThrowException();

        StepVerifier.create(factoryUnderTest.createA1Client(ric)) //
            .expectSubscription() //
            .expectErrorMatches(throwable -> throwable instanceof Exception)
            .verify();

        assertEquals(A1ProtocolType.UNKNOWN, ric.getProtocolVersion(), "Not correct protocol");
    }

    @Test
    public void createWithProtocolInRic_noTrialAndError() {
        doReturn(stdA1ClientMock).when(factoryUnderTest).createStdA1ClientImpl(any(Ric.class));

        ric.setProtocolVersion(A1ProtocolType.STD_V1);

        StepVerifier.create(factoryUnderTest.createA1Client(ric)) //
            .expectSubscription() //
            .expectNext(stdA1ClientMock) //
            .verifyComplete();

        assertEquals(A1ProtocolType.STD_V1, ric.getProtocolVersion(), "Not correct protocol");
    }

    private void whenGetProtocolVersionSdnrOnapA1ClientThrowException() {
        doReturn(sdnrOnapA1ClientMock).when(factoryUnderTest).createSdnrOnapA1Client(ric);
        when(sdnrOnapA1ClientMock.getProtocolVersion()).thenReturn(Mono.error(new Exception("Error")));
    }

    private void whenGetProtocolVersionSdnrOnapA1ClientReturnCorrectProtocol() {
        doReturn(sdnrOnapA1ClientMock).when(factoryUnderTest).createSdnrOnapA1Client(any(Ric.class));
        when(sdnrOnapA1ClientMock.getProtocolVersion()).thenReturn(Mono.just(A1ProtocolType.SDNR_ONAP));
    }

    private void whenGetProtocolVersionSdncOscA1ClientThrowException() {
        doReturn(sdncOscA1ClientMock).when(factoryUnderTest).createSdncOscA1Client(any(Ric.class));
        when(sdncOscA1ClientMock.getProtocolVersion()).thenReturn(Mono.error(new Exception("Error")));
    }

    private void whenGetProtocolVersionSdncOscA1ClientReturnCorrectProtocol() {
        doReturn(sdncOscA1ClientMock).when(factoryUnderTest).createSdncOscA1Client(any(Ric.class));
        when(sdncOscA1ClientMock.getProtocolVersion()).thenReturn(Mono.just(A1ProtocolType.SDNC_OSC));
    }

    private void whenGetProtocolVersionOscA1ClientThrowException() {
        doReturn(oscA1ClientMock).when(factoryUnderTest).createOscA1Client(any(Ric.class));
        when(oscA1ClientMock.getProtocolVersion()).thenReturn(Mono.error(new Exception("Error")));
    }

    private void whenGetProtocolVersionOscA1ClientReturnCorrectProtocol() {
        doReturn(oscA1ClientMock).when(factoryUnderTest).createOscA1Client(any(Ric.class));
        when(oscA1ClientMock.getProtocolVersion()).thenReturn(Mono.just(A1ProtocolType.OSC_V1));
    }

    private void whenGetProtocolVersionStdA1ClientThrowException() {
        doReturn(stdA1ClientMock).when(factoryUnderTest).createStdA1ClientImpl(any(Ric.class));
        when(stdA1ClientMock.getProtocolVersion()).thenReturn(Mono.error(new Exception("Error")));
    }

    private void whenGetProtocolVersionStdA1ClientReturnCorrectProtocol() {
        doReturn(stdA1ClientMock).when(factoryUnderTest).createStdA1ClientImpl(any(Ric.class));
        when(stdA1ClientMock.getProtocolVersion()).thenReturn(Mono.just(A1ProtocolType.STD_V1));
    }
}
