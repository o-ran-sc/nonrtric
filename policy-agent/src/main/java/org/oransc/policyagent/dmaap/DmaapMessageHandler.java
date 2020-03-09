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

import java.io.IOException;
import java.util.Optional;

import org.onap.dmaap.mr.client.MRBatchingPublisher;
import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

public class DmaapMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(DmaapMessageHandler.class);

    private static Gson gson = new GsonBuilder() //
        .create(); //

    private final MRBatchingPublisher dmaapClient;
    private final AsyncRestClient agentClient;

    public DmaapMessageHandler(MRBatchingPublisher dmaapClient, AsyncRestClient agentClient) {
        this.agentClient = agentClient;
        this.dmaapClient = dmaapClient;
    }

    public void handleDmaapMsg(String msg) {
        this.createTask(msg) //
            .subscribe(message -> logger.debug("handleDmaapMsg: {}", message), //
                throwable -> logger.warn("handleDmaapMsg failure ", throwable), //
                () -> logger.debug("handleDmaapMsg complete"));
    }

    Mono<String> createTask(String msg) {
        try {
            DmaapRequestMessage dmaapRequestMessage = gson.fromJson(msg, ImmutableDmaapRequestMessage.class);

            return this.invokePolicyAgent(dmaapRequestMessage) //
                .onErrorResume(t -> handleAgentCallError(t, msg, dmaapRequestMessage)) //
                .flatMap(response -> sendDmaapResponse(response, dmaapRequestMessage, HttpStatus.OK));

        } catch (Exception e) {
            logger.warn("Received unparsable message from DMAAP: {}", msg);
            return Mono.error(e);
        }
    }

    private Mono<String> handleAgentCallError(Throwable t, String origianalMessage,
        DmaapRequestMessage dmaapRequestMessage) {
        logger.debug("Agent call failed: {}", t.getMessage());
        if (t instanceof ServiceException) {
            String errorMessage = prepareBadOperationErrorMessage(t, origianalMessage);
            return sendDmaapResponse(errorMessage, dmaapRequestMessage, HttpStatus.NOT_FOUND) //
                .flatMap(notUsed -> Mono.empty());
        } else {
            return sendDmaapResponse(t.toString(), dmaapRequestMessage, HttpStatus.NOT_FOUND) //
                .flatMap(notUsed -> Mono.empty());
        }
    }

    private String prepareBadOperationErrorMessage(Throwable t, String origianalMessage) {
        String badOperation = origianalMessage.substring(origianalMessage.indexOf("operation\":\"") + 12,
            origianalMessage.indexOf(",\"url\":"));
        String errorMessage = t.getMessage().replace("null", badOperation);
        return errorMessage;
    }

    private Mono<String> invokePolicyAgent(DmaapRequestMessage dmaapRequestMessage) {
        DmaapRequestMessage.Operation operation = dmaapRequestMessage.operation();
        if (operation == null) {
            return Mono.error(new ServiceException("Not implemented operation: " + operation));
        }
        Mono<String> result = null;
        String uri = dmaapRequestMessage.url();
        switch (operation) {
            case DELETE:
                result = agentClient.delete(uri);
                break;
            case GET:
                result = agentClient.get(uri);
                break;
            case PUT:
                result = agentClient.put(uri, payload(dmaapRequestMessage));
                break;
            case POST:
                result = agentClient.post(uri, payload(dmaapRequestMessage));
                break;
            default:
                // Nothing, can never get here.
        }
        return result;
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
        return getDmaapResponseMessage(dmaapRequestMessage, response, status) //
            .flatMap(this::sendToDmaap) //
            .onErrorResume(this::handleResponseCallError);
    }

    private Mono<String> sendToDmaap(String body) {
        try {
            logger.debug("sendToDmaap: {} ", body);
            dmaapClient.send(body);
            dmaapClient.sendBatchWithResponse();
            return Mono.just("OK");
        } catch (IOException e) {
            return Mono.error(e);
        }
    }

    private Mono<String> handleResponseCallError(Throwable t) {
        logger.debug("Failed to send respons to DMaaP: {}", t.getMessage());
        return Mono.empty();
    }

    private Mono<String> getDmaapResponseMessage(DmaapRequestMessage dmaapRequestMessage, String response,
        HttpStatus status) {
        DmaapResponseMessage dmaapResponseMessage = ImmutableDmaapResponseMessage.builder() //
            .status(status.toString()) //
            .message(response) //
            .type("response") //
            .correlationId(dmaapRequestMessage.correlationId()) //
            .originatorId(dmaapRequestMessage.originatorId()) //
            .requestId(dmaapRequestMessage.requestId()) //
            .timestamp(dmaapRequestMessage.timestamp()) //
            .build();
        String str = gson.toJson(dmaapResponseMessage);

        return Mono.just(str);

    }
}
