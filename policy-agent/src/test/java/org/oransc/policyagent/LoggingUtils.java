package org.oransc.policyagent;

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
