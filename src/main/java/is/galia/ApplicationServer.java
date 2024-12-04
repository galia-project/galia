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

package is.galia;

import is.galia.config.Configuration;
import is.galia.config.ConfigurationException;
import is.galia.config.Key;
import is.galia.resource.RequestHandler;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.StateTrackingHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.lang.management.ManagementFactory;
import java.time.Duration;

/**
 * <p>Provides the embedded web server.</p>
 */
public class ApplicationServer {

    private static final String REMOTE_JMX_PORT_VM_ARG =
            "com.sun.management.jmxremote.port";

    /**
     * {@code 0} tells Jetty to use an OS default.
     */
    static final int DEFAULT_ACCEPT_QUEUE_LIMIT = 0;

    static final String DEFAULT_HTTP_HOST       = "0.0.0.0";

    static final int DEFAULT_HTTP_PORT          = 8182;

    static final String DEFAULT_HTTPS_HOST      = "0.0.0.0";

    static final int DEFAULT_HTTPS_PORT         = 8183;

    static final Duration DEFAULT_IDLE_TIMEOUT  = Duration.ofSeconds(30);

    /**
     * Minimum number of threads in the pool. {@code 8} is the default in
     * Jetty 12.
     */
    static final int DEFAULT_MIN_THREADS = 8;

    /**
     * Maximum number of threads in the pool. {@code 200} is the default in
     * Jetty 12.
     */
    static final int DEFAULT_MAX_THREADS = 200;

    private int acceptQueueLimit          = DEFAULT_ACCEPT_QUEUE_LIMIT;
    private boolean isHTTPEnabled;
    private String httpHost               = DEFAULT_HTTP_HOST;
    private int httpPort                  = DEFAULT_HTTP_PORT;
    private boolean isHTTPSEnabled;
    private String httpsHost              = DEFAULT_HTTPS_HOST;
    private String httpsKeyPassword;
    private String httpsKeyStorePassword;
    private String httpsKeyStorePath;
    private String httpsKeyStoreType;
    private int httpsPort                 = DEFAULT_HTTPS_PORT;
    private Duration idleTimeout          = DEFAULT_IDLE_TIMEOUT;
    private boolean isStarted;
    private int minThreads                = DEFAULT_MIN_THREADS;
    private int maxThreads                = DEFAULT_MAX_THREADS;
    private Server server;

    /**
     * Initializes the instance with arbitrary defaults.
     */
    public ApplicationServer() {
    }

    /**
     * Initializes the instance with defaults from a {@link Configuration}
     * object.
     *
     * @param config Instance from which to obtain settings.
     */
    public ApplicationServer(Configuration config) {
        this();

        setHTTPEnabled(config.getBoolean(Key.HTTP_ENABLED, false));
        setHTTPHost(config.getString(Key.HTTP_HOST, DEFAULT_HTTP_HOST));
        setHTTPPort(config.getInt(Key.HTTP_PORT, DEFAULT_HTTP_PORT));

        setHTTPSEnabled(config.getBoolean(Key.HTTPS_ENABLED, false));
        setHTTPSHost(config.getString(Key.HTTPS_HOST, DEFAULT_HTTPS_HOST));
        setHTTPSKeyPassword(config.getString(Key.HTTPS_KEY_PASSWORD));
        setHTTPSKeyStorePassword(
                config.getString(Key.HTTPS_KEY_STORE_PASSWORD));
        setHTTPSKeyStorePath(
                config.getString(Key.HTTPS_KEY_STORE_PATH));
        setHTTPSKeyStoreType(
                config.getString(Key.HTTPS_KEY_STORE_TYPE));
        setHTTPSPort(config.getInt(Key.HTTPS_PORT, DEFAULT_HTTPS_PORT));
        setMaxThreads(config.getInt(Key.HTTP_MAX_THREADS, DEFAULT_MAX_THREADS));
        setMinThreads(config.getInt(Key.HTTP_MIN_THREADS, DEFAULT_MIN_THREADS));
        setAcceptQueueLimit(config.getInt(Key.HTTP_ACCEPT_QUEUE_LIMIT,
                DEFAULT_ACCEPT_QUEUE_LIMIT));
        setIdleTimeout(Duration.ofSeconds(config.getInt(Key.HTTP_IDLE_TIMEOUT,
                (int) DEFAULT_IDLE_TIMEOUT.toSeconds())));
    }

    private void createServer() {
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMinThreads(getMinThreads());
        threadPool.setMaxThreads(getMaxThreads());

        if (Application.isDeveloping()) {
            threadPool.setDetailedDump(true);
        }

        // A StateTrackingHandler will wrap our RequestHandler, warning us
        // (via WARN-level log messages) of bugs like failing to complete
        // callbacks. See:
        // https://jetty.org/docs/jetty/12/programming-guide/troubleshooting/state-tracking.html
        StateTrackingHandler trackingHandler = new StateTrackingHandler();
        trackingHandler.setHandlerCallbackTimeout(idleTimeout.toMillis());
        trackingHandler.setHandler(new RequestHandler());

        server = new Server(threadPool);
        server.setHandler(trackingHandler);
        server.setErrorHandler(new ErrorHandler());

        // This is technically "NCSA Combined" format.
        RequestLog log = new CustomRequestLog(
                new Slf4jRequestLogWriter(),
                CustomRequestLog.EXTENDED_NCSA_FORMAT);
        server.setRequestLog(log);
    }

    public int getAcceptQueueLimit() {
        return acceptQueueLimit;
    }

    public String getHTTPHost() {
        return httpHost;
    }

    public int getHTTPPort() {
        return httpPort;
    }

    public String getHTTPSHost() {
        return httpsHost;
    }

    public String getHTTPSKeyPassword() {
        return httpsKeyPassword;
    }

    public String getHTTPSKeyStorePassword() {
        return httpsKeyStorePassword;
    }

    public String getHTTPSKeyStorePath() {
        return httpsKeyStorePath;
    }

    public String getHTTPSKeyStoreType() {
        return httpsKeyStoreType;
    }

    public int getHTTPSPort() {
        return httpsPort;
    }

    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public int getMinThreads() {
        return minThreads;
    }

    public boolean isHTTPEnabled() {
        return isHTTPEnabled;
    }

    public boolean isHTTPSEnabled() {
        return isHTTPSEnabled;
    }

    public boolean isStarted() {
        return (server != null && server.isStarted());
    }

    public boolean isStopped() {
        return (server == null || server.isStopped());
    }

    public void setAcceptQueueLimit(int size) {
        this.acceptQueueLimit = size;
    }

    public void setHTTPEnabled(boolean enabled) {
        this.isHTTPEnabled = enabled;
    }

    public void setHTTPHost(String host) {
        this.httpHost = host;
    }

    public void setHTTPPort(int port) {
        this.httpPort = port;
    }

    public void setHTTPSEnabled(boolean enabled) {
        this.isHTTPSEnabled = enabled;
    }

    public void setHTTPSHost(String host) {
        this.httpsHost = host;
    }

    public void setHTTPSKeyPassword(String password) {
        this.httpsKeyPassword = password;
    }

    public void setHTTPSKeyStorePassword(String password) {
        this.httpsKeyStorePassword = password;
    }

    public void setHTTPSKeyStorePath(String path) {
        this.httpsKeyStorePath = path;
    }

    public void setHTTPSKeyStoreType(String type) {
        this.httpsKeyStoreType = type;
    }

    public void setHTTPSPort(int port) {
        this.httpsPort = port;
    }

    public void setIdleTimeout(Duration timeout) {
        this.idleTimeout = timeout;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public void setMinThreads(int minThreads) {
        this.minThreads = minThreads;
    }

    /**
     * Starts the HTTP and/or HTTPS servers.
     *
     * @throws Exception if there is a problem starting the server.
     */
    public void start() throws Exception {
        if (isStarted) {
            return;
        }
        createServer();

        final HttpConfiguration config = new HttpConfiguration();
        // * AMBIGUOUS_PATH_SEPARATORS allows encoded forward slashes,
        //   commonly used in identifiers.
        // * ILLEGAL_PATH_CHARACTERS allows the unencoded carat, used in IIIF
        //   image requests.
        // * SUSPICIOUS_PATH_CHARACTERS allows encoded backslashes, which we
        //   imagine might be used in identifiers on Windows servers.
        // See: https://javadoc.jetty.org/jetty-12/org/eclipse/jetty/http/UriCompliance.Violation.html
        config.setUriCompliance(UriCompliance.from(
                "RFC3986,SUSPICIOUS_PATH_CHARACTERS,ILLEGAL_PATH_CHARACTERS,AMBIGUOUS_PATH_SEPARATOR"));

        if (!isHTTPEnabled() && !isHTTPSEnabled()) {
            throw new ConfigurationException("HTTP and HTTPS are disabled. Check your application configuration.");
        }

        // Initialize the HTTP server, handling both HTTP/1.1 and insecure
        // HTTP/2.
        if (isHTTPEnabled()) {
            HttpConnectionFactory http1 =
                    new HttpConnectionFactory(config);
            HTTP2CServerConnectionFactory http2 =
                    new HTTP2CServerConnectionFactory(config);
            ServerConnector connector =
                    new ServerConnector(server, http1, http2);
            connector.setHost(getHTTPHost());
            connector.setPort(getHTTPPort());
            connector.setIdleTimeout(idleTimeout.toMillis());
            connector.setAcceptQueueSize(getAcceptQueueLimit());
            server.addConnector(connector);
        }

        // Initialize the HTTPS server.
        if (isHTTPSEnabled()) {
            config.setSecureScheme("https");
            config.setSecurePort(getHTTPSPort());
            config.addCustomizer(new SecureRequestCustomizer());

            SslContextFactory.Server contextFactory =
                    new SslContextFactory.Server();
            contextFactory.setKeyStorePath(getHTTPSKeyStorePath());
            contextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
            contextFactory.setUseCipherSuitesOrder(true);
            if (getHTTPSKeyStorePassword() != null) {
                contextFactory.setKeyStorePassword(getHTTPSKeyStorePassword());
            }
            if (getHTTPSKeyPassword() != null) {
                contextFactory.setKeyManagerPassword(getHTTPSKeyPassword());
            }

            HttpConnectionFactory http1 =
                    new HttpConnectionFactory(config);
            HTTP2ServerConnectionFactory http2 =
                    new HTTP2ServerConnectionFactory(config);
            ALPNServerConnectionFactory alpn =
                    new ALPNServerConnectionFactory();
            alpn.setDefaultProtocol(http1.getProtocol());
            SslConnectionFactory connectionFactory =
                    new SslConnectionFactory(contextFactory,
                            alpn.getProtocol());

            ServerConnector connector = new ServerConnector(
                    server, connectionFactory, alpn, http2, http1);
            connector.setHost(getHTTPSHost());
            connector.setPort(getHTTPSPort());
            connector.setIdleTimeout(idleTimeout.toMillis());
            connector.setAcceptQueueSize(getAcceptQueueLimit());
            server.addConnector(connector);
        }

        // Disable the Server header on all connectors.
        for (Connector connector : server.getConnectors()) {
            connector.getConnectionFactories().stream()
                    .filter(cf -> cf instanceof HttpConnectionFactory)
                    .map(cf -> (HttpConnectionFactory) cf)
                    .forEach(cf -> cf.getHttpConfiguration().setSendServerVersion(false));
        }

        // If the server is started with jmxremote, add the Jetty JMX
        // extensions to the MBeanServer.
        if (!System.getProperty(REMOTE_JMX_PORT_VM_ARG, "").isBlank()) {
            MBeanContainer mbeanContainer =
                    new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
            server.addBean(mbeanContainer);
        }

        server.start();
        isStarted = true;
    }

    public void stop() throws Exception {
        if (server != null) {
            // for debugging, also see QueuedThreadPool.setDetailedDump()
            //System.out.println(server.dump());
            server.stop();
        }
        server    = null;
        isStarted = false;
    }

}
