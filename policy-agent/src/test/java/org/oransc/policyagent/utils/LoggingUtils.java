/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2019 Nordix Foundation
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

package org.oransc.policyagent.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.slf4j.LoggerFactory;

public class LoggingUtils {

    /**
     * Returns a ListAppender that contains all logging events. Call this method right before calling the tested
     * method.
     *
     * @return the log list appender for the given class.
     */
    public static ListAppender<ILoggingEvent> getLogListAppender(Class<?> logClass) {
        return getLogListAppender(logClass, Level.ALL);
    }

    /**
     * Returns a ListAppender that contains events for the given level. Call this method right before calling the tested
     * method.
     *
     * @param logClass class whose appender is wanted.
     * @param level the log level to log at.
     *
     * @return the log list appender for the given class logging on the given level.
     */
    public static ListAppender<ILoggingEvent> getLogListAppender(Class<?> logClass, Level level) {
        Logger logger = (Logger) LoggerFactory.getLogger(logClass);
        logger.setLevel(level);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        return listAppender;
    }
}
