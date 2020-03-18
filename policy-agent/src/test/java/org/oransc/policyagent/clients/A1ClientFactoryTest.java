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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private static final String RIC_NAME = "Name";
    private static final String EXCEPTION_MESSAGE = "Error";

    @Mock
    private ApplicationConfig applicationConfigMock;

    @Mock
    A1Client clientMock1;

    @Mock
    A1Client clientMock2;

    @Mock
    A1Client clientMock3;

    @Mock
    A1Client clientMock4;

    private ImmutableRicConfig ricConfig =
        ImmutableRicConfig.builder().name(RIC_NAME).baseUrl("baseUrl").managedElementIds(new Vector<>()).build();
    private Ric ric = new Ric(ricConfig);

    private A1ClientFactory factoryUnderTest;

    @BeforeEach
    public void createFactoryUnderTest() {
        factoryUnderTest = spy(new A1ClientFactory(applicationConfigMock));
    }

    @Test
    public void getProtocolVersion_ok() {
        whenGetProtocolVersionThrowException(clientMock1);
        whenGetProtocolVersionReturn(clientMock2, A1ProtocolType.STD_V1_1);
        doReturn(clientMock1, clientMock2).when(factoryUnderTest).createClient(any(), any());

        A1Client client = factoryUnderTest.createA1Client(ric).block();

        assertEquals(clientMock2, client, "Not correct client returned");
        assertEquals(A1ProtocolType.STD_V1_1, ric.getProtocolVersion(), "Not correct protocol");
    }

    @Test
    public void getProtocolVersion_ok_Last() {
        whenGetProtocolVersionThrowException(clientMock1, clientMock2, clientMock3);
        whenGetProtocolVersionReturn(clientMock4, A1ProtocolType.STD_V1_1);
        doReturn(clientMock1, clientMock2, clientMock3, clientMock4).when(factoryUnderTest).createClient(any(), any());

        A1Client client = factoryUnderTest.createA1Client(ric).block();

        assertEquals(clientMock4, client, "Not correct client returned");
        assertEquals(A1ProtocolType.STD_V1_1, ric.getProtocolVersion(), "Not correct protocol");
    }

    @Test
    public void getProtocolVersion_error() {
        whenGetProtocolVersionThrowException(clientMock1, clientMock2, clientMock3, clientMock4);
        doReturn(clientMock1, clientMock2, clientMock3, clientMock4).when(factoryUnderTest).createClient(any(), any());

        StepVerifier.create(factoryUnderTest.createA1Client(ric)) //
            .expectSubscription() //
            .expectErrorMatches(throwable -> throwable.getMessage().equals(EXCEPTION_MESSAGE)) //
            .verify();

        assertEquals(A1ProtocolType.UNKNOWN, ric.getProtocolVersion(), "Not correct protocol");
    }

    private A1Client createClient(A1ProtocolType version) {
        return factoryUnderTest.createClient(ric, version);
    }

    @Test
    public void create_check_types() {
        assertTrue(createClient(A1ProtocolType.STD_V1_1) instanceof StdA1ClientVersion1);
        assertTrue(createClient(A1ProtocolType.OSC_V1) instanceof OscA1Client);
        assertTrue(createClient(A1ProtocolType.SDNC_ONAP) instanceof SdncOnapA1Client);
        assertTrue(createClient(A1ProtocolType.SDNC_OSC) instanceof SdncOscA1Client);
        assertTrue(createClient(A1ProtocolType.UNKNOWN) == null);
    }

    private void whenGetProtocolVersionThrowException(A1Client... clientMocks) {
        for (A1Client clientMock : clientMocks) {
            when(clientMock.getProtocolVersion()).thenReturn(Mono.error(new Exception(EXCEPTION_MESSAGE)));
        }
    }

    private void whenGetProtocolVersionReturn(A1Client clientMock, A1ProtocolType protocol) {
        when(clientMock.getProtocolVersion()).thenReturn(Mono.just(protocol));
    }

}
