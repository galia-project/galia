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

import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServerTest extends BaseTest {

    private Client client;
    private Server server;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        server = new Server();
        server.setRoot(TestUtils.getFixturePath());

        client = ClientFactory.newClient();
        client.setTransport(Transport.HTTP1_1);
        Reference uri = server.getHTTPURI().rebuilder().withPath("/text.txt").build();
        client.setURI(uri);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        try {
            client.stop();
        } finally {
            server.stop();
        }
    }

    @Test
    void testAcceptingRangesWhenSetToTrue() throws Exception {
        server.setAcceptingRanges(true);
        server.start();

        try (Response response = client.send()) {
            assertEquals("bytes", response.getHeaders().getFirstValue("Accept-Ranges"));
        }
    }

    @Test
    void testAcceptingRangesWhenSetToFalse() throws Exception {
        server.setAcceptingRanges(false);
        server.start();

        try (Response response = client.send()) {
            assertEquals(0, response.getHeaders().getAll("Accept-Ranges").size());
        }
    }

    @Test
    void testBasicAuthWithValidCredentials() throws Exception {
        final String realm = "Test Realm";
        final String user = "dogs";
        final String secret = "monkeys";

        server.setBasicAuthEnabled(true);
        server.setAuthRealm(realm);
        server.setAuthUser(user);
        server.setAuthSecret(secret);
        server.start();

        client.setRealm(realm);
        client.setUsername(user);
        client.setSecret(secret);

        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());
        }
    }

    @Test
    void testBasicAuthWithInvalidCredentials() throws Exception {
        final String realm = "Test Realm";
        final String user = "dogs";

        server.setBasicAuthEnabled(true);
        server.setAuthRealm(realm);
        server.setAuthUser(user);
        server.setAuthSecret("bugs");
        server.start();

        client.setRealm(realm);
        client.setUsername(user);
        client.setSecret("horses");

        try (Response response = client.send()) {
        } catch (HTTPException e) {
            assertEquals(Status.UNAUTHORIZED, e.getStatus());
        }
    }

    @Test
    void testHandler() throws Exception {
        final String path = "/unauthorized";

        server.setHandler(new DefaultHandler() {
            @Override
            public boolean handle(Request baseRequest,
                                  org.eclipse.jetty.server.Response response,
                                  Callback callback) {
                if (baseRequest.getHttpURI().getPath().startsWith(path)) {
                    response.setStatus(500);
                }
                callback.succeeded();
                return true;
            }
        });
        server.start();

        Reference uri = server.getHTTPURI().rebuilder().withPath(path).build();
        client.setURI(uri);
        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(Status.INTERNAL_SERVER_ERROR, e.getStatus());
        }
    }

    @Test
    void testHTTP1() throws Exception {
        server.setHTTP2Enabled(false);
        server.start();

        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());
            assertEquals(Transport.HTTP1_1, response.getTransport());
        }
    }

    @Disabled // TODO: this fails using the JDK 11 HTTP client
    @Test
    void testHTTP2() throws Exception {
        server.setHTTP1Enabled(false);
        server.start();

        client.setTransport(Transport.HTTP2_0);

        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());
            assertEquals(Transport.HTTP2_0, response.getTransport());
        }
    }

    @Test
    void testHTTPS1() throws Exception {
        server.setHTTPS1Enabled(true);
        server.setHTTPS2Enabled(false);
        server.setKeyManagerPassword("password");
        server.setKeyStorePassword("password");
        server.setKeyStorePath(TestUtils.getFixture("keystore-password.jks"));
        server.start();

        client.setTransport(Transport.HTTP1_1);

        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());
            assertEquals(Transport.HTTP1_1, response.getTransport());
        }
    }

    @Test
    void testHTTPS2() throws Exception {
        server.setHTTPS1Enabled(false);
        server.setHTTPS2Enabled(true);
        server.setKeyManagerPassword("password");
        server.setKeyStorePassword("password");
        server.setKeyStorePath(TestUtils.getFixture("keystore-password.jks"));
        server.start();

        client.setTransport(Transport.HTTP2_0);

        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());
            assertEquals(Transport.HTTP2_0, response.getTransport());
        }
    }

}
