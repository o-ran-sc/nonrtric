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

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.Loggers;

class AsyncRestClientTest {
    private static final String BASE_URL = "BaseUrl";
    private static final String REQUEST_URL = "/test";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String TEST_JSON = "{\"type\":\"type1\"}";
    private static final int SUCCESS_CODE = 200;
    private static final int ERROR_CODE = 500;

    private static MockWebServer mockWebServer;

    private static AsyncRestClient clientUnderTest;

    private static final SecurityContext securityContext = new SecurityContext("");

    @BeforeAll
    static void init() {
        // skip a lot of unnecessary logs from MockWebServer
        InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
        Loggers.useJdkLoggers();
        mockWebServer = new MockWebServer();
        clientUnderTest = new AsyncRestClient(mockWebServer.url(BASE_URL).toString(), null, null, securityContext);
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testGetNoError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(SUCCESS_CODE) //
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) //
            .setBody(TEST_JSON));

        Mono<String> returnedMono = clientUnderTest.get(REQUEST_URL);
        StepVerifier.create(returnedMono).expectNext(TEST_JSON).expectComplete().verify();
    }

    @Test
    void testGetError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(ERROR_CODE));

        Mono<String> returnedMono = clientUnderTest.get(REQUEST_URL);
        StepVerifier.create(returnedMono)
            .expectErrorMatches(throwable -> throwable instanceof WebClientResponseException).verify();
    }

    @Test
    void testPutNoError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(SUCCESS_CODE) //
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) //
            .setBody(TEST_JSON));

        Mono<String> returnedMono = clientUnderTest.put(REQUEST_URL, TEST_JSON);
        StepVerifier.create(returnedMono).expectNext(TEST_JSON).expectComplete().verify();
    }

    @Test
    void testPutError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(ERROR_CODE));

        Mono<String> returnedMono = clientUnderTest.put(REQUEST_URL, TEST_JSON);
        StepVerifier.create(returnedMono)
            .expectErrorMatches(throwable -> throwable instanceof WebClientResponseException).verify();
    }

    @Test
    void testDeleteNoError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(SUCCESS_CODE));

        Mono<String> returnedMono = clientUnderTest.delete(REQUEST_URL);
        StepVerifier.create(returnedMono).expectNext("").expectComplete().verify();
    }

    @Test
    void testDeleteError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(ERROR_CODE));

        Mono<String> returnedMono = clientUnderTest.delete(REQUEST_URL);
        StepVerifier.create(returnedMono)
            .expectErrorMatches(throwable -> throwable instanceof WebClientResponseException).verify();
    }

    @Test
    void testPostNoError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(SUCCESS_CODE) //
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) //
            .setBody(TEST_JSON));

        Mono<String> returnedMono = clientUnderTest.post(REQUEST_URL, TEST_JSON);
        StepVerifier.create(returnedMono).expectNext(TEST_JSON).expectComplete().verify();
    }

    @Test
    void testPostError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(ERROR_CODE));

        Mono<String> returnedMono = clientUnderTest.post(REQUEST_URL, TEST_JSON);
        StepVerifier.create(returnedMono)
            .expectErrorMatches(throwable -> throwable instanceof WebClientResponseException).verify();
    }

    @Test
    void testPostWithAuthHeaderNoError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(SUCCESS_CODE) //
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) //
            .setBody(TEST_JSON));

        Mono<String> returnedMono = clientUnderTest.postWithAuthHeader(REQUEST_URL, TEST_JSON, USERNAME, PASSWORD);
        StepVerifier.create(returnedMono).expectNext(TEST_JSON).expectComplete().verify();
    }

    @Test
    void testPostWithAuthHeaderError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(ERROR_CODE));

        Mono<String> returnedMono = clientUnderTest.postWithAuthHeader(REQUEST_URL, TEST_JSON, USERNAME, PASSWORD);
        StepVerifier.create(returnedMono)
            .expectErrorMatches(throwable -> throwable instanceof WebClientResponseException).verify();
    }
}
