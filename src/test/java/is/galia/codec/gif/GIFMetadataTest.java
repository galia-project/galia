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

import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GIFMetadataTest extends BaseTest {

    private ImageInputStream newImageInputStream(String fixtureName)
            throws IOException {
        final Path srcFile = TestUtils.getSampleImage(fixtureName);
        return ImageIO.createImageInputStream(srcFile.toFile());
    }

    @Test
    void getMetadataWithStaticImage() throws Exception {
        try (GIFMetadataReader reader = new GIFMetadataReader()) {
            reader.setSource(newImageInputStream("gif/gif.gif"));
            GIFMetadata metadata = new GIFMetadata(reader);
            GIFNativeMetadata nativeMD =
                    (GIFNativeMetadata) metadata.getNativeMetadata().orElseThrow();
            assertEquals(0, nativeMD.getDelayTime());
            assertEquals(0, nativeMD.getLoopCount());
        }
    }

    @Test
    void getMetadataWithAnimatedImage() throws Exception {
        try (GIFMetadataReader reader = new GIFMetadataReader()) {
            reader.setSource(newImageInputStream("gif/animated-looping.gif"));
            GIFMetadata metadata = new GIFMetadata(reader);
            GIFNativeMetadata nativeMD =
                    (GIFNativeMetadata) metadata.getNativeMetadata().orElseThrow();
            assertEquals(15, nativeMD.getDelayTime());
            assertEquals(2, nativeMD.getLoopCount());
        }
    }

    @Test
    void getMetadataWithAnimatedNonLoopingImage() throws Exception {
        try (GIFMetadataReader reader = new GIFMetadataReader()) {
            reader.setSource(newImageInputStream("gif/animated-non-looping.gif"));
            GIFMetadata metadata = new GIFMetadata(reader);
            GIFNativeMetadata nativeMD =
                    (GIFNativeMetadata) metadata.getNativeMetadata().orElseThrow();
            assertEquals(15, nativeMD.getDelayTime());
            assertEquals(0, nativeMD.getLoopCount());
        }
    }

    @Test
    void getXMP() throws Exception {
        try (GIFMetadataReader reader = new GIFMetadataReader()) {
            reader.setSource(newImageInputStream("gif/xmp.gif"));
            GIFMetadata metadata = new GIFMetadata(reader);
            assertTrue(metadata.getXMP().isPresent());
        }
    }

    @Test
    void toMap() throws Exception {
        try (GIFMetadataReader reader = new GIFMetadataReader()) {
            reader.setSource(newImageInputStream("gif/xmp.gif"));
            GIFMetadata metadata = new GIFMetadata(reader);
            Map<String,Object> map = metadata.toMap();
            assertNotNull(map.get("xmp_model"));
            assertNotNull(map.get("xmp_string"));
            assertInstanceOf(Map.class, map.get("native"));
            assertInstanceOf(Map.class, map.get("xmp_elements"));
        }
    }

}
