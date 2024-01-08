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

package org.oran.helloworld.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HelloWorldController.class)
public class HelloWorldControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HelloWorldController helloWorldController;

    @Test
    public void testHelloWorldEndpoint() throws Exception {
        when(helloWorldController.helloWorld()).thenReturn("Hello World from service stub\n");

        mockMvc.perform(get("/helloworld/v1"))
            .andExpect(status().isOk())
            .andExpect(content().string("Hello World from service stub\n"));
    }

    @Test
    public void testHelloWorldSmeEndpoint() throws Exception {
        when(helloWorldController.helloWorldSme()).thenReturn("Hello World from SME\n");

        mockMvc.perform(get("/helloworld/v1/sme"))
            .andExpect(status().isOk())
            .andExpect(content().string("Hello World from SME\n"));
    }

    @Test
    public void testHelloWorld2Endpoint() throws Exception {
        when(helloWorldController.helloWorld2()).thenReturn("Hello World 2!\n");

        mockMvc.perform(get("/helloworld2/v1"))
            .andExpect(status().isOk())
            .andExpect(content().string("Hello World 2!\n"));
    }
}
