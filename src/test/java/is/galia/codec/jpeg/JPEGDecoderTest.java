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

import is.galia.codec.AbstractDecoderTest;
import is.galia.codec.DecoderHint;
import is.galia.codec.SourceFormatException;
import is.galia.image.Metadata;
import is.galia.image.Region;
import is.galia.image.ReductionFactor;
import is.galia.image.Size;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class JPEGDecoderTest extends AbstractDecoderTest {

    @Override
    protected Path getSupportedFixture() {
        return TestUtils.getSampleImage("jpg/jpg.jpg");
    }

    @Override
    protected Path getUnsupportedFixture() {
        return TestUtils.getSampleImage("png/png.png");
    }

    protected JPEGDecoder newInstance() throws IOException {
        JPEGDecoder decoder = new JPEGDecoder();
        decoder.setSource(getSupportedFixture());
        return decoder;
    }

    /* detectFormat() */

    @Test
    void detectFormatWithSupportedMagicBytes() throws Exception {
        instance.setSource(getSupportedFixture());
        assertEquals(JPEGDecoder.FORMAT, instance.detectFormat());
    }

    /* getNumImages() */

    @Override
    @Test
    public void getNumImagesWithIncompatibleImage() throws Exception {
        instance.setSource(getUnsupportedFixture());
        assertEquals(1, instance.getNumImages());
    }

    /* getNumThumbnails() */

    @Test
    void getNumThumbnailsWithNoThumbnail() throws Exception {
        assertEquals(0, instance.getNumThumbnails(0));
    }

    @Test
    void getNumThumbnailsWithUnsupportedCompression() {
        // TODO: write this
    }

    @Test
    void getNumThumbnails() throws Exception {
        instance.setSource(TestUtils.getSampleImage("jpg/thumbnail-jpg.jpg"));
        assertEquals(1, instance.getNumThumbnails(0));
    }

    /* getPreferredIIOImplementations() */

    @Test
    void getPreferredIIOImplementations() {
        String[] impls = ((JPEGDecoder) instance).getPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.jpeg.JPEGImageReader", impls[0]);
    }

    /* getThumbnailSize() */

    @Test
    void getThumbnailSizeWithIllegalThumbnailIndex() {
        instance.setSource(TestUtils.getSampleImage("jpg/thumbnail-jpg.jpg"));
        assertThrows(IndexOutOfBoundsException.class,
                () -> instance.getThumbnailSize(0, 1));
    }

    @Test
    void getThumbnailSizeWithUnsupportedCompression() {
        // TODO: write this
    }

    @Test
    void getThumbnailSize() throws Exception {
        instance.setSource(TestUtils.getSampleImage("jpg/thumbnail-jpg.jpg"));
        Size thumbSize = instance.getThumbnailSize(0, 0);
        assertEquals(64, thumbSize.intWidth());
        assertEquals(56, thumbSize.intHeight());
    }

    /* read() */

    @Test
    void decode1WithGrayImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("jpg/gray.jpg"));

        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithYCCKImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("jpg/ycck.jpg"));

        BufferedImage result = instance.decode(0);
        assertEquals(64, result.getWidth());
        assertEquals(56, result.getHeight());
    }

    /* read(int, ...) */

    @Test
    void decode2WithOrientation() throws Exception {
        instance.setSource(TestUtils.getSampleImage("jpg/exif-orientation-270.jpg"));

        BufferedImage result = instance.decode(0,
                new Region(0, 0, 56, 64, true),
                new double[] {1,1},
                new ReductionFactor(),
                new double[] {1, 1},
                EnumSet.noneOf(DecoderHint.class));
        assertEquals(64, result.getWidth());
        assertEquals(56, result.getHeight());
    }

    /* readMetadata() */

    @Test
    void decodeMetadataWithoutEXIF() throws Exception {
        Metadata metadata = instance.readMetadata(0);
        assertTrue(metadata.getEXIF().isEmpty());
    }

    @Test
    void decodeMetadataWithEXIF() throws Exception {
        instance.setSource(TestUtils.getSampleImage("jpg/exif.jpg"));
        Metadata metadata = instance.readMetadata(0);
        assertFalse(metadata.getEXIF().isEmpty());
    }

    @Test
    void decodeMetadataWithoutIPTC() throws Exception {
        Metadata metadata = instance.readMetadata(0);
        assertTrue(metadata.getIPTC().isEmpty());
    }

    @Test
    void decodeMetadataWithIPTC() throws Exception {
        instance.setSource(TestUtils.getSampleImage("jpg/iptc.jpg"));
        Metadata metadata = instance.readMetadata(0);
        assertFalse(metadata.getIPTC().isEmpty());
    }

    @Test
    void decodeMetadataWithoutXMP() throws Exception {
        Metadata metadata = instance.readMetadata(0);
        assertFalse(metadata.getXMP().isPresent());
    }

    @Test
    void decodeMetadataWithXMP() throws Exception {
        instance.setSource(TestUtils.getSampleImage("jpg/xmp.jpg"));
        Metadata metadata = instance.readMetadata(0);
        assertTrue(metadata.getXMP().isPresent());
    }

    /* readThumbnail() */

    @Test
    public void decodeThumbnailWithEmptyImage() {
        instance.setSource(TestUtils.getFixture("empty"));
        assertThrows(SourceFormatException.class, () ->
                instance.readThumbnail(0, 0));
    }

    @Test
    public void decodeThumbnailWithInvalidImage() {
        instance.setSource(getUnsupportedFixture());
        assertThrows(SourceFormatException.class,
                () -> instance.readThumbnail(0, 0));
    }

    @Test
    public void decodeThumbnailWithIllegalImageIndex() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> instance.readThumbnail(99999, 0));
    }

    @Test
    void decodeThumbnailWithIllegalThumbnailIndex() {
        instance.setSource(TestUtils.getSampleImage("jpg/thumbnail-jpg.jpg"));
        assertThrows(IndexOutOfBoundsException.class,
                () -> instance.readThumbnail(0, 1));
    }

    @Test
    void decodeThumbnailWithUnsupportedCompression() {
        // TODO: write this
    }

    @Test
    void decodeThumbnail() throws Exception {
        instance.setSource(TestUtils.getSampleImage("jpg/thumbnail-jpg.jpg"));
        BufferedImage image = instance.readThumbnail(0, 0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
        assertEquals(3, image.getSampleModel().getNumBands());
    }

}
