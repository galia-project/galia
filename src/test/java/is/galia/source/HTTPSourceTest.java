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

import is.galia.delegate.Delegate;
import is.galia.http.Reference;
import is.galia.resource.RequestContext;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import is.galia.Application;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Headers;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.test.TestUtils;
import is.galia.test.StaticFileServer;
import is.galia.util.IOUtils;
import is.galia.util.SocketUtils;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class HTTPSourceTest extends AbstractSourceTest {

    private static class RequestCountingHandler extends DefaultHandler {

        private int numHEADRequests, numGETRequests;

        @Override
        public boolean handle(Request request,
                              Response response,
                              Callback callback) {
            switch (request.getMethod().toUpperCase()) {
                case "HEAD" -> numHEADRequests++;
                case "GET"  -> numGETRequests++;
                default     ->
                        throw new IllegalArgumentException(
                            "Unexpected method: " + request.getMethod());
            }
            callback.succeeded();
            return true;
        }

    }

    private static final Identifier PRESENT_READABLE_IDENTIFIER =
            new Identifier("sample-images/jpg/rgb-64x56x8-baseline.jpg");

    private HTTPSource instance;
    private StaticFileServer server;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
        server = new StaticFileServer();
        server.setRoot(TestUtils.getFixturePath());

        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HTTPSOURCE_URL_PREFIX, server.getHTTPURI() + "/");
        config.setProperty(Key.HTTPSOURCE_ALLOW_INSECURE, true);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        destroyEndpoint();
    }

    @Override
    HTTPSource newInstance() {
        HTTPSource instance = new HTTPSource();
        instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        return instance;
    }

    @Override
    void useBasicLookupStrategy() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HTTPSOURCE_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
    }

    @Override
    void useDelegateLookupStrategy() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HTTPSOURCE_LOOKUP_STRATEGY,
                "DelegateLookupStrategy");
    }

    //region AbstractSourceTest methods

    @Override
    void destroyEndpoint() throws Exception {
        server.stop();
    }

    @Override
    void initializeEndpoint() throws Exception {
        server.start();
    }

    //endregion
    //region HTTPSource tests

    @Test
    void HTTP1() throws Exception {
        doStatWithPresentReadableImage(PRESENT_READABLE_IDENTIFIER);
    }

    @Test
    void HTTPS1() throws Exception {
        destroyEndpoint();
        server.setHTTP1Enabled(false);
        server.setHTTP2Enabled(false);
        server.setHTTPS1Enabled(true);
        server.setHTTPS2Enabled(false);

        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HTTPSOURCE_URL_PREFIX, server.getHTTPSURI() + "/");

        doStatWithPresentReadableImage(PRESENT_READABLE_IDENTIFIER);
    }

    @Test
    void HTTPS2() throws Exception {
        destroyEndpoint();
        server = new StaticFileServer();
        server.setRoot(TestUtils.getFixturePath());
        server.setHTTP1Enabled(false);
        server.setHTTP2Enabled(false);
        server.setHTTPS1Enabled(false);
        server.setHTTPS2Enabled(true);

        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HTTPSOURCE_URL_PREFIX, server.getHTTPSURI() + "/");

        doStatWithPresentReadableImage(PRESENT_READABLE_IDENTIFIER);
    }

    /**
     * Simulates a full usage cycle, verifying that no unnecessary requests are
     * made.
     */
    @Test
    void noUnnecessaryRequestsWithHEADRequestsEnabled() throws Exception {
        final RequestCountingHandler handler = new RequestCountingHandler();
        server.setHandler(handler);
        server.start();

        instance.stat();
        instance.getFormatIterator().next();

        try (ImageInputStream is = instance.newInputStream()) {
            IOUtils.consume(is);
        }

        assertEquals(1, handler.numHEADRequests);
        assertEquals(1, handler.numGETRequests);
    }

    /**
     * Simulates a full usage cycle, verifying that no unnecessary requests are
     * made.
     */
    @Test
    void noUnnecessaryRequestsWithHEADRequestsDisabled() throws Exception {
        var config = Configuration.forApplication();
        config.setProperty(Key.HTTPSOURCE_SEND_HEAD_REQUESTS, false);

        final RequestCountingHandler handler = new RequestCountingHandler();
        server.setHandler(handler);
        server.start();

        instance.stat();
        instance.getFormatIterator().next();

        try (ImageInputStream is = instance.newInputStream()) {
            IOUtils.consume(is);
        }

        assertEquals(0, handler.numHEADRequests);
        assertEquals(2, handler.numGETRequests);
    }

    /* getFormatIterator() */

    @Test
    void getFormatIteratorHasNext() {
        instance.setIdentifier(new Identifier("jpg.jpg"));

        HTTPSource.FormatIterator<Format> it = instance.getFormatIterator();

        assertTrue(it.hasNext());
        it.next(); // URI path extension
        assertTrue(it.hasNext());
        it.next(); // identifier extension
        assertTrue(it.hasNext());
        it.next(); // Content-Type is null
        assertTrue(it.hasNext());
        it.next(); // magic bytes
        assertFalse(it.hasNext());
    }

    @Test
    void getFormatIteratorNext() throws Exception {
        final String fixture = "sample-images/jpg-incorrect-extension.png";
        instance.setIdentifier(new Identifier(fixture));
        server.setHandler(new DefaultHandler() {
            @Override
            public boolean handle(Request request,
                                  Response response,
                                  Callback callback) throws Exception {
                response.getHeaders().put("Accept-Ranges", "bytes");
                try (OutputStream os = Content.Sink.asOutputStream(response)) {
                    Files.copy(TestUtils.getFixture(fixture), os);
                    callback.succeeded();
                }
                callback.succeeded();
                return true;
            }
        });
        server.start();

        HTTPSource.FormatIterator<Format> it = instance.getFormatIterator();
        assertEquals(Format.get("png"), it.next()); // URI path extension
        assertEquals(Format.get("png"), it.next()); // identifier extension
        assertEquals(Format.UNKNOWN, it.next());    // Content-Type is null
        assertEquals(Format.get("jpg"), it.next()); // magic bytes
        assertThrows(NoSuchElementException.class, it::next);
    }

    /* getRequestInfo() */

    @Test
    void getRequestInfoUsingBasicLookupStrategyWithPrefix() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                "http://example.org/prefix/");

        server.start();

        instance.setIdentifier(new Identifier("id"));
        assertEquals("http://example.org/prefix/id",
                instance.getRequestInfo().getURI());
    }

    @Test
    void getRequestInfoUsingBasicLookupStrategyWithPrefixAndSuffix()
            throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                "http://example.org/prefix/");
        config.setProperty(Key.HTTPSOURCE_URL_SUFFIX, "/suffix");

        server.start();

        instance.setIdentifier(new Identifier("id"));
        assertEquals("http://example.org/prefix/id/suffix",
                instance.getRequestInfo().toString());
    }

    @Test
    void getRequestInfoUsingBasicLookupStrategyWithoutPrefixOrSuffix()
            throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HTTPSOURCE_URL_PREFIX, "");
        config.setProperty(Key.HTTPSOURCE_URL_SUFFIX, "");

        server.start();

        instance.setIdentifier(new Identifier("http://example.org/images/image.jpg"));
        assertEquals("http://example.org/images/image.jpg",
                instance.getRequestInfo().toString());
    }

    @Test
    void getRequestInfoUsingDelegateLookupStrategyReturningString()
            throws Exception {
        useDelegateLookupStrategy();

        Identifier identifier = new Identifier("http-" +
                PRESENT_READABLE_IDENTIFIER);
        Delegate delegate = TestUtils.newDelegate();
        RequestContext requestContext = delegate.getRequestContext();
        requestContext.setIdentifier(identifier);
        requestContext.setRequestHeaders(Map.of());
        instance.setDelegate(delegate);

        server.start();

        instance.setIdentifier(identifier);
        assertEquals("http://example.org/bla/" + identifier,
                instance.getRequestInfo().getURI());
    }

    @Test
    void getRequestInfoUsingDelegateLookupStrategyWithContextReturningString()
            throws Exception {
        useDelegateLookupStrategy();

        final Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-Proto", "http");

        Delegate delegate = TestUtils.newDelegate();
        RequestContext requestContext = delegate.getRequestContext();
        requestContext.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        requestContext.setClientIP("1.2.3.4");
        delegate.getRequestContext().setRequestHeaders(headers);

        instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        instance.setDelegate(delegate);

        server.start();

        assertEquals("http://other-example.org/bleh/" + PRESENT_READABLE_IDENTIFIER,
                instance.getRequestInfo().getURI());
    }

    @Test
    void getRequestInfoUsingDelegateLookupStrategyReturningHash()
            throws Exception {
        useDelegateLookupStrategy();

        Identifier identifier = new Identifier(
                "http-sample-images/jpg/rgb-64x56x8-plane.jpg");
        Delegate delegate = TestUtils.newDelegate();
        RequestContext requestContext = delegate.getRequestContext();
        requestContext.setIdentifier(identifier);
        requestContext.setRequestHeaders(Map.of());
        instance.setDelegate(delegate);
        instance.setIdentifier(identifier);

        server.start();

        HTTPRequestInfo actual = instance.getRequestInfo();
        assertEquals("http://example.org/bla/" + identifier,
                actual.getURI());
        assertEquals("username", actual.getUsername());
        assertEquals("secret", actual.getSecret());
        Headers headers = actual.getHeaders();
        assertEquals("yes", headers.getFirstValue("X-Custom"));
        assertTrue(actual.isSendingHeadRequest());
    }

    @Test
    void getRequestInfoUsingDelegateLookupStrategyReturningNil()
            throws Exception {
        useDelegateLookupStrategy();
        server.start();

        Identifier identifier = new Identifier("bogus");
        Delegate delegate = TestUtils.newDelegate();
        RequestContext requestContext = delegate.getRequestContext();
        requestContext.setIdentifier(identifier);
        requestContext.setRequestHeaders(Map.of());
        instance.setDelegate(delegate);
        instance.setIdentifier(identifier);

        assertThrows(NoSuchFileException.class, instance::getRequestInfo);
    }

    /* newInputStream() */

    @Test
    void newInputStreamUsingBasicLookupStrategyWithValidAuthentication()
            throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HTTPSOURCE_BASIC_AUTH_USERNAME,
                StaticFileServer.BASIC_USER);
        config.setProperty(Key.HTTPSOURCE_BASIC_AUTH_SECRET,
                StaticFileServer.BASIC_SECRET);

        server.setBasicAuthEnabled(true);
        server.start();

        instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        try (ImageInputStream is = instance.newInputStream()) {
            assertNotNull(is);
        }
    }

    @Test
    void newInputStreamUsingBasicLookupStrategyWithPresentReadableImage()
            throws Exception {
        doTestNewInputStreamWithPresentReadableImage(PRESENT_READABLE_IDENTIFIER);
    }

    @Test
    void newInputStreamUsingDelegateLookupStrategyWithValidAuthentication()
            throws Exception {
        useDelegateLookupStrategy();

        server.setBasicAuthEnabled(true);
        server.start();

        Reference uri = server.getHTTPURI().rebuilder()
                .appendPath("/" + PRESENT_READABLE_IDENTIFIER)
                .build();
        Identifier identifier = new Identifier("valid-auth-" + uri);
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(identifier);
        instance.setDelegate(delegate);
        instance.setIdentifier(identifier);

        try (ImageInputStream is = instance.newInputStream()) {
            assertNotNull(is);
        }
    }

    @Test
    void newInputStreamUsingDelegateLookupStrategyWithPresentReadableImage()
            throws Exception {
        useDelegateLookupStrategy();
        Reference uri = server.getHTTPURI().rebuilder()
                .appendPath("/" + PRESENT_READABLE_IDENTIFIER)
                .build();
        Identifier identifier = new Identifier(uri.toString());
        doTestNewInputStreamWithPresentReadableImage(identifier);
    }

    private void doTestNewInputStreamWithPresentReadableImage(Identifier identifier)
            throws Exception {
        server.start();

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(identifier);
        instance.setDelegate(delegate);
        instance.setIdentifier(identifier);

        try (ImageInputStream is = instance.newInputStream()) {
            assertNotNull(is);
        }
    }

    /* stat() */

    @Test
    void statUsingBasicLookupStrategyWithPresentUnreadableImage()
            throws Exception {
        doStatWithPresentUnreadableImage(new Identifier("gif"));
    }

    @Test
    void statUsingDelegateLookupStrategyWithPresentReadableImage()
            throws Exception {
        useDelegateLookupStrategy();
        Reference uri = server.getHTTPURI().rebuilder()
                .appendPath("/" + PRESENT_READABLE_IDENTIFIER)
                .build();
        Identifier identifier = new Identifier(uri.toString());
        doStatWithPresentReadableImage(identifier);
    }

    @Test
    void statUsingDelegateLookupStrategyWithMissingImage() throws Exception {
        useDelegateLookupStrategy();
        Reference uri = server.getHTTPURI().rebuilder()
                .appendPath("/bogus")
                .build();
        Identifier identifier = new Identifier(uri.toString());
        doStatWithMissingImage(identifier);
    }

    @Test
    void statUsingDelegateLookupStrategyWithPresentUnreadableImage()
            throws Exception {
        useDelegateLookupStrategy();
        Reference uri = server.getHTTPURI().rebuilder()
                .appendPath("/gif")
                .build();
        Identifier identifier = new Identifier(uri.toString());
        doStatWithPresentUnreadableImage(identifier);
    }

    private void doStatWithPresentReadableImage(Identifier identifier)
            throws Exception {
        server.start();

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(identifier);
        instance.setDelegate(delegate);
        instance.setIdentifier(identifier);

        instance.stat();
    }

    private void doStatWithPresentUnreadableImage(Identifier identifier)
            throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public boolean handle(Request request,
                                  Response response,
                                  Callback callback) {
                response.setStatus(403);
                callback.succeeded();
                return true;
            }
        });
        server.start();

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(identifier);
        instance.setDelegate(delegate);
        instance.setIdentifier(identifier);
        instance.setIdentifier(identifier);
        assertThrows(AccessDeniedException.class, instance::stat);
    }

    private void doStatWithMissingImage(Identifier identifier)
            throws Exception {
        try {
            server.start();

            Delegate delegate = TestUtils.newDelegate();
            delegate.getRequestContext().setIdentifier(identifier);
            instance.setDelegate(delegate);
            instance.setIdentifier(identifier);

            instance.stat();
            fail("Expected exception");
        } catch (NoSuchFileException e) {
            // pass
        }
    }

    @Test
    void statUsingDelegateLookupStrategyWithValidAuthentication()
            throws Exception {
        useDelegateLookupStrategy();

        server.setBasicAuthEnabled(true);
        server.start();

        Reference uri = server.getHTTPURI().rebuilder()
                .appendPath("/" + PRESENT_READABLE_IDENTIFIER)
                .build();
        Identifier identifier = new Identifier("valid-auth-" + uri);
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(identifier);
        instance.setDelegate(delegate);
        instance.setIdentifier(identifier);

        instance.stat();
    }

    @Test
    void statUsingDelegateLookupStrategyWithInvalidAuthentication()
            throws Exception {
        useDelegateLookupStrategy();

        server.setBasicAuthEnabled(true);
        server.start();

        Reference uri = server.getHTTPURI().rebuilder()
                .appendPath("/" + PRESENT_READABLE_IDENTIFIER)
                .build();
        Identifier identifier = new Identifier("invalid-auth-" + uri);
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(identifier);
        instance.setDelegate(delegate);
        instance.setIdentifier(identifier);

        assertThrows(AccessDeniedException.class, instance::stat);
    }

    @Test
    void statWith403Response() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public boolean handle(Request request,
                                  Response response,
                                  Callback callback) {
                response.setStatus(403);
                callback.succeeded();
                return true;
            }
        });
        server.start();

        try {
            instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
            instance.stat();
            fail("Expected exception");
        } catch (AccessDeniedException e) {
            assertTrue(e.getMessage().contains("403"));
        }
    }

    @Test
    void statWith500Response() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public boolean handle(Request request,
                                  Response response,
                                  Callback callback) {
                response.setStatus(500);
                callback.succeeded();
                return true;
            }
        });
        server.start();

        try {
            instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
            instance.stat();
            fail("Expected exception");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("500"));
        }
    }

    @Disabled
    @Test
    void statUsingProxy() throws Exception {
        server.start();

        final int proxyPort = SocketUtils.getOpenPort();

        // Set up the proxy
        // TODO; write this

        // Set up HTTPSource
        final Configuration config = Configuration.forApplication();
        config.setProperty(Key.HTTPSOURCE_HTTP_PROXY_HOST, "127.0.0.1");
        config.setProperty(Key.HTTPSOURCE_HTTP_PROXY_PORT, proxyPort);

        // Expect no exception
        instance.stat();
    }

    @Test
    void statSendsUserAgentHeader() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public boolean handle(Request request,
                                  Response response,
                                  Callback callback) {
                String expected = String.format("%s/%s (%s/%s; java/%s; %s/%s)",
                        HTTPSource.class.getSimpleName(),
                        Application.getVersion(),
                        Application.getName(),
                        Application.getVersion(),
                        System.getProperty("java.version"),
                        System.getProperty("os.name"),
                        System.getProperty("os.version"));
                assertEquals(expected, request.getHeaders().get("User-Agent"));
                callback.succeeded();
                return true;
            }
        });
        server.start();

        instance.stat();
    }

    @Test
    void statSendsCustomHeaders() throws Exception {
        useDelegateLookupStrategy();

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

        Reference uri = server.getHTTPURI().rebuilder()
                .appendPath("/" + PRESENT_READABLE_IDENTIFIER)
                .build();
        Identifier identifier = new Identifier(uri.toString());
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(identifier);
        instance.setDelegate(delegate);
        instance.setIdentifier(identifier);

        instance.stat();
    }

    @Test
    void statWithMalformedURI() throws Exception {
        server.start();

        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HTTPSOURCE_URL_PREFIX, "");

        Reference uri = server.getHTTPURI().rebuilder()
                .appendPath("/" + PRESENT_READABLE_IDENTIFIER)
                .build();
        Identifier identifier = new Identifier("valid-auth-" +
                uri.toString().replace("://", "///"));
        instance.setIdentifier(identifier);

        assertThrows(IOException.class, () -> instance.stat());
    }

}
