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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.onap.dmaap.mr.client.MRBatchingPublisher;
import org.onap.dmaap.mr.client.response.MRPublisherResponse;
import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.dmaap.DmaapRequestMessage.Operation;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.utils.LoggingUtils;
import org.springframework.http.HttpStatus;

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
        Optional<JsonObject> payload =
            ((operation == Operation.PUT || operation == Operation.POST) ? Optional.of(payloadAsJson())
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
    public void testMessageParsing() {
        String message = dmaapInputMessage(Operation.DELETE);
        System.out.println(message);
        DmaapRequestMessage parsedMessage = gson.fromJson(message, ImmutableDmaapRequestMessage.class);
        assertTrue(parsedMessage != null);
        assertFalse(parsedMessage.payload().isPresent());

        message = dmaapInputMessage(Operation.PUT);
        System.out.println(message);
        parsedMessage = gson.fromJson(message, ImmutableDmaapRequestMessage.class);
        assertTrue(parsedMessage != null);
        assertTrue(parsedMessage.payload().isPresent());
    }

    @Test
    public void unparseableMessage_thenWarning() {
        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(DmaapMessageHandler.class);

        testedObject.handleDmaapMsg("bad message");

        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(logAppender.list.toString().contains("handleDmaapMsg failure ")).isTrue();
    }

    @Test
    public void successfulDelete() throws IOException {
        doReturn(Mono.just("OK")).when(agentClient).delete(anyString());
        doReturn(1).when(dmaapClient).send(anyString());
        doReturn(new MRPublisherResponse()).when(dmaapClient).sendBatchWithResponse();

        String message = dmaapInputMessage(Operation.DELETE);

        StepVerifier //
            .create(testedObject.createTask(message)) //
            .expectSubscription() //
            .expectNext("OK") //
            .verifyComplete(); //

        verify(agentClient).delete(URL);
        verifyNoMoreInteractions(agentClient);

        verify(dmaapClient).send(anyString());
        verify(dmaapClient).sendBatchWithResponse();
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

        verify(agentClient).get(URL);
        verifyNoMoreInteractions(agentClient);

        verify(dmaapClient).send(anyString());
        verify(dmaapClient).sendBatchWithResponse();
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

        verify(agentClient).put(URL, payloadAsString());
        verifyNoMoreInteractions(agentClient);

        verify(dmaapClient).send(anyString());
        verify(dmaapClient).sendBatchWithResponse();
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

        verify(agentClient).post(URL, payloadAsString());
        verifyNoMoreInteractions(agentClient);

        verify(dmaapClient).send(anyString());
        verify(dmaapClient).sendBatchWithResponse();
        verifyNoMoreInteractions(dmaapClient);
    }

    @Test
    public void exceptionWhenCallingPolicyAgent_thenNotFoundResponse() throws IOException {
        String errorCause = "Refused";
        doReturn(Mono.error(new Exception(errorCause))).when(agentClient).put(anyString(), any());
        doReturn(1).when(dmaapClient).send(anyString());
        doReturn(new MRPublisherResponse()).when(dmaapClient).sendBatchWithResponse();

        StepVerifier //
            .create(testedObject.createTask(dmaapInputMessage(Operation.PUT))) //
            .expectSubscription() //
            .verifyComplete(); //

        verify(agentClient).put(anyString(), anyString());
        verifyNoMoreInteractions(agentClient);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(dmaapClient).send(captor.capture());
        String actualMessage = captor.getValue();
        assertThat(actualMessage.contains(HttpStatus.NOT_FOUND + "\",\"message\":\"java.lang.Exception: " + errorCause))
            .isTrue();

        verify(dmaapClient).sendBatchWithResponse();
        verifyNoMoreInteractions(dmaapClient);
    }

    @Test
    public void unsupportedOperationInMessage_thenNotFoundResponseWithNotImplementedOperation() throws Exception {
        String message = dmaapInputMessage(Operation.PUT).toString();
        String badOperation = "BAD";
        message = message.replace(Operation.PUT.toString(), badOperation);

        testedObject.handleDmaapMsg(message);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(dmaapClient).send(captor.capture());
        String actualMessage = captor.getValue();
        assertThat(actualMessage
            .contains(HttpStatus.NOT_FOUND + "\",\"message\":\"Not implemented operation: " + badOperation)).isTrue();

        verify(dmaapClient).sendBatchWithResponse();
        verifyNoMoreInteractions(dmaapClient);
    }

    @Test
    public void putWithoutPayload_thenNotFoundResponseWithWarning() throws Exception {
        String message = dmaapInputMessage(Operation.PUT).toString();
        message = message.replace(",\"payload\":{\"name\":\"name\",\"schema\":\"schema\"}", "");

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(DmaapMessageHandler.class);

        testedObject.handleDmaapMsg(message);

        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(logAppender.list.toString().contains("Expected payload in message from DMAAP: ")).isTrue();
    }
}
