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

import static ch.qos.logback.classic.Level.WARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.dmaap.DmaapRequestMessage.Operation;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.utils.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class DmaapMessageHandlerTest {
    private static final Logger logger = LoggerFactory.getLogger(DmaapMessageHandlerTest.class);
    private static final String URL = "url";

    private final AsyncRestClient dmaapClient = mock(AsyncRestClient.class);
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
        return ImmutableDmaapRequestMessage.builder() //
            .apiVersion("apiVersion") //
            .correlationId("correlationId") //
            .operation(operation) //
            .originatorId("originatorId") //
            .payload(payload) //
            .requestId("requestId") //
            .target("target") //
            .timestamp("timestamp") //
            .url(URL) //
            .build();
    }

    private String dmaapInputMessage(Operation operation) {
        return gson.toJson(dmaapRequestMessage(operation));
    }

    private Mono<ResponseEntity<String>> okResponse() {
        ResponseEntity<String> entity = new ResponseEntity<>("OK", HttpStatus.OK);
        return Mono.just(entity);
    }

    private Mono<ResponseEntity<String>> notOkResponse() {
        ResponseEntity<String> entity = new ResponseEntity<>("NOK", HttpStatus.BAD_GATEWAY);
        return Mono.just(entity);
    }

    @Test
    void testMessageParsing() {
        String message = dmaapInputMessage(Operation.DELETE);
        logger.info(message);
        DmaapRequestMessage parsedMessage = gson.fromJson(message, ImmutableDmaapRequestMessage.class);
        assertNotNull(parsedMessage);
        assertFalse(parsedMessage.payload().isPresent());

        message = dmaapInputMessage(Operation.PUT);
        logger.info(message);
        parsedMessage = gson.fromJson(message, ImmutableDmaapRequestMessage.class);
        assertNotNull(parsedMessage);
        assertTrue(parsedMessage.payload().isPresent());
    }

    @Test
    void unparseableMessage_thenWarning() {
        final ListAppender<ILoggingEvent> logAppender =
            LoggingUtils.getLogListAppender(DmaapMessageHandler.class, WARN);

        String msg = "bad message";
        testedObject.handleDmaapMsg(msg);

        assertThat(logAppender.list.get(0).getFormattedMessage()).startsWith(
            "handleDmaapMsg failure org.oransc.policyagent.exceptions.ServiceException: Received unparsable "
                + "message from DMAAP: \"" + msg + "\", reason: ");
    }

    @Test
    void successfulDelete() throws IOException {
        doReturn(okResponse()).when(agentClient).deleteForEntity(anyString());
        doReturn(Mono.just("OK")).when(dmaapClient).put(anyString(), anyString());

        String message = dmaapInputMessage(Operation.DELETE);

        StepVerifier //
            .create(testedObject.createTask(message)) //
            .expectSubscription() //
            .expectNext("OK") //
            .verifyComplete(); //

        verify(agentClient).deleteForEntity(URL);
        verifyNoMoreInteractions(agentClient);

        verify(dmaapClient).put(anyString(), anyString());

        verifyNoMoreInteractions(dmaapClient);
    }

    @Test
    void successfulGet() throws IOException {
        doReturn(okResponse()).when(agentClient).getForEntity(anyString());
        doReturn(Mono.just("OK")).when(dmaapClient).put(anyString(), anyString());

        StepVerifier //
            .create(testedObject.createTask(dmaapInputMessage(Operation.GET))) //
            .expectSubscription() //
            .expectNext("OK") //
            .verifyComplete(); //

        verify(agentClient).getForEntity(URL);
        verifyNoMoreInteractions(agentClient);

        verify(dmaapClient).put(anyString(), anyString());
        verifyNoMoreInteractions(dmaapClient);
    }

    @Test
    void successfulPut() throws IOException {
        doReturn(okResponse()).when(agentClient).putForEntity(anyString(), anyString());
        doReturn(Mono.just("OK")).when(dmaapClient).put(anyString(), anyString());

        StepVerifier //
            .create(testedObject.createTask(dmaapInputMessage(Operation.PUT))) //
            .expectSubscription() //
            .expectNext("OK") //
            .verifyComplete(); //

        verify(agentClient).putForEntity(URL, payloadAsString());
        verifyNoMoreInteractions(agentClient);

        verify(dmaapClient).put(anyString(), anyString());
        verifyNoMoreInteractions(dmaapClient);
    }

    @Test
    void successfulPost() throws IOException {
        doReturn(okResponse()).when(agentClient).postForEntity(anyString(), anyString());
        doReturn(Mono.just("OK")).when(dmaapClient).put(anyString(), anyString());

        StepVerifier //
            .create(testedObject.createTask(dmaapInputMessage(Operation.POST))) //
            .expectSubscription() //
            .expectNext("OK") //
            .verifyComplete(); //

        verify(agentClient).postForEntity(URL, payloadAsString());
        verifyNoMoreInteractions(agentClient);

        verify(dmaapClient).put(anyString(), anyString());
        verifyNoMoreInteractions(dmaapClient);
    }

    @Test
    void exceptionWhenCallingPolicyAgent_thenNotFoundResponse() throws IOException {

        doReturn(notOkResponse()).when(agentClient).putForEntity(anyString(), anyString());
        doReturn(Mono.just("OK")).when(dmaapClient).put(anyString(), anyString());

        testedObject.createTask(dmaapInputMessage(Operation.PUT)).block();

        verify(agentClient).putForEntity(anyString(), anyString());
        verifyNoMoreInteractions(agentClient);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(dmaapClient).put(anyString(), captor.capture());
        String actualMessage = captor.getValue();
        assertThat(actualMessage.contains(HttpStatus.BAD_GATEWAY.toString()))
            .as("Message \"%s\" sent to DMaaP contains %s", actualMessage, HttpStatus.BAD_GATEWAY) //
            .isTrue();

        verifyNoMoreInteractions(dmaapClient);
    }

    @Test
    void unsupportedOperationInMessage_thenNotFoundResponseWithNotImplementedOperation() throws Exception {
        String message = dmaapInputMessage(Operation.PUT).toString();
        String badOperation = "BAD";
        message = message.replace(Operation.PUT.toString(), badOperation);

        testedObject.handleDmaapMsg(message);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(dmaapClient).put(anyString(), captor.capture());
        String actualMessage = captor.getValue();
        assertThat(actualMessage
            .contains(HttpStatus.BAD_REQUEST + "\",\"message\":\"Not implemented operation: " + badOperation)) //
                .as("Message \"%s\" sent to DMaaP contains %s", actualMessage, HttpStatus.BAD_REQUEST) //
                .isTrue();
    }

    @Test
    void putWithoutPayload_thenNotFoundResponseWithWarning() throws Exception {
        String message = dmaapInputMessage(Operation.PUT).toString();
        message = message.replace(",\"payload\":{\"name\":\"name\",\"schema\":\"schema\"}", "");

        final ListAppender<ILoggingEvent> logAppender =
            LoggingUtils.getLogListAppender(DmaapMessageHandler.class, WARN);

        testedObject.handleDmaapMsg(message);

        assertThat(logAppender.list.get(0).getFormattedMessage())
            .startsWith("Expected payload in message from DMAAP: ");
    }
}
