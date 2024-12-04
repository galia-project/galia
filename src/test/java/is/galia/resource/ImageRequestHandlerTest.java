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
import is.galia.delegate.Delegate;
import is.galia.stream.CompletableOutputStream;
import is.galia.cache.InfoCache;
import is.galia.cache.VariantCache;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Reference;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.image.Info;
import is.galia.image.MetaIdentifier;
import is.galia.image.StatResult;
import is.galia.operation.Encode;
import is.galia.operation.OperationList;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageRequestHandlerTest extends BaseTest {

    @Nested
    class BuilderTest {

        @Test
        void buildWithNullOperationList() {
            assertThrows(NullPointerException.class, () ->
                    ImageRequestHandler.builder()
                            .withReference(new Reference("http://example.org"))
                            .withRequestContext(new RequestContext())
                            .build());
        }

        @Test
        void buildWithNullReference() {
            assertThrows(NullPointerException.class, () ->
                    ImageRequestHandler.builder()
                            .withRequestContext(new RequestContext())
                            .withOperationList(new OperationList())
                            .build());
        }

        @Test
        void buildWithNullRequestContext() {
            assertThrows(NullPointerException.class, () ->
                    ImageRequestHandler.builder()
                            .withReference(new Reference("http://example.org"))
                            .withOperationList(new OperationList())
                            .build());
        }

        @Test
        void buildWithValidArguments() {
            Delegate delegate             = TestUtils.newDelegate();
            OperationList opList          = new OperationList();
            RequestContext requestContext = new RequestContext();
            ImageRequestHandler handler   = ImageRequestHandler.builder()
                    .withReference(new Reference("http://example.org"))
                    .withDelegate(delegate)
                    .withOperationList(opList)
                    .withRequestContext(requestContext)
                    .build();
            assertNotNull(handler);
            assertSame(delegate, handler.delegate);
            assertSame(requestContext, handler.requestContext);
        }

    }

    private static class IntrospectiveCallback implements ImageRequestHandler.Callback {
        private boolean isAuthorizeBeforeAccessCalled, isAuthorizeCalled,
                isSourceAccessedCalled,
                isWillStreamImageFromVariantCacheCalled,
                isInfoAvailableCalled, isWillProcessImageCalled;

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
        public void willStreamImageFromVariantCache(StatResult result) {
            isWillStreamImageFromVariantCacheCalled = true;
        }

        @Override
        public void infoAvailable(Info info) {
            isInfoAvailableCalled = true;
        }

        @Override
        public void willProcessImage(Info info) {
            isWillProcessImageCalled = true;
        }
    }

    private static final Identifier IDENTIFIER =
            new Identifier("jpg/rgb-64x56x8-baseline.jpg");

    @Test
    void handleCallsPreAuthorizationCallback() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(IDENTIFIER);
        opList.add(new Encode(Format.get("jpg")));

        IntrospectiveCallback callback = new IntrospectiveCallback();
        ImageRequestHandler handler    = ImageRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withOperationList(opList)
                .withRequestContext(new RequestContext())
                .build();
        handler.handle(new MockResponse());
        assertTrue(callback.isAuthorizeBeforeAccessCalled);
    }

    @Test
    void handleCallsAuthorizationCallback() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(IDENTIFIER);
        opList.add(new Encode(Format.get("jpg")));

        IntrospectiveCallback callback = new IntrospectiveCallback();
        ImageRequestHandler handler    = ImageRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withOperationList(opList)
                .withRequestContext(new RequestContext())
                .build();
        handler.handle(new MockResponse());
        assertTrue(callback.isAuthorizeCalled);
    }

    @Test
    void handleCallsSourceAccessedCallbackWhenResolvingFirst() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(IDENTIFIER);
        opList.add(new Encode(Format.get("jpg")));

        IntrospectiveCallback callback = new IntrospectiveCallback();
        ImageRequestHandler handler    = ImageRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withOperationList(opList)
                .withRequestContext(new RequestContext())
                .build();
        handler.handle(new MockResponse());
        assertTrue(callback.isSourceAccessedCalled);
    }

    @Test
    void handleCallsSourceAccessedCallbackWhenNotResolvingFirst()
            throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(IDENTIFIER);
        opList.add(new Encode(Format.get("jpg")));

        IntrospectiveCallback callback = new IntrospectiveCallback();
        ImageRequestHandler handler    = ImageRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withOperationList(opList)
                .withRequestContext(new RequestContext())
                .build();
        handler.handle(new MockResponse());
        assertTrue(callback.isSourceAccessedCalled);
    }

    @Test
    void handleCallsCacheStreamingCallback() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
            config.setProperty(Key.VARIANT_CACHE_ENABLED, true);
            config.setProperty(Key.VARIANT_CACHE, "HeapCache");
            config.setProperty(Key.INFO_CACHE_ENABLED, true);
            config.setProperty(Key.INFO_CACHE, "HeapCache");
            config.setProperty(Key.HEAPCACHE_TARGET_SIZE, "10M");
        }

        // Configure the request.
        final OperationList opList = new OperationList();
        opList.setIdentifier(IDENTIFIER);
        Encode encode = new Encode(Format.get("jpg"));
        opList.add(encode);

        // Add an info to the info cache.
        CacheFacade facade = new CacheFacade();
        InfoCache infoCache = facade.getInfoCache().orElseThrow();
        Info info = Info.builder()
                .withSize(64, 48)
                .withFormat(Format.get("jpg"))
                .withIdentifier(IDENTIFIER)
                .build();
        infoCache.put(IDENTIFIER, info);

        // Add an "image" to the variant cache.
        VariantCache variantCache = facade.getVariantCache().orElseThrow();
        try (CompletableOutputStream os =
                     variantCache.newVariantImageOutputStream(opList)) {
            os.write(new byte[] { 0x35, 0x35, 0x35 });
            os.complete();
        }

        IntrospectiveCallback callback = new IntrospectiveCallback();
        ImageRequestHandler handler    = ImageRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withOperationList(opList)
                .withRequestContext(new RequestContext())
                .build();
        handler.handle(new MockResponse());
        assertTrue(callback.isWillStreamImageFromVariantCacheCalled);
    }

    @Test
    void handleCallsInfoAvailableCallback() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(IDENTIFIER);
        opList.add(new Encode(Format.get("jpg")));

        IntrospectiveCallback callback = new IntrospectiveCallback();
        ImageRequestHandler handler    = ImageRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withOperationList(opList)
                .withRequestContext(new RequestContext())
                .build();
        handler.handle(new MockResponse());
        assertTrue(callback.isInfoAvailableCalled);
    }

    @Test
    void handleCallsProcessingCallback() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(IDENTIFIER);
        opList.add(new Encode(Format.get("jpg")));

        IntrospectiveCallback callback = new IntrospectiveCallback();
        ImageRequestHandler handler    = ImageRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withOperationList(opList)
                .withRequestContext(new RequestContext())
                .build();
        handler.handle(new MockResponse());
        assertTrue(callback.isWillProcessImageCalled);
    }

    @Test
    void handleProcessesImage() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(IDENTIFIER);
        opList.add(new Encode(Format.get("jpg")));

        IntrospectiveCallback callback = new IntrospectiveCallback();
        ImageRequestHandler handler    = ImageRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withOperationList(opList)
                .withRequestContext(new RequestContext())
                .build();
        MockResponse response = new MockResponse();
        handler.handle(response);
        assertTrue(response.getOutputStream().toByteArray().length > 5000);
    }

    @Test
    void handleStreamsFromVariantCache() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
            config.setProperty(Key.INFO_CACHE_ENABLED, true);
            config.setProperty(Key.INFO_CACHE, "HeapCache");
            config.setProperty(Key.VARIANT_CACHE_ENABLED, true);
            config.setProperty(Key.VARIANT_CACHE, "HeapCache");
        }

        // Configure the request.
        final OperationList opList = new OperationList();
        opList.setIdentifier(IDENTIFIER);
        Encode encode = new Encode(Format.get("jpg"));
        opList.add(encode);

        // Add an info to the info cache.
        CacheFacade facade = new CacheFacade();
        InfoCache infoCache = facade.getInfoCache().orElseThrow();
        Info info = Info.builder()
                .withSize(64, 48)
                .withFormat(Format.get("jpg"))
                .withIdentifier(IDENTIFIER)
                .build();
        infoCache.put(IDENTIFIER, info);

        // Add an "image" to the variant cache.
        VariantCache variantCache = facade.getVariantCache().orElseThrow();
        final byte[] expected = new byte[] { 0x35, 0x35, 0x35 };
        try (CompletableOutputStream os =
                     variantCache.newVariantImageOutputStream(opList)) {
            os.write(expected);
            os.complete();
        }

        IntrospectiveCallback callback = new IntrospectiveCallback();
        ImageRequestHandler handler    = ImageRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withOperationList(opList)
                .withRequestContext(new RequestContext())
                .build();
        MockResponse response = new MockResponse();
        handler.handle(response);
        assertArrayEquals(expected, response.getOutputStream().toByteArray());
    }

    @Test
    void handleWithFailedPreAuthorization() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(IDENTIFIER);
        opList.add(new Encode(Format.get("jpg")));

        ImageRequestHandler handler = ImageRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(new ImageRequestHandler.Callback() {
                    @Override
                    public boolean authorizeBeforeAccess() {
                        return false;
                    }
                    @Override
                    public boolean authorize() {
                        return true;
                    }
                    @Override
                    public void sourceAccessed(StatResult result) {
                    }
                    @Override
                    public void willStreamImageFromVariantCache(StatResult result) {
                    }
                    @Override
                    public void infoAvailable(Info info) {
                    }
                    @Override
                    public void willProcessImage(Info info) {
                    }
                })
                .withOperationList(opList)
                .withRequestContext(new RequestContext())
                .build();
        MockResponse response = new MockResponse();
        handler.handle(response);
        assertNull(response.getOutputStream());
    }

    @Test
    void handleWithFailedAuthorization() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(IDENTIFIER);
        opList.add(new Encode(Format.get("jpg")));

        ImageRequestHandler handler = ImageRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(new ImageRequestHandler.Callback() {
                    @Override
                    public boolean authorizeBeforeAccess() {
                        return true;
                    }
                    @Override
                    public boolean authorize() {
                        return false;
                    }
                    @Override
                    public void sourceAccessed(StatResult result) {
                    }
                    @Override
                    public void willStreamImageFromVariantCache(StatResult result) {
                    }
                    @Override
                    public void infoAvailable(Info info) {
                    }
                    @Override
                    public void willProcessImage(Info info) {
                    }
                })
                .withOperationList(opList)
                .withRequestContext(new RequestContext())
                .build();
        MockResponse response = new MockResponse();
        handler.handle(response);
        assertNull(response.getOutputStream());
    }

    @Test
    void handleWithIllegalPageIndex() {
        { // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
        }

        // Configure the request.
        MetaIdentifier metaIdentifier = MetaIdentifier.builder()
                .withIdentifier(IDENTIFIER)
                .withPageNumber(9999)
                .build();
        OperationList opList = OperationList.builder()
                .withMetaIdentifier(metaIdentifier)
                .withOperations(new Encode(Format.get("jpg")))
                .build();

        IntrospectiveCallback callback = new IntrospectiveCallback();
        ImageRequestHandler handler    = ImageRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withOperationList(opList)
                .withRequestContext(new RequestContext())
                .build();
        assertThrows(IllegalClientArgumentException.class, () ->
                handler.handle(new MockResponse()));
    }

    @Test
    void handleWithInvalidOperationList() {
        { // Configure the application.
            final Configuration config = Configuration.forApplication();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtils.getSampleImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList = new OperationList(IDENTIFIER);

        IntrospectiveCallback callback = new IntrospectiveCallback();
        ImageRequestHandler handler = ImageRequestHandler.builder()
                .withReference(new Reference("http://example.org"))
                .withCallback(callback)
                .withOperationList(opList)
                .withRequestContext(new RequestContext())
                .build();
        assertThrows(NullPointerException.class, () ->
                handler.handle(new MockResponse()));
    }

}
