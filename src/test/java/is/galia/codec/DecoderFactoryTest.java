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

import is.galia.codec.bmp.BMPDecoder;
import is.galia.codec.gif.GIFDecoder;
import is.galia.codec.jpeg.JPEGDecoder;
import is.galia.codec.png.PNGDecoder;
import is.galia.codec.tiff.TIFFDecoder;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.image.Format;
import is.galia.plugin.PluginManager;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DecoderFactoryTest extends BaseTest {

    private Arena arena;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        arena = Arena.ofConfined();
        Map<String,String> mappings = Map.of(
                "bmp", BMPDecoder.class.getSimpleName(),
                "gif", GIFDecoder.class.getSimpleName(),
                "jpg", JPEGDecoder.class.getSimpleName(),
                "png", PNGDecoder.class.getSimpleName(),
                "tif", TIFFDecoder.class.getSimpleName());
        Configuration.forApplication().setProperty(
                Key.DECODER_FORMATS, mappings);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        arena.close();
    }

    /* getAllDecoders() */

    @Test
    void getAllDecoders() {
        assertTrue(DecoderFactory.getAllDecoders().size() > 1);
    }

    /* getAllSupportedFormats() */

    @Test
    void getAllSupportedFormats() {
        final Set<Format> knownSupported = Set.of(
                Format.get("bmp"), Format.get("gif"), Format.get("jpg"),
                Format.get("png"), Format.get("tif"));
        final Set<Format> supported = DecoderFactory.getAllSupportedFormats();
        knownSupported.forEach(f -> assertTrue(supported.contains(f)));
    }

    /* getPluginDecoders() */

    @Test
    void getPluginDecoders() {
        Path pluginsDir = PluginManager.getPluginsDir();
        try {
            PluginManager.setPluginsDir(Path.of("/bogus"));
            assertFalse(DecoderFactory.getPluginDecoders().isEmpty());
        } finally {
            PluginManager.setPluginsDir(pluginsDir);
        }
    }

    /* newDecoder(Format, Arena) */

    @Test
    void newDecoderWithNoCompatibleDecoders() {
        assertThrows(SourceFormatException.class,
                () -> DecoderFactory.newDecoder(Format.UNKNOWN, arena));
    }

    @Test
    void newDecoderWithOneCompatibleDecoderAndNoPreference() throws Exception {
        assertNotNull(DecoderFactory.newDecoder(Format.get("jpg"), arena));
    }

    @Test
    void newDecoderWithOneCompatibleDecoderAndInvalidPreference() {
        Format format = Format.get("jpg");
        Configuration.forApplication().setProperty(
                Key.DECODER_FORMATS, Map.of("jpg", "BogusJPEGDecoder"));

        Exception e = assertThrows(DecoderConfigurationException.class,
                () -> DecoderFactory.newDecoder(format, arena));
        assertEquals("The " + Key.DECODER_FORMATS +
                        " configuration key contains an invalid " +
                        "implementation name for format " + format.key() +
                        " (is this a plugin that is installed and compatible?)",
                e.getMessage());
    }

    @Test
    void newDecoderWithOneCompatibleDecoderAndValidPreference()
            throws Exception {
        Format format = Format.get("jpg");
        Configuration.forApplication().setProperty(
                Key.DECODER_FORMATS, Map.of(format.key(), "JPEGDecoder"));
        assertNotNull(DecoderFactory.newDecoder(format, arena));
    }

    @Test
    void newDecoderWithMultipleCompatibleDecodersAndNoPreference() {
        Format format = MockDecoderPlugin.SUPPORTED_FORMAT;
        Exception e = assertThrows(SourceFormatException.class,
                () -> DecoderFactory.newDecoder(format, arena));
        assertEquals(Key.ENCODER_FORMATS + " does not contain an entry for " +
                        "format: " + format.key(),
                e.getMessage());
    }

    @Test
    void newDecoderWithMultipleCompatibleDecodersAndInvalidPreference() {
        Format format = MockDecoderPlugin.SUPPORTED_FORMAT;
        Configuration.forApplication().setProperty(
                Key.DECODER_FORMATS, Map.of(format.key(), "BogusMockDecoder"));

        Exception e = assertThrows(DecoderConfigurationException.class,
                () -> DecoderFactory.newDecoder(format, arena));
        assertEquals("The " + Key.DECODER_FORMATS +
                        " configuration key contains an invalid " +
                        "implementation name for format " + format.key() +
                        " (is this a plugin that is installed and compatible?)",
                e.getMessage());
    }

    @Test
    void newDecoderWithMultipleCompatibleDecodersAndValidPreference()
            throws Exception {
        Configuration.forApplication().setProperty(
                Key.DECODER_FORMATS, Map.of(
                        MockDecoderPlugin.SUPPORTED_FORMAT.key(),
                        MockDecoderPlugin.class.getSimpleName()));
        Decoder decoder = DecoderFactory.newDecoder(
                MockDecoderPlugin.SUPPORTED_FORMAT, arena);
        assertInstanceOf(MockDecoderPlugin.class, decoder);
    }

    @Test
    void newDecoderInitializesPlugin() throws Exception {
        final Configuration config = Configuration.forApplication();
        config.setProperty(Key.DECODER_FORMATS,
                Map.of("mock", MockDecoderPlugin.class.getSimpleName()));

        Format format = MockDecoderPlugin.SUPPORTED_FORMAT;
        try (MockDecoderPlugin decoder = (MockDecoderPlugin) DecoderFactory.newDecoder(format, arena)) {
            assertTrue(decoder.isInitialized);
        }
    }

    @Test
    void newDecoderWithFormatUnknown() {
        assertThrows(SourceFormatException.class, () -> {
            try (Decoder decoder = DecoderFactory.newDecoder(Format.UNKNOWN, arena)) {
                // no-op
            }
        });
    }

    @Test
    void newDecoderWithFormatBMP() throws Exception {
        try (Decoder decoder = DecoderFactory.newDecoder(Format.get("bmp"), arena)) {
            assertInstanceOf(BMPDecoder.class, decoder);
        }
    }

    @Test
    void newDecoderWithFormatGIF() throws Exception {
        try (Decoder decoder = DecoderFactory.newDecoder(Format.get("gif"), arena)) {
            assertInstanceOf(GIFDecoder.class, decoder);
        }
    }

    @Test
    void newDecoderWithFormatJPEG() throws Exception {
        try (Decoder decoder = DecoderFactory.newDecoder(Format.get("jpg"), arena)) {
            assertInstanceOf(JPEGDecoder.class, decoder);
        }
    }

    @Test
    void newDecoderWithFormatPNG() throws Exception {
        try (Decoder decoder = DecoderFactory.newDecoder(Format.get("png"), arena)) {
            assertInstanceOf(PNGDecoder.class, decoder);
        }
    }

    @Test
    void newDecoderWithFormatTIF() throws Exception {
        try (Decoder decoder = DecoderFactory.newDecoder(Format.get("tif"), arena)) {
            assertInstanceOf(TIFFDecoder.class, decoder);
        }
    }

}
