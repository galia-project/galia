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

package is.galia.codec.bmp;

import is.galia.codec.AbstractDecoderTest;
import is.galia.image.EmptyMetadata;
import is.galia.image.Format;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BMPDecoderTest extends AbstractDecoderTest {

    @Override
    protected Path getSupportedFixture() {
        return TestUtils.getSampleImage("bmp/bmp.bmp");
    }

    @Override
    protected Path getUnsupportedFixture() {
        return TestUtils.getSampleImage("png/png.png");
    }

    @Override
    protected BMPDecoder newInstance() throws IOException {
        BMPDecoder decoder = new BMPDecoder();
        decoder.setSource(getSupportedFixture());
        return decoder;
    }

    /* detectFormat() */

    @Test
    void detectFormatWithSupportedMagicBytes() throws Exception {
        instance.setSource(TestUtils.getSampleImage("bmp/bmp.bmp"));
        assertEquals(Format.get("bmp"), instance.detectFormat());
    }

    /* getPreferredIIOImplementations() */

    @Test
    void getPreferredIIOImplementations() {
        String[] impls = ((BMPDecoder) instance).
                getPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.bmp.BMPImageReader", impls[0]);
    }

    /* readMetadata() */

    @Test
    void decodeMetadata() throws Exception {
        assertInstanceOf(EmptyMetadata.class, instance.readMetadata(0));
    }

}
