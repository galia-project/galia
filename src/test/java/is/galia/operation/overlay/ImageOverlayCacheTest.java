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

package is.galia.operation.overlay;

import is.galia.test.BaseTest;
import is.galia.util.ConcurrentProducerConsumer;
import is.galia.test.TestUtils;
import is.galia.test.StaticFileServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

class ImageOverlayCacheTest extends BaseTest {

    private static final Path OVERLAY_FIXTURE =
            TestUtils.getSampleImage("jpg/jpg.jpg");

    private static StaticFileServer webServer;
    private ImageOverlayCache instance;

    @BeforeAll
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();
        webServer = new StaticFileServer();
        webServer.start();
    }

    @AfterAll
    public static void afterClass() throws Exception {
        BaseTest.afterClass();
        webServer.stop();
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new ImageOverlayCache();
    }

    /* putAndGet(URI) */

    @Test
    void putAndGetWithPresentFileURI() throws IOException {
        URI uri = OVERLAY_FIXTURE.toUri();
        byte[] bytes = instance.putAndGet(uri);
        assertEquals(Files.size(OVERLAY_FIXTURE), bytes.length);
    }

    @Test
    void putAndGetWithMissingFileURI() {
        URI uri = TestUtils.getSampleImage("blablabla").toUri();
        assertThrows(IOException.class, () -> instance.putAndGet(uri));
    }

    @Test
    void putAndGetWithPresentRemoteURI() throws Exception {
        URI uri = new URI(webServer.getHTTPURI() + "/sample-images/jpg/jpg.jpg");
        byte[] bytes = instance.putAndGet(uri);
        assertEquals(Files.size(OVERLAY_FIXTURE), bytes.length);
    }

    @Test
    void putAndGetWithMissingRemoteURI() throws Exception {
        URI uri = new URI(webServer.getHTTPURI() + "/blablabla");
        assertThrows(IOException.class, () -> instance.putAndGet(uri));
    }

    @Test
    void putAndGetConcurrently() throws Exception {
        Callable<Void> callable = () -> {
            URI uri = new URI(webServer.getHTTPURI() + "/sample-images/jpg/jpg.jpg");
            byte[] bytes = instance.putAndGet(uri);
            assertEquals(Files.size(OVERLAY_FIXTURE), bytes.length);
            return null;
        };
        new ConcurrentProducerConsumer(callable, callable, 5000).run();
    }

}
