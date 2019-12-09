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
     * Returns a ListAppender that contains all logging events. Call this method at the very beginning of the test
     */
    public static ListAppender<ILoggingEvent> getLogListAppender(Class<?> logClass) {
        return getLogListAppender(logClass, false);
    }

    /**
     * Returns a ListAppender that contains all logging events. Call this method at the very beginning of the test
     *
     * @param logClass class whose appender is wanted.
     * @param allLevels true if all log levels should be activated.
     */
    public static ListAppender<ILoggingEvent> getLogListAppender(Class<?> logClass, boolean allLevels) {
        Logger logger = (Logger) LoggerFactory.getLogger(logClass);
        if (allLevels) {
            logger.setLevel(Level.ALL);
        }
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        return listAppender;
    }
}
