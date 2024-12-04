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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GIFMetadataReaderTest extends BaseTest {

    private GIFMetadataReader instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new GIFMetadataReader();
    }

    /* getDelayTime() */

    @Test
    void testGetDelayTime() throws Exception {
        Path file = TestUtils.getSampleImage("gif/animated-looping.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(15, instance.getDelayTime());
        }
    }

    /* getHeight() */

    @Test
    void testGetHeightWithValidImage() throws Exception {
        Path file = TestUtils.getSampleImage("gif/gif.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(56, instance.getHeight());
        }
    }

    @Test
    void testGetHeightWithInvalidImage() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/jpg.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(IOException.class, () -> instance.getHeight());
        }
    }

    @Test
    void testGetHeightWithSourceNotSet() {
        assertThrows(IllegalStateException.class, () -> instance.getHeight());
    }

    /* getLoopCount() */

    @Test
    void testGetLoopCountWithLoopingImage() throws Exception {
        Path file = TestUtils.getSampleImage("gif/animated-looping.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(2, instance.getLoopCount());
        }
    }

    @Test
    void testGetLoopCountWithNonLoopingImage() throws Exception {
        Path file = TestUtils.getSampleImage("gif/animated-non-looping.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(0, instance.getLoopCount());
        }
    }

    /* getWidth() */

    @Test
    void testGetWidthWithValidImage() throws Exception {
        Path file = TestUtils.getSampleImage("gif/gif.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(64, instance.getWidth());
        }
    }

    @Test
    void testGetWidthWithInvalidImage() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/jpg.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(IOException.class, () -> instance.getWidth());
        }
    }

    @Test
    void testGetWidthWithSourceNotSet() {
        assertThrows(IllegalStateException.class, () -> instance.getWidth());
    }

    /* getXMP() */

    @Test
    void testGetXMPWithValidImageContainingXMP() throws Exception {
        Path file = TestUtils.getSampleImage("gif/xmp.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);

            String xmpStr = instance.getXMP();
            assertTrue(xmpStr.startsWith("<rdf:RDF"));
            assertTrue(xmpStr.endsWith("</rdf:RDF>"));
        }
    }

    @Test
    void testGetXMPWithValidImageNotContainingXMP() throws Exception {
        Path file = TestUtils.getSampleImage("gif/gif.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getXMP());
        }
    }

    @Test
    void testGetXMPWithInvalidImage() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/jpg.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(IOException.class, () -> instance.getXMP());
        }
    }

    @Test
    void testGetXMPWithSourceNotSet() {
        assertThrows(IllegalStateException.class, () -> instance.getXMP());
    }

}