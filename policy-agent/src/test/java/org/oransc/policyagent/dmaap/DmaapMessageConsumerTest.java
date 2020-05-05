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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.dmaap.mr.client.MRBatchingPublisher;
import org.onap.dmaap.mr.client.MRConsumer;
import org.onap.dmaap.mr.client.response.MRConsumerResponse;
import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.tasks.RefreshConfigTask;
import org.oransc.policyagent.utils.LoggingUtils;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class DmaapMessageConsumerTest {
    @Mock
    private ApplicationConfig applicationConfigMock;
    @Mock
    private MRConsumer messageRouterConsumerMock;
    @Mock
    private DmaapMessageHandler messageHandlerMock;

    private DmaapMessageConsumer messageConsumerUnderTest;

    @AfterEach
    public void resetLogging() {
        LoggingUtils.getLogListAppender(DmaapMessageConsumer.class);
    }

    @Test
    public void dmaapNotConfigured_thenSleepAndRetryUntilConfig() throws Exception {
        messageConsumerUnderTest = spy(new DmaapMessageConsumer(applicationConfigMock));

        doNothing().when(messageConsumerUnderTest).sleep(any(Duration.class));
        doReturn(false, false, false, true).when(messageConsumerUnderTest).isStopped();
        doReturn(false, true, true).when(messageConsumerUnderTest).isDmaapConfigured();
        doReturn(new LinkedList<>()).when(messageConsumerUnderTest).fetchAllMessages();

        messageConsumerUnderTest.start().join();

        verify(messageConsumerUnderTest).sleep(RefreshConfigTask.CONFIG_REFRESH_INTERVAL);
        verify(messageConsumerUnderTest).fetchAllMessages();
    }

    @Test
    public void dmaapConfiguredAndNoMessages_thenPollOnce() throws Exception {
        setUpMrConfig();

        messageConsumerUnderTest = spy(new DmaapMessageConsumer(applicationConfigMock));

        MRConsumerResponse response = new MRConsumerResponse();
        response.setResponseCode(Integer.toString(HttpStatus.OK.value()));
        response.setActualMessages(Collections.emptyList());

        doReturn(false, false, true).when(messageConsumerUnderTest).isStopped();
        doReturn(messageRouterConsumerMock).when(messageConsumerUnderTest)
            .getMessageRouterConsumer(any(Properties.class));
        when(messageRouterConsumerMock.fetchWithReturnConsumerResponse()).thenReturn(response);

        messageConsumerUnderTest.start().join();

        verify(messageRouterConsumerMock).fetchWithReturnConsumerResponse();
        verifyNoMoreInteractions(messageRouterConsumerMock);
    }

    @Test
    public void dmaapConfiguredAndErrorGettingMessages_thenLogWarningAndSleep() throws Exception {
        setUpMrConfig();

        messageConsumerUnderTest = spy(new DmaapMessageConsumer(applicationConfigMock));

        doNothing().when(messageConsumerUnderTest).sleep(any(Duration.class));
        doReturn(false, false, true).when(messageConsumerUnderTest).isStopped();
        doReturn(messageRouterConsumerMock).when(messageConsumerUnderTest)
            .getMessageRouterConsumer(any(Properties.class));

        MRConsumerResponse response = new MRConsumerResponse();
        int responseCode = HttpStatus.BAD_REQUEST.value();
        response.setResponseCode(Integer.toString(responseCode));
        String responseMessage = "Error";
        response.setResponseMessage(responseMessage);
        when(messageRouterConsumerMock.fetchWithReturnConsumerResponse()).thenReturn(response);

        final ListAppender<ILoggingEvent> logAppender =
            LoggingUtils.getLogListAppender(DmaapMessageConsumer.class, WARN);

        messageConsumerUnderTest.start().join();

        assertThat(logAppender.list.toString()
            .contains("Cannot fetch because of Error respons " + responseCode + " " + responseMessage + " from DMaaP."))
                .isTrue();

        verify(messageConsumerUnderTest).sleep(DmaapMessageConsumer.TIME_BETWEEN_DMAAP_RETRIES);
    }

    @Test
    public void dmaapConfiguredAndOneMessage_thenPollOnceAndProcessMessage() throws Exception {
        Properties properties = setUpMrConfig();

        messageConsumerUnderTest = spy(new DmaapMessageConsumer(applicationConfigMock));

        doReturn(false, false, true).when(messageConsumerUnderTest).isStopped();
        doReturn(messageRouterConsumerMock).when(messageConsumerUnderTest)
            .getMessageRouterConsumer(any(Properties.class));

        MRConsumerResponse response = new MRConsumerResponse();
        response.setResponseCode(Integer.toString(HttpStatus.OK.value()));
        String responseMessage = "message";
        List<String> messages = Arrays.asList(responseMessage);
        response.setActualMessages(messages);
        when(messageRouterConsumerMock.fetchWithReturnConsumerResponse()).thenReturn(response);

        doReturn(messageHandlerMock).when(messageConsumerUnderTest)
            .createDmaapMessageHandler(any(AsyncRestClient.class), any(MRBatchingPublisher.class));

        AsyncRestClient restClientMock = mock(AsyncRestClient.class);
        doReturn(restClientMock).when(messageConsumerUnderTest).createRestClient(anyString());

        MRBatchingPublisher publisherMock = mock(MRBatchingPublisher.class);
        doReturn(publisherMock).when(messageConsumerUnderTest).getMessageRouterPublisher(any(Properties.class));

        messageConsumerUnderTest.start().join();

        verify(messageConsumerUnderTest).createRestClient("https://localhost:0");
        verify(messageConsumerUnderTest).getMessageRouterPublisher(properties);

        verify(messageHandlerMock).handleDmaapMsg(responseMessage);
        verifyNoMoreInteractions(messageHandlerMock);
    }

    private Properties setUpMrConfig() {
        Properties properties = new Properties();
        properties.put("key", "value");
        when(applicationConfigMock.getDmaapConsumerConfig()).thenReturn(properties);
        when(applicationConfigMock.getDmaapPublisherConfig()).thenReturn(properties);
        return properties;
    }
}
