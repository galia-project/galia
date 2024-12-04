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

package is.galia.resource.iiif;

import is.galia.cache.CacheFacade;
import is.galia.cache.HeapCache;
import is.galia.cache.MockBrokenVariantInputStreamCache;
import is.galia.cache.MockBrokenVariantOutputStreamCache;
import is.galia.cache.VariantCache;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.delegate.Delegate;
import is.galia.http.Client;
import is.galia.http.ClientFactory;
import is.galia.http.Reference;
import is.galia.http.HTTPException;
import is.galia.http.Response;
import is.galia.http.Status;
import is.galia.http.Transport;
import is.galia.source.AccessDeniedSource;

import java.io.IOException;
import java.time.Instant;

import static is.galia.test.Assert.HTTPAssert.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Collection of tests common across major versions of IIIF Image and
 * Information endpoints.
 */
public class ImageAPIResourceTester {

    static final String IMAGE = "jpg/rgb-64x56x8-baseline.jpg";

    Client newClient(Reference uri) {
        Client client = ClientFactory.newClient();
        client.setURI(uri);
        return client;
    }

    public void testAuthorizationWhenAuthorized(Reference uri) {
        assertStatus(200, uri);
    }

    public void testAuthorizationWhenNotAuthorizedWhenAccessingCachedResource(Reference uri)
            throws Exception {
        initializeVariantCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, true);
        config.setProperty(Key.VARIANT_CACHE_TTL, 10);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);

        // Request the resource to cache it.
        // This status code may vary depending on the return value of a
        // delegate method, but the way the tests are set up, it's 403.
        assertStatus(403, uri);

        Thread.sleep(1000); // the resource may write asynchronously

        // Request it again. We expect to receive the same response. Any
        // different response would indicate a logic error.
        assertStatus(403, uri);
    }

    public void testAuthorizationWhenScaleConstraining(Reference uri)
            throws Exception {
        Client client = newClient(uri);
        try (Response response = client.send()) {
            assertEquals(Status.FOUND, response.getStatus());
            assertEquals(uri.toString().replace("reduce.jpg", "reduce.jpg;1:2"),
                    response.getHeaders().getFirstValue("Location"));
        } finally {
            client.stop();
        }
    }

    public void testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable(Reference uri)
            throws Exception {
        enableCacheControlHeaders();

        Client client = newClient(uri);
        try (Response response = client.send()) {
            String header = response.getHeaders().getFirstValue("Cache-Control");
            assertTrue(header.contains("max-age=1234"));
            assertTrue(header.contains("s-maxage=4567"));
            assertTrue(header.contains("public"));
            assertTrue(header.contains("no-transform"));
        } finally {
            client.stop();
        }
    }

    public void testCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable(Reference uri)
            throws Exception {
        enableCacheControlHeaders();

        Client client = newClient(uri);
        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals("no-cache, no-store, must-revalidate",
                    e.getResponse().getHeaders().getFirstValue("Cache-Control"));
        } finally {
            client.stop();
        }
    }

    /**
     * Tests that there is no {@code Cache-Control} header returned when
     * {@code cache.client.enabled = true} but a {@code cache=false} argument
     * is present in the URL query.
     */
    public void testCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL(Reference uri)
            throws Exception {
        enableCacheControlHeaders();

        Client client = newClient(uri);
        try (Response response = client.send()) {
            assertNull(response.getHeaders().getFirstValue("Cache-Control"));
        } finally {
            client.stop();
        }
    }

    public void testCacheHeadersWhenClientCachingIsDisabled(Reference uri)
            throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.CLIENT_CACHE_ENABLED, false);

        Client client = newClient(uri);
        try (Response response = client.send()) {
            assertNull(response.getHeaders().getFirstValue("Cache-Control"));
        } finally {
            client.stop();
        }
    }

    public void testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(
            Reference uri) throws Exception {
        HeapCache cache = initializeVariantCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, true);

        // request an info
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // it may write asynchronously
            // assert that an info has not been added to the heap info cache
            assertEquals(0, new CacheFacade().getHeapInfoCacheSize());

            // assert that neither an image nor an info have been added to the
            // variant or info caches
            assertEquals(0, cache.size());
        } finally {
            client.stop();
        }
    }

    /**
     * <p>Tests that all delegate context keys are set that should be set at
     * appropriate times during the request cycle. The first test is in {@link
     * {@link Delegate#authorizeBeforeAccess()} (when not all keys are available yet)
     * and the second is in {@link {@link Delegate#authorize()}} (where all
     * are).</p>
     *
     * <p>N.B.: When this test fails, it's usually because {@link
     * Client#send()} is throwing an HTTP 500 {@link HTTPException} with no
     * message. Set a breakpoint there to see the response message.</p>
     *
     * @param uri IIIF image or information URI.
     */
    public void testDelegateContext(Reference uri)
            throws Exception {
        Client client = newClient(uri);
        try {
            client.send().close();
        } finally {
            client.stop();
        }
    }

    public void testForwardSlashInIdentifier(Reference uri) {
        assertStatus(200, uri);
    }

    public void testBackslashInIdentifier(Reference uri) {
        assertStatus(200, uri);
    }

    public void testIllegalCharactersInIdentifier(Reference uri) {
        // TODO: write this
    }

    public void testHTTP2(Reference uri) throws Exception {
        Client client = newClient(uri);
        client.setTransport(Transport.HTTP2_0);

        try (Response response = client.send()) {
        } finally {
            client.stop();
        }
    }

    public void testHTTPS1_1(Reference uri) throws Exception {
        Client client = newClient(uri);
        client.setTrustAll(true);

        try (Response response = client.send()) {
        } finally {
            client.stop();
        }
    }

    public void testHTTPS2(Reference uri) throws Exception {
        Client client = newClient(uri);
        client.setTransport(Transport.HTTP2_0);
        client.setTrustAll(true);

        try (Response response = client.send()) {
        } finally {
            client.stop();
        }
    }

    public void testForbidden(Reference uri) {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.SOURCE_STATIC,
                AccessDeniedSource.class.getName());

        assertStatus(403, uri);
    }

    public void testNotFound(Reference uri) {
        assertStatus(404, uri);
    }

    /**
     * Tests recovery from an exception thrown by
     * {@link VariantCache#newVariantImageInputStream}.
     */
    public void testRecoveryFromVariantCacheNewVariantImageInputStreamException(Reference uri)
            throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, true);
        config.setProperty(Key.VARIANT_CACHE,
                MockBrokenVariantInputStreamCache.class.getSimpleName());
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        Client client = newClient(uri);
        try {
            client.send().close();
        } finally {
            client.stop();
        }
    }

    /**
     * Tests recovery from an exception thrown by
     * {@link VariantCache#newVariantImageInputStream}.
     */
    public void testRecoveryFromVariantCacheNewVariantImageOutputStreamException(Reference uri)
            throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, true);
        config.setProperty(Key.VARIANT_CACHE,
                MockBrokenVariantOutputStreamCache.class.getSimpleName());
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        Client client = newClient(uri);
        try {
            client.send().close();
        } finally {
            client.stop();
        }
    }

    public void testRecoveryFromIncorrectSourceFormat(Reference uri) throws Exception {
        Client client = newClient(uri);
        try (Response response = client.send()) {
        } finally {
            client.stop();
        }
    }

    /**
     * @param uri URI containing {@code CATS} as the slash substitute.
     */
    public void testSlashSubstitution(Reference uri) {
        Configuration.forApplication().setProperty(Key.SLASH_SUBSTITUTE, "CATS");

        assertStatus(200, uri);
    }

    public void testUnavailableSourceFormat(Reference uri) {
        assertStatus(501, uri);
    }

    private void enableCacheControlHeaders() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.CLIENT_CACHE_ENABLED, "true");
        config.setProperty(Key.CLIENT_CACHE_MAX_AGE, "1234");
        config.setProperty(Key.CLIENT_CACHE_SHARED_MAX_AGE, "4567");
        config.setProperty(Key.CLIENT_CACHE_PUBLIC, "true");
        config.setProperty(Key.CLIENT_CACHE_PRIVATE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_CACHE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_STORE, "false");
        config.setProperty(Key.CLIENT_CACHE_MUST_REVALIDATE, "false");
        config.setProperty(Key.CLIENT_CACHE_PROXY_REVALIDATE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_TRANSFORM, "true");
    }

    HeapCache initializeInfoCache() throws IOException {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.INFO_CACHE_ENABLED, true);
        config.setProperty(Key.INFO_CACHE, "HeapCache");
        config.setProperty(Key.INFO_CACHE_TTL, 10);

        CacheFacade facade = new CacheFacade();
        facade.purge();
        return ((HeapCache) facade.getInfoCache().orElseThrow());
    }

    HeapCache initializeVariantCache() throws IOException {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, true);
        config.setProperty(Key.VARIANT_CACHE, "HeapCache");
        config.setProperty(Key.VARIANT_CACHE_TTL, 10);

        CacheFacade facade = new CacheFacade();
        facade.purge();
        return ((HeapCache) facade.getVariantCache().orElseThrow());
    }

}
