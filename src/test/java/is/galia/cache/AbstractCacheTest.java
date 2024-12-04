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
import is.galia.config.Key;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.image.Info;
import is.galia.image.StatResult;
import is.galia.operation.Encode;
import is.galia.operation.OperationList;
import is.galia.stream.CompletableOutputStream;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import is.galia.util.ConcurrentProducerConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractCacheTest extends BaseTest {

    static final int ASYNC_WAIT = 3500;
    static final Path FIXTURE   = TestUtils.getSampleImage("jpg/rgb-64x56x8-baseline.jpg");

    abstract VariantCache newVariantCache();
    abstract InfoCache newInfoCache();

    static void assertExists(VariantCache cache,
                             OperationList opList) {
        try (InputStream is = cache.newVariantImageInputStream(opList)) {
            assertNotNull(is);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    static void assertNotExists(VariantCache cache,
                                OperationList opList) {
        try (InputStream is = cache.newVariantImageInputStream(opList)) {
            assertNull(is);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.INFO_CACHE_TTL, 300);
        config.setProperty(Key.VARIANT_CACHE_TTL, 300);
    }

    /* evict(Identifier) */

    @Test
    void evict() throws Exception {
        VariantCache variantCache = newVariantCache();
        InfoCache infoCache       = newInfoCache();

        // add an image
        final Identifier id1        = new Identifier("cats");
        final OperationList opList1 = OperationList.builder()
                .withIdentifier(id1)
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        try (CompletableOutputStream os =
                     variantCache.newVariantImageOutputStream(opList1)) {
            Files.copy(FIXTURE, os);
            os.complete();
        }
        // add an info
        infoCache.put(id1, new Info());

        // add another image
        final Identifier id2        = new Identifier("dogs");
        final OperationList opList2 = OperationList.builder()
                .withIdentifier(id2)
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        try (CompletableOutputStream os =
                     variantCache.newVariantImageOutputStream(opList2)) {
            Files.copy(FIXTURE, os);
            os.complete();
        }
        // add another info
        infoCache.put(id2, new Info());

        assertNotNull(infoCache.fetchInfo(id1));
        assertNotNull(infoCache.fetchInfo(id2));

        // evict one of the info/image pairs
        infoCache.evict(id1);
        variantCache.evict(id1);

        Thread.sleep(2000); // TODO: use a CacheObserver

        // assert that its info and image are gone
        assertFalse(infoCache.fetchInfo(id1).isPresent());
        assertFalse(variantCache.exists(opList1));

        // ... but the other one is still there
        assertNotNull(infoCache.fetchInfo(id2));
        assertTrue(variantCache.exists(opList2));
    }

    /* evict(OperationList) */

    @Test
    void evictWithOperationList() throws Exception {
        final VariantCache instance = newVariantCache();

        // Seed a variant image
        OperationList ops1 = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        try (CompletableOutputStream os =
                     instance.newVariantImageOutputStream(ops1)) {
            Files.copy(FIXTURE, os);
            os.complete();
        }

        // Seed another variant image
        OperationList ops2 = OperationList.builder()
                .withIdentifier(new Identifier("dogs"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        try (CompletableOutputStream os =
                     instance.newVariantImageOutputStream(ops2)) {
            Files.copy(FIXTURE, os);
            os.complete();
        }

        Thread.sleep(ASYNC_WAIT);

        // Purge the first one
        instance.evict(ops1);

        // Assert that it was purged
        assertNotExists(instance, ops1);

        // Assert that the other one was NOT purged
        assertExists(instance, ops2);
    }

    /* evictInfos() */

    @Test
    void evictInfos() throws Exception {
        VariantCache variantCache = newVariantCache();
        InfoCache infoCache       = newInfoCache();
        Identifier identifier     = new Identifier("jpg/rgb-64x56x8-baseline.jpg");
        OperationList opList      = OperationList.builder()
                .withIdentifier(identifier)
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        Info info = new Info();

        // assert that a particular image doesn't exist
        assertFalse(variantCache.exists(opList));

        // assert that a particular info doesn't exist
        assertFalse(infoCache.fetchInfo(identifier).isPresent());

        // add the image
        try (CompletableOutputStream outputStream =
                     variantCache.newVariantImageOutputStream(opList)) {
            Files.copy(FIXTURE, outputStream);
            outputStream.complete();
        }

        // add the info
        infoCache.put(identifier, info);

        Thread.sleep(ASYNC_WAIT);

        // assert that they've been added
        assertExists(variantCache, opList);
        assertNotNull(infoCache.fetchInfo(identifier));

        // purge infos
        infoCache.evictInfos();

        // assert that the info has been purged
        assertFalse(infoCache.fetchInfo(identifier).isPresent());

        // assert that the image has NOT been purged
        assertExists(variantCache, opList);
    }

    /* evictInvalid() */

    @Test
    void evictInvalid() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.INFO_CACHE_TTL, 2);
        config.setProperty(Key.VARIANT_CACHE_TTL, 2);
        VariantCache variantCache = newVariantCache();
        InfoCache infoCache       = newInfoCache();

        // add an image
        Identifier id1     = new Identifier("id1");
        OperationList ops1 = OperationList.builder()
                .withIdentifier(id1)
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        try (CompletableOutputStream outputStream =
                     variantCache.newVariantImageOutputStream(ops1)) {
            Files.copy(FIXTURE, outputStream);
            outputStream.complete();
        }

        // add an Info
        Info info1 = new Info();
        infoCache.put(id1, info1);

        // wait for them to invalidate
        Thread.sleep(2100);

        // add another image
        Identifier id2     = new Identifier("cats");
        OperationList ops2 = OperationList.builder()
                .withIdentifier(id2)
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        try (CompletableOutputStream outputStream =
                     variantCache.newVariantImageOutputStream(ops2)) {
            Files.copy(FIXTURE, outputStream);
            outputStream.complete();
        }

        // add another info
        Info info2 = new Info();
        infoCache.put(id2, info2);

        variantCache.evictInvalid();
        infoCache.evictInvalid();

        // assert that one image and one info have been purged
        assertFalse(infoCache.fetchInfo(id1).isPresent());
        assertTrue(infoCache.fetchInfo(id2).isPresent());
        assertNotExists(variantCache, ops1);
        assertExists(variantCache, ops2);
    }

    /* fetchInfo(Identifier) */

    @Test
    void fetchInfoWithExistingValidImage() throws Exception {
        final InfoCache infoCache = newInfoCache();

        Identifier identifier = new Identifier("cats");
        Info info             = new Info();
        infoCache.put(identifier, info);

        Optional<Info> actual = infoCache.fetchInfo(identifier);
        assertEquals(actual.orElseThrow(), info);
    }

    @Test
    void fetchInfoWithExistingInvalidImage() throws Exception {
        Configuration.forApplication().setProperty(Key.INFO_CACHE_TTL, 1);

        InfoCache cache       = newInfoCache();
        Identifier identifier = new Identifier("cats");
        Info info             = new Info();
        cache.put(identifier, info);

        Thread.sleep(ASYNC_WAIT);

        assertFalse(cache.fetchInfo(identifier).isPresent());
    }

    @Test
    void fetchInfoWithNonexistentImage() throws Exception {
        final InfoCache instance = newInfoCache();
        assertFalse(instance.fetchInfo(new Identifier("bogus")).isPresent());
    }

    @Test
    void fetchInfoPopulatesSerializationTimestampWhenNotAlreadySet()
            throws Exception {
        final InfoCache instance = newInfoCache();

        Identifier identifier = new Identifier("cats");
        Info info = new Info();
        instance.put(identifier, info);

        info = instance.fetchInfo(identifier).orElseThrow();
        assertNotNull(info.getSerializationTimestamp());
    }

    @Test
    void fetchInfoConcurrently() {
        // This is tested in testPutConcurrently()
    }

    /* newVariantImageInputStream(OperationList) */

    @Test
    void newVariantImageInputStreamWithZeroTTL() throws Exception {
        Configuration.forApplication().setProperty(Key.VARIANT_CACHE_TTL, 0);

        VariantCache cache   = newVariantCache();
        CountDownLatch latch = new CountDownLatch(1);
        cache.addObserver(new CacheObserver() {
            @Override
            public void onImageWritten(OperationList opList) {
                latch.countDown();
            }
        });

        // Write an image to the cache
        OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        try (CompletableOutputStream os =
                     cache.newVariantImageOutputStream(opList)) {
            Files.copy(FIXTURE, os);
            os.complete();
        }

        // (jump to onImageWritten())
        latch.await(10, TimeUnit.SECONDS);

        // Read it back in and assert same size
        try (InputStream is = cache.newVariantImageInputStream(opList);
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            is.transferTo(os);
            os.close();
            assertEquals(Files.size(FIXTURE), os.toByteArray().length);
        }
    }

    @Test
    void newVariantImageInputStreamWithNonzeroTTL() throws Exception {
        Configuration.forApplication().setProperty(Key.VARIANT_CACHE_TTL, 2);

        VariantCache cache   = newVariantCache();
        OperationList opList = OperationList.builder()
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

        // Add an image. (The write may complete before data is fully or even
        // partially written to the cache.)
        try (CompletableOutputStream os =
                     cache.newVariantImageOutputStream(opList)) {
            Files.copy(FIXTURE, os);
            os.complete();
        }

        // (jump to onImageWritten())
        latch.await(10, TimeUnit.SECONDS);

        // Assert that it has been added.
        assertExists(cache, opList);
        // Wait for it to invalidate.
        Thread.sleep(3000);
        // Assert that it has been purged.
        assertNotExists(cache, opList);
    }

    @Test
    void newVariantImageInputStreamWithNonexistentImage() throws Exception {
        final VariantCache instance = newVariantCache();
        final OperationList ops = new OperationList(new Identifier("cats"));

        instance.purge();
        assertNotExists(instance, ops);
    }

    @Test
    void newVariantImageInputStreamConcurrently() throws Exception {
        final VariantCache instance = newVariantCache();
        final OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();

        new ConcurrentProducerConsumer(() -> {
            try (CompletableOutputStream os =
                         instance.newVariantImageOutputStream(ops)) {
                Files.copy(FIXTURE, os);
                os.complete();
            }
            return null;
        }, () -> {
            try (InputStream is = instance.newVariantImageInputStream(ops)) {
                if (is != null) {
                    //noinspection StatementWithEmptyBody
                    while (is.read() != -1) {
                        // consume the stream fully
                    }
                }
            }
            return null;
        }).run();
    }

    /* newVariantImageInputStream(OperationList, StatResult) */

    @Test
    void newVariantImageInputStreamPopulatesStatResult() throws Exception {
        Configuration.forApplication().setProperty(Key.VARIANT_CACHE_TTL, 0);
        VariantCache cache   = newVariantCache();
        OperationList opList = OperationList.builder()
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

        // Write an image to the cache
        try (CompletableOutputStream os =
                     cache.newVariantImageOutputStream(opList)) {
            Files.copy(FIXTURE, os);
            os.complete();
        }

        // (jump to onImageWritten())
        latch.await(10, TimeUnit.SECONDS);

        // Read it back in
        StatResult statResult = new StatResult();
        try (InputStream is = cache.newVariantImageInputStream(opList, statResult)) {
            assertNotNull(statResult.getLastModified());
            is.readAllBytes();
        }
    }

    /* newVariantImageOutputStream() */

    @Test
    void newVariantImageOutputStream() throws Exception {
        VariantCache cache = newVariantCache();
        OperationList ops  = OperationList.builder()
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

        // Read it back in
        try (InputStream is = cache.newVariantImageInputStream(ops)) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            is.transferTo(os);
            os.close();
            assertEquals(Files.size(FIXTURE), os.toByteArray().length);
        }
    }

    @Test
    void newVariantImageOutputStreamDoesNotLeaveDetritusWhenStreamIsIncompletelyWritten()
            throws Exception {
        VariantCache cache = newVariantCache();
        OperationList ops  = OperationList.builder()
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
            // don't set it complete
        }

        // (jump to onImageWritten())
        latch.await(10, TimeUnit.SECONDS);

        // Try to read it back in
        try (InputStream is = cache.newVariantImageInputStream(ops)) {
            assertNull(is);
        }
    }

    @Test
    void newVariantImageOutputStreamConcurrently() {
        // This is tested in testNewVariantImageInputStreamConcurrently()
    }

    @Test
    void newVariantImageOutputStreamOverwritesExistingImage() {
        // TODO: write this
    }

    /* purge() */

    @Test
    void purge() throws Exception {
        VariantCache variantCache = newVariantCache();
        InfoCache infoCache       = newInfoCache();
        Identifier identifier     = new Identifier("jpg/rgb-64x56x8-baseline.jpg");
        OperationList opList      = OperationList.builder()
                .withIdentifier(identifier)
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        Info info = new Info();

        // assert that a particular image doesn't exist
        assertFalse(variantCache.exists(opList));

        // assert that a particular info doesn't exist
        assertFalse(infoCache.fetchInfo(identifier).isPresent());

        // add the image
        try (CompletableOutputStream outputStream =
                     variantCache.newVariantImageOutputStream(opList)) {
            Files.copy(FIXTURE, outputStream);
            outputStream.complete();
        }

        // add the info
        infoCache.put(identifier, info);

        Thread.sleep(ASYNC_WAIT);

        // assert that they've been added
        assertExists(variantCache, opList);
        assertNotNull(infoCache.fetchInfo(identifier));

        // purge everything
        infoCache.purge();
        variantCache.purge();

        // assert that the info has been purged
        assertFalse(infoCache.fetchInfo(identifier).isPresent());

        // assert that the image has been purged
        assertNotExists(variantCache, opList);
    }

    /* put(Identifier, Info) */

    @Test
    void putWithInfo() throws Exception {
        final InfoCache instance    = newInfoCache();
        final Identifier identifier = new Identifier("cats");
        final Info info             = new Info();

        instance.put(identifier, info);

        Optional<Info> actualInfo = instance.fetchInfo(identifier);
        assertEquals(info, actualInfo.orElseThrow());
    }

    /**
     * Tests that concurrent calls of {@link InfoCache#put(Identifier, Info)}
     * and {@link InfoCache#fetchInfo(Identifier)} don't conflict.
     */
    @Test
    void putWithInfoConcurrently() throws Exception {
        final InfoCache instance    = newInfoCache();
        final Identifier identifier = new Identifier("monkeys");
        final Info info             = new Info();

        new ConcurrentProducerConsumer(() -> {
            instance.put(identifier, info);
            return null;
        }, () -> {
            Optional<Info> otherInfo = instance.fetchInfo(identifier);
            if (otherInfo.isPresent() && !info.equals(otherInfo.get())) {
                fail();
            }
            return null;
        }).run();
    }

    /* put(Identifier, String) */

    @Test
    void putWithString() throws Exception {
        final InfoCache instance    = newInfoCache();
        final Identifier identifier = new Identifier("cats");
        final Info info             = new Info();
        final String infoStr        = info.toJSON();

        instance.put(identifier, infoStr);

        Optional<Info> actualInfo = instance.fetchInfo(identifier);
        assertEquals(info, actualInfo.orElseThrow());
    }

    /**
     * Tests that concurrent calls of {@link InfoCache#put(Identifier, String)}
     * and {@link InfoCache#fetchInfo(Identifier)} don't conflict.
     */
    @Test
    void putWithStringConcurrently() throws Exception {
        final InfoCache instance    = newInfoCache();
        final Identifier identifier = new Identifier("monkeys");
        final Info info             = new Info();
        final String infoStr        = info.toJSON();

        new ConcurrentProducerConsumer(() -> {
            instance.put(identifier, infoStr);
            return null;
        }, () -> {
            Optional<Info> otherInfo = instance.fetchInfo(identifier);
            if (otherInfo.isPresent() && !info.equals(otherInfo.get())) {
                fail();
            }
            return null;
        }).run();
    }

}
