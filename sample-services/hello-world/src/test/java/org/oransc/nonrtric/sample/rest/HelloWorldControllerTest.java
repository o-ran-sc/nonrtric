/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2023-2024 OpenInfra Foundation Europe.
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

package org.oransc.nonrtric.sample.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpServletRequest;

@WebMvcTest(HelloWorldController.class)
public class HelloWorldControllerTest {

    @InjectMocks
    private HelloWorldController helloWorldController;

    @Test
    public void testHelloWorldEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/helloworld/v1");

        String result = helloWorldController.helloWorld(request);

        assertEquals("Hello from /helloworld/v1", result);
    }

    @Test
    public void testHelloWorldSubpathEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/helloworld/v1/subpath1");

        String result = helloWorldController.helloWorldSubpath1(request);

        assertEquals("Hello from /helloworld/v1/subpath1", result);
    }

    @Test
    public void testHelloWorld2Endpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/helloworld2/v1");

        String result = helloWorldController.helloWorld2(request);

        assertEquals("Hello from /helloworld2/v1", result);
    }

    @Test
    public void testHelloWorldEndpointV2() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/helloworld/v2");

        String result = helloWorldController.helloWorldV2(request);

        assertEquals("Hello from /helloworld/v2", result);
    }

    @Test
    public void testHelloWorldSubpathEndpointV2() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/helloworld/v2/subpath1");

        String result = helloWorldController.helloWorldSubpath1V2(request);

        assertEquals("Hello from /helloworld/v2/subpath1", result);
    }
}
