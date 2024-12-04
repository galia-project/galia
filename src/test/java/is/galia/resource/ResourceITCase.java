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

package is.galia.resource;

import is.galia.Application;
import is.galia.ApplicationServer;
import is.galia.cache.CacheFacade;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Client;
import is.galia.http.ClientFactory;
import is.galia.http.Reference;
import is.galia.test.BaseITCase;
import is.galia.test.TestUtils;
import is.galia.util.SocketUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

/**
 * Abstract base class for functional HTTP endpoint tests.
 */
public abstract class ResourceITCase extends BaseITCase {

    protected static ApplicationServer appServer;

    protected Client client;
    private int httpPort, httpsPort;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        final Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_SCALE, 0);
        //config.setProperty(Key.ADMIN_ENABLED, true);
        config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
        config.setProperty(Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                TestUtils.getFixturePath() + "/");

        new CacheFacade().purge();

        // Retry starting the app server several times, in case it throws a
        // port-in-use exception.
        final int maxRetries = 5;
        for (int retryCount = 0; retryCount < maxRetries; retryCount++) {
            try {
                httpPort = SocketUtils.getOpenPort();
                do {
                    httpsPort = SocketUtils.getOpenPort();
                } while (httpsPort == httpPort);

                appServer = Application.getAppServer();
                appServer.setHTTPEnabled(true);
                appServer.setHTTPPort(httpPort);
                appServer.setHTTPSEnabled(true);
                appServer.setHTTPSPort(httpsPort);
                appServer.setHTTPSKeyStoreType("JKS");
                appServer.setHTTPSKeyStorePath(
                        TestUtils.getFixture("keystore-password.jks").toString());
                appServer.setHTTPSKeyStorePassword("password");
                appServer.setHTTPSKeyPassword("password");

                appServer.start();
                break;
            } catch (IOException e) {
                if (retryCount == maxRetries - 1) {
                    throw e;
                }
            }
        }
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        appServer.stop();
        if (client != null) {
            client.stop();
        }
    }

    protected int getHTTPPort() {
        return httpPort;
    }

    /**
     * @param path Full URI path.
     */
    protected Reference getHTTPURI(String path) {
        return new Reference("http://localhost:" + appServer.getHTTPPort() +
                path);
    }

    protected int getHTTPSPort() {
        return httpsPort;
    }

    /**
     * @param path Full URI path.
     * @return HTTPS URI.
     */
    protected Reference getHTTPSURI(String path) {
        return new Reference("https://localhost:" + appServer.getHTTPSPort() +
                path);
    }

    /**
     * @param path Full URI path.
     * @return New client instance. Clients should call {@link Client#stop()}
     *         on it when they are done with it. Or, if they assign it to
     *         {@link #client}, {@link #tearDown()} will take care of it.
     */
    protected Client newClient(String path) {
        Client client = ClientFactory.newClient();
        client.setURI(getHTTPURI(path));
        return client;
    }

    /**
     * @param path   Full URI path.
     * @param user   HTTP Basic user.
     * @param secret HTTP Basic secret.
     * @param realm  HTTP Basic realm.
     * @return New client instance, initialized to use HTTP Basic
     *         authentication. Clients should call {@link Client#stop()}
     *         on it when they are done. Or, if they assign it to
     *         {@link #client}, {@link #tearDown()} will take care of it.
     */
    protected Client newClient(String path, String user, String secret,
                               String realm) {
        Client client = ClientFactory.newClient();
        client.setURI(getHTTPURI(path));
        client.setRealm(realm);
        client.setUsername(user);
        client.setSecret(secret);
        return client;
    }

}
