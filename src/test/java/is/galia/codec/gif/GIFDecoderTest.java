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

package is.galia.codec.gif;

import is.galia.codec.AbstractDecoderTest;
import is.galia.codec.DecoderHint;
import is.galia.image.Format;
import is.galia.image.Metadata;
import is.galia.image.Region;
import is.galia.image.ReductionFactor;
import is.galia.codec.BufferedImageSequence;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GIFDecoderTest extends AbstractDecoderTest {

    @Override
    protected Path getSupportedFixture() {
        return TestUtils.getSampleImage("gif/gif.gif");
    }

    @Override
    protected Path getUnsupportedFixture() {
        return TestUtils.getSampleImage("png/png.png");
    }

    @Override
    protected GIFDecoder newInstance() throws IOException {
        GIFDecoder decoder = new GIFDecoder();
        decoder.setSource(getSupportedFixture());
        return decoder;
    }

    /* detectFormat() */

    @Test
    void detectFormatWithGIF87a() throws Exception {
        instance.setSource(TestUtils.getSampleImage("gif/87a.gif"));
        assertEquals(Format.get("gif"), instance.detectFormat());
    }

    @Test
    void detectFormatWithGIF89a() throws Exception {
        instance.setSource(TestUtils.getSampleImage("gif/89a.gif"));
        assertEquals(Format.get("gif"), instance.detectFormat());
    }

    /* getPreferredIIOImplementations() */

    @Test
    void getPreferredIIOImplementations() {
        String[] impls = ((GIFDecoder) instance).getPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.gif.GIFImageReader", impls[0]);
    }

    /* read() */

    @Test
    void decodeWithArguments() throws Exception {
        Region region       = new Region(10, 10, 40, 40);
        double[] scales        = { 0.875, 0.875 };
        double[] diffScales    = new double[2];
        ReductionFactor rf     = new ReductionFactor();
        Set<DecoderHint> hints = new HashSet<>();

        BufferedImage image = instance.decode(
                0, region, scales, rf, diffScales, hints);

        assertEquals(40, image.getWidth());
        assertEquals(40, image.getHeight());
        assertEquals(0, rf.factor);
    }

    /* readMetadata() */

    @Test
    void decodeMetadataWithXMP() throws Exception {
        instance.setSource(TestUtils.getSampleImage("gif/xmp.gif"));
        Metadata metadata = instance.readMetadata(0);
        assertTrue(metadata.getXMP().isPresent());
    }

    @Test
    void decodeMetadataWithAnimation() throws Exception {
        instance.setSource(TestUtils.getSampleImage("gif/animated-looping.gif"));
        Metadata metadata = instance.readMetadata(0);
        GIFNativeMetadata nativeMD =
                (GIFNativeMetadata) metadata.getNativeMetadata().orElseThrow();
        assertEquals(15, nativeMD.getDelayTime());
        assertEquals(2, nativeMD.getLoopCount());
    }

    /* readSequence() */

    @Test
    void decodeSequenceWithAnimatedImage() throws Exception {
        instance = new GIFDecoder();
        instance.setSource(TestUtils.getSampleImage("gif/animated-looping.gif"));
        BufferedImageSequence seq = instance.decodeSequence();
        assertEquals(2, seq.length());
    }

}
