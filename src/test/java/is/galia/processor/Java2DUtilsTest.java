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

package is.galia.processor;

import is.galia.image.ComponentOrder;
import is.galia.image.Size;
import is.galia.image.Orientation;
import is.galia.image.Region;
import is.galia.image.ScaleConstraint;
import is.galia.operation.Color;
import is.galia.operation.ColorTransform;
import is.galia.operation.Crop;
import is.galia.operation.CropByPercent;
import is.galia.operation.CropByPixels;
import is.galia.operation.CropToSquare;
import is.galia.image.ReductionFactor;
import is.galia.operation.Rotate;
import is.galia.operation.ScaleByPercent;
import is.galia.operation.ScaleByPixels;
import is.galia.operation.Sharpen;
import is.galia.operation.Transpose;
import is.galia.operation.redaction.Redaction;
import is.galia.operation.overlay.ImageOverlay;
import is.galia.operation.overlay.Position;
import is.galia.operation.overlay.StringOverlay;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import static is.galia.test.Assert.ImageAssert.*;
import static org.junit.jupiter.api.Assertions.*;

class Java2DUtilsTest extends BaseTest {

    private static final double DELTA = 0.0000001;

    private static BufferedImage newColorImage(int componentSize,
                                               boolean hasAlpha) {
        return newColorImage(100, 100, componentSize, hasAlpha);
    }

    private static BufferedImage newColorImage(int width,
                                               int height,
                                               int componentSize,
                                               boolean hasAlpha) {
        if (componentSize <= 8) {
            int type = hasAlpha ?
                    BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;
            return new BufferedImage(width, height, type);
        }
        final ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        final boolean isAlphaPremultiplied = false;
        final int transparency = (hasAlpha) ?
                Transparency.TRANSLUCENT : Transparency.OPAQUE;
        final int dataType = DataBuffer.TYPE_USHORT;
        final ColorModel colorModel = new ComponentColorModel(
                colorSpace, hasAlpha, isAlphaPremultiplied, transparency,
                dataType);
        final WritableRaster raster =
                colorModel.createCompatibleWritableRaster(width, height);
        return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
    }

    private static BufferedImage newGrayImage(int componentSize,
                                              boolean hasAlpha) {
        return newGrayImage(20, 20, componentSize, hasAlpha);
    }

    private static BufferedImage newGrayImage(int width,
                                              int height,
                                              int componentSize,
                                              boolean hasAlpha) {
        if (!hasAlpha) {
            int type = (componentSize > 8) ?
                    BufferedImage.TYPE_USHORT_GRAY : BufferedImage.TYPE_BYTE_GRAY;
            return new BufferedImage(width, height, type);
        }
        final ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        final int[] componentSizes = new int[] { componentSize, componentSize };
        final boolean isAlphaPremultiplied = false;
        final int transparency = Transparency.TRANSLUCENT;
        final int dataType = (componentSize > 8) ?
                DataBuffer.TYPE_USHORT : DataBuffer.TYPE_BYTE;
        final ColorModel colorModel = new ComponentColorModel(
                colorSpace, componentSizes, hasAlpha, isAlphaPremultiplied,
                transparency, dataType);
        final WritableRaster raster =
                colorModel.createCompatibleWritableRaster(width, height);
        return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
    }

    /* applyRedactions() */

    @Test
    void applyRedactions() {
        final Size fullSize = new Size(64, 56);
        final BufferedImage image = newColorImage(
                fullSize.intWidth(), fullSize.intHeight(), 8, false);

        // fill it with white
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE.toAWTColor());
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.dispose();

        // create some Redactions
        final Set<Redaction> redactions = Set.of(
                new Redaction(new Region(0, 0, 20, 20), Color.RED),
                new Redaction(new Region(20, 20, 20, 20), Color.BLACK));
        final Crop crop = new CropByPixels(
                0, 0, image.getWidth(), image.getTileHeight());

        // apply them
        Java2DUtils.applyRedactions(
                image,
                fullSize,
                crop,
                new double[] { 1.0, 1.0 },
                new ReductionFactor(0),
                new ScaleConstraint(1, 1),
                redactions);

        // test for the first one
        assertRGBA(image.getRGB(0, 0), 255, 0, 0, 255);

        // test for the second one
        assertRGBA(image.getRGB(25, 25), 0, 0, 0, 255);
    }

    /* applyOverlay() */

    @Test
    void applyOverlayWithImageOverlay() {
        final BufferedImage baseImage = newColorImage(8, false);

        // fill it with white
        Graphics2D g2d = baseImage.createGraphics();
        g2d.setColor(Color.WHITE.toAWTColor());
        g2d.fillRect(0, 0, baseImage.getWidth(), baseImage.getHeight());
        g2d.dispose();

        // create an Overlay
        final ImageOverlay overlay = new ImageOverlay(
                TestUtils.getSampleImage("png/rgb-1x1x8.png").toUri(),
                Position.TOP_LEFT, 0);

        // apply it
        Java2DUtils.applyOverlay(baseImage, overlay);

        assertRGBA(baseImage.getRGB(0, 0), 0, 0, 0, 255);
    }

    @Test
    void applyOverlayWithImageOverlayAndInset() {
        final BufferedImage baseImage = newColorImage(8, false);

        // fill it with white
        Graphics2D g2d = baseImage.createGraphics();
        g2d.setColor(Color.WHITE.toAWTColor());
        g2d.fillRect(0, 0, baseImage.getWidth(), baseImage.getHeight());
        g2d.dispose();

        // create a Overlay
        final int inset = 2;
        final ImageOverlay overlay = new ImageOverlay(
                TestUtils.getSampleImage("png/rgb-1x1x8.png").toUri(),
                Position.BOTTOM_RIGHT,
                inset);

        // apply it
        Java2DUtils.applyOverlay(baseImage, overlay);

        int pixel = baseImage.getRGB(
                baseImage.getWidth() - inset - 1,
                baseImage.getHeight() - inset - 1);
        assertRGBA(pixel, 0, 0, 0, 255);
    }

    @Test
    void applyOverlayWithMissingImageOverlay() throws Exception {
        final BufferedImage baseImage = newColorImage(8, false);

        // fill it with white
        Graphics2D g2d = baseImage.createGraphics();
        g2d.setColor(Color.WHITE.toAWTColor());
        g2d.fillRect(0, 0, baseImage.getWidth(), baseImage.getHeight());
        g2d.dispose();

        // create an Overlay
        final ImageOverlay overlay = new ImageOverlay(
                new URI("file:///bla/bla/bogus"),
                Position.TOP_LEFT, 0);

        // apply it
        Java2DUtils.applyOverlay(baseImage, overlay);

        // assert that it wasn't applied
        assertRGBA(baseImage.getRGB(0, 0), 255, 255, 255, 255);
    }

    @Test
    void applyOverlayWithImageOverlayAndScaled() {
        final BufferedImage baseImage = newColorImage(8, false);

        // fill it with white
        Graphics2D g2d = baseImage.createGraphics();
        g2d.setColor(Color.WHITE.toAWTColor());
        g2d.fillRect(0, 0, baseImage.getWidth(), baseImage.getHeight());
        g2d.dispose();

        // create a Overlay
        final ImageOverlay overlay = new ImageOverlay(
                TestUtils.getSampleImage("png/rgb-64x56x8.png").toUri(),
                Position.SCALED,
                0);

        // apply it
        Java2DUtils.applyOverlay(baseImage, overlay);

        assertRGBA(baseImage.getRGB(0, 0), 255, 255, 255, 255);
        assertNotRGBA(baseImage.getRGB(0, 50), 255, 255, 255, 255);
    }

    @Test
    void applyOverlayWithImageOverlayAndScaledWithInset() {
        final BufferedImage baseImage = newColorImage(8, false);

        // fill it with white
        Graphics2D g2d = baseImage.createGraphics();
        g2d.setColor(Color.WHITE.toAWTColor());
        g2d.fillRect(0, 0, baseImage.getWidth(), baseImage.getHeight());
        g2d.dispose();

        // create a Overlay
        final ImageOverlay overlay = new ImageOverlay(
                TestUtils.getSampleImage("png/rgb-64x56x8.png").toUri(),
                Position.SCALED,
                2);

        // apply it
        Java2DUtils.applyOverlay(baseImage, overlay);

        assertRGBA(baseImage.getRGB(0, 0), 255, 255, 255, 255);
        assertNotRGBA(baseImage.getRGB(50, 50), 255, 255, 255, 255);
    }

    @Test
    void applyOverlayWithStringOverlay() {
        final BufferedImage baseImage = newColorImage(8, false);

        // create a StringOverlay
        final StringOverlay overlay = new StringOverlay(
                "H", Position.TOP_LEFT, 0,
                new Font("SansSerif", Font.BOLD, 80), 11,
                Color.WHITE, Color.BLACK, Color.WHITE, 0f, false);

        // apply it
        Java2DUtils.applyOverlay(baseImage, overlay);

        // Test the background color
        assertRGBA(baseImage.getRGB(2, 2), 0, 0, 0, 255);

        // Test the font color
        int pixel = baseImage.getRGB(12, 50);
        assertRGBA(pixel, 255, 255, 255, 255);
    }

    /* convertColorToLinearRGB(BufferedImage) */

    @Test
    void convertColorToLinearRGB() {
        BufferedImage inImage, outImage;

        // Non-linear image
        inImage  = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        outImage = Java2DUtils.convertColorToLinearRGB(inImage);
        assertEquals(ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB),
                outImage.getColorModel().getColorSpace());

        // Linear image
        ColorSpace linearCS =
                ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
        ComponentColorModel cm = new ComponentColorModel(
                linearCS, false, false,
                Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        WritableRaster raster = cm.createCompatibleWritableRaster(
                inImage.getWidth(), inImage.getHeight());
        inImage = new BufferedImage(
                cm, raster, cm.isAlphaPremultiplied(), null);
        outImage = Java2DUtils.convertColorToLinearRGB(inImage);
        assertSame(inImage, outImage);
    }

    /* convertIndexedToARGB() */

    @Test
    void convertIndexedToARGBWithIndexedImage() {
        BufferedImage inImage  = new BufferedImage(
                10, 10, BufferedImage.TYPE_BYTE_INDEXED);
        BufferedImage outImage = Java2DUtils.convertIndexedToARGB(inImage);
        assertEquals(BufferedImage.TYPE_4BYTE_ABGR, outImage.getType());
    }

    @Test
    void convertIndexedToARGBWithNonIndexedImage() {
        BufferedImage inImage  =
                new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        BufferedImage outImage = Java2DUtils.convertIndexedToARGB(inImage);
        assertSame(inImage, outImage);
    }

    /* crop(BufferedImage, Crop, ReductionFactor, ScaleConstraint) */

    @Test
    void cropWithCropToSquare() {
        Crop crop = new CropToSquare();
        final int width = 200, height = 100;
        BufferedImage inImage = newColorImage(width, height, 8, false);
        BufferedImage outImage;

        // no reduction factor or scale constraint
        ReductionFactor rf = new ReductionFactor();
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        outImage = Java2DUtils.crop(inImage, crop, rf, sc);
        assertEquals(height, outImage.getWidth());
        assertEquals(height, outImage.getHeight());

        // reduction factor 1
        rf = new ReductionFactor();
        sc = new ScaleConstraint(1, 1);
        outImage = Java2DUtils.crop(inImage, crop, rf, sc);
        assertEquals(height, outImage.getWidth());
        assertEquals(height, outImage.getHeight());
    }

    @Test
    void cropWithCropByPixels() {
        CropByPixels crop = new CropByPixels(0, 0, 50, 50);
        final int width = 200, height = 100;
        BufferedImage inImage = newColorImage(width, height, 8, false);
        BufferedImage outImage;

        // no reduction factor or scale constraint
        ReductionFactor rf = new ReductionFactor();
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        outImage = Java2DUtils.crop(inImage, crop, rf, sc);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // reduction factor 1
        rf = new ReductionFactor(1);
        sc = new ScaleConstraint(1, 1);
        outImage = Java2DUtils.crop(inImage, crop, rf, sc);
        assertEquals(25, outImage.getWidth());
        assertEquals(25, outImage.getHeight());
    }

    @Test
    void cropWithCropByPercent() {
        CropByPercent crop = new CropByPercent(0.5, 0.5, 0.5, 0.5);
        final int width = 200, height = 100;
        BufferedImage inImage = newColorImage(width, height, 8, false);
        BufferedImage outImage;

        // no reduction factor or scale constraint
        ReductionFactor rf = new ReductionFactor();
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        outImage = Java2DUtils.crop(inImage, crop, rf, sc);
        assertEquals(width * crop.getWidth(), outImage.getWidth(), DELTA);
        assertEquals(height * crop.getHeight(), outImage.getHeight(), DELTA);

        // reduction factor 1
        rf = new ReductionFactor(1);
        sc = new ScaleConstraint(1, 1);
        outImage = Java2DUtils.crop(inImage, crop, rf, sc);
        assertEquals(width * crop.getWidth(), outImage.getWidth(), DELTA);
        assertEquals(height * crop.getHeight(), outImage.getHeight(), DELTA);
    }

    /* getOverlayImage() */

    @Test
    void getOverlayImage() {
        ImageOverlay overlay = new ImageOverlay(
                TestUtils.getSampleImage("png/png.png").toUri(), Position.BOTTOM_RIGHT, 0);
        assertNotNull(Java2DUtils.getOverlayImage(overlay));
    }

    /* newImage() */

    @Test
    void newImageWithBGR() {
        int width  = 300;
        int height = 200;
        BufferedImage testImage = Java2DUtils.newTestPatternImage(
                width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] samples = ((DataBufferByte) testImage.getRaster().getDataBuffer()).getData();

        BufferedImage image = Java2DUtils.newImage(
                width, height, samples, ComponentOrder.BGR);
        assertRGBA(image.getRGB(50, 100), 255, 0, 0, 255);
    }

    @Test
    void newImageWithABGR() {
        int width  = 300;
        int height = 200;
        BufferedImage testImage = Java2DUtils.newTestPatternImage(
                width, height, BufferedImage.TYPE_4BYTE_ABGR);
        byte[] samples = ((DataBufferByte) testImage.getRaster().getDataBuffer()).getData();

        BufferedImage image = Java2DUtils.newImage(
                width, height, samples, ComponentOrder.ABGR);
        assertRGBA(image.getRGB(40, 100), 255, 0, 0, 255);
    }

    @Test
    void newImageWithRGB() {
        int width  = 300;
        int height = 200;
        byte[] samples = new byte[width * height * 3];
        Arrays.fill(samples, (byte) 0xff);

        BufferedImage image = Java2DUtils.newImage(
                width, height, samples, ComponentOrder.RGB);
        assertRGBA(image.getRGB(50, 100), 255, 255, 255, 255);
    }

    @Test
    void newImageWithARGB() {
        int width  = 300;
        int height = 200;
        byte[] samples = new byte[width * height * 4];
        Arrays.fill(samples, (byte) 0xff);

        BufferedImage image = Java2DUtils.newImage(
                width, height, samples, ComponentOrder.ARGB);
        assertRGBA(image.getRGB(50, 100), 255, 255, 255, 255);
    }

    /* newTestPatternImage() */

    @Test
    void newTestPatternImageWithRGB() {
        int type            = BufferedImage.TYPE_3BYTE_BGR;
        BufferedImage image = Java2DUtils.newTestPatternImage(300, 200, type);
        assertEquals(type, image.getType());
        assertEquals(300, image.getWidth());
        assertEquals(200, image.getHeight());
        assertRGBA(image.getRGB(50, 100), 255, 0, 0, 255);
        assertRGBA(image.getRGB(150, 100), 0, 255, 0, 255);
        assertRGBA(image.getRGB(250, 100), 0, 0, 255, 255);
    }

    @Test
    void newTestPatternImageWithRGBA() {
        int type            = BufferedImage.TYPE_4BYTE_ABGR;
        BufferedImage image = Java2DUtils.newTestPatternImage(300, 200, type);
        assertEquals(type, image.getType());
        assertEquals(300, image.getWidth());
        assertEquals(200, image.getHeight());
        assertRGBA(image.getRGB(40, 100), 255, 0, 0, 255);
        assertRGBA(image.getRGB(110, 100), 0, 255, 0, 255);
        assertRGBA(image.getRGB(190, 100), 0, 0, 255, 255);
        assertRGBA(image.getRGB(260, 100), 0, 0, 0, 0);
    }

    /* reduceTo8Bits() */

    @Test
    void reduceTo8BitsWith8BitGray() {
        BufferedImage image = newGrayImage(8, false);
        BufferedImage result = Java2DUtils.reduceTo8Bits(image);
        assertSame(image, result);
    }

    @Test
    void reduceTo8BitsWith8BitRGBA() {
        BufferedImage image = newColorImage(8, true);
        BufferedImage result = Java2DUtils.reduceTo8Bits(image);
        assertSame(image, result);
    }

    @Test
    void reduceTo8BitsWith16BitGray() {
        BufferedImage image = newGrayImage(16, false);
        BufferedImage result = Java2DUtils.reduceTo8Bits(image);
        assertEquals(8, result.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, result.getType());
    }

    @Test
    void reduceTo8BitsWith16BitRGBA() {
        BufferedImage image = newColorImage(16, true);
        BufferedImage result = Java2DUtils.reduceTo8Bits(image);
        assertEquals(8, result.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_4BYTE_ABGR, result.getType());
    }

    /* removeAlpha(BufferedImage, Color) */

    @Test
    void removeAlphaOn8BitGrayImage() {
        BufferedImage inImage = newGrayImage(8, false);
        assertFalse(inImage.getColorModel().hasAlpha());

        BufferedImage outImage = Java2DUtils.removeAlpha(inImage, Color.WHITE);
        assertSame(inImage, outImage);
    }

    @Test
    void removeAlphaOn8BitGrayImageWithAlpha() {
        BufferedImage inImage = newGrayImage(8, true);
        assertTrue(inImage.getColorModel().hasAlpha());

        BufferedImage outImage = Java2DUtils.removeAlpha(inImage, Color.WHITE);
        assertFalse(outImage.getColorModel().hasAlpha());
    }

    @Test
    void removeAlphaOn8BitRGBImage() {
        BufferedImage inImage = newColorImage(8, false);
        assertFalse(inImage.getColorModel().hasAlpha());

        BufferedImage outImage = Java2DUtils.removeAlpha(inImage, Color.WHITE);
        assertSame(inImage, outImage);
    }

    @Test
    void removeAlphaOn8BitRGBAImage() {
        BufferedImage inImage = newColorImage(16, true);
        assertTrue(inImage.getColorModel().hasAlpha());

        BufferedImage outImage = Java2DUtils.removeAlpha(inImage, Color.WHITE);
        assertFalse(outImage.getColorModel().hasAlpha());
    }

    @Test
    void removeAlphaOn16BitGrayImage() {
        BufferedImage inImage = newGrayImage(16, false);
        assertFalse(inImage.getColorModel().hasAlpha());

        BufferedImage outImage = Java2DUtils.removeAlpha(inImage, Color.WHITE);
        assertSame(inImage, outImage);
    }

    @Test
    void removeAlphaOn16BitGrayImageWithAlpha() {
        BufferedImage inImage = newGrayImage(16, true);
        assertTrue(inImage.getColorModel().hasAlpha());

        BufferedImage outImage = Java2DUtils.removeAlpha(inImage, Color.WHITE);
        assertFalse(outImage.getColorModel().hasAlpha());
    }

    @Test
    void removeAlphaOn16BitRGBImage() {
        BufferedImage inImage = newColorImage(16, false);
        assertFalse(inImage.getColorModel().hasAlpha());

        BufferedImage outImage = Java2DUtils.removeAlpha(inImage, Color.WHITE);
        assertSame(inImage, outImage);
    }

    @Test
    void removeAlphaOn16BitRGBAImage() {
        BufferedImage inImage = newColorImage(16, true);
        assertTrue(inImage.getColorModel().hasAlpha());

        BufferedImage outImage = Java2DUtils.removeAlpha(inImage, Color.WHITE);
        assertFalse(outImage.getColorModel().hasAlpha());
    }

    @Test
    void removeAlphaPaintsBackgroundColor() throws IOException {
        Path file = TestUtils.getSampleImage("png/rgba-64x56x8.png");
        BufferedImage inImage = ImageIO.read(file.toFile());
        assertTrue(inImage.getColorModel().hasAlpha());

        int[] rgba = { 0 };
        inImage.getAlphaRaster().setPixel(0, 0, rgba);

        BufferedImage outImage = Java2DUtils.removeAlpha(inImage, Color.RED);

        int[] expected = new int[] {255, 0, 0, 0};
        int[] actual = new int[4];
        assertArrayEquals(expected, outImage.getRaster().getPixel(0, 0, actual));
    }

    /* rotate(BufferedImage, Orientation) */

    @Test
    void rotate1() {
        BufferedImage inImage = newColorImage(8, false);
        BufferedImage outImage = Java2DUtils.rotate(inImage, Orientation.ROTATE_0);
        assertSame(inImage, outImage);

        outImage = Java2DUtils.rotate(inImage, Orientation.ROTATE_90);
        assertEquals(inImage.getHeight(), outImage.getWidth());
        assertEquals(inImage.getWidth(), outImage.getHeight());

        outImage = Java2DUtils.rotate(inImage, Orientation.ROTATE_180);
        assertEquals(inImage.getWidth(), outImage.getWidth());
        assertEquals(inImage.getHeight(), outImage.getHeight());

        outImage = Java2DUtils.rotate(inImage, Orientation.ROTATE_270);
        assertEquals(inImage.getHeight(), outImage.getWidth());
        assertEquals(inImage.getWidth(), outImage.getHeight());
    }

    /* rotate(BufferedImage, Rotate) */

    @Test
    void rotate2Dimensions() {
        BufferedImage inImage = newColorImage(8, false);
        final int sourceWidth = inImage.getWidth();
        final int sourceHeight = inImage.getHeight();

        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtils.rotate(inImage, rotate);

        final double radians = Math.toRadians(rotate.getDegrees());
        final int expectedWidth = (int) Math.round(Math.abs(sourceWidth *
                Math.cos(radians)) + Math.abs(sourceHeight *
                Math.sin(radians)));
        final int expectedHeight = (int) Math.round(Math.abs(sourceHeight *
                Math.cos(radians)) + Math.abs(sourceWidth *
                Math.sin(radians)));

        assertEquals(expectedWidth, outImage.getWidth());
        assertEquals(expectedHeight, outImage.getHeight());
    }

    @Test
    void rotate2With8BitGray() {
        BufferedImage inImage = newGrayImage(8, false);
        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtils.rotate(inImage, rotate);

        assertTrue(outImage.getColorModel().hasAlpha());
        assertEquals(8, outImage.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_INT_ARGB, outImage.getType());
    }

    @Test
    void rotate2With8BitGrayWithAlpha() {
        BufferedImage inImage = newGrayImage(8, true);
        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtils.rotate(inImage, rotate);

        assertTrue(outImage.getColorModel().hasAlpha());
        assertEquals(8, outImage.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_INT_ARGB, outImage.getType());
    }

    @Test
    void rotate2With8BitRGB() {
        BufferedImage inImage = newColorImage(8, false);
        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtils.rotate(inImage, rotate);

        assertTrue(outImage.getColorModel().hasAlpha());
        assertEquals(8, outImage.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_INT_ARGB, outImage.getType());
    }

    @Test
    void rotate2With8BitRGBA() {
        BufferedImage inImage = newColorImage(8, true);
        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtils.rotate(inImage, rotate);

        assertTrue(outImage.getColorModel().hasAlpha());
        assertEquals(8, outImage.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_INT_ARGB, outImage.getType());
    }

    @Test
    void rotate2With16BitGray() {
        BufferedImage inImage = newGrayImage(16, false);
        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtils.rotate(inImage, rotate);

        assertTrue(outImage.getColorModel().hasAlpha());
        assertEquals(16, outImage.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_CUSTOM, outImage.getType());
    }

    @Test
    void rotate2With16BitGrayWithAlpha() {
        BufferedImage inImage = newGrayImage(16, true);
        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtils.rotate(inImage, rotate);

        assertTrue(outImage.getColorModel().hasAlpha());
        assertEquals(16, outImage.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_CUSTOM, outImage.getType());
    }

    @Test
    void rotate2With16BitRGB() {
        BufferedImage inImage = newColorImage(16, false);
        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtils.rotate(inImage, rotate);

        assertTrue(outImage.getColorModel().hasAlpha());
        assertEquals(16, outImage.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_CUSTOM, outImage.getType());
    }

    @Test
    void rotate2With16BitRGBA() {
        BufferedImage inImage = newColorImage(16, true);
        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtils.rotate(inImage, rotate);

        assertTrue(outImage.getColorModel().hasAlpha());
        assertEquals(16, outImage.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_CUSTOM, outImage.getType());
    }

    /* scale */

    @Test
    void scaleWithWithAspectFitWidth() {
        BufferedImage inImage = newColorImage(100, 100, 8, false);

        ScaleByPixels scale = new ScaleByPixels(
                50, null, ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        ReductionFactor rf = new ReductionFactor(1);

        BufferedImage outImage = Java2DUtils.scale(inImage, scale, sc, rf, true);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());
    }

    @Test
    void scaleWithAspectFitHeight() {
        BufferedImage inImage = newColorImage(100, 100, 8, false);

        ScaleByPixels scale = new ScaleByPixels(
                null, 50, ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        ReductionFactor rf = new ReductionFactor(1);

        BufferedImage outImage = Java2DUtils.scale(inImage, scale, sc, rf, true);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());
    }

    @Test
    void scaleWithAspectFitInside() {
        BufferedImage inImage = newColorImage(100, 100, 8, false);

        ScaleByPixels scale = new ScaleByPixels(
                50, 50, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        ReductionFactor rf = new ReductionFactor(1);

        BufferedImage outImage = Java2DUtils.scale(inImage, scale, sc, rf, true);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());
    }

    @Test
    void scaleWithNonAspectFill() {
        BufferedImage inImage = newColorImage(100, 100, 8, false);

        ScaleByPixels scale = new ScaleByPixels();
        scale.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        scale.setWidth(80);
        scale.setHeight(50);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        ReductionFactor rf = new ReductionFactor(1);

        BufferedImage outImage = Java2DUtils.scale(inImage, scale, sc, rf, true);
        assertEquals(80, outImage.getWidth());
        assertEquals(50, outImage.getHeight());
    }

    @Test
    void scaleWithWithScaleByPercent() {
        BufferedImage inImage = newColorImage(100, 100, 8, false);

        ScaleByPercent scale = new ScaleByPercent(0.25);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        ReductionFactor rf = new ReductionFactor(2);

        BufferedImage outImage = Java2DUtils.scale(inImage, scale, sc, rf, true);
        assertEquals(100, outImage.getWidth());
        assertEquals(100, outImage.getHeight());
    }

    @Test
    void scaleWithSub3PixelSourceDimension() {
        BufferedImage inImage = newColorImage(2, 1, 8, false);

        ScaleByPixels scale = new ScaleByPixels(
                200, 1000, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        ReductionFactor rf = new ReductionFactor(2);

        BufferedImage outImage = Java2DUtils.scale(inImage, scale, sc, rf, true);
        assertEquals(200, outImage.getWidth());
        assertEquals(100, outImage.getHeight());
    }

    @Test
    void scaleWithSub3PixelTargetDimension() {
        BufferedImage inImage = newColorImage(100, 100, 8, false);

        ScaleByPixels scale = new ScaleByPixels(
                2, 1, ScaleByPixels.Mode.NON_ASPECT_FILL);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        ReductionFactor rf = new ReductionFactor(1);

        BufferedImage outImage = Java2DUtils.scale(inImage, scale, sc, rf, true);
        assertEquals(2, outImage.getWidth());
        assertEquals(1, outImage.getHeight());
    }

    /* sharpen() */

    @Test
    void sharpen() {
        BufferedImage inImage = newColorImage(20, 20, 8, false);
        Sharpen sharpen = new Sharpen(0.1f);
        BufferedImage outImage = Java2DUtils.sharpen(inImage, sharpen);

        assertEquals(20, outImage.getWidth());
        assertEquals(20, outImage.getHeight());
    }

    /* transformColor() */

    @Test
    void transformColorFrom8BitRGBToBitonal() {
        BufferedImage inImage = newColorImage(100, 100, 8, false);
        BufferedImage outImage;

        // Create a cyan image.
        Graphics2D g2d = inImage.createGraphics();
        g2d.setColor(java.awt.Color.CYAN);
        g2d.fill(new Region(0, 0, 100, 100).toAWTRectangle());
        g2d.dispose();

        // Transform to bitonal.
        outImage = Java2DUtils.transformColor(inImage, ColorTransform.BITONAL);

        // Expect it to be transformed to black.
        assertRGBA(outImage.getRGB(0, 0), 0, 0, 0, 255);

        // Create a red image.
        g2d = inImage.createGraphics();
        g2d.setColor(java.awt.Color.RED);
        g2d.fill(new Region(0, 0, 100, 100).toAWTRectangle());
        g2d.dispose();

        // Transform to bitonal.
        outImage = Java2DUtils.transformColor(inImage, ColorTransform.BITONAL);

        // Expect it to be transformed to white.
        assertRGBA(outImage.getRGB(0, 0), 255, 255, 255, 255);
    }

    @Test
    void transformColorFrom16BitRGBAToBitonal() {
        BufferedImage inImage = newColorImage(16, true);

        // Create a cyan image.
        Graphics2D g2d = inImage.createGraphics();
        g2d.setColor(java.awt.Color.CYAN);
        g2d.fill(new Region(0, 0, 100, 100).toAWTRectangle());
        g2d.dispose();

        // Transform to bitonal.
        BufferedImage outImage = Java2DUtils.transformColor(inImage,
                ColorTransform.BITONAL);

        // Expect it to be transformed to black.
        assertRGBA(outImage.getRGB(0, 0), 0, 0, 0, 255);

        // Create a red image.
        g2d = inImage.createGraphics();
        g2d.setColor(java.awt.Color.RED);
        g2d.fill(new Region(0, 0, 100, 100).toAWTRectangle());
        g2d.dispose();

        // Transform to bitonal.
        outImage = Java2DUtils.transformColor(inImage, ColorTransform.BITONAL);

        // Expect it to be transformed to white.
        assertRGBA(outImage.getRGB(0, 0), 255, 255, 255, 255);
    }

    @Test
    void transformColorFrom8BitRGBToGray() {
        BufferedImage inImage = newColorImage(100, 100, 8, false);

        // Start with a red image.
        Graphics2D g2d = inImage.createGraphics();
        g2d.setColor(java.awt.Color.RED);
        g2d.fill(new Region(0, 0, 100, 100).toAWTRectangle());
        g2d.dispose();

        // Transform to grayscale.
        BufferedImage outImage = Java2DUtils.transformColor(inImage,
                ColorTransform.GRAY);

        assertGray(outImage.getRGB(0, 0));
        assertEquals(BufferedImage.TYPE_3BYTE_BGR, outImage.getType());
    }

    @Test
    void transformColorFrom16BitRGBAToGray() {
        BufferedImage inImage = newColorImage(100, 100, 16, true);

        // Start with a red image.
        Graphics2D g2d = inImage.createGraphics();
        g2d.setColor(java.awt.Color.RED);
        g2d.fill(new Region(0, 0, 100, 100).toAWTRectangle());
        g2d.dispose();

        // Transform to grayscale.
        BufferedImage outImage = Java2DUtils.transformColor(inImage,
                ColorTransform.GRAY);

        assertGray(outImage.getRGB(0, 0));
        assertEquals(16, outImage.getColorModel().getComponentSize(0));
    }

    @Test
    void transformColorFromBitonalToBitonal() {
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_BYTE_BINARY);
        BufferedImage outImage = Java2DUtils.transformColor(inImage,
                ColorTransform.BITONAL);
        assertSame(inImage, outImage);
    }

    @Test
    void transformColorFromGrayToGray() {
        BufferedImage inImage = newGrayImage(100, 100, 8, false);
        BufferedImage outImage = Java2DUtils.transformColor(inImage,
                ColorTransform.GRAY);
        assertSame(inImage, outImage);
    }

    @Test
    void transformColorFromGrayAlphaToGray() {
        BufferedImage inImage = newGrayImage(100, 100, 8, true);
        BufferedImage outImage = Java2DUtils.transformColor(inImage,
                ColorTransform.GRAY);
        assertSame(inImage, outImage);
    }

    /* transpose() */

    @Test
    void transposeImage() {
        BufferedImage inImage = newColorImage(200, 100, 8, false);
        Transpose transpose = Transpose.HORIZONTAL;
        BufferedImage outImage = Java2DUtils.transpose(inImage, transpose);

        assertEquals(200, outImage.getWidth());
        assertEquals(100, outImage.getHeight());
    }

}
