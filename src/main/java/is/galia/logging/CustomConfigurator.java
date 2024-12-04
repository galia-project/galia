/*
 * Copyright © 2024 Baird Creek Software LLC
 *
 * Licensed under the PolyForm Noncommercial License, version 1.0.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://polyformproject.org/licenses/noncommercial/1.0.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package is.galia.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.net.ssl.SSLConfiguration;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.spi.ContextAwareBase;
import is.galia.Application;
import is.galia.config.Configuration;
import is.galia.config.Key;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import net.logstash.logback.appender.LogstashUdpSocketAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import net.logstash.logback.layout.LogstashLayout;
import org.slf4j.LoggerFactory;

/**
 * <p>Configures Logback programmatically.</p>
 *
 * <p>Logging works differently in different environments. For normal use in
 * development and production, we want it to work normally, as specified in
 * the application configuration. But in testing, we don't want any logging. As
 * for how that is accomplished:</p>
 *
 * <h2>Development &amp; production</h2>
 *
 * <p>Logback supports a variety of configuration methods, but regardless, it
 * really wants to take the reins of its own initialization and not have to
 * wait for the application main method. This is a problem because the logging
 * configuration is part of the application configuration, which doesn't get
 * read until the {@code -config} command-line argument has been handled, long
 * after Logback would want to have initialized itself.</p>
 *
 * <p>The Logback docs suggest using a custom {@link Configurator} (like this
 * one) and telling Logback about it using the {@link java.util.ServiceLoader}
 * mechanism, so that it can be loaded at startup. But since we don't want it
 * to load at startup, we don't use that mechanism. Instead we reset the logger
 * context manually when ready using {@link #resetLoggerContext()}.</p>
 *
 * <h3>Testing</h3>
 *
 * <p>None of the above applies in testing. In order to disable logging in
 * testing, we put a skeletal {@literal logback.xml} file in {@literal
 * src/main/resources}. This serves as a &quot;poison pill&quot; that
 * effectively silences Logback.</p>
 *
 * <p>Why {@literal src/main/resources} and not {@literal src/test/resources}?
 * Because the former will cause Logback to not fall back to its own default
 * configuration in development/production.</p>
 */
public class CustomConfigurator extends ContextAwareBase
        implements Configurator {

    private static final String LOGSTASH_FORMAT = "logstash";
    private static final String STANDARD_FORMAT = "standard";
    private static final String LOGSTASH_CUSTOM_FIELDS = "{" +
            "\"app_name\":\"" + Application.getName() + "\"," +
            "\"app_version\":\"" + Application.getVersion() + "\"" +
            "}";

    private LoggerContext loggerContext;
    private Logger rootLogger;

    /**
     * To be invoked when the {@link Configuration#forApplication() application
     * configuration} becomes available at startup. (See class documentation.)
     */
    public static void resetLoggerContext() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        CustomConfigurator configurator = new CustomConfigurator();
        configurator.setContext(loggerContext);
        loggerContext.reset();
        configurator.configure(loggerContext);
    }

    @Override
    public ExecutionStatus configure(final LoggerContext loggerContext) {
        // We don't want to log anything when testing.
        if (!Application.TEST_ENVIRONMENT.equals(
                System.getProperty(Application.ENVIRONMENT_VM_ARGUMENT))) {
            final Configuration config = Configuration.forApplication();
            this.loggerContext = loggerContext;
            this.rootLogger    = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
            this.rootLogger.setLevel(Level.valueOf(config.getString(Key.APPLICATION_LOG_LEVEL)));
            addAccessLogAppenders();
            addApplicationLogAppenders();
        }
        return ExecutionStatus.NEUTRAL;
    }

    private void addAccessLogAppenders() {
        addAccessConsoleLogAppender();
        addAccessFileLogAppender();
        addAccessRollingFileLogAppender();
        addAccessSyslogLogAppender();
    }

    private void addAccessConsoleLogAppender() {
        final Configuration config = Configuration.forApplication();
        if (config.getBoolean(Key.ACCESS_LOG_CONSOLEAPPENDER_ENABLED, false)) {
            ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
            appender.setName("AccessStdoutLog");
            appender.setContext(loggerContext);
            Filter<ILoggingEvent> filter = new AccessLogFilter();
            filter.setContext(loggerContext);
            filter.start();
            appender.addFilter(filter);
            PatternLayout layout = new PatternLayout();
            layout.setPattern("%msg%n"); // already in W3C Extended Log File Format
            layout.setContext(loggerContext);
            layout.start();
            LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
            encoder.setLayout(layout);
            encoder.setContext(loggerContext);
            encoder.start();
            appender.setEncoder(encoder);
            appender.setLayout(layout);
            appender.start();
            rootLogger.addAppender(appender);
        }
    }

    private void addAccessFileLogAppender() {
        final Configuration config = Configuration.forApplication();
        if (config.getBoolean(Key.ACCESS_LOG_FILEAPPENDER_ENABLED, false)) {
            FileAppender<ILoggingEvent> appender = new FileAppender<>();
            appender.setName("AccessFileLog");
            appender.setContext(loggerContext);
            appender.setAppend(true);
            Filter<ILoggingEvent> filter = new AccessLogFilter();
            filter.setContext(loggerContext);
            filter.start();
            appender.addFilter(filter);
            appender.setFile(config.getString(Key.ACCESS_LOG_FILEAPPENDER_PATHNAME));
            PatternLayout layout = new PatternLayout();
            layout.setPattern("%msg%n"); // already in W3C Extended Log File Format
            layout.setContext(loggerContext);
            layout.start();
            LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
            encoder.setLayout(layout);
            encoder.setContext(loggerContext);
            encoder.start();
            appender.setEncoder(encoder);
            appender.setLayout(layout);
            appender.start();
            rootLogger.addAppender(appender);
        }
    }

    private void addAccessRollingFileLogAppender() {
        final Configuration config = Configuration.forApplication();
        if (config.getBoolean(Key.ACCESS_LOG_ROLLINGFILEAPPENDER_ENABLED, false)) {
            RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
            appender.setName("AccessRollingFileLog");
            appender.setContext(loggerContext);
            appender.setAppend(true);
            Filter<ILoggingEvent> filter = new AccessLogFilter();
            filter.setContext(loggerContext);
            filter.start();
            appender.addFilter(filter);
            appender.setFile(config.getString(Key.ACCESS_LOG_ROLLINGFILEAPPENDER_PATHNAME));
            if ("TimeBasedRollingPolicy".equals(config.getString(Key.ACCESS_LOG_ROLLINGFILEAPPENDER_POLICY))) {
                TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
                policy.setFileNamePattern(config.getString(Key.ACCESS_LOG_ROLLINGFILEAPPENDER_FILENAME_PATTERN));
                policy.setMaxHistory(config.getInt(Key.ACCESS_LOG_ROLLINGFILEAPPENDER_MAX_HISTORY));
                policy.setParent(appender);
                policy.setContext(loggerContext);
                policy.start();
            }
            PatternLayout layout = new PatternLayout();
            layout.setPattern("%msg%n"); // already in W3C Extended Log File Format
            layout.setContext(loggerContext);
            layout.start();
            LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
            encoder.setLayout(layout);
            encoder.setContext(loggerContext);
            encoder.start();
            appender.setEncoder(encoder);
            appender.setLayout(layout);
            appender.start();
            rootLogger.addAppender(appender);
        }
    }

    private void addAccessSyslogLogAppender() {
        final Configuration config = Configuration.forApplication();
        if (config.getBoolean(Key.ACCESS_LOG_SYSLOGAPPENDER_ENABLED, false)) {
            SyslogAppender appender = new SyslogAppender();
            appender.setName("AccessSyslogLog");
            appender.setContext(loggerContext);
            Filter<ILoggingEvent> filter = new AccessLogFilter();
            filter.setContext(loggerContext);
            filter.start();
            appender.addFilter(filter);
            appender.setSyslogHost(config.getString(Key.ACCESS_LOG_SYSLOGAPPENDER_HOST));
            appender.setPort(config.getInt(Key.ACCESS_LOG_SYSLOGAPPENDER_PORT));
            appender.setFacility(config.getString(Key.ACCESS_LOG_SYSLOGAPPENDER_FACILITY));
            appender.start();
            rootLogger.addAppender(appender);
        }
    }

    private void addApplicationLogAppenders() {
        addApplicationConsoleLogAppenders();
        addApplicationFileLogAppenders();
        addApplicationRollingFileLogAppenders();
        addApplicationLogstashLogAppender();
        addApplicationSyslogLogAppender();
    }

    private void addApplicationConsoleLogAppenders() {
        final Configuration config = Configuration.forApplication();
        if (config.getBoolean(Key.APPLICATION_LOG_CONSOLEAPPENDER_ENABLED, true)) {
            // Non-error log
            ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
            appender.setName("AppStdoutLog");
            appender.setContext(loggerContext);
            Filter<ILoggingEvent> filter = new ApplicationLogFilter();
            filter.setContext(loggerContext);
            filter.start();
            appender.addFilter(filter);
            filter = new StandardOutputLogFilter();
            filter.setContext(loggerContext);
            filter.start();
            appender.addFilter(filter);
            switch (config.getString(Key.APPLICATION_LOG_CONSOLEAPPENDER_FORMAT, STANDARD_FORMAT)) {
                case LOGSTASH_FORMAT -> {
                    LogstashEncoder encoder = new LogstashEncoder();
                    encoder.setIncludeContext(false);
                    encoder.setCustomFields(LOGSTASH_CUSTOM_FIELDS);
                    encoder.start();
                    appender.setEncoder(encoder);
                }
                default -> {
                    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
                    String pattern = config.getBoolean(Key.APPLICATION_LOG_CONSOLEAPPENDER_COLOR, true)
                            ? "%d{HH:mm:ss.SSS} [%thread] %highlight(%-5level) %cyan(%logger{15}) - %msg%n"
                            : "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{15} - %msg%n";
                    encoder.setPattern(pattern);
                    encoder.setContext(loggerContext);
                    encoder.start();
                    appender.setEncoder(encoder);
                }
            }
            appender.start();
            rootLogger.addAppender(appender);

            // Error log
            appender = new ConsoleAppender<>();
            appender.setName("AppStderrLog");
            appender.setContext(loggerContext);
            appender.setTarget("System.err");
            filter = new ApplicationLogFilter();
            filter.setContext(loggerContext);
            filter.start();
            appender.addFilter(filter);
            filter = new StandardErrorLogFilter();
            filter.setContext(loggerContext);
            filter.start();
            appender.addFilter(filter);
            switch (config.getString(Key.APPLICATION_LOG_CONSOLEAPPENDER_FORMAT, STANDARD_FORMAT)) {
                case LOGSTASH_FORMAT -> {
                    LogstashEncoder encoder = new LogstashEncoder();
                    encoder.setIncludeContext(false);
                    encoder.setCustomFields(LOGSTASH_CUSTOM_FIELDS);
                    encoder.start();
                    appender.setEncoder(encoder);
                }
                default -> {
                    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
                    encoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{15} - %msg%n");
                    encoder.setContext(loggerContext);
                    encoder.start();
                    appender.setEncoder(encoder);
                }
            }
            appender.start();
            rootLogger.addAppender(appender);
        }
    }

    private void addApplicationFileLogAppenders() {
        final Configuration config = Configuration.forApplication();
        if (config.getBoolean(Key.APPLICATION_LOG_FILEAPPENDER_ENABLED, false)) {
            // Non-error log
            FileAppender<ILoggingEvent> appender = new FileAppender<>();
            appender.setName("AppFileLog");
            appender.setContext(loggerContext);
            appender.setAppend(true);
            Filter<ILoggingEvent> filter = new ApplicationLogFilter();
            filter.setContext(loggerContext);
            filter.start();
            appender.addFilter(filter);
            appender.setFile(config.getString(Key.APPLICATION_LOG_FILEAPPENDER_PATHNAME));
            switch (config.getString(Key.APPLICATION_LOG_FILEAPPENDER_FORMAT, STANDARD_FORMAT)) {
                case LOGSTASH_FORMAT -> {
                    LogstashEncoder encoder = new LogstashEncoder();
                    encoder.setIncludeContext(false);
                    encoder.setCustomFields(LOGSTASH_CUSTOM_FIELDS);
                    encoder.start();
                    appender.setEncoder(encoder);
                }
                default -> {
                    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
                    encoder.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
                    encoder.setContext(loggerContext);
                    encoder.start();
                    appender.setEncoder(encoder);
                }
            }
            appender.start();
            rootLogger.addAppender(appender);
        }
        if (config.getBoolean(Key.ERROR_LOG_FILEAPPENDER_ENABLED, false)) {
            // Error log
            FileAppender<ILoggingEvent> appender = new FileAppender<>();
            appender.setName("ErrorFileLog");
            appender.setContext(loggerContext);
            appender.setAppend(true);
            Filter<ILoggingEvent> filter = new StandardErrorLogFilter();
            filter.setContext(loggerContext);
            filter.start();
            appender.addFilter(filter);
            appender.setFile(config.getString(Key.ERROR_LOG_FILEAPPENDER_PATHNAME));
            switch (config.getString(Key.ERROR_LOG_FILEAPPENDER_FORMAT, STANDARD_FORMAT)) {
                case LOGSTASH_FORMAT -> {
                    LogstashEncoder encoder = new LogstashEncoder();
                    encoder.setIncludeContext(false);
                    encoder.setCustomFields(LOGSTASH_CUSTOM_FIELDS);
                    encoder.start();
                    appender.setEncoder(encoder);
                }
                default -> {
                    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
                    encoder.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
                    encoder.setContext(loggerContext);
                    encoder.start();
                    appender.setEncoder(encoder);
                }
            }
            appender.start();
            rootLogger.addAppender(appender);
        }
    }

    private void addApplicationRollingFileLogAppenders() {
        final Configuration config = Configuration.forApplication();
        if (config.getBoolean(Key.APPLICATION_LOG_ROLLINGFILEAPPENDER_ENABLED, false)) {
            // Non-error log
            RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
            appender.setName("AppRollingFileLog");
            appender.setContext(loggerContext);
            appender.setAppend(true);
            appender.addFilter(new ApplicationLogFilter());
            appender.setFile(config.getString(Key.APPLICATION_LOG_ROLLINGFILEAPPENDER_PATHNAME));
            if ("TimeBasedRollingPolicy".equals(config.getString(Key.APPLICATION_LOG_ROLLINGFILEAPPENDER_POLICY))) {
                TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
                policy.setFileNamePattern(config.getString(Key.APPLICATION_LOG_ROLLINGFILEAPPENDER_FILENAME_PATTERN));
                policy.setMaxHistory(config.getInt(Key.APPLICATION_LOG_ROLLINGFILEAPPENDER_MAX_HISTORY));
                policy.setParent(appender);
                policy.setContext(loggerContext);
                policy.start();
                appender.setRollingPolicy(policy);
            }
            switch (config.getString(Key.APPLICATION_LOG_ROLLINGFILEAPPENDER_FORMAT, STANDARD_FORMAT)) {
                case LOGSTASH_FORMAT -> {
                    LogstashEncoder encoder = new LogstashEncoder();
                    encoder.setIncludeContext(false);
                    encoder.setCustomFields(LOGSTASH_CUSTOM_FIELDS);
                    encoder.start();
                    appender.setEncoder(encoder);
                }
                default -> {
                    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
                    encoder.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
                    encoder.setContext(loggerContext);
                    encoder.start();
                    appender.setEncoder(encoder);
                }
            }
            appender.start();
            rootLogger.addAppender(appender);
        }
        if (config.getBoolean(Key.ERROR_LOG_ROLLINGFILEAPPENDER_ENABLED, false)) {
            // Error log
            RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
            appender.setName("ErrorRollingFileLog");
            appender.setContext(loggerContext);
            appender.setAppend(true);
            Filter<ILoggingEvent> filter = new StandardErrorLogFilter();
            filter.setContext(loggerContext);
            filter.start();
            appender.addFilter(filter);
            appender.setFile(config.getString(Key.ERROR_LOG_ROLLINGFILEAPPENDER_PATHNAME));
            if ("TimeBasedRollingPolicy".equals(config.getString(Key.ERROR_LOG_ROLLINGFILEAPPENDER_POLICY))) {
                TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
                policy.setFileNamePattern(config.getString(Key.ERROR_LOG_ROLLINGFILEAPPENDER_FILENAME_PATTERN));
                policy.setMaxHistory(config.getInt(Key.ERROR_LOG_ROLLINGFILEAPPENDER_MAX_HISTORY));
                policy.setParent(appender);
                policy.setContext(loggerContext);
                policy.start();
                appender.setRollingPolicy(policy);
            }
            switch (config.getString(Key.ERROR_LOG_ROLLINGFILEAPPENDER_FORMAT, STANDARD_FORMAT)) {
                case LOGSTASH_FORMAT -> {
                    LogstashEncoder encoder = new LogstashEncoder();
                    encoder.setIncludeContext(false);
                    encoder.setCustomFields(LOGSTASH_CUSTOM_FIELDS);
                    encoder.start();
                    appender.setEncoder(encoder);
                }
                default -> {
                    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
                    encoder.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
                    encoder.setContext(loggerContext);
                    encoder.start();
                    appender.setEncoder(encoder);
                }
            }
            appender.start();
            rootLogger.addAppender(appender);
        }
    }

    private void addApplicationLogstashLogAppender() {
        final Configuration config = Configuration.forApplication();
        if (config.getBoolean(Key.APPLICATION_LOG_LOGSTASHAPPENDER_ENABLED, false)) {
            switch (config.getString(Key.APPLICATION_LOG_LOGSTASHAPPENDER_PROTOCOL)) {
                case "UDP" -> {
                    LogstashUdpSocketAppender appender = new LogstashUdpSocketAppender();
                    appender.setName("AppLogstashLog");
                    appender.setContext(loggerContext);
                    Filter<ILoggingEvent> filter = new ApplicationLogFilter();
                    filter.setContext(loggerContext);
                    filter.start();
                    appender.addFilter(filter);
                    appender.setHost(config.getString(Key.APPLICATION_LOG_LOGSTASHAPPENDER_HOST));
                    appender.setPort(config.getInt(Key.APPLICATION_LOG_LOGSTASHAPPENDER_PORT));
                    appender.setLayout(new LogstashLayout());
                    appender.start();
                    rootLogger.addAppender(appender);
                }
                case "TCP" -> {
                    LogstashTcpSocketAppender appender = new LogstashTcpSocketAppender();
                    appender.setName("AppLogstashLog");
                    appender.setContext(loggerContext);
                    Filter<ILoggingEvent> filter = new ApplicationLogFilter();
                    filter.setContext(loggerContext);
                    filter.start();
                    appender.addFilter(filter);
                    String destination = config.getString(Key.APPLICATION_LOG_LOGSTASHAPPENDER_HOST) +
                            config.getString(Key.APPLICATION_LOG_LOGSTASHAPPENDER_PORT);
                    appender.addDestination(destination);
                    LogstashEncoder encoder = new LogstashEncoder();
                    encoder.setIncludeContext(false);
                    encoder.setCustomFields(LOGSTASH_CUSTOM_FIELDS);
                    encoder.start();
                    appender.setEncoder(encoder);
                    if (config.getBoolean(Key.APPLICATION_LOG_LOGSTASHAPPENDER_SSL, false)) {
                        appender.setSsl(new SSLConfiguration());
                    }
                    appender.start();
                    rootLogger.addAppender(appender);
                }
                default ->
                    throw new IllegalArgumentException("Unsupported value for " +
                            Key.APPLICATION_LOG_LOGSTASHAPPENDER_PROTOCOL);
            }
        }
    }

    private void addApplicationSyslogLogAppender() {
        final Configuration config = Configuration.forApplication();
        if (config.getBoolean(Key.APPLICATION_LOG_SYSLOGAPPENDER_ENABLED, false)) {
            SyslogAppender appender = new SyslogAppender();
            appender.setName("AppSyslogLog");
            appender.setContext(loggerContext);
            Filter<ILoggingEvent> filter = new ApplicationLogFilter();
            filter.setContext(loggerContext);
            filter.start();
            appender.addFilter(filter);
            appender.setSyslogHost(config.getString(Key.APPLICATION_LOG_SYSLOGAPPENDER_HOST));
            appender.setPort(config.getInt(Key.APPLICATION_LOG_SYSLOGAPPENDER_PORT));
            appender.setFacility(config.getString(Key.APPLICATION_LOG_SYSLOGAPPENDER_FACILITY));
            appender.start();
            rootLogger.addAppender(appender);
        }
    }

}