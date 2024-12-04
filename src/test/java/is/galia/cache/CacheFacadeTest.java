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

import is.galia.codec.Decoder;
import is.galia.codec.DecoderFactory;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.image.Info;
import is.galia.operation.OperationList;
import is.galia.stream.CompletableOutputStream;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CacheFacadeTest extends BaseTest {

    private static final int ASYNC_WAIT = 2000;
    private static final double DELTA   = 0.00000001;
    private static final Path FIXTURE   =
            TestUtils.getSampleImage("jpg/rgb-64x56x8-baseline.jpg");

    private CacheFacade instance;

    @BeforeEach
    public void setUp() throws Exception {
        // temporarily enable all caches in order to evict infos from them
        enableVariantCache();
        enableInfoCache();
        enableHeapInfoCache();

        instance = new CacheFacade();
        instance.evictInfos();

        disableVariantCache();
        disableInfoCache();
        disableHeapInfoCache();
    }

    private void enableVariantCache() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, true);
        config.setProperty(Key.VARIANT_CACHE, HeapCache.class.getSimpleName());
        config.setProperty(Key.VARIANT_CACHE_TTL, 10);
        try {
            CacheFactory.getVariantCache().get().purge();
        } catch (IOException e) {
            fail(e);
        }
    }

    private void disableVariantCache() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, false);
    }

    private void enableInfoCache() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.INFO_CACHE_ENABLED, true);
        config.setProperty(Key.INFO_CACHE, HeapCache.class.getSimpleName());
        config.setProperty(Key.INFO_CACHE_TTL, 10);
        try {
            CacheFactory.getInfoCache().get().purge();
        } catch (IOException e) {
            fail(e);
        }
    }

    private void disableInfoCache() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
    }

    private void enableHeapInfoCache() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, true);
        HeapInfoCache.getInstance().purge();
    }

    private void disableHeapInfoCache() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
    }

    /* evictInfos() */

    @Test
    void evictInfos() throws Exception {
        final Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_TTL, 1);

        enableInfoCache();
        InfoCache infoCache = CacheFactory.getInfoCache().orElseThrow();

        Identifier identifier = new Identifier("jpg");
        Info info             = new Info();

        // Add info to the variant cache.
        infoCache.put(identifier, info);

        // Assert that everything has been added.
        assertNotNull(infoCache.fetchInfo(identifier));

        instance.evictInfos();

        Thread.sleep(ASYNC_WAIT);

        // Assert that it's gone.
        assertEquals(0, new CacheFacade().getHeapInfoCacheSize());
        assertFalse(infoCache.fetchInfo(identifier).isPresent());
    }

    /* evict(Identifier) */

    @Test
    void evictWithIdentifier() throws Exception {
        enableVariantCache();
        enableInfoCache();
        VariantCache variantCache = CacheFactory.getVariantCache().orElseThrow();
        InfoCache infoCache       = CacheFactory.getInfoCache().orElseThrow();

        Identifier identifier = new Identifier("jpg");
        OperationList opList  = new OperationList(identifier);
        Info info             = new Info();

        // Add opList to the variant cache.
        try (CompletableOutputStream os =
                     instance.newVariantImageOutputStream(opList)) {
            Files.copy(FIXTURE, os);
            os.complete();
        }

        // Add info to the info cache.
        infoCache.put(identifier, info);

        // Assert that everything has been added.
        assertTrue(variantCache.exists(opList));
        assertNotNull(infoCache.fetchInfo(identifier));
        try (InputStream is = variantCache.newVariantImageInputStream(opList)) {
            assertNotNull(is);
        }

        instance.evict(identifier);

        // Assert that everything is gone.
        assertFalse(variantCache.exists(opList));
        assertFalse(infoCache.fetchInfo(identifier).isPresent());
        try (InputStream is = variantCache.newVariantImageInputStream(opList)) {
            assertNull(is);
        }
    }

    /* evictAsync(Identifier) */

    @Test
    void evictAsyncWithIdentifier() throws Exception {
        enableVariantCache();
        enableInfoCache();
        VariantCache variantCache = CacheFactory.getVariantCache().orElseThrow();
        InfoCache infoCache       = CacheFactory.getInfoCache().orElseThrow();

        Identifier identifier = new Identifier("jpg");
        OperationList opList  = new OperationList(identifier);
        Info info             = new Info();

        // Add opList to the variant cache.
        try (CompletableOutputStream os =
                     instance.newVariantImageOutputStream(opList)) {
            Files.copy(FIXTURE, os);
            os.complete();
        }

        // Add info to the info cache.
        infoCache.put(identifier, info);

        // Assert that everything has been added.
        assertTrue(variantCache.exists(opList));
        assertNotNull(infoCache.fetchInfo(identifier));
        try (InputStream is = variantCache.newVariantImageInputStream(opList)) {
            assertNotNull(is);
        }

        instance.evictAsync(identifier);

        Thread.sleep(ASYNC_WAIT);

        // Assert that everything is gone.
        assertFalse(variantCache.exists(opList));
        assertFalse(infoCache.fetchInfo(identifier).isPresent());
        try (InputStream is = variantCache.newVariantImageInputStream(opList)) {
            assertNull(is);
        }
    }

    /* evict(OperationList) */

    @Test
    void evictWithOperationList() throws Exception {
        enableVariantCache();
        OperationList opList = new OperationList(new Identifier("cats"));

        try (CompletableOutputStream os =
                     instance.newVariantImageOutputStream(opList)) {
            Files.copy(FIXTURE, os);
            os.complete();
        }
        try (InputStream is = instance.newVariantImageInputStream(opList)) {
            assertNotNull(is);
        }

        instance.evict(opList);

        try (InputStream is = instance.newVariantImageInputStream(opList)) {
            assertNull(is);
        }
    }

    /* evictInvalid() */

    @Test
    void evictInvalid() throws Exception {
        enableVariantCache();
        enableInfoCache();
        VariantCache variantCache = CacheFactory.getVariantCache().orElseThrow();
        InfoCache infoCache       = CacheFactory.getInfoCache().orElseThrow();

        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_TTL, 1);
        config.setProperty(Key.INFO_CACHE_TTL, 1);

        Identifier identifier = new Identifier("jpg");
        OperationList opList  = OperationList.builder()
                .withIdentifier(identifier)
                .build();
        Info info             = new Info();

        // Add an image to the variant cache.
        try (CompletableOutputStream os =
                     instance.newVariantImageOutputStream(opList)) {
            Files.copy(FIXTURE, os);
            os.complete();
        }

        // Add an info to the info cache.
        infoCache.put(identifier, info);

        // wait for everything to invalidate
        Thread.sleep(1100);

        instance.evictInvalid();

        // Assert that everything is gone.
        assertEquals(0, new CacheFacade().getHeapInfoCacheSize());
        assertFalse(variantCache.exists(opList));
        assertFalse(infoCache.fetchInfo(identifier).isPresent());
        try (InputStream is = variantCache.newVariantImageInputStream(opList)) {
            assertNull(is);
        }
    }

    /* fetchInfo() */

    @Test
    void fetchInfoWithHitInHeapInfoCacheAndInfoCacheDisabled() throws Exception {
        enableHeapInfoCache();
        disableInfoCache();
        final Identifier identifier = new Identifier("jpg");
        final Info info = new Info();
        instance.getHeapInfoCache().get().put(identifier, info);

        Optional<Info> actualInfo = instance.fetchInfo(identifier);
        assertEquals(info, actualInfo.orElseThrow());
    }

    @Test
    void fetchInfoWithHitInInfoCacheAndHeapInfoCacheDisabled() throws Exception {
        enableInfoCache();
        disableHeapInfoCache();
        final Identifier identifier = new Identifier("jpg");
        final Info info = new Info();

        InfoCache cache = CacheFactory.getInfoCache().get();
        cache.put(identifier, info);

        Optional<Info> actualInfo = instance.fetchInfo(identifier);
        assertEquals(info, actualInfo.orElseThrow());
    }

    @Test
    void fetchInfoWithHitInHeapInfoCacheAndBothCachesEnabled() throws Exception {
        enableHeapInfoCache();
        enableInfoCache();
        final Identifier identifier = new Identifier("jpg");
        final Info info = new Info();
        instance.getHeapInfoCache().get().put(identifier, info);

        Optional<Info> actualInfo = instance.fetchInfo(identifier);
        assertEquals(info, actualInfo.orElseThrow());
    }

    @Test
    void fetchInfoWithHitInInfoCacheAndBothCachesEnabled() throws Exception {
        enableHeapInfoCache();
        enableInfoCache();

        final Identifier identifier = new Identifier("jpg");
        final Info info = new Info();

        assertFalse(instance.fetchInfo(identifier).isPresent());

        InfoCache cache = CacheFactory.getInfoCache().get();
        cache.put(identifier, info);

        Optional<Info> actualInfo = instance.fetchInfo(identifier);
        assertEquals(info, actualInfo.orElseThrow());
    }

    @Test
    void fetchInfoWithMissInBothCaches() throws Exception {
        enableInfoCache();
        enableHeapInfoCache();
        final Identifier identifier = new Identifier("jpg");

        Optional<Info> info = instance.fetchInfo(identifier);
        assertFalse(info.isPresent());
    }

    @Test
    void fetchInfoFollowingCorruptSerializationAddedToInfoCache()
            throws Exception {
        enableInfoCache();
        disableHeapInfoCache();
        final Identifier identifier = new Identifier("jpg");
        final String info           = "{\"this\": is corrupt JSON}";

        InfoCache cache = CacheFactory.getInfoCache().get();
        cache.put(identifier, info);

        Optional<Info> actualInfo = instance.fetchInfo(identifier);
        assertFalse(actualInfo.isPresent());
    }

    /* fetchOrReadInfo() */

    @Test
    void fetchOrReadInfoWithHitInHeapInfoCache() throws Exception {
        enableInfoCache();
        enableHeapInfoCache();
        final Identifier identifier = new Identifier("jpg");
        final Info info = new Info();
        instance.getHeapInfoCache().get().put(identifier, info);

        Format format = Format.get("jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(format, arena)) {
            decoder.setSource(FIXTURE);
            Optional<Info> actualInfo = instance.fetchOrReadInfo(
                    identifier, format, decoder);
            assertEquals(info, actualInfo.orElse(null));
        }
    }

    @Test
    void fetchOrReadInfoWithHitInInfoCache() throws Exception {
        enableInfoCache();
        enableHeapInfoCache();

        final Identifier identifier = new Identifier("jpg");
        final Info info = new Info();

        InfoCache cache = CacheFactory.getInfoCache().get();
        cache.put(identifier, info);

        Format format = Format.get("jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(format, arena)) {
            decoder.setSource(FIXTURE);
            Optional<Info> actualInfo = instance.fetchOrReadInfo(
                    identifier, format, decoder);
            assertEquals(info, actualInfo.orElse(null));
        }
    }

    /**
     * Tests that if a cached info exists but is corrupt, an info is retrieved
     * from the processor instead.
     */
    @Test
    void fetchOrReadInfoWithCorruptHitInInfoCache() throws Exception {
        disableHeapInfoCache();
        enableInfoCache();

        final Identifier identifier = new Identifier("jpg");
        final String info = "{\"this\": is corrupt JSON}";

        InfoCache cache = CacheFactory.getInfoCache().get();
        cache.put(identifier, info);

        Format format = Format.get("jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(format, arena)) {
            decoder.setSource(FIXTURE);
            Optional<Info> actualInfo = instance.fetchOrReadInfo(
                    identifier, format, decoder);
            assertEquals(identifier, actualInfo.get().getIdentifier());
        }
    }

    @Test
    void fetchOrReadInfoWithHitInProcessor() throws Exception {
        disableHeapInfoCache();
        enableInfoCache();

        final Identifier identifier = new Identifier("jpg");

        Format format = Format.get("jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(format, arena)) {
            decoder.setSource(FIXTURE);
            Optional<Info> info = instance.fetchOrReadInfo(
                    identifier, format, decoder);
            assertEquals(identifier, info.orElseThrow().getIdentifier());
            assertEquals(64, info.orElseThrow().getSize(0).width(), DELTA);
        }
    }

    /* getHeapInfoCache() */

    @Test
    void getHeapInfoCacheWhenEnabled() {
        enableHeapInfoCache();
        assertTrue(instance.getHeapInfoCache().isPresent());
    }

    @Test
    void getHeapInfoCacheWhenDisabled() {
        disableHeapInfoCache();
        assertFalse(instance.getHeapInfoCache().isPresent());
    }

    /* getHeapInfoCacheSize() */

    @Test
    void getHeapInfoCacheSizeWhenEnabled() {
        enableHeapInfoCache();
        assertEquals(0, instance.getHeapInfoCacheSize());
    }

    @Test
    void getHeapInfoCacheSizeWhenDisabled() {
        disableHeapInfoCache();
        assertEquals(0, instance.getHeapInfoCacheSize());
    }

    /* getInfoCache() */

    @Test
    void getInfoCacheWhenEnabled() {
        enableInfoCache();
        assertTrue(instance.getInfoCache().isPresent());
    }

    @Test
    void getInfoCacheWhenDisabled() {
        disableInfoCache();
        assertFalse(instance.getInfoCache().isPresent());
    }

    /* getVariantCache() */

    @Test
    void getVariantCacheWhenEnabled() {
        enableVariantCache();
        assertTrue(instance.getVariantCache().isPresent());
    }

    @Test
    void getVariantCacheWhenDisabled() {
        disableVariantCache();
        assertFalse(instance.getVariantCache().isPresent());
    }

    /* isHeapInfoCacheEnabled() */

    @Test
    void isHeapInfoCacheEnabledWhenEnabled() {
        enableHeapInfoCache();
        assertTrue(instance.isHeapInfoCacheEnabled());
    }

    @Test
    void isHeapInfoCacheEnabledWhenDisabled() {
        disableHeapInfoCache();
        assertFalse(instance.isHeapInfoCacheEnabled());
    }

    /* isInfoCacheEnabled() */

    @Test
    void isInfoCacheEnabledWhenEnabled() {
        enableInfoCache();
        assertTrue(instance.isInfoCacheEnabled());
    }

    @Test
    void isInfoCacheEnabledWhenDisabled() {
        disableInfoCache();
        assertFalse(instance.isInfoCacheEnabled());
    }

    /* isVariantCacheEnabled() */

    @Test
    void isVariantCacheEnabledWhenEnabled() {
        enableVariantCache();
        assertTrue(instance.isVariantCacheEnabled());
    }

    @Test
    void isVariantCacheEnabledWhenDisabled() {
        disableVariantCache();
        assertFalse(instance.isVariantCacheEnabled());
    }

    /* newVariantImageInputStream() */

    @Test
    void newVariantImageInputStreamWhenVariantCacheIsEnabled()
            throws Exception {
        enableVariantCache();
        OperationList opList = new OperationList(new Identifier("jpg"));

        try (CompletableOutputStream os =
                     instance.newVariantImageOutputStream(opList)) {
            Files.copy(FIXTURE, os);
            os.complete();
        }
        try (InputStream is = instance.newVariantImageInputStream(opList)) {
            assertNotNull(is);
        }
    }

    @Test
    void newVariantImageInputStreamWhenVariantCacheIsDisabled()
            throws Exception {
        disableVariantCache();
        OperationList opList = new OperationList();
        try (InputStream is = instance.newVariantImageInputStream(opList)) {
            assertNull(is);
        }
    }

    /* newVariantImageOutputStream() */

    @Test
    void newVariantImageOutputStreamWhenVariantCacheIsEnabled()
            throws Exception {
        enableVariantCache();
        OperationList opList = new OperationList(new Identifier("jpg"));
        try (CompletableOutputStream os =
                     instance.newVariantImageOutputStream(opList)) {
            assertNotNull(os);
            os.complete();
        }
    }

    @Test
    void newVariantImageOutputStreamWhenVariantCacheIsDisabled()
            throws Exception {
        disableVariantCache();
        OperationList opList = new OperationList();
        try (CompletableOutputStream os =
                     instance.newVariantImageOutputStream(opList)) {
            assertNull(os);
        }
    }

    /* purge() */

    @Test
    void purge() throws Exception {
        enableVariantCache();
        enableInfoCache();
        enableHeapInfoCache();
        VariantCache variantCache   = CacheFactory.getVariantCache().orElseThrow();
        InfoCache infoCache         = CacheFactory.getInfoCache().orElseThrow();
        HeapInfoCache heapInfoCache = HeapInfoCache.getInstance();

        Identifier identifier = new Identifier("jpg");
        OperationList opList  = new OperationList(identifier);
        Info info             = new Info();

        // Add opList to the variant cache.
        try (CompletableOutputStream os =
                     instance.newVariantImageOutputStream(opList)) {
            Files.copy(FIXTURE, os);
            os.complete();
        }

        // Add info to the info cache.
        infoCache.put(identifier, info);

        // Add info to the heap info cache.
        heapInfoCache.put(identifier, info);

        // Assert that everything has been added.
        assertTrue(variantCache.exists(opList));
        assertNotNull(infoCache.fetchInfo(identifier));
        assertNotNull(heapInfoCache.get(identifier));

        instance.purge();

        // Assert that everything is gone.
        assertEquals(0, new CacheFacade().getHeapInfoCacheSize());
        assertFalse(variantCache.exists(opList));
        assertFalse(infoCache.fetchInfo(identifier).isPresent());
        try (InputStream is = variantCache.newVariantImageInputStream(opList)) {
            assertNull(is);
        }
    }

}
