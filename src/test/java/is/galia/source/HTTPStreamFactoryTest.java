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

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Reference;
import is.galia.image.Identifier;
import is.galia.stream.ClosingFileCacheImageInputStream;
import is.galia.stream.HTTPImageInputStream;
import is.galia.test.BaseTest;
import is.galia.test.StaticFileServer;
import is.galia.util.SocketUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.ImageInputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HTTPStreamFactoryTest extends BaseTest {

    private static final Identifier PRESENT_READABLE_IDENTIFIER =
            new Identifier("sample-images/jpg/rgb-64x56x8-baseline.jpg");

    private StaticFileServer server;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        server = new StaticFileServer();
        server.setHTTP1Enabled(true);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        server.stop();
    }

    private HTTPStreamFactory newInstance() {
        return newInstance(true);
    }

    private HTTPStreamFactory newInstance(boolean serverAcceptsRanges) {
        Map<String,Object> headers = Map.of("X-Custom", "yes");
        HTTPRequestInfo requestInfo = new HTTPRequestInfo();
        Reference uri = server.getHTTPURI().rebuilder()
                .withPath("/" + PRESENT_READABLE_IDENTIFIER).build();
        requestInfo.setURI(uri.toString());
        requestInfo.setHeaders(headers);

        return new HTTPStreamFactory(
                requestInfo,
                5439,
                serverAcceptsRanges);
    }

    @Disabled
    @Test
    void newInputStreamWithProxy() throws Exception {
        final int proxyPort = SocketUtils.getOpenPort();

        // Set up the proxy
        // TODO: write this

        // Set up HTTPSource
        final var config = Configuration.forApplication();
        config.setProperty(Key.HTTPSOURCE_HTTP_PROXY_HOST, "127.0.0.1");
        config.setProperty(Key.HTTPSOURCE_HTTP_PROXY_PORT, proxyPort);

        int length = 0;
        try (ImageInputStream is = newInstance(true).newSeekableStream()) {
            while (is.read() != -1) {
                length++;
            }
        }
        assertTrue(length > 1000);
    }

    @Test
    void newInputStreamSendsCustomHeaders() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public boolean handle(Request request,
                                  Response response,
                                  Callback callback) {
                assertEquals("yes", request.getHeaders().get("X-Custom"));
                callback.succeeded();
                return true;
            }
        });
        server.start();

        try (ImageInputStream is = newInstance().newSeekableStream()) {}
    }

    @Test
    void newInputStreamReturnsContent() throws Exception {
        server.start();

        int length = 0;
        try (ImageInputStream is = newInstance().newSeekableStream()) {
            while (is.read() != -1) {
                length++;
            }
        }
        assertEquals(5439, length);
    }

    @Test
    void newSeekableStreamWhenChunkingIsEnabledAndServerAcceptsRanges()
            throws Exception {
        server.start();

        final Configuration config = Configuration.forApplication();
        config.setProperty(Key.HTTPSOURCE_CHUNKING_ENABLED, true);
        config.setProperty(Key.HTTPSOURCE_CHUNK_SIZE, "777K");

        try (ImageInputStream is = newInstance(true).newSeekableStream()) {
            assertInstanceOf(HTTPImageInputStream.class, is);
            assertEquals(777 * 1024, ((HTTPImageInputStream) is).getWindowSize());
        }
    }

    @Test
    void newSeekableStreamWhenChunkingIsEnabledButServerDoesNotAcceptRanges()
            throws Exception {
        server.setAcceptingRanges(false);
        server.start();

        Configuration.forApplication().setProperty(Key.HTTPSOURCE_CHUNKING_ENABLED, true);
        try (ImageInputStream is = newInstance(false).newSeekableStream()) {
            assertInstanceOf(ClosingFileCacheImageInputStream.class, is);
        }
    }

    @Test
    void newSeekableStreamWhenChunkingIsDisabled() throws Exception {
        server.start();

        Configuration.forApplication().setProperty(Key.HTTPSOURCE_CHUNKING_ENABLED, false);
        try (ImageInputStream is = newInstance(true).newSeekableStream()) {
            assertInstanceOf(ClosingFileCacheImageInputStream.class, is);
        }
    }

    @Disabled
    @Test
    void newSeekableStreamWithProxy() throws Exception {
        final int proxyPort = SocketUtils.getOpenPort();

        // Set up the proxy
        // TODO: write this

        // Set up HTTPSource
        final var config = Configuration.forApplication();
        config.setProperty(Key.HTTPSOURCE_HTTP_PROXY_HOST, "127.0.0.1");
        config.setProperty(Key.HTTPSOURCE_HTTP_PROXY_PORT, proxyPort);

        int length = 0;
        try (ImageInputStream is = newInstance(true).newSeekableStream()) {
            while (is.read() != -1) {
                length++;
            }
        }
        assertTrue(length > 1000);
    }

    @Test
    void newSeekableStreamSendsCustomHeaders() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public boolean handle(Request request,
                                  Response response,
                                  Callback callback) {
                assertEquals("yes", request.getHeaders().get("X-Custom"));
                callback.succeeded();
                return true;
            }
        });
        server.start();

        try (ImageInputStream is = newInstance().newSeekableStream()) {}
    }

    @Test
    void newSeekableStreamReturnsContent() throws Exception {
        server.start();

        int length = 0;
        try (ImageInputStream is = newInstance().newSeekableStream()) {
            while (is.read() != -1) {
                length++;
            }
        }
        assertEquals(5439, length);
    }

    @Test
    void newSeekableStreamWithChunkingEnabled() throws Exception {
        final Configuration config = Configuration.forApplication();
        config.setProperty(Key.HTTPSOURCE_CHUNKING_ENABLED, true);
        config.setProperty(Key.HTTPSOURCE_CHUNK_SIZE, "777K");

        try (ImageInputStream is = newInstance().newSeekableStream()) {
            assertInstanceOf(HTTPImageInputStream.class, is);
            HTTPImageInputStream htis = (HTTPImageInputStream) is;
            assertEquals(777 * 1024, htis.getWindowSize());
        }
    }

}
