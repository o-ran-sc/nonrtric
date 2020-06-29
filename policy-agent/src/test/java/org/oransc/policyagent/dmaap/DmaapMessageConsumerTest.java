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

import static ch.qos.logback.classic.Level.WARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.time.Duration;
import java.util.LinkedList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.utils.LoggingUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class DmaapMessageConsumerTest {
    @Mock
    private ApplicationConfig applicationConfigMock;
    @Mock
    private AsyncRestClient messageRouterConsumerMock;
    @Mock
    private DmaapMessageHandler messageHandlerMock;

    private DmaapMessageConsumer messageConsumerUnderTest;

    @AfterEach
    void resetLogging() {
        LoggingUtils.getLogListAppender(DmaapMessageConsumer.class);
    }

    @Test
    void dmaapNotConfigured_thenSleepAndRetryUntilConfig() throws Exception {
        messageConsumerUnderTest = spy(new DmaapMessageConsumer(applicationConfigMock));

        doNothing().when(messageConsumerUnderTest).sleep(any(Duration.class));
        doReturn(false, false, false, true).when(messageConsumerUnderTest).isStopped();
        doReturn(false, true, true).when(messageConsumerUnderTest).isDmaapConfigured();
        doReturn(new LinkedList<>()).when(messageConsumerUnderTest).fetchAllMessages();

        messageConsumerUnderTest.start().join();

        InOrder orderVerifier = inOrder(messageConsumerUnderTest);
        orderVerifier.verify(messageConsumerUnderTest).sleep(DmaapMessageConsumer.TIME_BETWEEN_DMAAP_RETRIES);
        orderVerifier.verify(messageConsumerUnderTest).fetchAllMessages();
    }

    @Test
    void dmaapConfigurationRemoved_thenStopPollingDmaapSleepAndRetry() throws Exception {
        messageConsumerUnderTest = spy(new DmaapMessageConsumer(applicationConfigMock));

        doNothing().when(messageConsumerUnderTest).sleep(any(Duration.class));
        doReturn(false, false, false, false, true).when(messageConsumerUnderTest).isStopped();
        doReturn(true, true, false).when(messageConsumerUnderTest).isDmaapConfigured();
        doReturn(new LinkedList<>()).when(messageConsumerUnderTest).fetchAllMessages();

        messageConsumerUnderTest.start().join();

        InOrder orderVerifier = inOrder(messageConsumerUnderTest);
        orderVerifier.verify(messageConsumerUnderTest).fetchAllMessages();
        orderVerifier.verify(messageConsumerUnderTest).sleep(DmaapMessageConsumer.TIME_BETWEEN_DMAAP_RETRIES);
    }

    @Test
    void dmaapConfiguredAndNoMessages_thenPollOnce() throws Exception {
        setUpMrConfig();

        messageConsumerUnderTest = spy(new DmaapMessageConsumer(applicationConfigMock));

        Mono<ResponseEntity<String>> response = Mono.empty();

        doReturn(false, true).when(messageConsumerUnderTest).isStopped();
        doReturn(messageRouterConsumerMock).when(messageConsumerUnderTest).getMessageRouterConsumer();
        doReturn(response).when(messageRouterConsumerMock).getForEntity(any());

        messageConsumerUnderTest.start().join();

        verify(messageRouterConsumerMock).getForEntity(any());
        verifyNoMoreInteractions(messageRouterConsumerMock);
    }

    @Test
    void dmaapConfiguredAndErrorGettingMessages_thenLogWarningAndSleep() throws Exception {
        setUpMrConfig();

        messageConsumerUnderTest = spy(new DmaapMessageConsumer(applicationConfigMock));

        doNothing().when(messageConsumerUnderTest).sleep(any(Duration.class));
        doReturn(false, true).when(messageConsumerUnderTest).isStopped();
        doReturn(messageRouterConsumerMock).when(messageConsumerUnderTest).getMessageRouterConsumer();

        Mono<ResponseEntity<String>> response = Mono.just(new ResponseEntity<>("Error", HttpStatus.BAD_REQUEST));
        when(messageRouterConsumerMock.getForEntity(any())).thenReturn(response);

        final ListAppender<ILoggingEvent> logAppender =
                LoggingUtils.getLogListAppender(DmaapMessageConsumer.class, WARN);

        messageConsumerUnderTest.start().join();

        assertThat(logAppender.list.get(0).getFormattedMessage())
                .isEqualTo("Cannot fetch because of Error respons: 400 BAD_REQUEST Error");

        verify(messageConsumerUnderTest).sleep(DmaapMessageConsumer.TIME_BETWEEN_DMAAP_RETRIES);
    }

    @Test
    void dmaapConfiguredAndOneMessage_thenPollOnceAndProcessMessage() throws Exception {
        // The message from MR is here an array of Json objects
        setUpMrConfig();
        messageConsumerUnderTest = spy(new DmaapMessageConsumer(applicationConfigMock));

        String message = "{\"apiVersion\":\"1.0\"," //
                + "\"operation\":\"GET\"," //
                + "\"correlationId\":\"1592341013115594000\"," //
                + "\"originatorId\":\"849e6c6b420\"," //
                + "\"payload\":{}," //
                + "\"requestId\":\"23343221\", " //
                + "\"target\":\"policy-agent\"," //
                + "\"timestamp\":\"2020-06-16 20:56:53.115665\"," //
                + "\"type\":\"request\"," //
                + "\"url\":\"/rics\"}";
        String messages = "[" + message + "]";

        doReturn(false, true).when(messageConsumerUnderTest).isStopped();
        doReturn(messageRouterConsumerMock).when(messageConsumerUnderTest).getMessageRouterConsumer();

        Mono<ResponseEntity<String>> response = Mono.just(new ResponseEntity<>(messages, HttpStatus.OK));
        when(messageRouterConsumerMock.getForEntity(any())).thenReturn(response);

        doReturn(messageHandlerMock).when(messageConsumerUnderTest).getDmaapMessageHandler();

        messageConsumerUnderTest.start().join();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(messageHandlerMock).handleDmaapMsg(captor.capture());
        String messageAfterJsonParsing = captor.getValue();
        assertThat(messageAfterJsonParsing).contains("apiVersion");

        verifyNoMoreInteractions(messageHandlerMock);
    }

    @Test
    void dmaapConfiguredAndOneMessage_thenPollOnceAndProcessMessage2() throws Exception {
        // The message from MR is here an array of String (which is the case when the MR
        // simulator is used)
        setUpMrConfig();
        messageConsumerUnderTest = spy(new DmaapMessageConsumer(applicationConfigMock));

        doReturn(false, true).when(messageConsumerUnderTest).isStopped();
        doReturn(messageRouterConsumerMock).when(messageConsumerUnderTest).getMessageRouterConsumer();

        Mono<ResponseEntity<String>> response = Mono.just(new ResponseEntity<>("[\"aMessage\"]", HttpStatus.OK));
        when(messageRouterConsumerMock.getForEntity(any())).thenReturn(response);

        doReturn(messageHandlerMock).when(messageConsumerUnderTest).getDmaapMessageHandler();

        messageConsumerUnderTest.start().join();

        verify(messageHandlerMock).handleDmaapMsg("aMessage");
        verifyNoMoreInteractions(messageHandlerMock);
    }

    private void setUpMrConfig() {
        when(applicationConfigMock.getDmaapConsumerTopicUrl()).thenReturn("url");
        when(applicationConfigMock.getDmaapProducerTopicUrl()).thenReturn("url");
    }
}
