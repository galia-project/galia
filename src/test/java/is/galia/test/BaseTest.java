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

package is.galia.test;

import is.galia.Application;
import is.galia.codec.gif.GIFEncoder;
import is.galia.codec.jpeg.JPEGEncoder;
import is.galia.codec.png.PNGEncoder;
import is.galia.codec.tiff.TIFFEncoder;
import is.galia.config.Configuration;
import is.galia.config.ConfigurationFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import is.galia.config.Key;
import is.galia.config.MapConfiguration;

import javax.imageio.ImageIO;
import java.util.Map;

/**
 * Base class for all unit tests.
 */
public abstract class BaseTest {

    static {
        // Suppress a Dock icon and annoying Space transition in full-screen
        // mode in macOS.
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeAll
    public static void beforeClass() throws Exception {
        System.setProperty(Application.ENVIRONMENT_VM_ARGUMENT,
                Application.TEST_ENVIRONMENT);
        ImageIO.scanForPlugins();
        ImageIO.setUseCache(false);
    }

    @AfterAll
    public static void afterClass() throws Exception {}

    @BeforeEach
    public void setUp() throws Exception {
        Configuration config = new MapConfiguration();
        config.setProperty(Key.PRINT_STACK_TRACE_ON_ERROR_PAGES, true);
        config.setProperty(Key.CACHE_WORKER_ENABLED, false);
        config.setProperty(Key.ENCODER_FORMATS, Map.of(
                "gif", GIFEncoder.class.getSimpleName(),
                "jpg", JPEGEncoder.class.getSimpleName(),
                "png", PNGEncoder.class.getSimpleName(),
                "tif", TIFFEncoder.class.getSimpleName()));
        ConfigurationFactory.setAppInstance(config);
    }

    @AfterEach
    public void tearDown() throws Exception {}

}
