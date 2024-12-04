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

package is.galia.source;

import is.galia.http.Reference;
import is.galia.http.Status;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import is.galia.http.Range;
import is.galia.http.Response;
import is.galia.test.BaseTest;
import is.galia.test.StaticFileServer;

import static org.junit.jupiter.api.Assertions.*;

public class OkHttpHTTPImageInputStreamClientTest extends BaseTest {

    private StaticFileServer server;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        server = new StaticFileServer();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        server.stop();
    }

    @Test
    void getReference() {
        final HTTPRequestInfo requestInfo = new HTTPRequestInfo();
        Reference uri = server.getHTTPURI();
        requestInfo.setURI(uri.toString());
        final OkHttpHTTPImageInputStreamClient instance =
                new OkHttpHTTPImageInputStreamClient(requestInfo);
        assertEquals(uri, instance.getReference());
    }

    @Test
    void sendHEADRequest() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public boolean handle(Request request,
                                  org.eclipse.jetty.server.Response response,
                                  Callback callback) {
                assertEquals("HEAD", request.getMethod());
                response.setStatus(200);
                callback.succeeded();
                return true;
            }
        });
        server.start();

        final HTTPRequestInfo requestInfo = new HTTPRequestInfo();
        requestInfo.setURI(server.getHTTPURI().toString());
        final OkHttpHTTPImageInputStreamClient instance =
                new OkHttpHTTPImageInputStreamClient(requestInfo);

        instance.sendHEADRequest();
    }

    @Test
    void sendHEADRequestSendsRequestInfoCredentials() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public boolean handle(Request request,
                                  org.eclipse.jetty.server.Response response,
                                  Callback callback) {
                assertEquals("Basic dXNlcjpzZWNyZXQ=",
                        request.getHeaders().get("Authorization"));
                callback.succeeded();
                return true;
            }
        });
        server.start();

        final HTTPRequestInfo requestInfo = new HTTPRequestInfo();
        requestInfo.setURI(server.getHTTPURI().toString());
        requestInfo.setUsername("user");
        requestInfo.setSecret("secret");
        final OkHttpHTTPImageInputStreamClient instance =
                new OkHttpHTTPImageInputStreamClient(requestInfo);

        instance.sendHEADRequest();
    }

    @Test
    void sendHEADRequestSendsRequestInfoHeaders() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public boolean handle(Request request,
                                  org.eclipse.jetty.server.Response response,
                                  Callback callback) {
                assertEquals("yes", request.getHeaders().get("X-Cats"));
                callback.succeeded();
                return true;
            }
        });
        server.start();

        final HTTPRequestInfo requestInfo = new HTTPRequestInfo();
        requestInfo.setURI(server.getHTTPURI().toString());
        requestInfo.getHeaders().add("X-Cats", "yes");

        final OkHttpHTTPImageInputStreamClient instance =
                new OkHttpHTTPImageInputStreamClient(requestInfo);

        instance.sendHEADRequest();
    }

    @Test
    void sendGETRequest() throws Exception {
        server.start();

        final HTTPRequestInfo requestInfo = new HTTPRequestInfo();
        requestInfo.setURI(server.getHTTPURI() + "/text.txt");
        final OkHttpHTTPImageInputStreamClient instance =
                new OkHttpHTTPImageInputStreamClient(requestInfo);

        try (Response response = instance.sendGETRequest(new Range(0, 3, 4))) {
            assertEquals("some", response.getBodyAsString());
            assertEquals(Status.PARTIAL_CONTENT, response.getStatus());
        }
    }

    @Test
    void sendGETRequestSendsRequestInfoCredentials() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public boolean handle(Request request,
                                  org.eclipse.jetty.server.Response response,
                                  Callback callback) {
                assertEquals("Basic dXNlcjpzZWNyZXQ=",
                        request.getHeaders().get("Authorization"));
                callback.succeeded();
                return true;
            }
        });
        server.start();

        final HTTPRequestInfo requestInfo = new HTTPRequestInfo();
        requestInfo.setURI(server.getHTTPURI() + "/jpg");
        requestInfo.setUsername("user");
        requestInfo.setSecret("secret");
        final OkHttpHTTPImageInputStreamClient instance =
                new OkHttpHTTPImageInputStreamClient(requestInfo);

        instance.sendGETRequest(new Range(0, 1, 4)).close();
    }

    @Test
    void sendGETRequestSendsRequestInfoHeaders() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public boolean handle(Request request,
                                  org.eclipse.jetty.server.Response response,
                                  Callback callback) {
                assertEquals("yes", request.getHeaders().get("X-Cats"));
                callback.succeeded();
                return true;
            }
        });
        server.start();

        final HTTPRequestInfo requestInfo = new HTTPRequestInfo();
        requestInfo.setURI(server.getHTTPURI() + "/jpg");
        requestInfo.getHeaders().add("X-Cats", "yes");
        final OkHttpHTTPImageInputStreamClient instance =
                new OkHttpHTTPImageInputStreamClient(requestInfo);

        instance.sendGETRequest(new Range(0, 1, 4)).close();
    }

}
