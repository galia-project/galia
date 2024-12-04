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

package is.galia.stream;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Client;
import is.galia.http.ClientFactory;
import is.galia.http.Method;
import is.galia.http.Range;
import is.galia.http.Reference;
import is.galia.http.Response;
import is.galia.test.BaseTest;
import is.galia.test.StaticFileServer;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

class HTTPImageInputStreamTest extends BaseTest {

    private static class MockHTTPImageInputStreamClient
            implements HTTPImageInputStreamClient {

        private final Client backingClient;

        MockHTTPImageInputStreamClient(Reference uri) {
            backingClient = ClientFactory.newClient();
            backingClient.setURI(uri);
        }

        @Override
        public Reference getReference() {
            return backingClient.getURI();
        }

        @Override
        public Response sendHEADRequest() throws IOException {
            try {
                backingClient.setMethod(Method.HEAD);
                return backingClient.send();
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public Response sendGETRequest(Range range) throws IOException {
            try {
                backingClient.setMethod(Method.GET);
                backingClient.getHeaders().set("Range",
                        "bytes=" + range.start() + "-" + range.end());
                return backingClient.send();
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    private static final Path FIXTURE = TestUtils.getSampleImage("tif/tif.tif");

    private StaticFileServer webServer;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        webServer = newWebServer();
        webServer.start();
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        webServer.stop();
    }

    private StaticFileServer newWebServer() {
        StaticFileServer webServer = new StaticFileServer();
        webServer.setRoot(TestUtils.getSampleImagesPath());
        return webServer;
    }

    private HTTPImageInputStream newInstanceFromConstructor1(String uriPath)
            throws IOException {
        Reference uri = webServer.getHTTPURI().rebuilder()
                .withPath(uriPath).build();
        HTTPImageInputStreamClient client =
                new MockHTTPImageInputStreamClient(uri);
        return new HTTPImageInputStream(client);
    }

    private HTTPImageInputStream newInstanceFromConstructor2(
            Path fixture,
            String uriPath) throws IOException {
        Reference uri = webServer.getHTTPURI().rebuilder()
                .withPath(uriPath).build();
        HTTPImageInputStreamClient client =
                new MockHTTPImageInputStreamClient(uri);
        return new HTTPImageInputStream(client, Files.size(fixture));
    }

    @Test
    void testConstructor1ThrowsExceptionWhenServerDoesNotSupportRanges()
            throws Exception {
        webServer.stop();

        webServer = newWebServer();
        webServer.setAcceptingRanges(false);
        webServer.start();

        assertThrows(RangesNotSupportedException.class, () -> {
            final String uriPath = "/tif/tif.tif";
            try (HTTPImageInputStream is = newInstanceFromConstructor1(uriPath)) {
            }
        });
    }

    @Test
    void testConstructor1ReadsLength() throws Exception {
        final String uriPath = "/tif/tif.tif";
        try (HTTPImageInputStream is = newInstanceFromConstructor1(uriPath)) {
            assertEquals(Files.size(FIXTURE), is.length());
        }
    }

    @Test
    void testGetWindowSize() throws Exception {
        final String uriPath = "/tif/tif.tif";
        try (HTTPImageInputStream instance = newInstanceFromConstructor1(uriPath)) {
            instance.setWindowSize(555);
            assertEquals(555, instance.getWindowSize());
        }
    }

    @Test
    void testRead1() throws Exception {
        final String uriPath       = "/tif/tif.tif";
        final int fixtureLength    = (int) Files.size(FIXTURE);
        final byte[] expectedBytes = Files.readAllBytes(FIXTURE);
        final byte[] actualBytes   = new byte[fixtureLength];

        try (HTTPImageInputStream instance =
                     newInstanceFromConstructor2(FIXTURE, uriPath)) {
            instance.setWindowSize(1024);
            for (int i = 0; i < actualBytes.length; i++) {
                actualBytes[i] = (byte) (instance.read() & 0xff);
            }
            assertArrayEquals(expectedBytes, actualBytes);
        }
    }

    @Test
    void testRead2() throws Exception {
        final String uriPath       = "/tif/tif.tif";
        final int fixtureLength    = (int) Files.size(FIXTURE);
        final byte[] expectedBytes = Files.readAllBytes(FIXTURE);
        final byte[] actualBytes   = new byte[fixtureLength];

        try (HTTPImageInputStream instance =
                     newInstanceFromConstructor2(FIXTURE, uriPath)) {
            instance.setWindowSize(1024);
            instance.read(actualBytes, 0, fixtureLength);
        }
        assertArrayEquals(expectedBytes, actualBytes);
    }

    @Test
    void testSeek() throws Exception {
        final String uriPath = "/tif/tif.tif";
        final int fixtureLength    = (int) Files.size(FIXTURE);
        final byte[] expectedBytes = Files.readAllBytes(FIXTURE);
        final byte[] actualBytes   = new byte[fixtureLength];

        try (HTTPImageInputStream instance =
                     newInstanceFromConstructor2(FIXTURE, uriPath)) {
            instance.setWindowSize(1024);
            instance.read(actualBytes, 0, fixtureLength);
            instance.seek(0);
            instance.read(actualBytes, 0, fixtureLength);
        }
        assertArrayEquals(expectedBytes, actualBytes);
    }

    @Test
    void functionalTestWithBMP() throws Exception {
        final String uriPath = "/bmp/bmp.bmp";
        final Path fixture   = TestUtils.getSampleImage("bmp/bmp.bmp");
        try (HTTPImageInputStream instance =
                     newInstanceFromConstructor2(fixture, uriPath);
             ImageInputStream is =
                     ImageIO.createImageInputStream(fixture.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

    @Test
    void functionalTestWithGIF() throws Exception {
        final String uriPath = "/gif/gif.gif";
        final Path fixture   = TestUtils.getSampleImage("gif/gif.gif");
        try (HTTPImageInputStream instance =
                     newInstanceFromConstructor2(fixture, uriPath);
             ImageInputStream is =
                     ImageIO.createImageInputStream(fixture.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

    @Test
    void functionalTestWithJPEG() throws Exception {
        final String file    = "jpg/jpg.jpg";
        final String uriPath = "/" + file;
        final Path fixture   = TestUtils.getSampleImage(file);
        try (HTTPImageInputStream instance =
                     newInstanceFromConstructor2(fixture, uriPath);
             ImageInputStream is =
                     ImageIO.createImageInputStream(fixture.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

    @Test
    void functionalTestWithPNG() throws Exception {
        final String file    = "png/png.png";
        final String uriPath = "/" + file;
        final Path fixture   = TestUtils.getSampleImage(file);
        try (HTTPImageInputStream instance =
                     newInstanceFromConstructor2(fixture, uriPath);
             ImageInputStream is =
                     ImageIO.createImageInputStream(fixture.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

    @Test
    void functionalTestWithTIFF() throws Exception {
        final String file    = "tif/tif.tif";
        final String uriPath = "/" + file;
        final Path fixture   = TestUtils.getSampleImage(file);
        try (HTTPImageInputStream instance =
                     newInstanceFromConstructor2(fixture, uriPath);
             ImageInputStream is =
                     ImageIO.createImageInputStream(fixture.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

    @Test
    void functionalTestWithChunkCacheDisabled() throws Exception {
        Configuration.forApplication()
                .setProperty(Key.CHUNK_CACHE_ENABLED, false);
        final String uriPath = "/tif/tif.tif";
        try (HTTPImageInputStream instance =
                     newInstanceFromConstructor2(FIXTURE, uriPath);
             ImageInputStream is =
                     ImageIO.createImageInputStream(FIXTURE.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

    @Test
    void functionalTestWithZeroSizeChunkCache() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.CHUNK_CACHE_ENABLED, true);
        config.setProperty(Key.CHUNK_CACHE_MAX_SIZE, 0);
        final String uriPath = "/tif/tif.tif";
        try (HTTPImageInputStream instance =
                     newInstanceFromConstructor2(FIXTURE, uriPath);
             ImageInputStream is =
                     ImageIO.createImageInputStream(FIXTURE.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

    @Test
    void functionalTestWithChunkCacheEnabled() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.CHUNK_CACHE_ENABLED, true);
        config.setProperty(Key.CHUNK_CACHE_MAX_SIZE, "50M");
        final String uriPath = "/tif/tif.tif";
        try (HTTPImageInputStream instance =
                     newInstanceFromConstructor2(FIXTURE, uriPath);
             ImageInputStream is =
                     ImageIO.createImageInputStream(FIXTURE.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

    @Test
    void functionalTestWithWindowSizeSmallerThanLength() throws Exception {
        final String uriPath = "/tif/tif.tif";
        try (HTTPImageInputStream instance =
                     newInstanceFromConstructor2(FIXTURE, uriPath);
             ImageInputStream is =
                     ImageIO.createImageInputStream(FIXTURE.toFile())) {
            instance.setWindowSize(1024);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

    @Test
    void functionalTestWithWindowSizeLargerThanLength() throws Exception {
        final String uriPath = "/tif/tif.tif";
        try (HTTPImageInputStream instance =
                     newInstanceFromConstructor2(FIXTURE, uriPath);
             ImageInputStream is =
                     ImageIO.createImageInputStream(FIXTURE.toFile())) {
            instance.setWindowSize(65536);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

}
