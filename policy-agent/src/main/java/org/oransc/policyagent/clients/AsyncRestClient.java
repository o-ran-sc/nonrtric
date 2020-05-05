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
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

/**
 * Generic reactive REST client.
 */
public class AsyncRestClient {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private WebClient webClient = null;
    private final String baseUrl;
    private static final AtomicInteger sequenceNumber = new AtomicInteger();

    public AsyncRestClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Mono<ResponseEntity<String>> postForEntity(String uri, @Nullable String body) {
        Object traceTag = createTraceTag();
        logger.debug("{} POST uri = '{}{}''", traceTag, baseUrl, uri);
        logger.trace("{} POST body: {}", traceTag, body);
        Mono<String> bodyProducer = body != null ? Mono.just(body) : Mono.empty();
        return getWebClient() //
            .flatMap(client -> {
                RequestHeadersSpec<?> request = client.post() //
                    .uri(uri) //
                    .contentType(MediaType.APPLICATION_JSON) //
                    .body(bodyProducer, String.class);
                return retrieve(traceTag, request);
            });
    }

    public Mono<String> post(String uri, @Nullable String body) {
        return postForEntity(uri, body) //
            .flatMap(this::toBody);
    }

    public Mono<String> postWithAuthHeader(String uri, String body, String username, String password) {
        Object traceTag = createTraceTag();
        logger.debug("{} POST (auth) uri = '{}{}''", traceTag, baseUrl, uri);
        logger.trace("{} POST body: {}", traceTag, body);
        return getWebClient() //
            .flatMap(client -> {
                RequestHeadersSpec<?> request = client.post() //
                    .uri(uri) //
                    .headers(headers -> headers.setBasicAuth(username, password)) //
                    .contentType(MediaType.APPLICATION_JSON) //
                    .bodyValue(body);
                return retrieve(traceTag, request) //
                    .flatMap(this::toBody);
            });
    }

    public Mono<ResponseEntity<String>> putForEntity(String uri, String body) {
        Object traceTag = createTraceTag();
        logger.debug("{} PUT uri = '{}{}''", traceTag, baseUrl, uri);
        logger.trace("{} PUT body: {}", traceTag, body);
        return getWebClient() //
            .flatMap(client -> {
                RequestHeadersSpec<?> request = client.put() //
                    .uri(uri) //
                    .contentType(MediaType.APPLICATION_JSON) //
                    .bodyValue(body);
                return retrieve(traceTag, request);
            });
    }

    public Mono<ResponseEntity<String>> putForEntity(String uri) {
        Object traceTag = createTraceTag();
        logger.debug("{} PUT uri = '{}{}''", traceTag, baseUrl, uri);
        logger.trace("{} PUT body: <empty>", traceTag);
        return getWebClient() //
            .flatMap(client -> {
                RequestHeadersSpec<?> request = client.put() //
                    .uri(uri);
                return retrieve(traceTag, request);
            });
    }

    public Mono<String> put(String uri, String body) {
        return putForEntity(uri, body) //
            .flatMap(this::toBody);
    }

    public Mono<ResponseEntity<String>> getForEntity(String uri) {
        Object traceTag = createTraceTag();
        logger.debug("{} GET uri = '{}{}''", traceTag, baseUrl, uri);
        return getWebClient() //
            .flatMap(client -> {
                RequestHeadersSpec<?> request = client.get().uri(uri);
                return retrieve(traceTag, request);
            });
    }

    public Mono<String> get(String uri) {
        return getForEntity(uri) //
            .flatMap(this::toBody);
    }

    public Mono<ResponseEntity<String>> deleteForEntity(String uri) {
        Object traceTag = createTraceTag();
        logger.debug("{} DELETE uri = '{}{}''", traceTag, baseUrl, uri);
        return getWebClient() //
            .flatMap(client -> {
                RequestHeadersSpec<?> request = client.delete().uri(uri);
                return retrieve(traceTag, request);
            });
    }

    public Mono<String> delete(String uri) {
        return deleteForEntity(uri) //
            .flatMap(this::toBody);
    }

    private Mono<ResponseEntity<String>> retrieve(Object traceTag, RequestHeadersSpec<?> request) {
        return request.retrieve() //
            .toEntity(String.class) //
            .doOnNext(entity -> logger.trace("{} Received: {}", traceTag, entity.getBody()))
            .doOnError(throwable -> onHttpError(traceTag, throwable));
    }

    private static Object createTraceTag() {
        return sequenceNumber.incrementAndGet();
    }

    private void onHttpError(Object traceTag, Throwable t) {
        if (t instanceof WebClientResponseException) {
            WebClientResponseException exception = (WebClientResponseException) t;
            logger.debug("{} HTTP error status = '{}', body '{}'", traceTag, exception.getStatusCode(),
                exception.getResponseBodyAsString());
        } else {
            logger.debug("{} HTTP error: {}", traceTag, t.getMessage());
        }
    }

    private Mono<String> toBody(ResponseEntity<String> entity) {
        if (entity.getBody() == null) {
            return Mono.just("");
        } else {
            return Mono.just(entity.getBody());
        }
    }

    private static SslContext createSslContext() throws SSLException {
        return SslContextBuilder.forClient() //
            .trustManager(InsecureTrustManagerFactory.INSTANCE) //
            .build();
    }

    private static WebClient createWebClient(String baseUrl, SslContext sslContext) {
        TcpClient tcpClient = TcpClient.create() //
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000) //
            .secure(c -> c.sslContext(sslContext)) //
            .doOnConnected(connection -> {
                connection.addHandlerLast(new ReadTimeoutHandler(30));
                connection.addHandlerLast(new WriteTimeoutHandler(30));
            });
        HttpClient httpClient = HttpClient.from(tcpClient);
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

        return WebClient.builder() //
            .clientConnector(connector) //
            .baseUrl(baseUrl) //
            .build();
    }

    private Mono<WebClient> getWebClient() {
        if (this.webClient == null) {
            try {
                SslContext sslContext = createSslContext();
                this.webClient = createWebClient(this.baseUrl, sslContext);
            } catch (SSLException e) {
                logger.error("Could not create WebClient {}", e.getMessage());
                return Mono.error(e);
            }
        }
        return Mono.just(this.webClient);
    }

}
