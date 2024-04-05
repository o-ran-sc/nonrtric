/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 *
 * Copyright (C) 2024: OpenInfra Foundation Europe
 *
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

package com.demo.producer;

import com.demo.producer.controllers.ThreadsController;
import com.demo.producer.producer.SimpleProducer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
@ContextConfiguration(classes = SimpleProducer.class)
public class ThreadsControllerTest {

    @Mock
    @Autowired
    private SimpleProducer simpleProducer;

    @InjectMocks
    private ThreadsController threadsController;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private AutoCloseable closable;

    @BeforeEach
    public void setUp() {
        closable = MockitoAnnotations.openMocks(this);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    }

    @AfterEach
    public void close() throws Exception {
        closable.close();
    }

    @Test
    public void testStopProducerWhenNoActiveProducer() throws Exception {
        String result = threadsController.stopProducer();
        assertEquals("No active producer to stop", result);
        verify(simpleProducer, never()).shutdown();
    }

    @Test
    public void testPublishNMessage() throws Exception {
        String topicName = "testTopic";
        int numberOfMessages = 10;
        CompletableFuture<String> future = CompletableFuture.completedFuture("Producer started for topic: " + topicName);
        CompletableFuture<String> result = threadsController.publishNMessage(numberOfMessages, topicName);
        assertEquals(future.getClass(), result.getClass());
    }

}
