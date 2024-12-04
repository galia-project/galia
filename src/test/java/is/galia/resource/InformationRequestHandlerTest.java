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

import is.galia.cache.CacheFacade;
import is.galia.cache.HeapInfoCache;
import is.galia.cache.InfoCache;
import is.galia.codec.Decoder;
import is.galia.codec.DecoderFactory;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.delegate.Delegate;
import is.galia.http.Reference;
import is.galia.image.InfoReader;
import is.galia.image.MutableMetadata;
import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.image.Info;
import is.galia.image.Metadata;
import is.galia.image.StatResult;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;

import static org.junit.jupiter.api.Assertions.*;

class InformationRequestHandlerTest extends BaseTest {

    @Nested
    class BuilderTest {

        @Test
        void buildWithNullIdentifier() {
            assertThrows(NullPointerException.class, () ->
                    InformationRequestHandler.builder()
                            .withReference(new Reference("http://example.org"))
                            .withRequestContext(new RequestContext())
                            .build());
        }

        @Test
        void buildWithNullReference() {
            assertThrows(NullPointerException.class, () ->
                    InformationRequestHandler.builder()
                            .withRequestContext(new RequestContext())
                            .withIdentifier(new Identifier("cats"))
                            .build());
        }

        @Test
        void buildWithNullRequestContext() {
            assertThrows(NullPointerException.class, () ->
                    InformationRequestHandler.builder()
                            .withReference(new Reference("http://example.org"))
                            .withIdentifier(new Identifier("cats"))
                            .build());
        }

        @Test
        void buildWithValidArguments() {
            Delegate delegate                 = TestUtils.newDelegate();
            Identifier identifier             = new Identifier("cats");
            RequestContext requestContext     = new RequestContext();
            InformationRequestHandler handler = InformationRequestHandler.builder()
                    .withReference(new Reference("http://example.org"))
                    .withDelegate(delegate)
                    .withIdentifier(identifier)
                    .withRequestContext(requestContext)
                    .build();
            assertNotNull(handler);
            assertSame(delegate, handler.delegate);
            assertSame(requestContext, handler.requestContext);
        }

    }

    private static class IntrospectiveCallback
            implements InformationRequestHandler.Callback {
        private boolean isAuthorizeBeforeAccessCalled, isAuthorizeCalled,
                isSourceAccessedCalled, isCacheAccessedCalled;

        @Override
        public boolean authorizeBeforeAccess() {
            isAuthorizeBeforeAccessCalled = true;
            return true;
        }

        @Override
        public boolean authorize() {
            isAuthorizeCalled = true;
            return true;
        }

        @Override
        public void sourceAccessed(StatResult result) {
            isSourceAccessedCalled = true;
        }

        @Override
        public void cacheAccessed(StatResult result) {
            isCacheAccessedCalled = true;
        }
    }

    private static final Identifier IDENTIFIER =
            new Identifier("jpg/rgb-64x56x8-baseline.jpg");

    @Test
    void handleCallsAuthorizationCallback() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
        }

        IntrospectiveCallback callback    = new IntrospectiveCallback();
        InformationRequestHandler handler = InformationRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withIdentifier(IDENTIFIER)
                .withRequestContext(new RequestContext())
                .build();
        handler.handle();
        assertTrue(callback.isAuthorizeBeforeAccessCalled);
        assertTrue(callback.isAuthorizeCalled);
    }

    @Test
    void handleCallsSourceAccessedCallbackWhenResolvingFirst() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
        }

        IntrospectiveCallback callback    = new IntrospectiveCallback();
        InformationRequestHandler handler = InformationRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withIdentifier(IDENTIFIER)
                .withRequestContext(new RequestContext())
                .build();
        handler.handle();
        assertTrue(callback.isSourceAccessedCalled);
        assertFalse(callback.isCacheAccessedCalled);
    }

    @Test
    void handleCallsSourceAccessedCallbackWhenNotResolvingFirstAndNoCachedInstanceAvailable()
            throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
        }

        IntrospectiveCallback callback    = new IntrospectiveCallback();
        InformationRequestHandler handler = InformationRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withIdentifier(IDENTIFIER)
                .withRequestContext(new RequestContext())
                .build();
        handler.handle();
        assertTrue(callback.isSourceAccessedCalled);
        assertFalse(callback.isCacheAccessedCalled);
    }

    @Test
    void handleCallsCacheAccessedCallbackWhenNotResolvingFirstAndInstanceAvailableInInfoCache()
            throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
            config.setProperty(Key.INFO_CACHE_ENABLED, true);
            config.setProperty(Key.INFO_CACHE, "HeapCache");
            config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
        }

        CacheFacade facade = new CacheFacade();
        InfoCache cache = facade.getInfoCache().orElseThrow();
        Info info = Info.builder()
                .withSize(64, 56)
                .withFormat(Format.get("jpg"))
                .withIdentifier(IDENTIFIER)
                .build();
        cache.put(IDENTIFIER, info);

        IntrospectiveCallback callback    = new IntrospectiveCallback();
        InformationRequestHandler handler = InformationRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withIdentifier(IDENTIFIER)
                .withRequestContext(new RequestContext())
                .build();
        handler.handle();
        assertFalse(callback.isSourceAccessedCalled);
        assertTrue(callback.isCacheAccessedCalled);
    }

    @Test
    void handleCallsCacheAccessedCallbackWhenNotResolvingFirstAndInstanceAvailableInHeapInfoCache()
            throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
            config.setProperty(Key.INFO_CACHE_ENABLED, false);
            config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, true);
        }

        CacheFacade facade = new CacheFacade();
        HeapInfoCache cache = facade.getHeapInfoCache().orElseThrow();
        Info info = Info.builder()
                .withSize(64, 56)
                .withFormat(Format.get("jpg"))
                .withIdentifier(IDENTIFIER)
                .build();
        cache.put(IDENTIFIER, info);

        IntrospectiveCallback callback    = new IntrospectiveCallback();
        InformationRequestHandler handler = InformationRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withIdentifier(IDENTIFIER)
                .withRequestContext(new RequestContext())
                .build();
        handler.handle();
        assertFalse(callback.isSourceAccessedCalled);
        assertTrue(callback.isCacheAccessedCalled);
    }

    @Test
    void handleReturnsInstanceFromInfoCache() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
            config.setProperty(Key.INFO_CACHE_ENABLED, true);
            config.setProperty(Key.INFO_CACHE, "HeapCache");
            config.setProperty(Key.HEAPCACHE_TARGET_SIZE, "10M");
            config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
        }

        CacheFacade facade = new CacheFacade();
        facade.purge();

        // Read an Info from an image.
        Format format         = Format.get("jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(format, arena)) {
            decoder.setSource(TestUtils.getSampleImage(IDENTIFIER.toString()));
            InfoReader infoReader = new InfoReader();
            infoReader.setDecoder(decoder);
            infoReader.setFormat(format);
            Info expected = infoReader.read();
            expected.setIdentifier(IDENTIFIER);

            // Add it to the info cache.
            InfoCache cache = facade.getInfoCache().orElseThrow();
            cache.put(IDENTIFIER, expected);

            // Read another info from our test subject and compare it to the
            // canonical one.
            InformationRequestHandler handler = InformationRequestHandler.builder()
                    .withReference(new Reference("http://example.org"))
                    .withIdentifier(IDENTIFIER)
                    .withRequestContext(new RequestContext())
                    .build();
            Info cachedInfo = handler.handle();
            assertEquals(expected, cachedInfo);
        }
    }

    @Test
    void handleReturnsInstanceFromHeapInfoCache() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
            config.setProperty(Key.INFO_CACHE_ENABLED, false);
            config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, true);
            config.setProperty(Key.HEAPCACHE_TARGET_SIZE, "10M");
        }

        CacheFacade facade = new CacheFacade();
        facade.purge();

        // Read an Info from an image.
        Format format         = Format.get("jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(format, arena)) {
            decoder.setSource(TestUtils.getSampleImage(IDENTIFIER.toString()));
            InfoReader infoReader = new InfoReader();
            infoReader.setDecoder(decoder);
            infoReader.setFormat(format);
            Info expected = infoReader.read();
            expected.setIdentifier(IDENTIFIER);

            // Add it to the heap info cache.
            HeapInfoCache cache = facade.getHeapInfoCache().orElseThrow();
            cache.put(IDENTIFIER, expected);

            // Read another info from our test subject and compare it to the
            // canonical one.
            InformationRequestHandler handler = InformationRequestHandler.builder()
                    .withReference(new Reference("http://example.org"))
                    .withIdentifier(IDENTIFIER)
                    .withRequestContext(new RequestContext())
                    .build();
            Info cachedInfo = handler.handle();
            assertEquals(expected, cachedInfo);
        }
    }

    @Test
    void handleSetsRequestContextKeysBeforeReturningInstanceFromInfoCache()
            throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
            config.setProperty(Key.INFO_CACHE_ENABLED, true);
            config.setProperty(Key.INFO_CACHE, "HeapCache");
        }

        // Configure the request.
        final Metadata metadata = new MutableMetadata();

        // Add an info to the info cache.
        CacheFacade facade = new CacheFacade();
        InfoCache cache = facade.getInfoCache().orElseThrow();
        Info info = Info.builder()
                .withSize(64, 56)
                .withFormat(Format.get("jpg"))
                .withIdentifier(IDENTIFIER)
                .withMetadata(metadata)
                .build();
        cache.put(IDENTIFIER, info);

        IntrospectiveCallback callback    = new IntrospectiveCallback();
        RequestContext context            = new RequestContext();
        InformationRequestHandler handler = InformationRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withIdentifier(IDENTIFIER)
                .withRequestContext(context)
                .build();
        handler.handle();
        assertEquals(1, context.getPageCount());
        assertEquals(new Size(64, 56), context.getFullSize());
        assertNotNull(context.getMetadata());
    }

    @Test
    void handleReturnsInstanceFromProcessor() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
        }

        IntrospectiveCallback callback    = new IntrospectiveCallback();
        InformationRequestHandler handler = InformationRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withIdentifier(IDENTIFIER)
                .withRequestContext(new RequestContext())
                .build();
        Info info = handler.handle();
        assertNotNull(info);
    }

    @Test
    void handleSetsRequestContextPageCountBeforeReturningInstanceFromProcessor()
            throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
        }

        IntrospectiveCallback callback    = new IntrospectiveCallback();
        RequestContext context            = new RequestContext();
        InformationRequestHandler handler = InformationRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withIdentifier(IDENTIFIER)
                .withRequestContext(context)
                .build();
        handler.handle();
        assertEquals(1, context.getPageCount());
    }

    @Test
    void handleReturnsNullWhenAuthorizationFails() throws Exception {
        InformationRequestHandler handler = InformationRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(new InformationRequestHandler.Callback() {
                    @Override
                    public boolean authorizeBeforeAccess() { return false; }
                    @Override
                    public boolean authorize() { return false; }
                    @Override
                    public void sourceAccessed(StatResult result) {}
                    @Override
                    public void cacheAccessed(StatResult result) {}
                })
                .withIdentifier(IDENTIFIER)
                .withRequestContext(new RequestContext())
                .build();
        Info info = handler.handle();
        assertNull(info);
    }

}
