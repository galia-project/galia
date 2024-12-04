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

package is.galia.codec.jpeg;

import is.galia.codec.SourceFormatException;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JPEGMetadataReaderTest extends BaseTest {

    private JPEGMetadataReader instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new JPEGMetadataReader();
    }

    /* getColorTransform() */

    @Test
    void getColorTransformWithNoColorTransform() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/exif.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getColorTransform());
        }
    }

    @Test
    void getColorTransformOnImageWithColorTransform() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/ycck.jpg");
        try (ImageInputStream is =
                     ImageIO.createImageInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            instance.setSource(is);
            assertEquals(JPEGMetadataReader.AdobeColorTransform.YCCK,
                    instance.getColorTransform());
        }
    }

    @Test
    void getColorTransformOnNonJPEGImage() throws Exception {
        Path file = TestUtils.getSampleImage("gif/xmp.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class,
                    () -> instance.getColorTransform());
        }
    }

    /* getEXIF() */

    @Test
    void getEXIFWithEXIFImage() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/exif.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            byte[] exif = instance.getEXIF();
            assertTrue((exif[0] == 0x49 && exif[1] == 0x49) ||
                    (exif[0] == 0x4d && exif[1] == 0x4d));
        }
    }

    @Test
    void getEXIFWithNonEXIFImage() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/jpg.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getEXIF());
        }
    }

    /* getEXIFOffset() */

    @Test
    void getEXIFOffsetWithNonEXIFImage() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/jpg.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(-1, instance.getEXIFOffset());
        }
    }

    @Test
    void getEXIFOffsetWithEXIFImage() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/exif.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(30, instance.getEXIFOffset());
        }
    }

    /* getICCProfile() */

    @Test
    void getICCProfileOnImageWithNoProfile() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/exif.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getICCProfile());
        }
    }

    @Test
    void getICCProfileOnImageWithSingleChunkProfile() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/icc.jpg");
        try (ImageInputStream is =
                     ImageIO.createImageInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            instance.setSource(is);
            assertNotNull(instance.getICCProfile());
        }
    }

    @Test
    void getICCProfileOnImageWithMultiChunkProfile() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/icc-chunked.jpg"); // 17 chunks
        try (ImageInputStream is =
                     ImageIO.createImageInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            instance.setSource(is);
            assertNotNull(instance.getICCProfile());
        }
    }

    @Test
    void getICCProfileOnNonJPEGImage() throws Exception {
        Path file = TestUtils.getSampleImage("gif/xmp.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class,
                    () -> instance.getICCProfile());
        }
    }

    /* getIPTC() */

    @Test
    void getIPTCWithIPTCImage() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/iptc.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            byte[] iptc = instance.getIPTC();
            assertEquals(18, iptc.length);
        }
    }

    @Test
    void getIPTCWithNonIPTCImage() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/jpg.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getIPTC());
        }
    }

    /* getThumbnailData() */

    /* getThumbnailCompression() */

    @Test
    void getThumbnailCompressionWithNoThumbnail() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/rgb-64x56x8-plane.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(-1, instance.getThumbnailCompression());
        }
    }

    @Test
    void getThumbnailCompressionWithThumbnail() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/thumbnail-jpg.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(6, instance.getThumbnailCompression());
        }
    }

    @Test
    void getThumbnailDataWithNoThumbnail() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/jpg.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getThumbnailData());
        }
    }

    @Test
    void getThumbnailDataWithSupportedThumbnail() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/thumbnail-jpg.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNotNull(instance.getThumbnailData());
        }
    }

    /* getWidth() */

    @Test
    void getWidth() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/jpg.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(64, instance.getWidth());
        }
    }

    /* getHeight() */

    @Test
    void getHeight() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/jpg.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(56, instance.getHeight());
        }
    }

    /* getXMP() */

    @Test
    void getXMPWithStandardXMPImage() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/xmp.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            String xmp = instance.getXMP();
            assertTrue(xmp.startsWith("<rdf:RDF"));
            assertTrue(xmp.endsWith("</rdf:RDF>"));
        }
    }

    @Test
    void getXMPWithExtendedXMPImage() throws Exception {
        // N.B.: easy XMP embed:
        // exiftool -tagsfromfile file.xmp -all:all jpg.jpg
        Path file = TestUtils.getSampleImage("jpg/xmp-extended-exiftool.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            String xmp = instance.getXMP();
            assertTrue(xmp.length() > 65502);
            assertTrue(xmp.startsWith("<rdf:RDF"));
            assertTrue(xmp.endsWith("</rdf:RDF>" + System.lineSeparator()));
            assertFalse(xmp.contains("HasExtendedXMP"));
        }
    }

    @Test
    void getXMPWithNonXMPImage() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/jpg.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getXMP());
        }
    }

    /* hasAdobeSegment() */

    @Test
    void hasAdobeSegmentOnImageWithNoAdobeSegment() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/exif.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertFalse(instance.hasAdobeSegment());
        }
    }

    @Test
    void hasAdobeSegmentOnImageWithAdobeSegment() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/ycck.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertTrue(instance.hasAdobeSegment());
        }
    }

    @Test
    void hasAdobeSegmentOnNonJPEGImage() throws Exception {
        Path file = TestUtils.getSampleImage("gif/xmp.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class,
                    () -> instance.hasAdobeSegment());
        }
    }

    /* isProgressive() */

    @Test
    void isProgressiveWithBaselineImage() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/rgb-64x56x8-baseline.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertFalse(instance.isProgressive());
        }
    }

    @Test
    void isProgressiveWithLineInterlacedImage() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/rgb-64x56x8-line.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertTrue(instance.isProgressive());
        }
    }

    @Test
    void isProgressiveWithPlaneInterlacedImage() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/rgb-64x56x8-plane.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertTrue(instance.isProgressive());
        }
    }

}
