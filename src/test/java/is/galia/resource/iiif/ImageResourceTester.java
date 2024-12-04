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
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Client;
import is.galia.http.Reference;
import is.galia.http.HTTPException;
import is.galia.http.Response;
import is.galia.http.Status;
import is.galia.image.Size;
import is.galia.image.Info;
import is.galia.operation.OperationList;
import is.galia.test.TestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;

import static is.galia.test.Assert.HTTPAssert.*;
import static is.galia.test.Assert.PathAssert.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Collection of tests shareable between major versions of IIIF Image API
 * endpoints.
 */
public class ImageResourceTester extends ImageAPIResourceTester {

    public void testAuthorizationWhenUnauthorized(Reference uri) {
        // This may vary depending on the return value of a delegate method,
        // but the test delegate script returns 403.
        assertStatus(401, uri);
        assertRepresentationContains("401 Unauthorized", uri);
    }

    public void testAuthorizationWhenForbidden(Reference uri) {
        // This may vary depending on the return value of a delegate method,
        // but the test delegate script returns 403.
        assertStatus(403, uri);
        assertRepresentationContains("403 Forbidden", uri);
    }

    public void testAuthorizationWhenRedirecting(Reference uri)
            throws Exception {
        Client client = newClient(uri);
        try (Response response = client.send()) {
            assertEquals(Status.SEE_OTHER, response.getStatus());
            assertEquals("http://example.org/",
                    response.getHeaders().getFirstValue("Location"));
        } finally {
            client.stop();
        }
    }

    public void testCacheWithCachesEnabledAndResolveFirstEnabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeVariantCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an image to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that a variant has been added to the variant cache
            assertEquals(1, cache.getNumVariantImages());

            // assert that an info has not been added to the info cache
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

    public void testCacheWithCachesEnabledAndResolveFirstDisabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache variantCache = initializeVariantCache();
        HeapCache infoCache    = initializeInfoCache();
        Configuration config   = Configuration.forApplication();
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an image to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that a variant has been added to the variant cache
            assertEquals(1, variantCache.getNumVariantImages());

            // assert that an info has been added to the info cache
            assertEquals(1, infoCache.getNumInfos());

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

    public void testCacheWithVariantCacheEnabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstEnabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeVariantCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an image to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that a variant has been added to the variant cache
            assertEquals(1, cache.getNumVariantImages());

            // assert that an info has not been added to the info cache
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

    public void testCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheDisabledAndResolveFirstEnabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeVariantCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an image to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            assertEquals(1, cache.getNumVariantImages());
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

    public void testCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstEnabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeVariantCache();
        Configuration config       = Configuration.forApplication();
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an image to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that a variant has been added to the variant cache
            assertEquals(1, cache.getNumVariantImages());

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

    public void testCacheWithVariantCacheEnabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstDisabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache variantCache = initializeVariantCache();
        HeapCache infoCache    = initializeInfoCache();
        Configuration config   = Configuration.forApplication();
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an image to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that a variant has been added to the variant cache
            assertEquals(1, variantCache.getNumVariantImages());

            // assert that an info has been added to the info cache
            assertEquals(1, infoCache.getNumInfos());

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

    public void testCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheDisabledAndResolveFirstDisabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeVariantCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an image to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that a variant has been added to the variant cache
            assertEquals(1, cache.getNumVariantImages());

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

    public void testCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstDisabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeVariantCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an image to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that a variant has been added to the variant cache
            assertEquals(1, cache.getNumVariantImages());

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

    public void testCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheEnabledAndResolveFirstEnabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeVariantCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, false);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an image to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that nothing has been added to the variant cache
            assertEquals(0, cache.getNumVariantImages());

            // assert that an info has not been added to the info cache
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

    public void testCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheEnabledAndResolveFirstDisabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache variantCache = initializeVariantCache();
        HeapCache infoCache    = initializeInfoCache();
        Configuration config   = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, false);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an image to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that nothing has been added to the variant cache
            assertEquals(0, variantCache.getNumVariantImages());

            // assert that an info has been added to the info cache
            assertEquals(1, infoCache.getNumInfos());

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

    public void testCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstEnabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeVariantCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, false);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an image to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that nothing has been added to the variant cache
            assertEquals(0, cache.getNumVariantImages());

            // assert that an info has not been added to the info cache
            assertEquals(0, cache.getNumInfos());

            // assert that nothing has been added to the heap info cache
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

    public void testCacheWithVariantCacheDisabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstEnabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeVariantCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, false);
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an image to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that nothing has been added to the variant or info caches
            assertEquals(0, cache.size());

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

    public void testCacheWithVariantCacheDisabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstDisabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeVariantCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, false);
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an image to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that nothing has been added to the variant or info caches
            assertEquals(0, cache.size());

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

    public void testCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstDisabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache variantCache = initializeVariantCache();
        HeapCache infoCache    = initializeInfoCache();
        Configuration config   = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, false);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an image to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that nothing has been added to the variant cache
            assertEquals(0, variantCache.getNumVariantImages());

            // assert that an info has been added to the info cache
            assertEquals(1, infoCache.getNumInfos());

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

    public void testCacheWithCachesDisabledAndResolveFirstEnabled(
            Reference uri,
            Path sourceFile) throws Exception {
        HeapCache cache = initializeVariantCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, false);
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an image to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that nothing has been added to the variant cache
            assertEquals(0, cache.size());

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
        HeapCache cache = initializeVariantCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, false);
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an image to cache it
        Client client = newClient(uri);
        try (Response response = client.send()) {
            Thread.sleep(1000); // the info may write asynchronously

            // assert that nothing has been added to the variant cache
            assertEquals(0, cache.size());

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
        HeapCache cache = initializeVariantCache();

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

    public void testContentDispositionHeaderWithNoHeader(Reference uri)
            throws Exception {
        Client client = newClient(uri);
        try (Response response = client.send()) {
            assertNull(response.getHeaders().getFirstValue("Content-Disposition"));
        } finally {
            client.stop();
        }
    }

    public void testContentDispositionHeaderSetToInline(
            Reference uri,
            String identifier) throws Exception {
        Client client = newClient(uri);
        try (Response response = client.send()) {
            String encodedIdentifier = Reference.encode(identifier.replace("/", "_"));
            String extension = List.of(identifier.split("\\.")).getLast();
            assertEquals("inline; filename=\"" + encodedIdentifier + "." + extension + "\"",
                    response.getHeaders().getFirstValue("Content-Disposition"));
        } finally {
            client.stop();
        }
    }

    public void testContentDispositionHeaderSetToAttachment(
            Reference uri,
            String identifier) throws Exception {
        Client client = newClient(uri);
        try (Response response = client.send()) {
            String encodedIdentifier = Reference.encode(identifier.replace("/", "_"));
            String extension = List.of(identifier.split("\\.")).getLast();
            assertEquals("attachment; filename=\"" + encodedIdentifier + "." + extension + "\"",
                    response.getHeaders().getFirstValue("Content-Disposition"));
        } finally {
            client.stop();
        }
    }

    public void testContentDispositionHeaderSetToAttachmentWithFilename(
            Reference uri,
            String filename) throws Exception {
        Client client = newClient(uri);
        try (Response response = client.send()) {
            assertEquals("attachment; filename=\"" + filename + "\"",
                    response.getHeaders().getFirstValue("Content-Disposition"));
        } finally {
            client.stop();
        }
    }

    public void testDimensions(Reference uri,
                               int expectedWidth,
                               int expectedHeight) throws Exception {
        Client client = newClient(uri);
        try (Response response = client.send()) {
            byte[] imageData = response.getBody();
            try (InputStream is = new ByteArrayInputStream(imageData)) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(expectedWidth, image.getWidth());
                assertEquals(expectedHeight, image.getHeight());
            }
        } finally {
            client.stop();
        }
    }

    public void testLessThanOrEqualToMaxScale(Reference uri) {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_SCALE, 1.0);
        assertStatus(200, uri);
    }

    public void testGreaterThanMaxScale(Reference uri, int expectedStatus) {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_SCALE, 1.0);
        assertStatus(expectedStatus, uri);
    }

    public void testMinPixels(Reference uri) {
        assertStatus(400, uri); // zero area
    }

    public void testLessThanMaxPixels(Reference uri) {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_PIXELS, 100000000);
        assertStatus(200, uri);
    }

    /**
     * When an image is requested with size {@code full}, and would end up
     * being larger than {@link Key#MAX_PIXELS}, the request should be
     * forbidden.
     */
    public void testForbiddingMoreThanMaxPixels(Reference uri) {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_PIXELS, 1000);
        assertStatus(403, uri);
    }

    /**
     * When an image is requested with size {@code max}, and would end up being
     * larger than {@link Key#MAX_PIXELS}, it should be downscaled to {@link
     * Key#MAX_PIXELS}.
     *
     * @param originalWidth  Source (or post-crop if cropped) image width.
     * @param originalHeight Source (or post-crop if cropped) image height.
     * @param maxPixels      Value to set to {@link Key#MAX_PIXELS}, which must
     *                       be less than
     *                       {@code originalWidth * originalHeight}.
     */
    public void testDownscalingToMaxPixels(Reference uri,
                                           int originalWidth,
                                           int originalHeight,
                                           int maxPixels) throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_PIXELS, maxPixels);

        Client client = newClient(uri);
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());
            byte[] body = response.getBody();
            try (InputStream is = new ByteArrayInputStream(body)) {
                BufferedImage image = ImageIO.read(is);
                Size expectedSize = Size.ofScaledArea(
                        new Size(originalWidth, originalHeight),
                        config.getInt(Key.MAX_PIXELS));
                assertEquals(Math.floor(expectedSize.width()), image.getWidth());
                assertEquals(Math.floor(expectedSize.height()), image.getHeight());
            }
        } finally {
            client.stop();
        }
    }

    public void testInvalidPageNumber(Reference uri) {
        assertStatus(400, uri);
    }

    public void testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse(
            Reference uri,
            OperationList opList) throws Exception {
        doEvictFromCacheWhenSourceIsMissing(uri, opList, false);
    }

    public void testPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue(
            Reference uri,
            OperationList opList) throws Exception {
        doEvictFromCacheWhenSourceIsMissing(uri, opList, true);
    }

    private void doEvictFromCacheWhenSourceIsMissing(
            Reference uri,
            OperationList opList,
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
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME, cacheDir.toString());
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX, sourceDir + "/");
        config.setProperty(Key.VARIANT_CACHE_ENABLED, true);
        config.setProperty(Key.VARIANT_CACHE, "FilesystemCache");
        config.setProperty(Key.VARIANT_CACHE_TTL, 60);
        config.setProperty(Key.INFO_CACHE_ENABLED, true);
        config.setProperty(Key.INFO_CACHE, "FilesystemCache");
        config.setProperty(Key.INFO_CACHE_TTL, config.getInt(Key.VARIANT_CACHE_TTL));
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);
        config.setProperty(Key.CACHE_SERVER_EVICT_MISSING, evictMissing);

        Client client = newClient(uri);
        try {
            Info info = Info.builder().withSize(64, 56).build();
            opList.applyNonEndpointMutations(info, null);

            assertRecursiveFileCount(cacheDir, 0);

            // Request an image to cache it. This will cache both a variant
            // and an info.
            client.send().close();

            // The info may write asynchronously, so wait.
            Thread.sleep(1000);

            // Assert that they've been cached.
            assertRecursiveFileCount(cacheDir, 2);

            // Delete the source image.
            Files.delete(sourceImage);

            // Request the same image which is now cached but underlying is
            // 404.
            try (Response response = client.send()) {
                fail("Expected exception");
            } catch (HTTPException e) {
                // good
            }

            // Stuff may be deleted asynchronously, so wait.
            Thread.sleep(1000);

            if (evictMissing) {
                assertRecursiveFileCount(cacheDir, 0);
            } else {
                assertRecursiveFileCount(cacheDir, 2);
            }
        } finally {
            client.stop();
        }
    }

    /**
     * Tests an output format that is not recognized by the application.
     */
    public void testInvalidOutputFormat(Reference uri) {
        assertStatus(415, uri);
    }

    public void testLastModifiedHeaderWhenVariantCacheIsEnabledAndNotResolvingFirst(Reference uri)
            throws Exception {
        initializeInfoCache();
        initializeVariantCache();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        Client client = newClient(uri);
        // request a resource once to cache it
        client.send().close();

        // It may write asynchronously
        Thread.sleep(1000);

        // request it again to get the Last-Modified header
        try (Response response = client.send()) {
            String value = response.getHeaders().getFirstValue("Last-Modified");
            TemporalAccessor ta = DateTimeFormatter.RFC_1123_DATE_TIME
                    .withLocale(Locale.UK)
                    .withZone(ZoneId.of("UTC"))
                    .parse(value);
            Instant instant = Instant.from(ta);
            // assert that the header value is now, more or less
            assertTrue(Instant.now().getEpochSecond() - instant.getEpochSecond() < 10);
        } finally {
            client.stop();
        }
    }

    public void testLastModifiedHeaderWhenVariantCacheIsDisabled(Reference uri)
            throws Exception {
        Client client = newClient(uri);
        try (Response response = client.send()) {
            String value = response.getHeaders().getFirstValue("Last-Modified");
            assertNotNull(value);
        } finally {
            client.stop();
        }
    }

    /**
     * Tests an output format that is recognized by the application but not
     * supported by a processor.
     */
    public void testUnsupportedOutputFormat(Reference uri) {
        assertStatus(415, uri);
    }

}
