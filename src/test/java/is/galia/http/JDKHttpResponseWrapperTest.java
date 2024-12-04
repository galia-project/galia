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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class JDKHttpResponseWrapperTest extends BaseTest {

    private HttpClient jdkClient;
    private StaticFileServer server;
    private HttpResponse<InputStream> jdkResponse;
    private JDKHttpResponseWrapper instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        server = new StaticFileServer();
        server.start();

        jdkClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        Reference uri = server.getHTTPURI().rebuilder()
                .withPath("/jpg").build();
        HttpRequest request = HttpRequest.newBuilder()
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .uri(uri.toURI())
                .build();
        jdkResponse = jdkClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        instance = new JDKHttpResponseWrapper(jdkResponse);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        instance.close();
        server.stop();
    }

    @Test
    void getBody() throws Exception {
        byte[] bytes1 = instance.getBody();
        instance.close();

        // We can only consume the stream once, so make another request
        Reference uri = server.getHTTPURI().rebuilder()
                .withPath("/jpg").build();
        HttpRequest request = HttpRequest.newBuilder()
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .uri(uri.toURI())
                .build();
        jdkResponse = jdkClient.send(
                request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream is = jdkResponse.body()) {
            byte[] bytes2 = is.readAllBytes();
            assertArrayEquals(bytes1, bytes2);
        }
    }

    @Test
    void getBodyAsStream() throws Exception {
        ByteArrayOutputStream os1 = new ByteArrayOutputStream();
        try (InputStream is = jdkResponse.body()) {
            is.transferTo(os1);
        }

        // We can only consume the stream once, so make another request
        Reference uri = server.getHTTPURI().rebuilder()
                .withPath("/jpg").build();
        HttpRequest request = HttpRequest.newBuilder()
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .uri(uri.toURI())
                .build();
        jdkResponse = jdkClient.send(
                request, HttpResponse.BodyHandlers.ofInputStream());
        instance = new JDKHttpResponseWrapper(jdkResponse);

        ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        try (InputStream is = instance.getBodyAsStream()) {
            is.transferTo(os2);
        }
        assertArrayEquals(os1.toByteArray(), os2.toByteArray());
    }

    @Test
    void getHeaders() {
        // Ignore the Date header because its value may be different
        // between the two request invocations above.
        final Headers expectedHeaders = new Headers();
        jdkResponse.headers().map().forEach((name, list) -> {
            if (!"Date".equalsIgnoreCase(name)) {
                list.forEach(h -> expectedHeaders.add(name, h));
            }
        });
        final Headers actualHeaders = instance.getHeaders();
        actualHeaders.removeAll("date");

        assertEquals(expectedHeaders, actualHeaders);
    }

    @Test
    void getStatus() {
        assertEquals(jdkResponse.statusCode(), instance.getStatus().code());
    }

    @Test
    void getTransport() {
        assertEquals(Transport.HTTP2_0, instance.getTransport());
    }

}
