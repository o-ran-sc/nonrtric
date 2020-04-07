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

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

/**
 * Generic reactive REST client.
 */
public class AsyncRestClient {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final WebClient client;
    private final String baseUrl;

    public AsyncRestClient(String baseUrl) {

        TcpClient tcpClient = TcpClient.create() //
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000) //
            .doOnConnected(connection -> {
                connection.addHandler(new ReadTimeoutHandler(10));
                connection.addHandler(new WriteTimeoutHandler(30));
            });
        HttpClient httpClient = HttpClient.from(tcpClient);
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

        this.client = WebClient.builder() //
            .clientConnector(connector) //
            .baseUrl(baseUrl) //
            .build();

        this.baseUrl = baseUrl;
    }

    public Mono<ResponseEntity<String>> postForEntity(String uri, @Nullable String body) {
        logger.debug("POST uri = '{}{}''", baseUrl, uri);
        Mono<String> bodyProducer = body != null ? Mono.just(body) : Mono.empty();
        RequestHeadersSpec<?> request = client.post() //
            .uri(uri) //
            .contentType(MediaType.APPLICATION_JSON) //
            .body(bodyProducer, String.class);
        return retrieve(request);
    }

    public Mono<String> post(String uri, @Nullable String body) {
        return postForEntity(uri, body) //
            .flatMap(this::toBody);
    }

    public Mono<String> postWithAuthHeader(String uri, String body, String username, String password) {
        logger.debug("POST (auth) uri = '{}{}''", baseUrl, uri);
        RequestHeadersSpec<?> request = client.post() //
            .uri(uri) //
            .headers(headers -> headers.setBasicAuth(username, password)) //
            .contentType(MediaType.APPLICATION_JSON) //
            .bodyValue(body);
        return retrieve(request) //
            .flatMap(this::toBody);
    }

    public Mono<ResponseEntity<String>> putForEntity(String uri, String body) {
        logger.debug("PUT uri = '{}{}''", baseUrl, uri);
        RequestHeadersSpec<?> request = client.put() //
            .uri(uri) //
            .contentType(MediaType.APPLICATION_JSON) //
            .bodyValue(body);
        return retrieve(request);
    }

    public Mono<ResponseEntity<String>> putForEntity(String uri) {
        logger.debug("PUT uri = '{}{}''", baseUrl, uri);
        RequestHeadersSpec<?> request = client.put() //
            .uri(uri);
        return retrieve(request);
    }

    public Mono<String> put(String uri, String body) {
        return putForEntity(uri, body) //
            .flatMap(this::toBody);
    }

    public Mono<ResponseEntity<String>> getForEntity(String uri) {
        logger.debug("GET uri = '{}{}''", baseUrl, uri);
        RequestHeadersSpec<?> request = client.get().uri(uri);
        return retrieve(request);
    }

    public Mono<String> get(String uri) {
        return getForEntity(uri) //
            .flatMap(this::toBody);
    }

    public Mono<ResponseEntity<String>> deleteForEntity(String uri) {
        logger.debug("DELETE uri = '{}{}''", baseUrl, uri);
        RequestHeadersSpec<?> request = client.delete().uri(uri);
        return retrieve(request);
    }

    public Mono<String> delete(String uri) {
        return deleteForEntity(uri) //
            .flatMap(this::toBody);
    }

    private Mono<ResponseEntity<String>> retrieve(RequestHeadersSpec<?> request) {
        return request.retrieve() //
            .toEntity(String.class);
    }

    Mono<String> toBody(ResponseEntity<String> entity) {
        if (entity.getBody() == null) {
            return Mono.just("");
        } else {
            return Mono.just(entity.getBody());
        }
    }

}
