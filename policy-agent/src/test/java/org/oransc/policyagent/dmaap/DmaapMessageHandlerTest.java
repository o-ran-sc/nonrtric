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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.dmaap.mr.client.MRBatchingPublisher;
import org.onap.dmaap.mr.client.response.MRPublisherResponse;
import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.dmaap.DmaapRequestMessage.Operation;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.PolicyType;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class DmaapMessageHandlerTest {

    private static final String URL = "url";

    private final MRBatchingPublisher dmaapClient = mock(MRBatchingPublisher.class);
    private final AsyncRestClient agentClient = mock(AsyncRestClient.class);
    private DmaapMessageHandler testedObject;
    private static Gson gson = new GsonBuilder() //
            .create(); //

    @BeforeEach
    private void setUp() throws Exception {
        testedObject = spy(new DmaapMessageHandler(dmaapClient, agentClient));
    }

    static JsonObject payloadAsJson() {
        return gson.fromJson(payloadAsString(), JsonObject.class);
    }

    static String payloadAsString() {
        PolicyType pt = ImmutablePolicyType.builder().name("name").schema("schema").build();
        return gson.toJson(pt);
    }

    DmaapRequestMessage dmaapRequestMessage(Operation operation) {
        Optional<JsonObject> payload = ((operation == Operation.PUT || operation == Operation.POST)
                ? Optional.of(payloadAsJson())
                : Optional.empty());
        return ImmutableDmaapRequestMessage.builder().apiVersion("apiVersion") //
                .correlationId("correlationId") //
                .operation(operation) //
                .originatorId("originatorId") //
                .payload(payload) //
                .requestId("requestId") //
                .target("target") //
                .timestamp("timestamp") //
                .type("type") //
                .url(URL) //
                .build();
    }

    private String dmaapInputMessage(Operation operation) {
        return gson.toJson(dmaapRequestMessage(operation));
    }

    @Test
    public void successfulDelete() throws IOException {
        doReturn(Mono.just("OK")).when(agentClient).delete(anyString());
        doReturn(1).when(dmaapClient).send(anyString());
        doReturn(new MRPublisherResponse()).when(dmaapClient).sendBatchWithResponse();

        String message = dmaapInputMessage(Operation.DELETE);
        System.out.println(message);
        DmaapRequestMessage parsedMessage = gson.fromJson(message, ImmutableDmaapRequestMessage.class);
        assertTrue(parsedMessage != null);

        StepVerifier //
                .create(testedObject.createTask(message)) //
                .expectSubscription() //
                .expectNext("OK") //
                .verifyComplete(); //

        verify(agentClient, times(1)).delete(URL);
        verifyNoMoreInteractions(agentClient);

        verify(dmaapClient, times(1)).send(anyString());
        verify(dmaapClient, times(1)).sendBatchWithResponse();
        verifyNoMoreInteractions(dmaapClient);
    }

    @Test
    public void successfulGet() throws IOException {
        doReturn(Mono.just("OK")).when(agentClient).get(anyString());
        doReturn(1).when(dmaapClient).send(anyString());
        doReturn(new MRPublisherResponse()).when(dmaapClient).sendBatchWithResponse();

        StepVerifier //
                .create(testedObject.createTask(dmaapInputMessage(Operation.GET))) //
                .expectSubscription() //
                .expectNext("OK") //
                .verifyComplete(); //

        verify(agentClient, times(1)).get(URL);
        verifyNoMoreInteractions(agentClient);

        verify(dmaapClient, times(1)).send(anyString());
        verify(dmaapClient, times(1)).sendBatchWithResponse();
        verifyNoMoreInteractions(dmaapClient);
    }

    @Test
    public void successfulPut() throws IOException {
        doReturn(Mono.just("OK")).when(agentClient).put(anyString(), anyString());
        doReturn(1).when(dmaapClient).send(anyString());
        doReturn(new MRPublisherResponse()).when(dmaapClient).sendBatchWithResponse();

        StepVerifier //
                .create(testedObject.createTask(dmaapInputMessage(Operation.PUT))) //
                .expectSubscription() //
                .expectNext("OK") //
                .verifyComplete(); //

        verify(agentClient, times(1)).put(URL, payloadAsString());
        verifyNoMoreInteractions(agentClient);

        verify(dmaapClient, times(1)).send(anyString());
        verify(dmaapClient, times(1)).sendBatchWithResponse();
        verifyNoMoreInteractions(dmaapClient);
    }

    @Test
    public void successfulPost() throws IOException {
        doReturn(Mono.just("OK")).when(agentClient).post(anyString(), anyString());
        doReturn(1).when(dmaapClient).send(anyString());
        doReturn(new MRPublisherResponse()).when(dmaapClient).sendBatchWithResponse();

        StepVerifier //
                .create(testedObject.createTask(dmaapInputMessage(Operation.POST))) //
                .expectSubscription() //
                .expectNext("OK") //
                .verifyComplete(); //

        verify(agentClient, times(1)).post(URL, payloadAsString());
        verifyNoMoreInteractions(agentClient);

        verify(dmaapClient, times(1)).send(anyString());
        verify(dmaapClient, times(1)).sendBatchWithResponse();
        verifyNoMoreInteractions(dmaapClient);
    }

    @Test
    public void errorCase() throws IOException {
        doReturn(Mono.error(new Exception("Refused"))).when(agentClient).put(anyString(), any());
        doReturn(1).when(dmaapClient).send(anyString());
        doReturn(new MRPublisherResponse()).when(dmaapClient).sendBatchWithResponse();
        StepVerifier //
                .create(testedObject.createTask(dmaapInputMessage(Operation.PUT))) //
                .expectSubscription() //
                .verifyComplete(); //

        verify(agentClient, times(1)).put(anyString(), anyString());
        verifyNoMoreInteractions(agentClient);

        // Error response
        verify(dmaapClient, times(1)).send(anyString());
        verify(dmaapClient, times(1)).sendBatchWithResponse();
        verifyNoMoreInteractions(dmaapClient);
    }

}
