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

package org.oransc.policyagent.dmaap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.Optional;

import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.dmaap.DmaapRequestMessage.Operation;
import org.oransc.policyagent.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * The class handles incoming requests from DMAAP.
 * <p>
 * That means: invoke a REST call towards this services and to send back a response though DMAAP
 */
public class DmaapMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(DmaapMessageHandler.class);
    private static Gson gson = new GsonBuilder() //
            .create(); //
    private final AsyncRestClient dmaapClient;
    private final AsyncRestClient agentClient;

    public DmaapMessageHandler(AsyncRestClient dmaapClient, AsyncRestClient agentClient) {
        this.agentClient = agentClient;
        this.dmaapClient = dmaapClient;
    }

    public void handleDmaapMsg(String msg) {
        try {
            String result = this.createTask(msg).block();
            logger.debug("handleDmaapMsg: {}", result);
        } catch (Exception throwable) {
            logger.warn("handleDmaapMsg failure {}", throwable.getMessage());
        }
    }

    Mono<String> createTask(String msg) {
        try {
            DmaapRequestMessage dmaapRequestMessage = gson.fromJson(msg, ImmutableDmaapRequestMessage.class);
            return this.invokePolicyAgent(dmaapRequestMessage) //
                    .onErrorResume(t -> handleAgentCallError(t, dmaapRequestMessage)) //
                    .flatMap(response -> sendDmaapResponse(response.getBody(), dmaapRequestMessage,
                            response.getStatusCode()));
        } catch (Exception e) {
            String errorMsg = "Received unparsable message from DMAAP: \"" + msg + "\", reason: " + e.getMessage();
            return Mono.error(new ServiceException(errorMsg)); // Cannot make any response
        }
    }

    private Mono<ResponseEntity<String>> handleAgentCallError(Throwable error,
            DmaapRequestMessage dmaapRequestMessage) {
        logger.debug("Agent call failed: {}", error.getMessage());
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorMessage = error.getMessage();
        if (error instanceof WebClientResponseException) {
            WebClientResponseException exception = (WebClientResponseException) error;
            status = exception.getStatusCode();
            errorMessage = exception.getResponseBodyAsString();
        } else if (error instanceof ServiceException) {
            status = HttpStatus.BAD_REQUEST;
            errorMessage = error.getMessage();
        } else if (!(error instanceof WebClientException)) {
            logger.warn("Unexpected exception ", error);
        }
        return sendDmaapResponse(errorMessage, dmaapRequestMessage, status) //
                .flatMap(notUsed -> Mono.empty());
    }

    private Mono<ResponseEntity<String>> invokePolicyAgent(DmaapRequestMessage dmaapRequestMessage) {
        DmaapRequestMessage.Operation operation = dmaapRequestMessage.operation();
        String uri = dmaapRequestMessage.url();

        if (operation == Operation.DELETE) {
            return agentClient.deleteForEntity(uri);
        } else if (operation == Operation.GET) {
            return agentClient.getForEntity(uri);
        } else if (operation == Operation.PUT) {
            return agentClient.putForEntity(uri, payload(dmaapRequestMessage));
        } else if (operation == Operation.POST) {
            return agentClient.postForEntity(uri, payload(dmaapRequestMessage));
        } else {
            return Mono.error(new ServiceException("Not implemented operation: " + operation));
        }
    }

    private String payload(DmaapRequestMessage message) {
        Optional<JsonObject> payload = message.payload();
        if (payload.isPresent()) {
            return gson.toJson(payload.get());
        } else {
            logger.warn("Expected payload in message from DMAAP: {}", message);
            return "";
        }
    }

    private Mono<String> sendDmaapResponse(String response, DmaapRequestMessage dmaapRequestMessage,
            HttpStatus status) {
        return createDmaapResponseMessage(dmaapRequestMessage, response, status) //
                .flatMap(this::sendToDmaap) //
                .onErrorResume(this::handleResponseCallError);
    }

    private Mono<String> sendToDmaap(String body) {
        logger.debug("sendToDmaap: {} ", body);
        return dmaapClient.post("", "[" + body + "]");
    }

    private Mono<String> handleResponseCallError(Throwable t) {
        logger.debug("Failed to send response to DMaaP: {}", t.getMessage());
        return Mono.empty();
    }

    private Mono<String> createDmaapResponseMessage(DmaapRequestMessage dmaapRequestMessage, String response,
            HttpStatus status) {
        DmaapResponseMessage dmaapResponseMessage = ImmutableDmaapResponseMessage.builder() //
                .status(status.toString()) //
                .message(response == null ? "" : response) //
                .type("response") //
                .correlationId(dmaapRequestMessage.correlationId() == null ? "" : dmaapRequestMessage.correlationId()) //
                .originatorId(dmaapRequestMessage.originatorId() == null ? "" : dmaapRequestMessage.originatorId()) //
                .requestId(dmaapRequestMessage.requestId() == null ? "" : dmaapRequestMessage.requestId()) //
                .timestamp(dmaapRequestMessage.timestamp() == null ? "" : dmaapRequestMessage.timestamp()) //
                .build();
        String str = gson.toJson(dmaapResponseMessage);
        return Mono.just(str);

    }
}
