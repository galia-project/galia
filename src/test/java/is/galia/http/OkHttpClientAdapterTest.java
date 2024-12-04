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
import is.galia.test.StaticFileServer;
import is.galia.util.SocketUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.*;

class OkHttpClientAdapterTest extends BaseTest {

    private Client instance;
    private StaticFileServer webServer;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        instance = new OkHttpClientAdapter();
        instance.setTrustAll(true);

        webServer = new StaticFileServer();

        assertEquals(Method.GET, instance.getMethod());
        assertEquals(Transport.HTTP1_1, instance.getTransport());
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        instance.stop();
        if (webServer != null) {
            webServer.stop();
        }
    }

    private Reference getValidHTTPURI() {
        return webServer.getHTTPURI().rebuilder()
                .withPath("/sample-images/wide.jpg")
                .build();
    }

    private Reference getValidHTTPSURI() {
        return webServer.getHTTPSURI().rebuilder()
                .withPath("/sample-images/wide.jpg")
                .build();
    }

    /* builder() */

    @Test
    void builder() {
        Reference uri = new Reference("http://example.org/cats");
        instance = new OkHttpClientAdapter();
        instance.setURI(uri);
        assertEquals(uri, instance.getURI());
    }

    /* send() */

    @Test
    void sendGETRequest() throws Exception {
        instance.setURI(getValidHTTPURI());
        instance.setMethod(Method.GET);
        webServer.start();

        try (Response response = instance.send()) {
            assertEquals(Status.OK, response.getStatus());
        }
    }

    @Test
    void sendHEADRequest() throws Exception {
        instance.setURI(getValidHTTPURI());
        instance.setMethod(Method.HEAD);
        webServer.start();

        try (Response response = instance.send()) {
            assertEquals(Status.OK, response.getStatus());
        }
    }

    @Test
    void sendWithUnknownHostThrowsException() {
        instance.setURI(new Reference("http://bogus.example.org"));
        assertThrows(ConnectException.class, () -> instance.send().close());
    }

    @Test
    void sendWithConnectionFailureThrowsException() {
        instance.setURI(new Reference("http://localhost:" + SocketUtils.getOpenPort()));
        assertThrows(ConnectException.class, () -> instance.send().close());
    }

    @Test
    void testSendWithValidBasicAuthCredentialsAndCorrectRealm()
            throws Exception {
        instance.setURI(getValidHTTPURI());
        instance.setRealm(StaticFileServer.BASIC_REALM);
        instance.setUsername(StaticFileServer.BASIC_USER);
        instance.setSecret(StaticFileServer.BASIC_SECRET);

        webServer.setBasicAuthEnabled(true);
        webServer.start();

        try (Response response = instance.send()) {
            assertEquals(Status.OK, response.getStatus());
        }
    }

    @Disabled // this test used to be used, but we're now sending credentials preemptively so the realm doesn't matter
    @Test
    void sendWithValidBasicAuthCredentialsAndIncorrectRealm()
            throws Exception {
        instance.setURI(getValidHTTPURI());
        instance.setRealm("bogus");
        instance.setUsername(StaticFileServer.BASIC_USER);
        instance.setSecret(StaticFileServer.BASIC_SECRET);

        webServer.setBasicAuthEnabled(true);
        webServer.start();

        try (Response response = instance.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(401, e.getStatus().code());
        }
    }

    @Test
    void sendWithInvalidBasicAuthCredentialsAndCorrectRealm() throws Exception {
        instance.setURI(getValidHTTPURI());
        instance.setRealm(StaticFileServer.BASIC_REALM);
        instance.setUsername("bogus");
        instance.setSecret("bogus");

        webServer.setBasicAuthEnabled(true);
        webServer.start();

        try (Response response = instance.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(401, e.getStatus().code());
        }
    }

    @Test
    void sendWithInvalidBasicAuthCredentialsAndIncorrectRealm()
            throws Exception {
        instance.setURI(getValidHTTPURI());
        instance.setRealm("bogus");
        instance.setUsername("bogus");
        instance.setSecret("bogus");

        webServer.setBasicAuthEnabled(true);
        webServer.start();

        try (Response response = instance.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(401, e.getStatus().code());
        }
    }

    @Test
    void sendWorksWithInsecureHTTP1_1() throws Exception {
        webServer.start();

        instance.setTransport(Transport.HTTP1_1);
        instance.setURI(getValidHTTPURI());

        try (Response response = instance.send()) {
            assertEquals(Status.OK, response.getStatus());
            assertEquals(Transport.HTTP1_1, response.getTransport());
        }
    }

    @Test
    void sendWorksWithInsecureHTTP2() throws Exception {
        webServer.start();

        instance.setTransport(Transport.HTTP2_0);
        instance.setURI(getValidHTTPURI());

        try (Response response = instance.send()) {
            assertEquals(Status.OK, response.getStatus());
            assertEquals(Transport.HTTP2_0, response.getTransport());
        }
    }

    @Test
    void sendWorksWithSecureHTTP1_1() throws Exception {
        webServer.start();

        instance.setTransport(Transport.HTTP1_1);
        instance.setURI(getValidHTTPSURI());

        try (Response response = instance.send()) {
            assertEquals(Status.OK, response.getStatus());
            assertEquals(Transport.HTTP1_1, response.getTransport());
        }
    }

    @Test
    void sendWorksWithSecureHTTP2() throws Exception {
        webServer.start();

        instance.setTransport(Transport.HTTP2_0);
        instance.setURI(getValidHTTPSURI());

        try (Response response = instance.send()) {
            assertEquals(Status.OK, response.getStatus());
            assertEquals(Transport.HTTP2_0, response.getTransport());
        }
    }

}
