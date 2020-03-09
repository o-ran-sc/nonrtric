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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.dmaap.mr.client.MRBatchingPublisher;
import org.onap.dmaap.mr.client.MRConsumer;
import org.onap.dmaap.mr.client.response.MRConsumerResponse;
import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.utils.LoggingUtils;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class DmaapMessageConsumerTest {
    final Duration TIME_BETWEEN_DMAAP_POLLS = Duration.ofSeconds(10);

    @Mock
    private ApplicationConfig applicationConfigMock;
    @Mock
    private MRConsumer messageRouterConsumerMock;
    @Mock
    private DmaapMessageHandler messageHandlerMock;

    private DmaapMessageConsumer messageConsumerUnderTest;

    @Test
    public void dmaapNotConfigured_thenDoNothing() {
        messageConsumerUnderTest = spy(new DmaapMessageConsumer(applicationConfigMock));

        doReturn(true).when(messageConsumerUnderTest).sleep(any(Duration.class));

        messageConsumerUnderTest.run();

        verify(messageConsumerUnderTest).sleep(TIME_BETWEEN_DMAAP_POLLS);
        verify(applicationConfigMock).getDmaapConsumerConfig();
        verify(applicationConfigMock).getDmaapPublisherConfig();
        verifyNoMoreInteractions(applicationConfigMock);
    }

    @Test
    public void dmaapConfiguredAndNoMessages_thenPollOnce() throws Exception {
        messageConsumerUnderTest = spy(new DmaapMessageConsumer(applicationConfigMock));

        doReturn(true, false).when(messageConsumerUnderTest).sleep(any(Duration.class));

        Properties properties = new Properties();
        properties.put("key", "value");
        when(applicationConfigMock.getDmaapConsumerConfig()).thenReturn(properties);
        when(applicationConfigMock.getDmaapPublisherConfig()).thenReturn(properties);

        MRConsumerResponse response = new MRConsumerResponse();
        response.setResponseCode(Integer.toString(HttpStatus.OK.value()));
        response.setActualMessages(Collections.emptyList());

        doReturn(messageRouterConsumerMock).when(messageConsumerUnderTest)
            .getMessageRouterConsumer(any(Properties.class));
        doReturn(response).when(messageRouterConsumerMock).fetchWithReturnConsumerResponse();

        messageConsumerUnderTest.run();

        verify(messageConsumerUnderTest, times(2)).sleep(TIME_BETWEEN_DMAAP_POLLS);

        verify(applicationConfigMock, times(2)).getDmaapConsumerConfig();
        verify(applicationConfigMock).getDmaapPublisherConfig();
        verifyNoMoreInteractions(applicationConfigMock);

        verify(messageRouterConsumerMock).fetchWithReturnConsumerResponse();
        verifyNoMoreInteractions(messageRouterConsumerMock);
    }

    @Test
    public void dmaapConfiguredAndErrorGettingMessages_thenLogWarning() throws Exception {
        messageConsumerUnderTest = spy(new DmaapMessageConsumer(applicationConfigMock));

        doReturn(true, false).when(messageConsumerUnderTest).sleep(any(Duration.class));

        Properties properties = new Properties();
        properties.put("key", "value");
        when(applicationConfigMock.getDmaapConsumerConfig()).thenReturn(properties);
        when(applicationConfigMock.getDmaapPublisherConfig()).thenReturn(properties);

        MRConsumerResponse response = new MRConsumerResponse();
        response.setResponseCode(Integer.toString(HttpStatus.BAD_REQUEST.value()));
        response.setResponseMessage("Error");
        doReturn(messageRouterConsumerMock).when(messageConsumerUnderTest)
            .getMessageRouterConsumer(any(Properties.class));
        doReturn(response).when(messageRouterConsumerMock).fetchWithReturnConsumerResponse();

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(DmaapMessageConsumer.class);

        messageConsumerUnderTest.run();

        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(
            logAppender.list.toString().contains(": cannot fetch because of Error respons 400 Error from DMaaP."))
                .isTrue();
    }

    @Test
    public void dmaapConfiguredAndOneMessage_thenPollOnceAndProcessMessage() throws Exception {
        messageConsumerUnderTest = spy(new DmaapMessageConsumer(applicationConfigMock));

        doReturn(true, false).when(messageConsumerUnderTest).sleep(any(Duration.class));

        Properties properties = new Properties();
        properties.put("key", "value");
        when(applicationConfigMock.getDmaapConsumerConfig()).thenReturn(properties);
        when(applicationConfigMock.getDmaapPublisherConfig()).thenReturn(properties);

        MRConsumerResponse response = new MRConsumerResponse();
        response.setResponseCode(Integer.toString(HttpStatus.OK.value()));
        List<String> messages = Arrays.asList("message");
        response.setActualMessages(messages);

        doReturn(messageRouterConsumerMock).when(messageConsumerUnderTest)
            .getMessageRouterConsumer(any(Properties.class));
        doReturn(response).when(messageRouterConsumerMock).fetchWithReturnConsumerResponse();

        doReturn(messageHandlerMock).when(messageConsumerUnderTest)
            .createDmaapMessageHandler(any(AsyncRestClient.class), any(MRBatchingPublisher.class));

        AsyncRestClient restClientMock = mock(AsyncRestClient.class);
        doReturn(restClientMock).when(messageConsumerUnderTest).createRestClient(anyString());

        MRBatchingPublisher publisherMock = mock(MRBatchingPublisher.class);
        doReturn(publisherMock).when(messageConsumerUnderTest).getMessageRouterPublisher(any(Properties.class));

        messageConsumerUnderTest.run();

        verify(messageConsumerUnderTest).createRestClient("http://localhost:0");
        verify(messageConsumerUnderTest).getMessageRouterPublisher(properties);

        verify(messageHandlerMock).handleDmaapMsg("message");
        verifyNoMoreInteractions(messageHandlerMock);
    }
}
