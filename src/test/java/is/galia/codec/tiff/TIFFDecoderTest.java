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

package is.galia.codec.tiff;

import is.galia.codec.AbstractDecoderTest;
import is.galia.codec.DecoderHint;
import is.galia.codec.SourceFormatException;
import is.galia.image.Format;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TIFFDecoderTest extends AbstractDecoderTest {

    @Override
    protected Path getSupportedFixture() {
        return TestUtils.getSampleImage("tif/tif.tif");
    }

    @Override
    protected Path getUnsupportedFixture() {
        return TestUtils.getSampleImage("jpg/jpg.jpg");
    }

    @Override
    protected TIFFDecoder newInstance() throws IOException {
        TIFFDecoder decoder = new TIFFDecoder();
        decoder.setSource(getSupportedFixture());
        return decoder;
    }

    /* detectFormat() */

    @Test
    void detectFormatWithLittleEndian() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/intel.tif"));
        assertEquals(Format.get("tif"), instance.detectFormat());
    }

    @Test
    void detectFormatWithBigEndian() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/motorola.tif"));
        assertEquals(Format.get("tif"), instance.detectFormat());
    }

    /* getNumImages() */

    @Test
    void getNumImagesWithNonPyramidalMultiImageTIFF() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/multipage.tif"));
        assertEquals(9, instance.getNumImages());
    }

    @Test
    void getNumImagesWithPyramidalTIFF() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/pyramid.tif"));
        assertEquals(4, instance.getNumImages());
    }

    /* getNumResolutions() */

    @Test
    void getNumResolutionsWithNonPyramidalMultiImageTIFF() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/multipage.tif"));
        assertEquals(1, instance.getNumResolutions());
    }

    @Test
    void getNumResolutionsWithPyramidalTIFF() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/pyramid.tif"));
        assertEquals(4, instance.getNumResolutions());
    }

    /* getPreferredIIOImplementations() */

    @Test
    void getPreferredIIOImplementations() {
        String[] impls = ((TIFFDecoder) instance).getPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.tiff.TIFFImageReader", impls[0]);
    }

    /* getTileSize() */

    @Override
    @Test
    public void getTileSize() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgb-8bit-uncompressed.tif"));
        assertEquals(new Size(16, 16), instance.getTileSize(0));
    }

    /* read(int) */

    @Test
    void decode1WithMultiResolutionImage() {
        // TODO: write this
    }

    @Test
    void decode1WithMultiPageImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/multipage.tif"));
        // Read two distinct images.
        BufferedImage image1 = instance.decode(0);
        BufferedImage image2 = instance.decode(2);
        // Assert that they are different.
        assertNotEquals(image1.getWidth(), image2.getWidth());
    }

    @Test
    void decode1WithBigTIFF() {
        instance.setSource(TestUtils.getSampleImage("tif/bigtiff.tf8"));
        assertThrows(SourceFormatException.class, () -> instance.decode(0));
    }

    @Test
    void decode1WithCMYK() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/cmyk.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(300, image.getWidth());
        assertEquals(400, image.getHeight());
    }

    @Test
    void decode1WithStripedRGB16BitLZWCompressedImage() {
        instance.setSource(TestUtils.getSampleImage("tif/striped-rgb-16bit-lzw.tif"));
        assertThrows(IOException.class, () -> instance.decode(0));
    }

    @Test
    void decode1WithStripedRGB16BitPackBitsCompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/striped-rgb-16bit-packbits.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithStripedRGB16BitUncompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/striped-rgb-16bit-uncompressed.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithStripedRGB16BitZipCompressedImage() {
        instance.setSource(TestUtils.getSampleImage("tif/striped-rgb-16bit-zip.tif"));
        assertThrows(IOException.class, () -> instance.decode(0));
    }

    @Test
    void decode1WithStripedRGBA8BitJPEGCompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/striped-rgba-8bit-jpeg.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithStripedRGBA8BitLZWCompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/striped-rgba-8bit-lzw.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithStripedRGBA8BitPackBitsCompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/striped-rgba-8bit-packbits.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithStripedRGBA8BitUncompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/striped-rgba-8bit-uncompressed.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithStripedRGBA8BitZipCompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/striped-rgba-8bit-zip.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithStripedRGBA16BitLZWCompressedImage() {
        instance.setSource(TestUtils.getSampleImage("tif/striped-rgba-16bit-lzw.tif"));
        assertThrows(IOException.class, () -> instance.decode(0));
    }

    @Test
    void decode1WithStripedRGBA16BitPackBitsCompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/striped-rgba-16bit-packbits.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithStripedRGBA16BitUncompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/striped-rgba-16bit-uncompressed.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithStripedRGBA16BitZipCompressedImage() {
        instance.setSource(TestUtils.getSampleImage("tif/striped-rgba-16bit-zip.tif"));
        assertThrows(IOException.class, () -> instance.decode(0));
    }

    @Test
    void decode1WithTileSizeLargerThanImageSize() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/tile-larger-than-image.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(69, image.getWidth());
        assertEquals(54, image.getHeight());
    }

    @Test
    void decode1WithTiledRGB8BitJPEGCompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgb-8bit-jpeg.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithTiledRGB8BitLZWCompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgb-8bit-lzw.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithTiledRGB8BitPackBitsCompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgb-8bit-packbits.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithTiledRGB8BitUncompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgb-8bit-uncompressed.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithTiledRGB8BitZipCompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgb-8bit-zip.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithTiledRGB16BitLZWCompressedImage() {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgb-16bit-lzw.tif"));
        assertThrows(IOException.class, () -> instance.decode(0));
    }

    @Test
    void decode1WithTiledRGB16BitPackBitsCompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgb-16bit-packbits.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithTiledRGB16BitUncompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgb-16bit-uncompressed.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithTiledRGB16BitZipCompressedImage() {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgb-16bit-zip.tif"));
        assertThrows(IOException.class, () -> instance.decode(0));
    }

    @Test
    void decode1WithTiledRGBA8BitJPEGCompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgba-8bit-jpeg.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithTiledRGBA8BitLZWCompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgba-8bit-lzw.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithTiledRGBA8BitPackBitsCompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgba-8bit-packbits.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithTiledRGBA8BitUncompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgba-8bit-uncompressed.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithTiledRGBA8BitZipCompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgba-8bit-zip.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithTiledRGBA16BitLZWCompressedImage() {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgba-16bit-lzw.tif"));
        assertThrows(IOException.class, () -> instance.decode(0));
    }

    @Test
    void decode1WithTiledRGBA16BitPackBitsCompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgba-16bit-packbits.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithTiledRGBA16BitUncompressedImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgba-16bit-uncompressed.tif"));
        BufferedImage image = instance.decode(0);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    void decode1WithTiledRGBA16BitZipCompressedImage() {
        instance.setSource(TestUtils.getSampleImage("tif/tiled-rgba-16bit-zip.tif"));
        assertThrows(IOException.class, () -> instance.decode(0));
    }

    /* read(int, ...) */

    @Test
    void decode2WithCMYK() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/cmyk.tif"));
        Region region                   = new Region(0, 0, 1694, 2170);
        double[] scales                 = { 1, 1 };
        double[] diffScales             = new double[2];
        ReductionFactor reductionFactor = new ReductionFactor(1);
        Set<DecoderHint> hints          = EnumSet.noneOf(DecoderHint.class);
        BufferedImage image = instance.decode(
                0, region, scales, reductionFactor, diffScales, hints);
        assertEquals(300, image.getWidth());
        assertEquals(400, image.getHeight());
    }

    @Test
    void decode2WithMultiPageImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/multipage.tif"));
        Region region                   = new Region(0, 0, 1694, 2170);
        double[] scales                 = { 1, 1 };
        double[] diffScales             = new double[2];
        ReductionFactor reductionFactor = new ReductionFactor(1);
        Set<DecoderHint> hints          = EnumSet.noneOf(DecoderHint.class);
        // Read two distinct images.
        BufferedImage image1 = instance.decode(
                1, region, scales, reductionFactor, diffScales, hints);
        BufferedImage image2 = instance.decode(
                2, region, scales, reductionFactor, diffScales, hints);
        // Assert that they are different.
        assertNotEquals(image1.getWidth(), image2.getWidth());
    }

    @Test
    void decode2WithOrientation0() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/striped-orientation-0.tif"));
        Region region                   = new Region(0, 0, 150, 200, true);
        double[] scales                 = { 1, 1 };
        double[] diffScales             = new double[2];
        ReductionFactor reductionFactor = new ReductionFactor(0);
        Set<DecoderHint> hints          = EnumSet.noneOf(DecoderHint.class);
        BufferedImage image = instance.decode(
                0, region, scales, reductionFactor, diffScales, hints);
        assertEquals(150, image.getWidth());
        assertEquals(200, image.getHeight());
        assertFalse(hints.contains(DecoderHint.ALREADY_ORIENTED));
    }

    @Test
    void decode2WithOrientation90() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/striped-orientation-90.tif"));
        Region region                   = new Region(0, 0, 150, 200, true);
        double[] scales                 = { 1, 1 };
        double[] diffScales             = new double[2];
        ReductionFactor reductionFactor = new ReductionFactor(0);
        Set<DecoderHint> hints          = EnumSet.noneOf(DecoderHint.class);
        BufferedImage image = instance.decode(
                0, region, scales, reductionFactor, diffScales, hints);
        assertEquals(200, image.getWidth());
        assertEquals(150, image.getHeight());
        assertFalse(hints.contains(DecoderHint.ALREADY_ORIENTED));
    }

    @Test
    void decode2WithOrientation180() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/striped-orientation-180.tif"));
        Region region                   = new Region(0, 0, 150, 200, true);
        double[] scales                 = { 1, 1 };
        double[] diffScales             = new double[2];
        ReductionFactor reductionFactor = new ReductionFactor(0);
        Set<DecoderHint> hints          = EnumSet.noneOf(DecoderHint.class);
        BufferedImage image = instance.decode(
                0, region, scales, reductionFactor, diffScales, hints);
        assertEquals(150, image.getWidth());
        assertEquals(200, image.getHeight());
        assertFalse(hints.contains(DecoderHint.ALREADY_ORIENTED));
    }

    @Test
    void decode2WithOrientation270() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/striped-orientation-270.tif"));
        Region region                   = new Region(0, 0, 150, 200, true);
        double[] scales                 = { 1, 1 };
        double[] diffScales             = new double[2];
        ReductionFactor reductionFactor = new ReductionFactor(0);
        Set<DecoderHint> hints          = EnumSet.noneOf(DecoderHint.class);
        BufferedImage image = instance.decode(
                0, region, scales, reductionFactor, diffScales, hints);
        assertEquals(200, image.getWidth());
        assertEquals(150, image.getHeight());
        assertFalse(hints.contains(DecoderHint.ALREADY_ORIENTED));
    }

    @Test
    void decode2WithTileLargerThanImage() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/tile-larger-than-image.tif"));
        Region region                   = new Region(0, 0, 69, 54, true);
        double[] scales                 = { 1, 1 };
        double[] diffScales             = new double[2];
        ReductionFactor reductionFactor = new ReductionFactor(0);
        Set<DecoderHint> hints          = EnumSet.noneOf(DecoderHint.class);
        BufferedImage image = instance.decode(
                0, region, scales, reductionFactor, diffScales, hints);
        assertEquals(69, image.getWidth());
        assertEquals(54, image.getHeight());
        assertFalse(hints.contains(DecoderHint.ALREADY_ORIENTED));
    }

    /**
     * No-op override because we want to use more specific tests.
     */
    @Test
    public void decode2AddsDecoderHints() {}

    @Test
    void decode2WithSingleResolutionImageIgnoresScale() throws Exception {
        Region region          = new Region(10, 10, 40, 40);
        double[] scales        = { 0.875, 0.875 };
        double[] diffScales    = new double[2];
        ReductionFactor rf     = new ReductionFactor();
        Set<DecoderHint> hints = EnumSet.noneOf(DecoderHint.class);

        instance.setSource(TestUtils.getSampleImage("tif/intel.tif"));
        instance.decode(0, region, scales, rf, diffScales, hints);

        assertTrue(hints.contains(DecoderHint.IGNORED_SCALE));
    }

    @Test
    void decode2WithMultiResolutionImageDoesNotIgnoreScale() throws Exception {
        Region region          = new Region(10, 10, 40, 40);
        double[] scales        = { 0.875, 0.875 };
        double[] diffScales    = new double[2];
        ReductionFactor rf     = new ReductionFactor();
        Set<DecoderHint> hints = EnumSet.noneOf(DecoderHint.class);

        instance.setSource(TestUtils.getSampleImage("tif/pyramid.tif"));
        instance.decode(0, region, scales, rf, diffScales, hints);

        assertFalse(hints.contains(DecoderHint.IGNORED_SCALE));
    }

    @Test
    void decode2WithMultiResolutionImageWhenDifferentialScalingIsNotRequired()
            throws Exception {
        Region region          = new Region(10, 10, 40, 40);
        double[] scales        = { 0.25, 0.25 };
        double[] diffScales    = new double[2];
        ReductionFactor rf     = new ReductionFactor();
        Set<DecoderHint> hints = EnumSet.noneOf(DecoderHint.class);

        instance.setSource(TestUtils.getSampleImage("tif/pyramid.tif"));
        instance.decode(0, region, scales, rf, diffScales, hints);

        assertFalse(hints.contains(DecoderHint.NEEDS_DIFFERENTIAL_SCALE));
    }

    @Test
    void decode2WithMultiResolutionImageWhenDifferentialScalingIsRequired()
            throws Exception {
        Region region          = new Region(10, 10, 40, 40);
        double[] scales        = { 0.875, 0.875 };
        double[] diffScales    = new double[2];
        ReductionFactor rf     = new ReductionFactor();
        Set<DecoderHint> hints = EnumSet.noneOf(DecoderHint.class);

        instance.setSource(TestUtils.getSampleImage("tif/pyramid.tif"));
        instance.decode(0, region, scales, rf, diffScales, hints);

        assertTrue(hints.contains(DecoderHint.NEEDS_DIFFERENTIAL_SCALE));
    }

    /* readMetadata() */

    @Test
    void decodeMetadataWithEXIF() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/exif.tif"));
        Metadata metadata = instance.readMetadata(0);
        assertFalse(metadata.getEXIF().isEmpty());
    }

    @Test
    void decodeMetadataWithIPTC() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/iptc.tif"));
        Metadata metadata = instance.readMetadata(0);
        assertFalse(metadata.getIPTC().isEmpty());
    }

    @Test
    void decodeMetadataWithXMP() throws Exception {
        instance.setSource(TestUtils.getSampleImage("tif/xmp.tif"));
        Metadata metadata = instance.readMetadata(0);
        assertTrue(metadata.getXMP().isPresent());
    }

    /* readSequence() */

    @Test
    public void decodeSequenceWithStaticImage() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.decodeSequence());
    }

    @Test
    public void decodeSequenceWithIncompatibleImage() {
        instance.setSource(getUnsupportedFixture());
        assertThrows(UnsupportedOperationException.class,
                () -> instance.decodeSequence());
    }

}
