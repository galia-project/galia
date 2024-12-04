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
import is.galia.cache.CacheFactory;
import is.galia.cache.HeapCache;
import is.galia.cache.InfoCache;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Client;
import is.galia.http.Reference;
import is.galia.http.HTTPException;
import is.galia.http.Response;
import is.galia.http.Status;
import is.galia.image.Identifier;
import is.galia.resource.AbstractResource;
import is.galia.test.TestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import static is.galia.test.Assert.HTTPAssert.*;
import static is.galia.test.Assert.PathAssert.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Collection of tests shareable between major versions of IIIF Information
 * endpoints.
 */
public class InformationResourceTester extends ImageAPIResourceTester {

    public void testAuthorizationWhenUnauthorized(Reference uri) {
        // This may vary depending on the return value of a delegate method,
        // but the test delegate script returns 401.
        assertStatus(401, uri);
        // If sizes is present, we can assume that everything else is.
        assertRepresentationContains("\"width\":", uri);
    }

    public void testAuthorizationWhenForbidden(Reference uri) {
        // This may vary depending on the return value of a delegate method,
        // but the test delegate script returns 401.
        assertStatus(403, uri);
        // Search for something that is known to be present in information
        // responses of all versions of the Image API.
        assertRepresentationContains("\"width\":", uri);
    }

    public void testCacheWithCachesEnabledAndResolveFirstEnabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache  = initializeInfoCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an info to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that an info has been added to the info cache
            assertEquals(1, cache.getNumInfos());

            // assert that an info has been added to the heap info cache
            assertEquals(1, new CacheFacade().getHeapInfoCacheSize());

            // move the source image out of the way
            File movedFile = new File(sourceFile + ".tmp");
            Files.move(sourceFile, movedFile.toPath());

            // request it again and assert HTTP 404
            try (Response response2 = client.send()) {
                fail("Expected exception");
            } catch (HTTPException e) {
                assertEquals(Status.NOT_FOUND, e.getStatus());
            } finally {
                Files.move(movedFile.toPath(), sourceFile);
            }
        } finally {
            client.stop();
        }
    }

    public void testCacheWithCachesEnabledAndResolveFirstDisabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeInfoCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an info to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that an info has been added to the info and heap info caches
            assertEquals(1, cache.getNumInfos());
            assertEquals(1, new CacheFacade().getHeapInfoCacheSize());

            // move the source image out of the way
            File movedFile = new File(sourceFile + ".tmp");
            Files.move(sourceFile, movedFile.toPath());

            // request it again and assert HTTP 200
            try (Response response2 = client.send()) {
                assertEquals(Status.OK, response2.getStatus());
            } finally {
                Files.move(movedFile.toPath(), sourceFile);
            }
        } finally {
            client.stop();
        }
    }

    public void testCacheWithInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstEnabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeInfoCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an info to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that an info has been added to the info cache
            assertEquals(1, cache.getNumInfos());

            // assert that an info has NOT been added to the heap info cache
            assertEquals(0, new CacheFacade().getHeapInfoCacheSize());

            // move the source image out of the way
            File movedFile = new File(sourceFile + ".tmp");
            Files.move(sourceFile, movedFile.toPath());

            // request it again and assert HTTP 404
            try (Response response2 = client.send()) {
                fail("Expected exception");
            } catch (HTTPException e) {
                assertEquals(Status.NOT_FOUND, e.getStatus());
            } finally {
                Files.move(movedFile.toPath(), sourceFile);
            }
        } finally {
            client.stop();
        }
    }

    public void testCacheWithInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstDisabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeInfoCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an info to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that an info has been added to the info cache
            assertEquals(1, cache.getNumInfos());

            // assert that an info has NOT been added to the heap info cache
            assertEquals(0, new CacheFacade().getHeapInfoCacheSize());

            // move the source image out of the way
            File movedFile = new File(sourceFile + ".tmp");
            Files.move(sourceFile, movedFile.toPath());

            // request it again and assert HTTP 200
            try (Response response2 = client.send()) {
                assertEquals(Status.OK, response2.getStatus());
            } finally {
                Files.move(movedFile.toPath(), sourceFile);
            }
        } finally {
            client.stop();
        }
    }

    public void testCacheWithInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstEnabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeInfoCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an info to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that nothing has been added to the info cache
            assertEquals(0, cache.getNumInfos());

            // assert that an info has been added to the heap info cache
            assertEquals(1, new CacheFacade().getHeapInfoCacheSize());

            // move the source image out of the way
            File movedFile = new File(sourceFile + ".tmp");
            Files.move(sourceFile, movedFile.toPath());

            // request it again and assert HTTP 404
            try (Response response2 = client.send()) {
                fail("Expected exception");
            } catch (HTTPException e) {
                assertEquals(Status.NOT_FOUND, e.getStatus());
            } finally {
                Files.move(movedFile.toPath(), sourceFile);
            }
        } finally {
            client.stop();
        }
    }

    public void testCacheWithInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstDisabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeInfoCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an info to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that nothing has been added to the info cache
            assertEquals(0, cache.getNumInfos());

            // assert that an info has been added to the heap info cache
            assertEquals(1, new CacheFacade().getHeapInfoCacheSize());

            // move the source image out of the way
            File movedFile = new File(sourceFile + ".tmp");
            Files.move(sourceFile, movedFile.toPath());

            // request it again and assert HTTP 200
            try (Response response2 = client.send()) {
                assertEquals(Status.OK, response2.getStatus());
            } finally {
                Files.move(movedFile.toPath(), sourceFile);
            }
        } finally {
            client.stop();
        }
    }

    public void testCacheWithCachesDisabledAndResolveFirstEnabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeInfoCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, false);
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an info to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that nothing has been added to the info cache
            assertEquals(0, cache.getNumInfos());

            // assert that an info has NOT been added to the heap info cache
            assertEquals(0, new CacheFacade().getHeapInfoCacheSize());

            // move the source image out of the way
            File movedFile = new File(sourceFile + ".tmp");
            Files.move(sourceFile, movedFile.toPath());

            // request it again and assert HTTP 404
            try (Response response2 = client.send()) {
                fail("Expected exception");
            } catch (HTTPException e) {
                assertEquals(Status.NOT_FOUND, e.getStatus());
            } finally {
                Files.move(movedFile.toPath(), sourceFile);

            }
        } finally {
            client.stop();
        }
    }

    public void testCacheWithCachesDisabledAndResolveFirstDisabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeInfoCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, false);
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an info to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that nothing has been added to the info cache
            assertEquals(0, cache.getNumInfos());

            // assert that an info has NOT been added to the heap info cache
            assertEquals(0, new CacheFacade().getHeapInfoCacheSize());

            // move the source image out of the way
            File movedFile = new File(sourceFile + ".tmp");
            Files.move(sourceFile, movedFile.toPath());

            // request it again and assert HTTP 404
            try (Response response2 = client.send()) {
                fail("Expected exception");
            } catch (HTTPException e) {
                assertEquals(Status.NOT_FOUND, e.getStatus());
            } finally {
                Files.move(movedFile.toPath(), sourceFile);
            }
        } finally {
            client.stop();
        }
    }

    public void testCachingWhenCachesAreEnabledAndRecacheQueryArgumentIsSupplied(
            Reference uri) throws Exception {
        HeapCache cache = initializeInfoCache();

        // request an image
        Client client = newClient(uri);
        try {
            client.send().close();

            // check its last-accessed time
            Instant time1 = cache.map().values().stream()
                    .findFirst().orElseThrow().getLastAccessed();

            Thread.sleep(500);

            // request it again
            client.send().close();

            // check its last-accessed time again
            Instant time2 = cache.map().values().stream()
                    .findFirst().orElseThrow().getLastAccessed();

            // assert that the times have changed
            assertTrue(time2.isAfter(time1));
        } finally {
            client.stop();
        }
    }

    public void testCustomizedInformationJSON(Reference uri) throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.DELEGATE_ENABLED, true);
        Client client = newClient(uri);
        try (Response response = client.send()) {
            String body = response.getBodyAsString();
            assertTrue(body.contains("\"new_key\":\"new value\""));
        } finally {
            client.stop();
        }
    }

    public void testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse(
            Reference uri,
            Identifier identifier) throws Exception {
        doPurgeFromCacheWhenSourceIsMissing(uri, identifier, false);
    }

    public void testPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue(
            Reference uri,
            Identifier identifier) throws Exception {
        doPurgeFromCacheWhenSourceIsMissing(uri, identifier, true);
    }

    private void doPurgeFromCacheWhenSourceIsMissing(
            Reference uri,
            Identifier identifier,
            boolean evictMissing) throws Exception {
        // Create a directory that will contain a source image. We don't want
        // to use the image fixtures dir because we'll need to delete one.
        Path sourceDir = Files.createTempDirectory("source");

        // Populate the source directory with an image.
        Path imageFixture = TestUtils.getSampleImage(IMAGE);
        Path sourceImage  = sourceDir.resolve(imageFixture.getFileName());
        Files.copy(imageFixture, sourceImage);

        // Create the cache directory.
        Path cacheDir = Files.createTempDirectory("cache");

        final Configuration config = Configuration.forApplication();
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX, sourceDir + "/");
        config.setProperty(Key.INFO_CACHE_ENABLED, true);
        config.setProperty(Key.INFO_CACHE, "FilesystemCache");
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                cacheDir.toString());
        config.setProperty(Key.INFO_CACHE_TTL, 60);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);
        config.setProperty(Key.CACHE_SERVER_EVICT_MISSING, evictMissing);

        Client client = newClient(uri);
        try {
            assertRecursiveFileCount(cacheDir, 0);

            // Request an image to cache its info.
            client.send().close();

            // The info may write asynchronously, so wait.
            Thread.sleep(1000);

            // Assert that it's been cached.
            InfoCache cache = CacheFactory.getInfoCache().orElseThrow();
            assertTrue(cache.fetchInfo(identifier).isPresent());

            // Delete the source image.
            Files.delete(sourceImage);

            // Request the same image which is now cached but underlying is
            // gone.
            try (Response response = client.send()) {
                fail("Expected exception");
            } catch (HTTPException e) {
                assertEquals(Status.NOT_FOUND, e.getStatus());
            }

            // Stuff may be deleted asynchronously, so wait.
            Thread.sleep(1000);

            if (evictMissing) {
                assertFalse(cache.fetchInfo(identifier).isPresent());
            } else {
                assertTrue(cache.fetchInfo(identifier).isPresent());
            }
        } finally {
            client.stop();
        }
    }

    public void testRedirectToInfoJSON(Reference fromURI, Reference toURI) {
        assertRedirect(fromURI, toURI, 303);
    }

    public void testRedirectToInfoJSONWithEncodedCharacters(Reference fromURI,
                                                            Reference toURI) {
        assertRedirect(fromURI, toURI, 303);
    }

    public void testRedirectToInfoJSONWithDifferentPublicIdentifier(Reference uri)
            throws Exception {
        Client client = newClient(uri);
        client.getHeaders().set(AbstractResource.PUBLIC_IDENTIFIER_HEADER, "foxes");
        try (Response response = client.send()) {
            assertEquals(Status.SEE_OTHER, response.getStatus());
            assertTrue(response.getHeaders().getFirstValue("Location").endsWith("/foxes/info.json"));
        } finally {
            client.stop();
        }
    }

    public void testLastModifiedHeaderWhenInfoCacheIsEnabledAndNotResolvingFirst(Reference uri)
            throws Exception {
        initializeInfoCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);

        Client client = newClient(uri);
        // request a resource once to cache it
        client.send().close();

        // The info may write asynchronously, so wait.
        Thread.sleep(1000);

        // request it again to get the Last-Modified header
        try (Response response = client.send()) {
            String value = response.getHeaders().getFirstValue("Last-Modified");
            TemporalAccessor ta = DateTimeFormatter.RFC_1123_DATE_TIME
                    .withLocale(Locale.UK)
                    .withZone(ZoneId.of("UTC"))
                    .parse(value);
            Instant instant = Instant.from(ta);
            // assert that the header value is less than 3 seconds in the past
            assertTrue(Instant.now().getEpochSecond() - instant.getEpochSecond() < 3);
        } finally {
            client.stop();
        }
    }

    public void testLastModifiedHeaderWhenInfoCacheIsDisabled(Reference uri,
                                                              String identifier)
            throws Exception {
        Client client = newClient(uri);
        try (Response response = client.send()) {
            String value = response.getHeaders().getFirstValue("Last-Modified");
            TemporalAccessor ta = DateTimeFormatter.RFC_1123_DATE_TIME
                    .withLocale(Locale.UK)
                    .withZone(ZoneId.of("UTC"))
                    .parse(value);
            Instant headerInstant          = Instant.from(ta);
            Instant fixtureModifiedInstant =
                    Files.getLastModifiedTime(TestUtils.getFixture(identifier)).toInstant();
            long headerMTime  = headerInstant.getEpochSecond();
            long fixtureMTime = fixtureModifiedInstant.getEpochSecond();
            // assert that the header value matches the source image's mtime
            assertTrue(Math.abs(headerMTime - fixtureMTime) < 1);
        } finally {
            client.stop();
        }
    }

}
