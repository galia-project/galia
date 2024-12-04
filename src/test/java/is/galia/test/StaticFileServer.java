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

package is.galia.test;

import is.galia.http.Reference;
import is.galia.http.Server;
import org.eclipse.jetty.server.Handler;

import java.nio.file.Path;

/**
 * Wraps a {@link Server} for convenient functional testing.
 */
public final class StaticFileServer {

    public static final String BASIC_REALM  = "Test Realm";
    public static final String BASIC_USER   = "user";
    public static final String BASIC_SECRET = "secret";

    private final Server wrappedServer = new Server();

    public StaticFileServer() {
        Path fixturePath = TestUtils.getFixturePath();
        wrappedServer.setRoot(fixturePath);
        wrappedServer.setKeyStorePath(TestUtils.getFixture("keystore-password.jks"));
        wrappedServer.setKeyStorePassword("password");
        wrappedServer.setKeyManagerPassword("password");
        wrappedServer.setHTTP1Enabled(true);
        wrappedServer.setHTTP2Enabled(true);
        wrappedServer.setHTTPS1Enabled(true);
        wrappedServer.setHTTPS2Enabled(true);
    }

    public Reference getHTTPURI() {
        return wrappedServer.getHTTPURI();
    }

    public Reference getHTTPSURI() {
        return wrappedServer.getHTTPSURI();
    }

    public void setAcceptingRanges(boolean isAcceptingRanges) {
        wrappedServer.setAcceptingRanges(isAcceptingRanges);
    }

    public void setBasicAuthEnabled(boolean enabled) {
        wrappedServer.setAuthRealm(BASIC_REALM);
        wrappedServer.setAuthUser(BASIC_USER);
        wrappedServer.setAuthSecret(BASIC_SECRET);
        wrappedServer.setBasicAuthEnabled(enabled);
    }

    public void setHandler(Handler handler) {
        wrappedServer.setHandler(handler);
    }

    public void setHTTP1Enabled(boolean enabled) {
        wrappedServer.setHTTP1Enabled(enabled);
    }

    public void setHTTP2Enabled(boolean enabled) {
        wrappedServer.setHTTP2Enabled(enabled);
    }

    public void setHTTPS1Enabled(boolean enabled) {
        wrappedServer.setHTTPS1Enabled(enabled);
    }

    public void setHTTPS2Enabled(boolean enabled) {
        wrappedServer.setHTTPS2Enabled(enabled);
    }

    public void setRoot(Path root) {
        wrappedServer.setRoot(root);
    }

    public void start() throws Exception {
        wrappedServer.start();
    }

    public void stop() throws Exception {
        wrappedServer.stop();
    }

}
