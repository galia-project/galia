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

package is.galia.codec;

import is.galia.codec.gif.GIFEncoder;
import is.galia.codec.jpeg.JPEGEncoder;
import is.galia.codec.png.PNGEncoder;
import is.galia.codec.tiff.TIFFEncoder;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.image.Format;
import is.galia.operation.Encode;
import is.galia.plugin.PluginManager;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EncoderFactoryTest extends BaseTest {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Map<String,String> mappings = Map.of(
                "gif", GIFEncoder.class.getSimpleName(),
                "jpg", JPEGEncoder.class.getSimpleName(),
                "png", PNGEncoder.class.getSimpleName(),
                "tif", TIFFEncoder.class.getSimpleName());
        Configuration.forApplication().setProperty(
                Key.ENCODER_FORMATS, mappings);
    }

    /* getAllEncoders() */

    @Test
    void getAllEncoders() {
        assertTrue(EncoderFactory.getAllEncoders().size() > 1);
    }

    /* getEnabledEncoders() */

    @Test
    void getEnabledEncodersWithNoEnabledEncoders() {
        Configuration.forApplication().setProperty(Key.ENCODER_FORMATS, Map.of());
        Set<Encoder> encoders = EncoderFactory.getEnabledEncoders();
        assertTrue(encoders.isEmpty());
    }

    @Test
    void getEnabledEncodersWithEnabledEncoders() {
        Set<Encoder> encoders = EncoderFactory.getEnabledEncoders();
        assertEquals(4, encoders.size());
    }

    /* getPluginEncoders() */

    @Test
    void getPluginEncoders() {
        Path pluginsDir = PluginManager.getPluginsDir();
        try {
            PluginManager.setPluginsDir(Path.of("/bogus"));
            assertFalse(EncoderFactory.getPluginEncoders().isEmpty());
        } finally {
            PluginManager.setPluginsDir(pluginsDir);
        }
    }

    /* getAllSupportedFormats() */

    @Test
    void getAllSupportedFormats() {
        final Set<Format> knownSupported = Set.of(
                MockDecoderPlugin.SUPPORTED_FORMAT, Format.get("gif"),
                Format.get("jpg"), Format.get("png"), Format.get("tif"));
        final Set<Format> supported = EncoderFactory.getAllSupportedFormats();
        knownSupported.forEach(f -> assertTrue(supported.contains(f)));
    }

    /* newEncoder() */

    @Test
    void newEncoderWithUnmappedFormat() {
        Format format = Format.UNKNOWN;
        Exception e = assertThrows(VariantFormatException.class, () -> {
            try (Arena arena = Arena.ofConfined();
                 Encoder encoder = EncoderFactory.newEncoder(new Encode(format), arena)) {
            }
        });
        assertEquals(Key.ENCODER_FORMATS + " does not contain an entry for " +
                        "format: " + format.key(),
                e.getMessage());
    }

    @Test
    void newEncoderWithMappingToInvalidImplementation() {
        Format format = Format.get("jpg");
        Configuration.forApplication().setProperty(
                Key.ENCODER_FORMATS, Map.of("jpg", "BogusJPEGEncoder"));

        Exception e = assertThrows(EncoderConfigurationException.class, () -> {
            try (Arena arena = Arena.ofConfined();
                 Encoder encoder = EncoderFactory.newEncoder(new Encode(format), arena)) {
            }
        });
        assertEquals("The " + Key.ENCODER_FORMATS +
                        " configuration key contains an invalid " +
                        "implementation name for format " + format.key() +
                        " (is this a plugin that is installed and compatible?)",
                e.getMessage());
    }

    @Test
    void newEncoderWithMappingToIncompatibleImplementation() {
        Format format = Format.get("jpg");
        Configuration.forApplication().setProperty(
                Key.ENCODER_FORMATS, Map.of("jpg", "PNGEncoder"));

        Exception e = assertThrows(VariantFormatException.class, () -> {
            try (Arena arena = Arena.ofConfined();
                 Encoder encoder = EncoderFactory.newEncoder(new Encode(format), arena)) {
            }
        });
        assertEquals("The " + Key.ENCODER_FORMATS +
                        " configuration key contains an incompatible " +
                        "implementation name for format " + format.key(),
                e.getMessage());
    }

    @Test
    void newEncoderInitializesPlugin() throws Exception {
        final Configuration config = Configuration.forApplication();
        config.setProperty(Key.ENCODER_FORMATS,
                Map.of("mock", MockEncoderPlugin.class.getSimpleName()));

        Format format = MockDecoderPlugin.SUPPORTED_FORMAT;
        Encode encode = new Encode(format);
        try (Arena arena = Arena.ofConfined();
             MockEncoderPlugin encoder = (MockEncoderPlugin) EncoderFactory.newEncoder(encode, arena)) {
            assertTrue(encoder.isInitialized);
        }
    }

    @Test
    void newEncoderWithValidMapping() throws Exception {
        try (Arena arena = Arena.ofConfined();
             Encoder encoder = EncoderFactory.newEncoder(new Encode(Format.get("jpg")), arena)) {
            assertNotNull(encoder);
        }
    }

}
