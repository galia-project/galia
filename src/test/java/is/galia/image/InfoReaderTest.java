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

package is.galia.image;

import is.galia.codec.Decoder;
import is.galia.codec.DecoderFactory;
import is.galia.codec.SourceFormatException;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class InfoReaderTest extends BaseTest {

    @Test
    void readWithFormatNotSet() throws Exception {
        final Path fixture = TestUtils.getSampleImage("jpg/exif.jpg");
        final Format format = Format.get("jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(format, arena)) {
            decoder.setSource(fixture);
            InfoReader reader = new InfoReader();
            reader.setDecoder(decoder);
            assertThrows(IllegalStateException.class, reader::read);
        }
    }

    @Test
    void readWithDecoderNotSet() {
        final Format format = Format.get("jpg");
        final InfoReader reader = new InfoReader();
        reader.setFormat(format);
        assertThrows(IllegalStateException.class, reader::read);
    }

    @Test
    void readWithActualFormatDifferentFromSetFormat() throws Exception {
        Path fixture  = TestUtils.getFixture("unknown");
        Format format = DecoderFactory.getAllSupportedFormats()
                .stream()
                .filter(f -> !"mock".equals(f.key()))
                .findFirst()
                .orElseThrow();
        final InfoReader reader = new InfoReader();
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(format, arena)) {
            decoder.setSource(fixture);
            reader.setDecoder(decoder);
            reader.setFormat(format);
            assertThrows(SourceFormatException.class, reader::read);
        }
    }

    /**
     * This implementation is tile-unaware. Tile-aware processors will need to
     * override it.
     */
    @Test
    void readOnAllFixtures() throws Exception {
        for (Format format : Format.all()) {
            for (Path fixture : TestUtils.getSampleImages(format)) {
                final InfoReader reader = new InfoReader();
                try (Arena arena = Arena.ofConfined();
                     Decoder decoder = DecoderFactory.newDecoder(format, arena)) {
                    decoder.setSource(fixture);
                    reader.setDecoder(decoder);
                    reader.setFormat(format);

                    final Info actualInfo = reader.read();
                    try (Decoder decoder2 = DecoderFactory.newDecoder(format, arena)) {
                        decoder2.setSource(fixture);
                        Size actualSize = decoder2.getSize(0);
                        assertEquals(format, actualInfo.getSourceFormat());
                        assertEquals(actualSize.intWidth(), actualInfo.getSize().width());
                        assertEquals(actualSize.intHeight(), actualInfo.getSize().height());
                    }

                    // Parse the resolution count from the filename.
                    int expectedNumResolutions = 1;
                    Pattern pattern = Pattern.compile("\\dres");
                    Matcher matcher = pattern.matcher(fixture.getFileName().toString());
                    if (matcher.find()) {
                        expectedNumResolutions =
                                Integer.parseInt(matcher.group(0).substring(0, 1));
                        assertEquals(expectedNumResolutions,
                                actualInfo.getNumResolutions());
                    }
                } catch (SourceFormatException ignore) {
                    // The processor doesn't support this format, which is
                    // fine. No processor supports all formats.
                } catch (Exception e) {
                    System.err.println(format + " : " + fixture);
                    throw e;
                }
            }
        }
    }

    @Test
    void readEXIFAwareness() throws Exception {
        final Path fixture = TestUtils.getSampleImage("jpg/exif.jpg");
        final Format format = Format.get("jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(format, arena)) {
            decoder.setSource(fixture);
            InfoReader reader = new InfoReader();
            reader.setFormat(format);
            reader.setDecoder(decoder);

            Info info = reader.read();
            assertTrue(info.getMetadata().getEXIF().isPresent());
        }
    }

    @Test
    void readIPTCAwareness() throws Exception {
        final Path fixture = TestUtils.getSampleImage("jpg/iptc.jpg");
        final Format format = Format.get("jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(format, arena)) {
            decoder.setSource(fixture);
            InfoReader reader = new InfoReader();
            reader.setFormat(format);
            reader.setDecoder(decoder);

            Info info = reader.read();
            assertFalse(info.getMetadata().getIPTC().isEmpty());
        }
    }

    @Test
    void readXMPAwareness() throws Exception {
        final Path fixture = TestUtils.getSampleImage("jpg/xmp.jpg");
        final Format format = Format.get("jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(format, arena)) {
            decoder.setSource(fixture);
            InfoReader reader = new InfoReader();
            reader.setFormat(format);
            reader.setDecoder(decoder);

            Info info = reader.read();
            assertTrue(info.getMetadata().getXMP().isPresent());
        }
    }

    @Test
    void readTileAwareness() throws Exception {
        final Path fixture = TestUtils.
                getSampleImage("tif/tiled-rgb-8bit-uncompressed.tif");
        final Format format = Format.get("tif");

        Info expectedInfo = Info.builder()
                .withSize(64, 56)
                .withTileSize(16, 16)
                .withNumResolutions(1)
                .withFormat(format)
                .build();

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(format, arena)) {
            decoder.setSource(fixture);
            InfoReader reader = new InfoReader();
            reader.setFormat(format);
            reader.setDecoder(decoder);

            Info actualInfo = reader.read();
            actualInfo.getImages().getFirst().setMetadata(new MutableMetadata()); // we don't care about this
            assertEquals(expectedInfo, actualInfo);
        }
    }

}
