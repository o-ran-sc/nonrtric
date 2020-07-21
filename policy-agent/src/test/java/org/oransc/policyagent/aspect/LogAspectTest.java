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


package org.oransc.policyagent.aspect;

import static ch.qos.logback.classic.Level.TRACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oransc.policyagent.utils.LoggingUtils;

@ExtendWith(MockitoExtension.class)
class LogAspectTest {
    @Rule
    MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    private LogAspect sampleAspect = new LogAspect();

    @Test
    void testExecutetimeTime_shouldLogTime() throws Throwable {
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(this.getClass());

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(LogAspect.class, TRACE);

        sampleAspect.executimeTime(proceedingJoinPoint);
        // 'proceed()' is called exactly once
        verify(proceedingJoinPoint, times(1)).proceed();
        // 'proceed(Object[])' is never called
        verify(proceedingJoinPoint, never()).proceed(null);

        assertThat(logAppender.list.get(0).getFormattedMessage()).startsWith("Execution time of");
    }

    @Test
    void testEntryLog_shouldLogEntry() throws Throwable {
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        String signature = "signature";
        when(methodSignature.getName()).thenReturn(signature);

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(LogAspect.class, TRACE);

        sampleAspect.entryLog(proceedingJoinPoint);

        assertThat(logAppender.list.get(0).getFormattedMessage()).isEqualTo("Entering method: " + signature);
    }

    @Test
    void testExitLog_shouldLogExit() throws Throwable {
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        String signature = "signature";
        when(methodSignature.getName()).thenReturn(signature);

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(LogAspect.class, TRACE);

        sampleAspect.exitLog(proceedingJoinPoint);

        assertThat(logAppender.list.get(0).getFormattedMessage()).isEqualTo("Exiting method: " + signature);
    }
}
