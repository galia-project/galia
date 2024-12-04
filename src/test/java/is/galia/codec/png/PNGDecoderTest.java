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

package is.galia.codec.png;

import is.galia.codec.AbstractDecoderTest;
import is.galia.codec.DecoderHint;
import is.galia.image.Format;
import is.galia.image.Metadata;
import is.galia.image.NativeMetadata;
import is.galia.image.Region;
import is.galia.image.ReductionFactor;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class PNGDecoderTest extends AbstractDecoderTest {

    @Override
    protected Path getSupportedFixture() {
        return TestUtils.getSampleImage("png/png.png");
    }

    @Override
    protected Path getUnsupportedFixture() {
        return TestUtils.getSampleImage("jpg/jpg.jpg");
    }

    @Override
    protected PNGDecoder newInstance() throws IOException {
        PNGDecoder decoder = new PNGDecoder();
        decoder.setSource(getSupportedFixture());
        return decoder;
    }

    /* detectFormat() */

    @Test
    void detectFormatWithSupportedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("png/png.png"));
        assertEquals(Format.get("png"), instance.detectFormat());
    }

    /* getPreferredIIOImplementations() */

    @Test
    void getPreferredIIOImplementations() {
        String[] impls = ((PNGDecoder) instance).getPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.png.PNGImageReader", impls[0]);
    }

    /* read(int, ...) */

    @Test
    void decode2WithOrientation() throws Exception {
        instance.setSource(TestUtils.getSampleImage("png/rotated.png"));

        BufferedImage result = instance.decode(0,
                new Region(0, 0, 56, 64, true),
                new double[] {1,1},
                new ReductionFactor(),
                new double[] {1, 1},
                EnumSet.noneOf(DecoderHint.class));
        assertEquals(56, result.getWidth());
        assertEquals(64, result.getHeight());
    }

    /* readMetadata() */

    @Test
    void decodeMetadataWithPNGMetadata() throws Exception {
        instance.setSource(TestUtils.getSampleImage("png/nativemetadata.png"));
        Metadata metadata = instance.readMetadata(0);
        NativeMetadata nativeMD = metadata.getNativeMetadata().orElseThrow();
        assertFalse(nativeMD.toMap().isEmpty());
    }

    @Test
    void decodeMetadataWithXMP() throws Exception {
        instance.setSource(TestUtils.getSampleImage("png/xmp.png"));
        Metadata metadata = instance.readMetadata(0);
        assertTrue(metadata.getXMP().isPresent());
    }

}
