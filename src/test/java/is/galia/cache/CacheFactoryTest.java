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

package is.galia.cache;

import is.galia.async.ThreadPool;
import is.galia.config.Key;
import is.galia.plugin.PluginManager;
import is.galia.test.BaseTest;
import is.galia.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class CacheFactoryTest extends BaseTest {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, true);
        config.setProperty(Key.VARIANT_CACHE, HeapCache.class.getSimpleName());
        config.setProperty(Key.INFO_CACHE_ENABLED, true);
        config.setProperty(Key.INFO_CACHE, HeapCache.class.getSimpleName());
    }

    /* getAllInfoCaches() */

    @Test
    void getAllInfoCaches() {
        assertFalse(CacheFactory.getAllInfoCaches().isEmpty());
    }

    /* getAllVariantCaches() */

    @Test
    void getAllVariantCaches() {
        assertFalse(CacheFactory.getAllVariantCaches().isEmpty());
    }

    /* getPluginInfoCacheByName() */

    @Test
    void getPluginInfoCacheByNameWithInvalidName() {
        assertNull(CacheFactory.getPluginInfoCacheByName("bogus"));
    }

    @Test
    void getPluginInfoCacheByNameWithValidName() {
        assertNotNull(CacheFactory.getPluginInfoCacheByName(MockCachePlugin.class.getSimpleName()));
    }

    @Test
    void getPluginInfoCacheByNameInitializesTheInstance() {
        MockCachePlugin cache = (MockCachePlugin) CacheFactory.getPluginInfoCacheByName(
                MockCachePlugin.class.getSimpleName());
        assertTrue(cache.isInitialized);
    }

    /* getPluginInfoCaches() */

    @Test
    void getPluginInfoCaches() {
        Path pluginsDir = PluginManager.getPluginsDir();
        try {
            PluginManager.setPluginsDir(Path.of("/bogus"));
            assertFalse(CacheFactory.getPluginInfoCaches().isEmpty());
        } finally {
            PluginManager.setPluginsDir(pluginsDir);
        }
    }

    /* getPluginVariantCacheByName() */

    @Test
    void getPluginVariantCacheByNameWithInvalidName() {
        assertNull(CacheFactory.getPluginVariantCacheByName("bogus"));
    }

    @Test
    void getPluginVariantCacheByNameWithValidName() {
        assertNotNull(CacheFactory.getPluginVariantCacheByName(MockCachePlugin.class.getSimpleName()));
    }

    @Test
    void getPluginVariantCacheByNameInitializesTheInstance() {
        MockCachePlugin cache = (MockCachePlugin) CacheFactory.getPluginVariantCacheByName(
                MockCachePlugin.class.getSimpleName());
        assertTrue(cache.isInitialized);
    }

    /* getPluginVariantCaches() */

    @Test
    void getPluginVariantCaches() {
        Path pluginsDir = PluginManager.getPluginsDir();
        try {
            PluginManager.setPluginsDir(Path.of("/bogus"));
            assertFalse(CacheFactory.getPluginVariantCaches().isEmpty());
        } finally {
            PluginManager.setPluginsDir(pluginsDir);
        }
    }

    /* getInfoCache() */

    @Test
    void getInfoCacheWithValidUnqualifiedClassName() {
        Configuration.forApplication().setProperty(Key.INFO_CACHE,
                HeapCache.class.getSimpleName());
        assertInstanceOf(HeapCache.class,
                CacheFactory.getInfoCache().orElseThrow());
    }

    @Test
    void getInfoCacheWithEmptyClassName() {
        Configuration.forApplication().setProperty(Key.INFO_CACHE, "");
        assertFalse(CacheFactory.getInfoCache().isPresent());
    }

    @Test
    void getInfoCacheWithNullClassName() {
        Configuration.forApplication().clearProperty(Key.INFO_CACHE);
        assertFalse(CacheFactory.getInfoCache().isPresent());
    }

    @Test
    void getInfoCacheWithInvalidClassName() {
        Configuration.forApplication().setProperty(Key.INFO_CACHE, "bogus");
        assertFalse(CacheFactory.getInfoCache().isPresent());
    }

    @Test
    void getInfoCacheWithFullyQualifiedClassName() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.INFO_CACHE, HeapCache.class.getName());

        assertInstanceOf(HeapCache.class,
                CacheFactory.getInfoCache().orElseThrow());
    }

    @Test
    void getInfoCacheWithPluginName() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.INFO_CACHE, MockCachePlugin.class.getName());

        assertInstanceOf(MockCachePlugin.class,
                CacheFactory.getInfoCache().orElseThrow());
    }

    @Test
    void getInfoCacheInitializesNewInstance() {
        Configuration config = Configuration.forApplication();
        final Key key = Key.INFO_CACHE;

        config.setProperty(key, MockCache.class.getSimpleName());
        MockCache cache = (MockCache) CacheFactory.getInfoCache().orElseThrow();

        assertTrue(cache.isInitializeCalled());
    }

    @Test
    void getInfoCacheShutsDownPreviousInstance() {
        Configuration config = Configuration.forApplication();
        final Key key = Key.INFO_CACHE;

        config.setProperty(key, MockCache.class.getSimpleName());
        MockCache cache1 = (MockCache) CacheFactory.getInfoCache().orElseThrow();

        config.setProperty(key, FilesystemCache.class.getSimpleName());
        CacheFactory.getInfoCache();

        assertTrue(cache1.isShutdownCalled());
    }

    @Test
    void getInfoCacheConcurrently() throws Exception {
        final Configuration config = Configuration.forApplication();
        final int numThreads       = 1000;
        final CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            ThreadPool.getInstance().submit(() -> {
                assertNotNull(CacheFactory.getInfoCache());
                latch.countDown();

                // Introduce some "writers" to try and mess things up.
                if (latch.getCount() % 3 == 0) {
                    config.setProperty(Key.INFO_CACHE,
                            FilesystemCache.class.getSimpleName());
                } else if (latch.getCount() % 5 == 0) {
                    config.setProperty(Key.INFO_CACHE, "");
                }

                return null;
            });
        }
        latch.await();
    }

    /* getVariantCache() */

    @Test
    void getVariantCacheWithValidUnqualifiedClassName() {
        Configuration.forApplication().setProperty(Key.VARIANT_CACHE,
                HeapCache.class.getSimpleName());
        assertInstanceOf(HeapCache.class,
                CacheFactory.getVariantCache().orElseThrow());
    }

    @Test
    void getVariantCacheWithEmptyClassName() {
        Configuration.forApplication().setProperty(Key.VARIANT_CACHE, "");
        assertFalse(CacheFactory.getVariantCache().isPresent());
    }

    @Test
    void getVariantCacheWithNullClassName() {
        Configuration.forApplication().clearProperty(Key.VARIANT_CACHE);
        assertFalse(CacheFactory.getVariantCache().isPresent());
    }

    @Test
    void getVariantCacheWithInvalidClassName() {
        Configuration.forApplication().setProperty(Key.VARIANT_CACHE, "bogus");
        assertFalse(CacheFactory.getVariantCache().isPresent());
    }

    @Test
    void getVariantCacheWithFullyQualifiedClassName() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE, HeapCache.class.getName());

        assertInstanceOf(HeapCache.class,
                CacheFactory.getVariantCache().orElseThrow());
    }

    @Test
    void getVariantCacheWithPluginName() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE, MockCachePlugin.class.getName());

        assertInstanceOf(MockCachePlugin.class,
                CacheFactory.getVariantCache().orElseThrow());
    }

    @Test
    void getVariantCacheInitializesNewInstance() {
        Configuration config = Configuration.forApplication();
        final Key key = Key.VARIANT_CACHE;

        config.setProperty(key, MockCache.class.getSimpleName());
        MockCache cache = (MockCache) CacheFactory.getVariantCache().orElseThrow();

        assertTrue(cache.isInitializeCalled());
    }

    @Test
    void getVariantCacheShutsDownPreviousInstance() {
        Configuration config = Configuration.forApplication();
        final Key key = Key.VARIANT_CACHE;

        config.setProperty(key, MockCache.class.getSimpleName());
        MockCache cache1 = (MockCache) CacheFactory.getVariantCache().orElseThrow();

        config.setProperty(key, FilesystemCache.class.getSimpleName());
        CacheFactory.getVariantCache();

        assertTrue(cache1.isShutdownCalled());
    }

    @Test
    void getVariantCacheConcurrently() throws Exception {
        final Configuration config = Configuration.forApplication();
        final int numThreads       = 1000;
        final CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            ThreadPool.getInstance().submit(() -> {
                assertNotNull(CacheFactory.getVariantCache());
                latch.countDown();

                // Introduce some "writers" to try and mess things up.
                if (latch.getCount() % 3 == 0) {
                    config.setProperty(Key.VARIANT_CACHE,
                            FilesystemCache.class.getSimpleName());
                } else if (latch.getCount() % 5 == 0) {
                    config.setProperty(Key.VARIANT_CACHE, "");
                }

                return null;
            });
        }
        latch.await();
    }

}
