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

package org.oransc.policyagent.dmaap;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.dmaap.mr.client.MRBatchingPublisher;
import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.dmaap.DmaapRequestMessage.Operation;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class DmaapMessageHandlerTest {

    private ApplicationConfig appConfig = mock(ApplicationConfig.class);
    private final MRBatchingPublisher dmaapClient = mock(MRBatchingPublisher.class);
    private final AsyncRestClient agentClient = mock(AsyncRestClient.class);
    private DmaapMessageHandler testedObject;
    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    @BeforeEach
    private void setUp() throws Exception {
        testedObject = spy(new DmaapMessageHandler(dmaapClient, appConfig, agentClient));
    }

    ImmutableDmaapRequestMessage dmaapRequestMessage(Operation operation) {
        return ImmutableDmaapRequestMessage.builder().apiVersion("apiVersion") //
            .correlationId("correlationId") //
            .operation(operation) //
            .originatorId("originatorId") //
            .payload("payload") //
            .requestId("requestId") //
            .target("target") //
            .timestamp("timestamp") //
            .type("type") //
            .url("url") //
            .build();
    }

    private String dmaapInputMessage(Operation operation) {
        return gson.toJson(dmaapRequestMessage(operation));
    }

    @Test
    public void successfulCase() throws IOException {
        doReturn(Mono.just("OK")).when(agentClient).delete("url");
        doReturn(1).when(dmaapClient).send(anyString());

        StepVerifier //
            .create(testedObject.createTask(dmaapInputMessage(Operation.DELETE))) //
            .expectSubscription() //
            .expectNext("OK") //
            .verifyComplete(); //

        verify(agentClient, times(1)).delete("url");
        verifyNoMoreInteractions(agentClient);

        verify(dmaapClient, times(1)).send(anyString());
        verify(dmaapClient).sendBatchWithResponse();
        verifyNoMoreInteractions(dmaapClient);
    }

    @Test
    public void errorCase() throws IOException {
        doReturn(Mono.error(new Exception("Refused"))).when(agentClient).put("url", "payload");
        doReturn(1).when(dmaapClient).send(anyString());

        StepVerifier //
            .create(testedObject.createTask(dmaapInputMessage(Operation.PUT))) //
            .expectSubscription() //
            .verifyComplete(); //

        verify(agentClient, times(1)).put("url", "payload");
        verifyNoMoreInteractions(agentClient);

        // Error response
        verify(dmaapClient, times(1)).send(anyString());
        verify(dmaapClient).sendBatchWithResponse();
        verifyNoMoreInteractions(dmaapClient);
    }

}
