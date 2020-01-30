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

package org.oransc.policyagent.dmaap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import org.onap.dmaap.mr.client.MRBatchingPublisher;
import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.dmaap.DmaapRequestMessage.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

public class DmaapMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(DmaapMessageHandler.class);

    private static Gson gson = new GsonBuilder() //
            .serializeNulls() //
            .create(); //

    private final MRBatchingPublisher dmaapClient;
    private final AsyncRestClient agentClient;

    public DmaapMessageHandler(MRBatchingPublisher dmaapClient, ApplicationConfig applicationConfig,
            AsyncRestClient agentClient) {
        this.agentClient = agentClient;
        this.dmaapClient = dmaapClient;
    }

    public void handleDmaapMsg(String msg) {
        try {
            this.createTask(msg) //
                    .subscribe(x -> logger.debug("handleDmaapMsg: " + x), //
                            throwable -> logger.warn("handleDmaapMsg failure ", throwable), //
                            () -> logger.debug("handleDmaapMsg complete"));
        } catch (Exception e) {
            logger.warn("Received unparsable message from DMAAP: {}", msg);
        }
    }

    Mono<String> createTask(String msg) {
        try {
            DmaapRequestMessage dmaapRequestMessage = gson.fromJson(msg, ImmutableDmaapRequestMessage.class);

            return this.invokePolicyAgent(dmaapRequestMessage) //
                    .onErrorResume(t -> handleAgentCallError(t, dmaapRequestMessage)) //
                    .flatMap(response -> sendDmaapResponse(response, dmaapRequestMessage, HttpStatus.OK));

        } catch (Exception e) {
            logger.warn("Received unparsable message from DMAAP: {}", msg);
            return Mono.error(e);
        }
    }

    private Mono<String> handleAgentCallError(Throwable t, DmaapRequestMessage dmaapRequestMessage) {
        logger.debug("Agent call failed: " + t.getMessage());
        return sendDmaapResponse(t.toString(), dmaapRequestMessage, HttpStatus.NOT_FOUND) //
                .flatMap(s -> Mono.empty());
    }

    private Mono<String> invokePolicyAgent(DmaapRequestMessage dmaapRequestMessage) {
        DmaapRequestMessage.Operation operation = dmaapRequestMessage.operation();
        Mono<String> result = null;
        String uri = dmaapRequestMessage.url();
        if (operation == Operation.DELETE) {
            result = agentClient.delete(uri);
        } else if (operation == Operation.GET) {
            result = agentClient.get(uri);
        } else if (operation == Operation.PUT) {
            result = agentClient.put(uri, dmaapRequestMessage.payload());
        } else if (operation == Operation.POST) {
            result = agentClient.post(uri, dmaapRequestMessage.payload());
        } else {
            return Mono.error(new Exception("Not implemented operation: " + operation));
        }
        return result;
    }

    private Mono<String> sendDmaapResponse(String response, DmaapRequestMessage dmaapRequestMessage,
            HttpStatus status) {
        return getDmaapResponseMessage(dmaapRequestMessage, response, status) //
                .flatMap(body -> sendToDmaap(body)) //
                .onErrorResume(t -> handleResponseCallError(t, dmaapRequestMessage));
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

    private Mono<String> handleResponseCallError(Throwable t, DmaapRequestMessage dmaapRequestMessage) {
        logger.debug("Failed to respond: " + t.getMessage());
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
