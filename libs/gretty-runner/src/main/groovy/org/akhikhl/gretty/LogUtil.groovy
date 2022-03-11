package org.akhikhl.gretty

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.joran.ReconfigureOnChangeTask
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.CoreConstants
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.util.Duration
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

class LogUtil {

    /**
     * Configure Gretty's logging as part of the bootstrap process.
     *
     * The very first step after launching the child process consists of configuring the
     * debug level. This will allow Gretty to log diagnostic output <i>before</i> we receive
     * detailed logging configuration (level, file paths, etc.) from the parent process
     * via the network.
     *
     * Having the detailed logging configuration, we proceed to {@link LogUtil#configureLogging}.
     */
    static setLevel(boolean debugEnabled) {
        def logger = LoggerFactory.getLogger(LogUtil.class.getPackage().getName())
        if (logger instanceof Logger) {
            logger.level = debugEnabled ? Level.DEBUG : Level.INFO
        }
    }

    /**
     * Configure a default logging configuration for Gretty.
     *
     * Especially the implementation for the scan interval bases off
     * {@code ch.qos.logback.classic.gaffer.ConfigurationDelegate}.
     *
     * Additionally, below implementation tries to avoid all pitfalls from
     * https://stackoverflow.com/a/46121422/345057, in particular:
     * <ul>
     *     <li>forgetting to set the logging context</li>
     *     <li>sharing encoders across loggers</li>
     *     <li>forgetting to start an appender or encoder</li>
     * </ul>
     */
    static configureLogging(
            Level level,
            boolean consoleLogEnabled,
            boolean fileLogEnabled,
            String logFileName,
            String logDir,
            boolean grettyDebug
    ) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory()
        configureScanInterval(context)
        def appender = collectAppender(context, consoleLogEnabled, fileLogEnabled, logDir, logFileName)
        configureRootLogger(context, level, appender)
        configureWellKnownLoggers(context, grettyDebug)
    }

    private static configureScanInterval(LoggerContext _context, String scanPeriodStr = '30 seconds') {
        def task = new ReconfigureOnChangeTask().tap {
            context = _context
        }

        _context.putObject(CoreConstants.RECONFIGURE_ON_CHANGE_TASK, task)
        def duration = Duration.valueOf(scanPeriodStr)
        def future = _context.scheduledExecutorService.scheduleAtFixedRate(task, duration.milliseconds, duration.milliseconds, TimeUnit.MILLISECONDS)
        _context.addScheduledFuture(future)
    }

    private static configureRootLogger(LoggerContext _context, Level _level, List<Appender<ILoggingEvent>> _appender) {
        _context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).tap {
            level = _level
            for (appender in _appender) addAppender(appender)
        }
    }

    private static List<Appender<ILoggingEvent>> collectAppender(
            LoggerContext context,
            boolean consoleLogEnabled,
            boolean fileLogEnabled,
            String logDir,
            String logFileName
    ) {
        List<Appender<ILoggingEvent>> appender = []
        if (consoleLogEnabled) appender += consoleAppender(context)
        if (fileLogEnabled) appender += fileAppender(context, logDir, logFileName)
        return appender
    }

    private static Appender<ILoggingEvent> consoleAppender(LoggerContext _context) {
        def patternLayoutEncoder = new PatternLayoutEncoder().tap {
            context = _context
            pattern = '%-8date{HH:mm:ss} %-5level %msg%n'
            start()
        }

        return new ConsoleAppender<ILoggingEvent>().tap {
            context = _context
            name = 'CONSOLE'
            encoder = patternLayoutEncoder
            start()
        }
    }

    private static Appender<ILoggingEvent> fileAppender(LoggerContext _context, String _logDir, String _logFileName) {
        def _encoder = new PatternLayoutEncoder().tap {
            context = _context
            pattern = '%-8date{HH:mm:ss} %-5level %logger{35} - %msg%n'
            start()
        }

        def _appender = new RollingFileAppender<ILoggingEvent>().tap {
            context = _context
            name = 'FILE'
            encoder = _encoder
            file = "${_logDir}/${_logFileName}.log"
            append = true
        }

        def _rollingPolicy = new TimeBasedRollingPolicy().tap {
            context = _context
            parent = _appender
            fileNamePattern = "${_logDir}/${_logFileName}-%d{yyyy-MM-dd_HH}.log"
            maxHistory = 7
            start()
        }

        return _appender.tap {
            rollingPolicy = _rollingPolicy
            start()
        }
    }

    private static configureWellKnownLoggers(LoggerContext _context, boolean grettyDebug) {
        def logger = { String name, Level level ->
            _context.getLogger(name).level = level
        }

        logger 'org.akhikhl.gretty', grettyDebug ? Level.DEBUG : Level.INFO

        logger 'org.apache.catalina', Level.WARN
        logger 'org.apache.coyote', Level.WARN
        logger 'org.apache.jasper', Level.WARN
        logger 'org.apache.tomcat', Level.WARN

        logger 'org.eclipse.jetty', Level.WARN
        logger 'org.eclipse.jetty.annotations.AnnotationConfiguration', Level.ERROR
        logger 'org.eclipse.jetty.annotations.AnnotationParser', Level.ERROR
        logger 'org.eclipse.jetty.util.component.AbstractLifeCycle', Level.ERROR
    }
}
