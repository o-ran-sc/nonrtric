/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation.
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

package org.o_ran_sc.nonrtric.sdnc_a1.northbound.restadapter;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

public class RestAdapterImplTest {
    private static MockWebServer mockWebServer;
    private static RestAdapter adapterUnderTest;

    private static final String VALID_PROTOCOL = "http";
    private static final String INVALID_PROTOCOL = "ftp";
    private static final String REQUEST_URL = "/test";
    private static final String TEST_BODY = "test";
    private static final Integer SUCCESS_CODE = 200;
    private static final Integer ERROR_CODE = 500;

    @Before
    public void init() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        adapterUnderTest = new RestAdapterImpl();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testInvalidUrlOrProtocol() throws InterruptedException {
        ResponseEntity<String> response = adapterUnderTest.get("://localhost:" + mockWebServer.getPort() + REQUEST_URL,
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCodeValue());
        response = adapterUnderTest.get(INVALID_PROTOCOL + "://localhost:" + mockWebServer.getPort() + REQUEST_URL,
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCodeValue());
    }

    @Test
    public void testGetNoError() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(SUCCESS_CODE).setBody(TEST_BODY));
        ResponseEntity<String> response = adapterUnderTest.get(VALID_PROTOCOL + "://localhost:"
                + mockWebServer.getPort() + REQUEST_URL, String.class);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals(TEST_BODY, response.getBody());
        assertEquals(SUCCESS_CODE.intValue(), response.getStatusCodeValue());
        assertEquals("GET", recordedRequest.getMethod());
        assertEquals(REQUEST_URL, recordedRequest.getPath());
    }

    @Test(expected = RestClientException.class)
    public void testGetError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(ERROR_CODE));
        adapterUnderTest.get(VALID_PROTOCOL + "://localhost:" + mockWebServer.getPort() + REQUEST_URL, String.class);
    }

    @Test
    public void testPutNoError() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(SUCCESS_CODE).setBody(TEST_BODY));
        ResponseEntity<String> response = adapterUnderTest.put(VALID_PROTOCOL + "://localhost:"
                + mockWebServer.getPort() + REQUEST_URL, TEST_BODY, String.class);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals(TEST_BODY, response.getBody());
        assertEquals(SUCCESS_CODE.intValue(), response.getStatusCodeValue());
        assertEquals("PUT", recordedRequest.getMethod());
        assertEquals(REQUEST_URL, recordedRequest.getPath());
        assertEquals(TEST_BODY, recordedRequest.getBody().readUtf8());
    }

    @Test(expected = RestClientException.class)
    public void testPutError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(ERROR_CODE));
        adapterUnderTest.put(VALID_PROTOCOL + "://localhost:" + mockWebServer.getPort() + REQUEST_URL, TEST_BODY,
                String.class);
    }

    @Test
    public void testDeleteNoError() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(SUCCESS_CODE));
        ResponseEntity<String> response = adapterUnderTest.delete(VALID_PROTOCOL + "://localhost:"
                + mockWebServer.getPort() + REQUEST_URL);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals(SUCCESS_CODE.intValue(), response.getStatusCodeValue());
        assertEquals("DELETE", recordedRequest.getMethod());
        assertEquals(REQUEST_URL, recordedRequest.getPath());
    }

    @Test(expected = RestClientException.class)
    public void testDeleteError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(ERROR_CODE));
        adapterUnderTest.delete(VALID_PROTOCOL + "://localhost:" + mockWebServer.getPort() + REQUEST_URL);
    }
}
