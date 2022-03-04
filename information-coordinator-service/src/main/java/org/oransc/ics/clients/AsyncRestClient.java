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

package org.oransc.ics.clients;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicInteger;

import org.oransc.ics.configuration.WebClientConfig.HttpProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

/**
 * Generic reactive REST client.
 */
public class AsyncRestClient {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private WebClient webClient = null;
    private final String baseUrl;
    private static final AtomicInteger sequenceNumber = new AtomicInteger();
    private final SslContext sslContext;
    private final HttpProxyConfig httpProxyConfig;
    private final SecurityContext securityContext;

    public AsyncRestClient(String baseUrl, @Nullable SslContext sslContext, @Nullable HttpProxyConfig httpProxyConfig,
        SecurityContext securityContext) {
        this.baseUrl = baseUrl;
        this.sslContext = sslContext;
        this.httpProxyConfig = httpProxyConfig;
        this.securityContext = securityContext;
    }

    public Mono<ResponseEntity<String>> postForEntity(String uri, @Nullable String body) {
        Mono<String> bodyProducer = body != null ? Mono.just(body) : Mono.empty();

        RequestHeadersSpec<?> request = getWebClient() //
            .post() //
            .uri(uri) //
            .contentType(MediaType.APPLICATION_JSON) //
            .body(bodyProducer, String.class);
        return retrieve(request);
    }

    public Mono<String> post(String uri, @Nullable String body) {
        return postForEntity(uri, body) //
            .map(this::toBody);
    }

    public Mono<String> postWithAuthHeader(String uri, String body, String username, String password) {
        RequestHeadersSpec<?> request = getWebClient() //
            .post() //
            .uri(uri) //
            .headers(headers -> headers.setBasicAuth(username, password)) //
            .contentType(MediaType.APPLICATION_JSON) //
            .bodyValue(body);
        return retrieve(request) //
            .map(this::toBody);
    }

    public Mono<ResponseEntity<String>> putForEntity(String uri, String body) {
        RequestHeadersSpec<?> request = getWebClient() //
            .put() //
            .uri(uri) //
            .contentType(MediaType.APPLICATION_JSON) //
            .bodyValue(body);
        return retrieve(request);
    }

    public Mono<ResponseEntity<String>> putForEntity(String uri) {
        RequestHeadersSpec<?> request = getWebClient() //
            .put() //
            .uri(uri);
        return retrieve(request);
    }

    public Mono<String> put(String uri, String body) {
        return putForEntity(uri, body) //
            .map(this::toBody);
    }

    public Mono<ResponseEntity<String>> getForEntity(String uri) {
        RequestHeadersSpec<?> request = getWebClient().get().uri(uri);
        return retrieve(request);
    }

    public Mono<String> get(String uri) {
        return getForEntity(uri) //
            .map(this::toBody);
    }

    public Mono<ResponseEntity<String>> deleteForEntity(String uri) {
        RequestHeadersSpec<?> request = getWebClient().delete().uri(uri);
        return retrieve(request);
    }

    public Mono<String> delete(String uri) {
        return deleteForEntity(uri) //
            .map(this::toBody);
    }

    private Mono<ResponseEntity<String>> retrieve(RequestHeadersSpec<?> request) {
        if (securityContext.isConfigured()) {
            request.headers(h -> h.setBearerAuth(securityContext.getBearerAuthToken()));
        }
        return request.retrieve() //
            .toEntity(String.class);
    }

    private static Object createTraceTag() {
        return sequenceNumber.incrementAndGet();
    }

    private String toBody(ResponseEntity<String> entity) {
        if (entity.getBody() == null) {
            return "";
        } else {
            return entity.getBody();
        }
    }

    private boolean isHttpProxyConfigured() {
        return httpProxyConfig != null && httpProxyConfig.httpProxyPort() > 0
            && !httpProxyConfig.httpProxyHost().isEmpty();
    }

    private HttpClient buildHttpClient() {
        HttpClient httpClient = HttpClient.create() //
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000) //
            .doOnConnected(connection -> {
                connection.addHandlerLast(new ReadTimeoutHandler(30));
                connection.addHandlerLast(new WriteTimeoutHandler(30));
            });

        if (this.sslContext != null) {
            httpClient = httpClient.secure(ssl -> ssl.sslContext(sslContext));
        }

        if (isHttpProxyConfigured()) {
            httpClient = httpClient.proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
                .host(httpProxyConfig.httpProxyHost()).port(httpProxyConfig.httpProxyPort()));
        }
        return httpClient;
    }

    public WebClient buildWebClient(String baseUrl) {
        Object traceTag = createTraceTag();

        final HttpClient httpClient = buildHttpClient();
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder() //
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1)) //
            .build();

        ExchangeFilterFunction reqLogger = ExchangeFilterFunction.ofRequestProcessor(req -> {
            logger.debug("{} {} uri = '{}''", traceTag, req.method(), req.url());
            return Mono.just(req);
        });

        ExchangeFilterFunction respLogger = ExchangeFilterFunction.ofResponseProcessor(resp -> {
            logger.debug("{} resp: {}", traceTag, resp.statusCode());
            return Mono.just(resp);
        });

        return WebClient.builder() //
            .clientConnector(new ReactorClientHttpConnector(httpClient)) //
            .baseUrl(baseUrl) //
            .exchangeStrategies(exchangeStrategies) //
            .filter(reqLogger) //
            .filter(respLogger) //
            .build();
    }

    private WebClient getWebClient() {
        if (this.webClient == null) {
            this.webClient = buildWebClient(baseUrl);
        }
        return this.webClient;
    }
}
