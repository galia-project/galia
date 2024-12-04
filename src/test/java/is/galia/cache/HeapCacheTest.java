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

import is.galia.config.Configuration;
import is.galia.config.ConfigurationException;
import is.galia.config.Key;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.image.Info;
import is.galia.operation.Encode;
import is.galia.operation.OperationList;
import is.galia.stream.CompletableOutputStream;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class HeapCacheTest extends AbstractCacheTest {

    @Nested
    class KeyTest extends BaseTest {

        @Test
        void equalsWithInfoKey() {
            // Equal
            HeapCache.CacheKey k1 = new HeapCache.CacheKey(new Identifier("cats"));
            HeapCache.CacheKey k2 = new HeapCache.CacheKey(new Identifier("cats"));
            assertEquals(k1, k2);

            // Unequal
            k2 = new HeapCache.CacheKey(new Identifier("dogs"));
            assertNotEquals(k1, k2);
        }

        @Test
        void equalsWithImageKey() {
            // Equal
            HeapCache.CacheKey k1 = new HeapCache.CacheKey(OperationList.builder()
                    .withIdentifier(new Identifier("birds")).build());
            HeapCache.CacheKey k2 = new HeapCache.CacheKey(OperationList.builder()
                    .withIdentifier(new Identifier("birds")).build());
            assertEquals(k1, k2);

            // Unequal identifiers
            k1 = new HeapCache.CacheKey(new Identifier("birds"));
            k2 = new HeapCache.CacheKey(new Identifier("goats"));
            assertNotEquals(k1, k2);

            // Unequal op lists
            k1 = new HeapCache.CacheKey(OperationList.builder()
                    .withIdentifier(new Identifier("birds")).build());
            k2 = new HeapCache.CacheKey(OperationList.builder()
                    .withIdentifier(new Identifier("goats")).build());
            assertNotEquals(k1, k2);
        }

        @Test
        void equalsWithMixedKeys() {
            HeapCache.CacheKey k1 = new HeapCache.CacheKey(new Identifier("birds"));
            HeapCache.CacheKey k2 = new HeapCache.CacheKey(OperationList.builder()
                    .withIdentifier(new Identifier("birds")).build());
            assertNotEquals(k1, k2);
        }

    }

    private HeapCache instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = newVariantCache();
    }

    @Override
    HeapCache newInfoCache() {
        return newVariantCache();
    }

    @Override
    HeapCache newVariantCache() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, Math.pow(1024, 2));
        return new HeapCache();
    }

    /* getByteSize() */

    @Test
    void getByteSize() throws Exception {
        // Initial size
        assertEquals(0, instance.getByteSize());

        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, 10000);

        // Seed an image
        Identifier id1    = new Identifier("cats");
        OperationList ops = new OperationList(id1);
        try (CompletableOutputStream os =
                     instance.newVariantImageOutputStream(ops)) {
            Files.copy(FIXTURE, os);
            os.complete();
        }

        final long fileSize = Files.size(FIXTURE);
        assertEquals(fileSize, instance.getByteSize());

        // Seed an info
        Info info = new Info();
        instance.put(id1, info);
        final long infoSize = info.toJSON().getBytes(StandardCharsets.UTF_8).length;

        assertEquals(fileSize + infoSize, instance.getByteSize());
    }

    /* getTargetByteSize() */

    @Test
    void getTargetByteSizeWithInvalidValue() {
        final Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, "");
        try {
            HeapCache.getTargetByteSize();
            fail("Expected exception");
        } catch (ConfigurationException e) {
            // pass
        }
    }

    @Test
    void getTargetByteSizeWithNumber() {
        final Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, 1000);
        assertEquals(1000, HeapCache.getTargetByteSize());
    }

    @Test
    void getTargetByteSizeWithUnitSuffix() {
        final Configuration config = Configuration.forApplication();
        final float base  = 500.5f;
        final float delta = 0.0001f;

        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, base + "M");
        assertEquals(base * (long) Math.pow(1024, 2), HeapCache.getTargetByteSize(), delta);

        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, base + "MB");
        assertEquals(base * (long) Math.pow(1024, 2), HeapCache.getTargetByteSize(), delta);

        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, base + "G");
        assertEquals(base * (long) Math.pow(1024, 3), HeapCache.getTargetByteSize(), delta);

        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, base + "GB");
        assertEquals(base * (long) Math.pow(1024, 3), HeapCache.getTargetByteSize(), delta);

        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, base + "T");
        assertEquals(base * (long) Math.pow(1024, 4), HeapCache.getTargetByteSize(), delta);

        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, base + "TB");
        assertEquals(base * (long) Math.pow(1024, 4), HeapCache.getTargetByteSize(), delta);

        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, base + "P");
        assertEquals(base * (long) Math.pow(1024, 5), HeapCache.getTargetByteSize(), delta);

        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, base + "PB");
        assertEquals(base * (long) Math.pow(1024, 5), HeapCache.getTargetByteSize(), delta);
    }

    /* purgeExcess() */

    @Test
    void evictExcessWithExcess() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, 5000);

        // Seed an image
        OperationList ops = new OperationList(new Identifier("cats"));
        try (CompletableOutputStream os =
                     instance.newVariantImageOutputStream(ops)) {
            Files.copy(FIXTURE, os);
            os.complete();
        }

        assertEquals(Files.size(FIXTURE), instance.getByteSize());

        instance.evictExcess();

        assertEquals(0, instance.getByteSize());
    }

    @Test
    void evictExcessWithNoExcess() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, 10000);

        // Seed an image
        OperationList ops = new OperationList(new Identifier("cats"));
        try (CompletableOutputStream os =
                     instance.newVariantImageOutputStream(ops)) {
            Files.copy(FIXTURE, os);
            os.complete();
        }

        long size = instance.getByteSize();

        assertEquals(size, instance.getByteSize());

        instance.evictExcess();

        assertEquals(size, instance.getByteSize());
    }

    @Test
    void evictExcessThrowsConfigurationExceptionWhenMaxSizeIsInvalid() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, 0);

        assertThrows(ConfigurationException.class, () -> instance.evictExcess());
    }

    @Test
    void getNumInfos() throws Exception {
        final HeapCache instance    = newInfoCache();
        final Identifier identifier = new Identifier("cats");
        final Info info             = new Info();

        instance.put(identifier, info);

        assertEquals(1, instance.getNumInfos());
    }

    @Test
    void getNumVariantImages() throws Exception {
        HeapCache cache   = newVariantCache();
        OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        CountDownLatch latch = new CountDownLatch(1);
        cache.addObserver(new CacheObserver() {
            @Override
            public void onImageWritten(OperationList opList) {
                latch.countDown();
            }
        });

        // Add an image to the cache
        try (CompletableOutputStream outputStream =
                     cache.newVariantImageOutputStream(ops)) {
            Files.copy(FIXTURE, outputStream);
            outputStream.complete();
        }

        // (jump to onImageWritten())
        latch.await(10, TimeUnit.SECONDS);

        assertEquals(1, cache.getNumVariantImages());
    }

    @Test
    void size() throws Exception {
        final Identifier identifier = new Identifier("cats");

        // Add an info
        instance.put(identifier, new Info());

        // Add an image to the cache
        OperationList ops = OperationList.builder()
                .withIdentifier(identifier)
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        CountDownLatch latch = new CountDownLatch(1);
        instance.addObserver(new CacheObserver() {
            @Override
            public void onImageWritten(OperationList opList) {
                latch.countDown();
            }
        });

        try (CompletableOutputStream outputStream =
                     instance.newVariantImageOutputStream(ops)) {
            Files.copy(FIXTURE, outputStream);
            outputStream.complete();
        }

        // (jump to onImageWritten())
        latch.await(10, TimeUnit.SECONDS);

        assertEquals(2, instance.size());
    }

}
