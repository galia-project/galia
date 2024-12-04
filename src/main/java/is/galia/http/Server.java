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

package is.galia.http;

import is.galia.ApplicationServer;
import is.galia.util.SocketUtils;
import org.eclipse.jetty.ee10.servlet.security.ConstraintMapping;
import org.eclipse.jetty.ee10.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.nio.file.Path;
import java.util.Collections;

/**
 * <p>Simple HTTP server wrapping a Jetty server. Supports HTTP and HTTPS and
 * protocol versions 1.1 and 2.</p>
 *
 * <p>The default handler serves static filesystem content, but can be
 * overridden via {@link #setHandler(Handler)}.</p>
 *
 * <p>N.B.: This is not the application server. (See {@link
 * ApplicationServer}.</p>
 *
 * @see <a href="https://eclipse.dev/jetty/javadoc/jetty-12/index.html">
 *     Jetty 12 Javadoc</a>
 */
public final class Server {

    private boolean isAcceptingRanges = true;
    private boolean isBasicAuthEnabled;
    private String authRealm;
    private String authUser;
    private String authSecret;

    private Handler handler;
    private int httpPort;
    private int httpsPort;
    private boolean isHTTP1Enabled = true;
    private boolean isHTTP2Enabled = true;
    private boolean isHTTPS1Enabled = false;
    private boolean isHTTPS2Enabled = false;
    private String keyManagerPassword;
    private String keyStorePassword;
    private Path keyStorePath;
    private Path root;
    private org.eclipse.jetty.server.Server server;

    /**
     * Initializes a static file HTTP(S) server using the image fixture path as
     * its root.
     */
    public Server() {
        httpPort = SocketUtils.getOpenPort();
        do {
            httpsPort = SocketUtils.getOpenPort();
        } while (httpPort == httpsPort);
    }

    private void initializeServer() {
        server = new org.eclipse.jetty.server.Server();

        ServerConnector connector;
        HttpConfiguration config = new HttpConfiguration();
        config.setUriCompliance(UriCompliance.LEGACY);

        HttpConnectionFactory http1 = new HttpConnectionFactory(config);
        HTTP2CServerConnectionFactory http2c =
                new HTTP2CServerConnectionFactory(config);

        // Initialize HTTP/H2C.
        if (isHTTP1Enabled || isHTTP2Enabled) {
            if (isHTTP1Enabled && isHTTP2Enabled) {
                connector = new ServerConnector(server, http1, http2c);
            } else if (isHTTP1Enabled) {
                connector = new ServerConnector(server, http1);
            } else {
                connector = new ServerConnector(server, http2c);
            }
            connector.setPort(httpPort);
            server.addConnector(connector);
        }

        // Initialize HTTPS.
        if (isHTTPS1Enabled || isHTTPS2Enabled) {
            config = new HttpConfiguration();
            config.setUriCompliance(UriCompliance.LEGACY);
            config.setSecureScheme("https");
            config.addCustomizer(new SecureRequestCustomizer());

            final SslContextFactory.Server contextFactory =
                    new SslContextFactory.Server();
            contextFactory.setKeyStorePath(keyStorePath.toString());
            contextFactory.setKeyStorePassword(keyStorePassword);
            contextFactory.setKeyManagerPassword(keyManagerPassword);

            http1 = new HttpConnectionFactory(config);
            HTTP2ServerConnectionFactory http2 =
                    new HTTP2ServerConnectionFactory(config);

            if (isHTTPS2Enabled) {
                ALPNServerConnectionFactory alpnFactory =
                        new ALPNServerConnectionFactory();
                alpnFactory.setDefaultProtocol(http1.getProtocol());

                contextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
                contextFactory.setUseCipherSuitesOrder(true);

                SslConnectionFactory connectionFactory =
                        new SslConnectionFactory(contextFactory,
                                alpnFactory.getProtocol());
                connector = new ServerConnector(server, connectionFactory,
                        alpnFactory, http2, http1);
            } else {
                SslConnectionFactory connectionFactory =
                        new SslConnectionFactory(contextFactory, "HTTP/1.1");
                connector = new ServerConnector(server, connectionFactory,
                        http1);
            }
            connector.setPort(httpsPort);
            server.addConnector(connector);
        }

        // If a custom handler has not been set, use a static file server.
        if (handler == null) {
            ResourceHandler handler = new ResourceHandler();
            handler.setDirAllowed(false);
            handler.setAcceptRanges(isAcceptingRanges);
            handler.setBaseResourceAsString(root.toString());
            this.handler = handler;
        }

        if (isBasicAuthEnabled) {
            final String[] roles = new String[] { "user" };

            HashLoginService loginService = new HashLoginService(authRealm);
            UserStore userStore = new UserStore();
            userStore.addUser(authUser, new Password(authSecret), roles);
            loginService.setUserStore(userStore);
            server.addBean(loginService);

            Constraint constraint = new Constraint.Builder()
                    .name("auth")
                    .transport(Constraint.Transport.ANY)
                    .roles(roles)
                    .build();
            ConstraintMapping mapping = new ConstraintMapping();
            mapping.setPathSpec("/*");
            mapping.setConstraint(constraint);

            ConstraintSecurityHandler security = new ConstraintSecurityHandler();
            security.setConstraintMappings(Collections.singletonList(mapping));
            security.setAuthenticator(new BasicAuthenticator());
            security.setLoginService(loginService);
            security.setHandler(handler);
            server.setHandler(security);
        } else {
            server.setHandler(handler);
        }
    }

    public Reference getHTTPURI() {
        return new Reference("http://localhost:" + httpPort);
    }

    public Reference getHTTPSURI() {
        return new Reference("https://localhost:" + httpsPort);
    }

    public void setAcceptingRanges(boolean isAcceptingRanges) {
        this.isAcceptingRanges = isAcceptingRanges;
    }

    public void setAuthRealm(String realm) {
        this.authRealm = realm;
    }

    public void setAuthSecret(String secret) {
        this.authSecret = secret;
    }

    public void setAuthUser(String user) {
        this.authUser = user;
    }

    public void setBasicAuthEnabled(boolean enabled) {
        this.isBasicAuthEnabled = enabled;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setHTTP1Enabled(boolean enabled) {
        this.isHTTP1Enabled = enabled;
    }

    public void setHTTP2Enabled(boolean enabled) {
        this.isHTTP2Enabled = enabled;
    }

    public void setHTTPS1Enabled(boolean enabled) {
        this.isHTTPS1Enabled = enabled;
    }

    public void setHTTPS2Enabled(boolean enabled) {
        this.isHTTPS2Enabled = enabled;
    }

    public void setKeyManagerPassword(String password) {
        this.keyManagerPassword = password;
    }

    public void setKeyStorePassword(String password) {
        this.keyStorePassword = password;
    }

    public void setKeyStorePath(Path path) {
        this.keyStorePath = path;
    }

    public void setRoot(Path root) {
        this.root = root;
    }

    public void start() throws Exception {
        initializeServer();
        server.start();
    }

    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

}
