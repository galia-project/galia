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
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class OkHttpResponseWrapperTest extends BaseTest {

    private OkHttpClient okClient;
    private StaticFileServer server;
    private okhttp3.Response okResponse;
    private OkHttpResponseWrapper instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        server = new StaticFileServer();
        server.start();

        okClient = new OkHttpClient.Builder()
                .followRedirects(false)
                .build();

        Reference uri = server.getHTTPURI().rebuilder()
                .withPath("/jpg").build();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(uri.toString())
                .build();
        okResponse = okClient.newCall(request).execute();
        instance = new OkHttpResponseWrapper(okResponse);
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
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(uri.toString())
                .build();
        okResponse = okClient.newCall(request).execute();
        byte[] bytes2 = okResponse.body().bytes();

        assertArrayEquals(bytes1, bytes2);
    }

    @Test
    void getBodyAsStream() throws Exception {
        ByteArrayOutputStream os1 = new ByteArrayOutputStream();
        try (InputStream is = okResponse.body().byteStream()) {
            is.transferTo(os1);
        }
        okResponse.close();

        // We can only consume the stream once, so make another request
        Reference uri = server.getHTTPURI().rebuilder()
                .withPath("/jpg").build();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(uri.toString())
                .build();
        okResponse = okClient.newCall(request).execute();
        instance = new OkHttpResponseWrapper(okResponse);

        ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        try (InputStream is = instance.getBodyAsStream()) {
            is.transferTo(os2);
        }
        assertArrayEquals(os1.toByteArray(), os2.toByteArray());
    }

    @Test
    void getHeaders() {
        // Ignore the Date header because its value may be different
        // between the two OkHttp request invocations above.
        final Headers expectedHeaders = new Headers();
        okResponse.headers().toMultimap().forEach((name, list) -> {
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
        assertEquals(okResponse.code(), instance.getStatus().code());
    }

    @Test
    void getTransport() {
        assertEquals(Transport.HTTP1_1, instance.getTransport());
    }

}
