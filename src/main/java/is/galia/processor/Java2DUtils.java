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

import is.galia.codec.Decoder;
import is.galia.image.Size;
import is.galia.image.Orientation;
import is.galia.image.Region;
import is.galia.image.ScaleConstraint;
import is.galia.operation.Color;
import is.galia.operation.ColorTransform;
import is.galia.operation.Crop;
import is.galia.image.ComponentOrder;
import is.galia.image.Format;
import is.galia.operation.Operation;
import is.galia.operation.OperationList;
import is.galia.image.ReductionFactor;
import is.galia.operation.ScaleByPixels;
import is.galia.operation.Sharpen;
import is.galia.operation.redaction.Redaction;
import is.galia.operation.overlay.ImageOverlay;
import is.galia.operation.overlay.Position;
import is.galia.operation.Rotate;
import is.galia.operation.Scale;
import is.galia.operation.Transpose;
import is.galia.operation.overlay.StringOverlay;
import is.galia.operation.overlay.Overlay;
import is.galia.codec.DecoderFactory;
import is.galia.processor.resample.ResampleFilter;
import is.galia.processor.resample.ResampleOp;
import is.galia.util.Stopwatch;
import is.galia.util.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.util.Arrays;
import java.util.Set;

/**
 * <p>Collection of methods for operating on {@link BufferedImage}s.</p>
 */
public final class Java2DUtils {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Java2DUtils.class);

    /**
     * This file exists in the resource bundle.
     */
    private static final String CMYK_ICC_PROFILE = "eciCMYK.icc";

    /**
     * See the inline documentation in {@link #scale} for a rationale for
     * choosing this.
     */
    private static final Scale.Filter DEFAULT_DOWNSCALE_FILTER =
            Scale.Filter.BOX;

    /**
     * See the inline documentation in {@link #scale} for a rationale for
     * choosing this.
     */
    private static final Scale.Filter DEFAULT_UPSCALE_FILTER =
            Scale.Filter.BICUBIC;

    /**
     * Redacts regions from the given image.
     *
     * @param image           Image to redact.
     * @param fullSize        Full pre-crop image size.
     * @param appliedCrop     Crop already applied to {@literal image}.
     * @param preScale        Two-element array of scale factors already
     *                        applied to {@literal image}.
     * @param reductionFactor Reduction factor already applied to
     *                        {@literal image}.
     * @param scaleConstraint Scale constraint.
     * @param redactions      Regions of the image to redact.
     */
    static void applyRedactions(final BufferedImage image,
                                final Size fullSize,
                                final Crop appliedCrop,
                                final double[] preScale,
                                final ReductionFactor reductionFactor,
                                final ScaleConstraint scaleConstraint,
                                final Set<Redaction> redactions) {
        if (image != null && !redactions.isEmpty()) {
            final Stopwatch watch = new Stopwatch();

            final Graphics2D g2d = image.createGraphics();
            // High quality is not needed for drawing simple rectangles.
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);

            for (final Redaction redaction : redactions) {
                Region region = redaction.getResultingRegion(
                        fullSize, scaleConstraint, appliedCrop);
                final double rfScale = reductionFactor.getScale();
                region = region.scaled(rfScale * preScale[0],
                        rfScale * preScale[1]);

                if (!region.isEmpty()) {
                    LOGGER.debug("applyRedactions(): applying {} at {},{}/{}x{}",
                            redaction,
                            region.longX(),
                            region.longY(),
                            region.longWidth(),
                            region.longHeight());
                    g2d.setColor(redaction.getColor().toAWTColor());
                    g2d.fill(region.toAWTRectangle());
                } else {
                    LOGGER.debug("applyRedactions(): {} is outside visible region; skipping",
                            redaction);
                }
            }
            g2d.dispose();
            LOGGER.trace("applyRedactions() executed in {}", watch);
        }
    }

    /**
     * Applies the given overlay to the given image. The overlay may be a
     * {@link StringOverlay string} or an {@link ImageOverlay image}.
     *
     * @param baseImage Image to apply the overlay on top of.
     * @param overlay   Overlay to apply to the base image.
     */
    static void applyOverlay(final BufferedImage baseImage,
                             final Overlay overlay) {
        if (overlay instanceof ImageOverlay) {
            BufferedImage overlayImage = getOverlayImage((ImageOverlay) overlay);
            if (overlayImage != null) {
                overlayImage(baseImage, overlayImage,
                        overlay.getPosition(), overlay.getInset());
            }
        } else if (overlay instanceof StringOverlay) {
            overlayString(baseImage, (StringOverlay) overlay);
        }
    }

    /**
     * @param inImage Image to convert.
     * @return New image of type {@link BufferedImage#TYPE_4BYTE_ABGR}, or the
     *         input image if it is not indexed.
     */
    public static BufferedImage convertIndexedToARGB(BufferedImage inImage) {
        BufferedImage outImage = inImage;
        if (inImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
            final Stopwatch watch = new Stopwatch();
            outImage = new BufferedImage(
                    inImage.getWidth(), inImage.getHeight(),
                    BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g2d = outImage.createGraphics();
            g2d.drawImage(inImage, 0, 0, null);
            g2d.dispose();
            LOGGER.trace("convertIndexedToARGB(): executed in {}", watch);
        }
        return outImage;
    }

    /**
     * @param image CMYK image.
     * @return New RGB instance.
     */
    public static BufferedImage convertCMYKToRGB(BufferedImage image) throws IOException {
        return convertCMYKToRGB(image.getRaster(), defaultCMYKProfile());
    }

    /**
     * @param cmykRaster  CMYK image raster.
     * @param cmykProfile If {@code null}, a {@link #defaultCMYKProfile()
     *                    default CMYK profile} will be used.
     */
    public static BufferedImage convertCMYKToRGB(Raster cmykRaster,
                                                 ICC_Profile cmykProfile) throws IOException {
        final Stopwatch watch = new Stopwatch();
        if (cmykProfile == null) {
            cmykProfile = defaultCMYKProfile();
        }

        if (cmykProfile.getProfileClass() != ICC_Profile.CLASS_DISPLAY) {
            byte[] profileData = cmykProfile.getData();

            if (profileData[ICC_Profile.icHdrRenderingIntent] == ICC_Profile.icPerceptual) {
                intToBigEndian(
                        ICC_Profile.icSigDisplayClass,
                        profileData,
                        ICC_Profile.icHdrDeviceClass); // Header is first
                cmykProfile = ICC_Profile.getInstance(profileData);
            }
        }

        ICC_ColorSpace cmykCS = new ICC_ColorSpace(cmykProfile);
        BufferedImage rgbImage = new BufferedImage(
                cmykRaster.getWidth(),
                cmykRaster.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR);
        WritableRaster rgbRaster = rgbImage.getRaster();
        ColorSpace rgbCS = rgbImage.getColorModel().getColorSpace();
        ColorConvertOp op = new ColorConvertOp(cmykCS, rgbCS, null);
        op.filter(cmykRaster, rgbRaster);
        LOGGER.trace("convertCMYKToRGB(): executed in {}", watch);
        return rgbImage;
    }

    /**
     * @return ICC profile to use for CMYK images that do not contain an
     *         embedded profile.
     */
    private static ICC_Profile defaultCMYKProfile() throws IOException {
        try (InputStream is = new BufferedInputStream(
                Java2DUtils.class.getResourceAsStream("/" + CMYK_ICC_PROFILE))) {
            return ICC_Profile.getInstance(is);
        }
    }

    public static void convertYCCKToCMYK(WritableRaster raster) {
        final Stopwatch watch = new Stopwatch();
        final int height      = raster.getHeight();
        final int width       = raster.getWidth();
        final int stride      = width * 4;
        final int[] pixelRow  = new int[stride];

        for (int h = 0; h < height; h++) {
            raster.getPixels(0, h, width, 1, pixelRow);

            for (int x = 0; x < stride; x += 4) {
                int y  = pixelRow[x];
                int cb = pixelRow[x + 1];
                int cr = pixelRow[x + 2];

                int c = (int) (y + 1.402 * cr - 178.956);
                int m = (int) (y - 0.34414 * cb - 0.71414 * cr + 135.95984);
                y     = (int) (y + 1.772 * cb - 226.316);

                if (c < 0) c = 0; else if (c > 255) c = 255;
                if (m < 0) m = 0; else if (m > 255) m = 255;
                if (y < 0) y = 0; else if (y > 255) y = 255;

                pixelRow[x]     = 255 - c;
                pixelRow[x + 1] = 255 - m;
                pixelRow[x + 2] = 255 - y;
            }
            raster.setPixels(0, h, width, 1, pixelRow);
        }
        LOGGER.trace("convertYCCKToCMYK(): executed in {}", watch);
    }

    private static void intToBigEndian(int value, byte[] array, int index) {
        array[index]     = (byte) (value >> 24);
        array[index + 1] = (byte) (value >> 16);
        array[index + 2] = (byte) (value >> 8);
        array[index + 3] = (byte) (value);
    }

    /**
     * @param inImage Image to convert.
     * @return        New image with a linear RGB color space, or the input
     *                image if it is already in that space.
     */
    static BufferedImage convertColorToLinearRGB(BufferedImage inImage) {
        BufferedImage outImage = inImage;

        final ColorSpace linearCS =
                ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
        if (!linearCS.equals(inImage.getColorModel().getColorSpace())) {
            final Stopwatch watch = new Stopwatch();

            // Create the destination image.
            ComponentColorModel cm = new ComponentColorModel(
                    linearCS, false, false,
                    Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            WritableRaster raster = cm.createCompatibleWritableRaster(
                    inImage.getWidth(), inImage.getHeight());
            outImage = new BufferedImage(
                    cm, raster, cm.isAlphaPremultiplied(), null);

            RenderingHints hints = new RenderingHints(null);
            ColorConvertOp op    = new ColorConvertOp(
                    inImage.getColorModel().getColorSpace(),
                    linearCS,
                    hints);
            op.filter(inImage, outImage);
            LOGGER.trace("convertColorToLinearRGB(): executed in {}", watch);
        }
        return outImage;
    }

    /**
     * @param inImage Image to convert.
     * @return        New image with sRGB color space, or the input image if it
     *                is already in that space.
     */
    static BufferedImage convertColorToSRGB(BufferedImage inImage) {
        BufferedImage outImage = inImage;

        final ColorSpace rgbCS = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        if (!rgbCS.equals(inImage.getColorModel().getColorSpace())) {
            final Stopwatch watch = new Stopwatch();

            // Create the destination image.
            outImage = new BufferedImage(
                    inImage.getWidth(), inImage.getHeight(),
                    inImage.getColorModel().hasAlpha() ?
                            BufferedImage.TYPE_4BYTE_ABGR :
                            BufferedImage.TYPE_3BYTE_BGR);

            RenderingHints hints = new RenderingHints(null);
            ColorConvertOp op    = new ColorConvertOp(
                    inImage.getColorModel().getColorSpace(),
                    rgbCS,
                    hints);
            op.filter(inImage, outImage);
            LOGGER.trace("convertColorToSRGB(): executed in {}", watch);
        }
        return outImage;
    }

    /**
     * Converts an image to sRGB color space using its embedded ICC profile.
     *
     * @param image   Image to apply the profile to.
     * @param profile The image's embedded profile.
     * @return        New image with profile applied.
     */
    public static BufferedImage convertToSRGB(BufferedImage image,
                                              ICC_Profile profile) {
        ICC_Profile destProfile   = ICC_Profile.getInstance(ColorSpace.CS_sRGB);
        ColorConvertOp convertOp  = new ColorConvertOp(
                new ICC_Profile[]{profile, destProfile}, null);
        WritableRaster raster = convertOp.filter(image.getRaster(), null);
        ColorSpace destColorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        return new BufferedImage(
                new ComponentColorModel(
                        destColorSpace,
                        image.getColorModel().hasAlpha(),
                        image.getColorModel().isAlphaPremultiplied(),
                        ColorModel.OPAQUE,
                        DataBuffer.TYPE_BYTE),
                raster,
                false,
                null);
    }

    /**
     * <p>Crops the given image taking into account a reduction factor. (In
     * other words, the dimensions of the input image have already been halved
     * {@code reductionFactor} times but the given region is relative to the
     * full-sized image.</p>
     *
     * <p>This method should only be invoked if {@link
     * Crop#hasEffect(Size, OperationList)} returns {@code true}.</p>
     *
     * @param inImage         Image to crop.
     * @param crop            Crop operation. Clients should call {@link
     *                        Operation#hasEffect(Size, OperationList)}
     *                        before invoking.
     * @param rf              Number of times the dimensions of {@code inImage}
     *                        have already been halved relative to the full-
     *                        sized version.
     * @param scaleConstraint Scale constraint.
     * @return                Cropped image, or the input image if the given
     *                        region is a no-op. Note that the image is simply
     *                        a wrapper around the same data buffer.
     */
    public static BufferedImage crop(final BufferedImage inImage,
                                     final Crop crop,
                                     final ReductionFactor rf,
                                     final ScaleConstraint scaleConstraint) {
        final Size inSize = new Size(
                inImage.getWidth(), inImage.getHeight());
        final Region roi = crop.getRegion(inSize, rf, scaleConstraint);
        return cropVirtually(inImage, roi);
    }

    /**
     * Creates an image that appears to have the given region dimensions, but
     * wraps the raster of the given image.
     */
    private static BufferedImage cropVirtually(final BufferedImage inImage,
                                               final Region region) {
        BufferedImage outImage = inImage;

        final Size inSize = new Size(
                inImage.getWidth(), inImage.getHeight());
        if (!inSize.equals(region.size())) {
            int x      = region.intX();
            int y      = region.intY();
            int width  = region.intWidth();
            int height = region.intHeight();
            // N.B.: getSubimage() will round 0.5 up (e.g. 10.5 + 10.5 = 22),
            // which could exceed the raster bounds and blow up. Let's try to
            // prevent that. But if the excess is > 1, then something else is
            // wrong.
            if (x + width == inImage.getWidth() + 1) {
                width -= 1;
            }
            if (y + height == inImage.getHeight() + 1) {
                height -= 1;
            }
            outImage = inImage.getSubimage(x, y, width, height);

            LOGGER.trace("cropVirtually(): cropped {}x{} image to {}x{}",
                    inImage.getWidth(), inImage.getHeight(),
                    region.intWidth(), region.intHeight());
        }
        return outImage;
    }

    public static void invertColor(WritableRaster raster) {
        final int height = raster.getHeight();
        final int width  = raster.getWidth();
        final int stride = width * 4;
        final int[] pixelRow = new int[stride];

        for (int h = 0; h < height; h++) {
            raster.getPixels(0, h, width, 1, pixelRow);
            for (int x = 0; x < stride; x++) {
                pixelRow[x] = 255 - pixelRow[x];
            }
            raster.setPixels(0, h, width, 1, pixelRow);
        }
    }

    /**
     * @return Overlay image.
     */
    static BufferedImage getOverlayImage(ImageOverlay overlay) {
        try (Arena arena = Arena.ofConfined();
             ImageInputStream is = overlay.openStream();
             Decoder decoder = DecoderFactory.newDecoder(Format.get("png"), arena)) {
            decoder.setSource(is);
            return decoder.decode(0);
        } catch (IOException e) {
            LOGGER.warn("{} (skipping overlay)", e.getMessage());
        }
        return null;
    }

    /**
     * @param baseImage    Image to overlay the image onto.
     * @param overlayImage Image to overlay.
     * @param position     Position of the overlaid image.
     * @param inset        Inset in pixels.
     */
    private static void overlayImage(final BufferedImage baseImage,
                                     final BufferedImage overlayImage,
                                     final Position position,
                                     final int inset) {
        if (overlayImage == null) {
            return;
        }
        final Stopwatch watch = new Stopwatch();

        final Graphics2D g2d = baseImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(baseImage, 0, 0, null);

        final int baseImageWidth     = baseImage.getWidth();
        final int baseImageHeight    = baseImage.getHeight();
        final int overlayImageWidth  = overlayImage.getWidth();
        final int overlayImageHeight = overlayImage.getHeight();

        if (Position.REPEAT.equals(position)) {
            int startX = Math.round(baseImageWidth / 2f);
            int startY = Math.round(baseImageHeight / 2f);
            while (startX >= 0) {
                startX -= overlayImageWidth;
            }
            while (startY >= 0) {
                startY -= overlayImageHeight;
            }
            for (int x = startX; x < baseImageWidth; x += overlayImageWidth) {
                for (int y = startY; y < baseImageHeight; y += overlayImageHeight) {
                    g2d.drawImage(overlayImage, x, y, null);
                }
            }
        } else if (Position.SCALED.equals(position)) {
            // Scale the overlay to the size of the image minus the inset * 2
            // in both dimensions.
            int calculatedOverlayWidth = baseImageWidth - (inset * 2);
            int calculatedOverlayHeight = baseImageHeight - (inset * 2);
            ScaleByPixels scale = new ScaleByPixels(
                    (calculatedOverlayWidth > 0) ? calculatedOverlayWidth : baseImageWidth,
                    (calculatedOverlayHeight > 0) ? calculatedOverlayHeight : baseImageHeight,
                    ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
            ScaleConstraint sc = new ScaleConstraint(1, 1);
            ReductionFactor rf = new ReductionFactor(1);
            BufferedImage scaledOverlay = Java2DUtils.scale(
                    overlayImage, scale, sc, rf, false);
            int xOffset = inset;
            int yOffset = inset;
            if (scaledOverlay.getWidth() < baseImageWidth) {
                xOffset = Math.round((baseImageWidth - scaledOverlay.getWidth()) / 2f);
            }
            if (scaledOverlay.getHeight() < baseImageHeight) {
                yOffset = Math.round((baseImageHeight - scaledOverlay.getHeight()) / 2f);
            }
            g2d.drawImage(Java2DUtils.scale(overlayImage, scale, sc, rf, false),
                    xOffset, yOffset, null);
        } else {
            int overlayX, overlayY;
            switch (position) {
                case TOP_LEFT:
                    overlayX = inset;
                    overlayY = inset;
                    break;
                case TOP_RIGHT:
                    overlayX = baseImageWidth - overlayImageWidth - inset;
                    overlayY = inset;
                    break;
                case BOTTOM_LEFT:
                    overlayX = inset;
                    overlayY = baseImageHeight - overlayImageHeight - inset;
                    break;
                // case BOTTOM_RIGHT: will be handled in default:
                case TOP_CENTER:
                    overlayX = (baseImageWidth - overlayImageWidth) / 2;
                    overlayY = inset;
                    break;
                case BOTTOM_CENTER:
                    overlayX = (baseImageWidth - overlayImageWidth) / 2;
                    overlayY = baseImageHeight - overlayImageHeight - inset;
                    break;
                case LEFT_CENTER:
                    overlayX = inset;
                    overlayY = (baseImageHeight - overlayImageHeight) / 2;
                    break;
                case RIGHT_CENTER:
                    overlayX = baseImageWidth - overlayImageWidth - inset;
                    overlayY = (baseImageHeight - overlayImageHeight) / 2;
                    break;
                case CENTER:
                    overlayX = (baseImageWidth - overlayImageWidth) / 2;
                    overlayY = (baseImageHeight - overlayImageHeight) / 2;
                    break;
                default: // bottom right
                    overlayX = baseImageWidth - overlayImageWidth - inset;
                    overlayY = baseImageHeight - overlayImageHeight - inset;
                    break;
            }
            g2d.drawImage(overlayImage, overlayX, overlayY, null);
        }
        g2d.dispose();

        LOGGER.trace("overlayImage() executed in {}", watch);
    }

    /**
     * <p>Overlays a string onto an image.</p>
     *
     * <p>There are two main layout strategies, depending on {@link
     * StringOverlay#isWordWrap()}:</p>
     *
     * <ol>
     *     <li>With the word wrap strategy, the font size is fixed, the
     *     available width is the full width of the image, and the height is
     *     whatever needed to accommodate the wrapped lines.</li>
     *     <li>With the other strategy, the largest font size that enables the
     *     string to fit entirely within the image is used, down to a
     *     configured minimum size.</li>
     * </ol>
     *
     * @param baseImage Image to overlay a string onto.
     * @param overlay   Overlay to apply to the image.
     */
    private static void overlayString(final BufferedImage baseImage,
                                      final StringOverlay overlay) {
        if (!overlay.hasEffect()) {
            return;
        }
        final Stopwatch watch = new Stopwatch();

        final Graphics2D g2d = baseImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        // Graphics2D.drawString() doesn't understand newlines. Each line must
        // be drawn separately.
        Font font                 = overlay.getFont();
        float fontSize            = font.getSize();
        final int inset           = overlay.getInset();
        final int padding         = getBoxPadding(overlay);
        final int availableWidth  = baseImage.getWidth() - (inset * 2) - (padding * 2);
        final int availableHeight = baseImage.getHeight() - (inset * 2) - (padding * 2);
        boolean fits              = false;
        String[] lines;
        int[] lineWidths;
        int maxLineWidth, lineHeight, totalHeight;

        if (overlay.isWordWrap()) {
            g2d.setFont(font);
            final FontMetrics fm = g2d.getFontMetrics();
            lines = StringUtils.wrap(overlay.getString(), fm, availableWidth)
                    .toArray(String[]::new);
            lineHeight   = fm.getHeight();
            totalHeight  = lineHeight * lines.length;
            lineWidths   = getLineWidths(lines, fm);
            maxLineWidth = Arrays.stream(lineWidths).max().orElse(0);

            if (totalHeight <= availableHeight) {
                fits = true;
            }
        } else {
            lines = overlay.getString().split("\n");
            // Starting at the initial font size, loop through smaller sizes
            // down to the minimum in order to find the largest that will fit
            // entirely within the image.
            while (true) {
                g2d.setFont(font);
                final FontMetrics fm = g2d.getFontMetrics();
                lineHeight   = fm.getHeight();
                totalHeight  = lineHeight * lines.length;
                lineWidths   = getLineWidths(lines, fm);
                maxLineWidth = Arrays.stream(lineWidths).max().orElse(0);

                // Will the overlay fit inside the image?
                if (maxLineWidth <= availableWidth &&
                        totalHeight <= availableHeight) {
                    fits = true;
                    break;
                } else {
                    if (fontSize - 1 >= overlay.getMinSize()) {
                        fontSize -= 1;
                        font = font.deriveFont(fontSize);
                    } else {
                        break;
                    }
                }
            }
        }

        if (!fits) {
            LOGGER.debug("overlayString(): {}-point ({}x{}) text won't fit in {}x{} image",
                    fontSize,
                    maxLineWidth + inset,
                    totalHeight + inset,
                    baseImage.getWidth(),
                    baseImage.getHeight());
            g2d.dispose();
            return;
        }

        LOGGER.debug("overlayString(): using {}-point font ({} min; {} max)",
                fontSize, overlay.getMinSize(),
                overlay.getFont().getSize());

        g2d.drawImage(baseImage, 0, 0, null);

        final Region bgBox = getBoundingBox(overlay, inset,
                lineWidths, lineHeight,
                new Size(baseImage.getWidth(), baseImage.getHeight()));

        // Draw the background if it is not transparent.
        if (overlay.getBackgroundColor().getAlpha() > 0) {
            g2d.setPaint(overlay.getBackgroundColor().toAWTColor());
            g2d.fillRect(bgBox.intX(), bgBox.intY(),
                    bgBox.intWidth(), bgBox.intHeight());
        }

        // Draw each line individually.
        for (int i = 0; i < lines.length; i++) {
            double x, y;
            switch (overlay.getPosition()) {
                case TOP_LEFT:
                    x = bgBox.x() + padding;
                    y = bgBox.y() + lineHeight * i + padding;
                    break;
                case TOP_RIGHT:
                    x = bgBox.x() + maxLineWidth - lineWidths[i] + padding;
                    y = bgBox.y() + lineHeight * i + padding;
                    break;
                case BOTTOM_LEFT:
                    x = bgBox.x() + padding;
                    y = bgBox.y() + lineHeight * i + padding;
                    break;
                // BOTTOM_RIGHT is handled in default:
                case TOP_CENTER:
                    x = bgBox.x() + (bgBox.width() - lineWidths[i]) / 2.0;
                    y = bgBox.y() + lineHeight * i + padding;
                    break;
                case BOTTOM_CENTER:
                    x = bgBox.x() + (bgBox.width() - lineWidths[i]) / 2.0;
                    y = bgBox.y() + lineHeight * i + padding;
                    break;
                case LEFT_CENTER:
                    x = bgBox.x() + padding;
                    y = bgBox.y() + lineHeight * i + padding;
                    break;
                case RIGHT_CENTER:
                    x = bgBox.x() + maxLineWidth - lineWidths[i] + padding;
                    y = bgBox.y() + lineHeight * i + padding;
                    break;
                case CENTER:
                    x = bgBox.x() + (bgBox.width() - lineWidths[i]) / 2.0;
                    y = bgBox.y() + lineHeight * i + padding;
                    break;
                default: // bottom right
                    x = bgBox.x() + maxLineWidth - lineWidths[i] + padding;
                    y = bgBox.y() + lineHeight * i + padding;
                    break;
            }

            // This is arbitrary fudge, but it seems to work OK.
            y += lineHeight * 0.73;

            // Draw the text outline.
            if (overlay.getStrokeWidth() > 0.001) {
                final FontRenderContext frc = g2d.getFontRenderContext();
                final GlyphVector gv = font.createGlyphVector(frc, lines[i]);
                final Shape shape = gv.getOutline(Math.round(x), Math.round(y));
                g2d.setStroke(new BasicStroke(overlay.getStrokeWidth()));
                g2d.setPaint(overlay.getStrokeColor().toAWTColor());
                g2d.draw(shape);
            }

            // Draw the string.
            g2d.setPaint(overlay.getColor().toAWTColor());
            g2d.drawString(lines[i], Math.round(x), Math.round(y));
        }
        g2d.dispose();
        LOGGER.trace("overlayString() executed in {}", watch);
    }

    private static int[] getLineWidths(String[] lines, FontMetrics fm) {
        final int[] lineWidths = new int[lines.length];
        for (int i = 0; i < lines.length; i++) {
            lineWidths[i] = fm.stringWidth(lines[i]);
        }
        return lineWidths;
    }

    private static Region getBoundingBox(final StringOverlay overlay,
                                         final int inset,
                                         final int[] lineWidths,
                                         final int lineHeight,
                                         final Size imageSize) {
        // If the overlay background is visible, add some padding between the
        // text and the margin.
        final int padding = getBoxPadding(overlay);
        final double boxWidth = NumberUtils.max(lineWidths) + padding * 2;
        final double boxHeight = lineHeight * lineWidths.length + padding * 2;
        double boxX, boxY;
        switch (overlay.getPosition()) {
            case TOP_LEFT:
                boxX = inset;
                boxY = inset;
                break;
            case TOP_CENTER:
                boxX = (imageSize.width() - boxWidth) / 2.0;
                boxY = inset;
                break;
            case TOP_RIGHT:
                boxX = imageSize.width() - boxWidth - inset - padding;
                boxY = inset;
                break;
            case LEFT_CENTER:
                boxX = inset;
                boxY = (imageSize.height() - boxHeight) / 2.0;
                break;
            case RIGHT_CENTER:
                boxX = imageSize.width() - boxWidth - inset - padding;
                boxY = (imageSize.height() - boxHeight) / 2.0;
                break;
            case CENTER:
                boxX = (imageSize.width() - boxWidth) / 2.0;
                boxY = (imageSize.height() - boxHeight) / 2.0;
                break;
            case BOTTOM_LEFT:
                boxX = inset;
                boxY = imageSize.height() - boxHeight - inset - padding;
                break;
            case BOTTOM_CENTER:
                boxX = (imageSize.width() - boxWidth) / 2.0;
                boxY = imageSize.height() - boxHeight - inset - padding;
                break;
            default: // bottom right
                boxX = imageSize.width() - boxWidth - inset - padding;
                boxY = imageSize.height() - boxHeight - inset - padding;
                break;
        }
        return new Region(boxX, boxY, boxWidth, boxHeight);
    }

    private static int getBoxPadding(StringOverlay overlay) {
        return (overlay.getBackgroundColor().getAlpha() > 0) ? 5 : 0;
    }

    /**
     * Creates a new blank image.
     *
     * @param width      Width of the new image.
     * @param height     Height of the new image.
     * @param colorModel Color model of the new image.
     * @param alpha      Whether the resulting image should have alpha.
     * @return           New image with the given color model and dimensions.
     */
    private static BufferedImage newImage(final int width,
                                          final int height,
                                          ColorModel colorModel,
                                          final boolean alpha) {
        final boolean isAlphaPremultiplied = colorModel.isAlphaPremultiplied();

        // Create a compatible ColorModel.
        if (colorModel instanceof ComponentColorModel) {
            int[] componentSizes = colorModel.getComponentSize();
            // If the array does not contain an alpha element but we need it,
            // add it.
            if (!colorModel.hasAlpha() && alpha) {
                int[] tmp = new int[componentSizes.length + 1];
                System.arraycopy(componentSizes, 0, tmp, 0, componentSizes.length);
                tmp[tmp.length - 1] = tmp[0];
                componentSizes = tmp;
            }
            colorModel = new ComponentColorModel(colorModel.getColorSpace(),
                    componentSizes, alpha, isAlphaPremultiplied,
                    colorModel.getTransparency(), colorModel.getTransferType());
        }

        WritableRaster raster =
                colorModel.createCompatibleWritableRaster(width, height);
        return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
    }

    /**
     * Creates a new image backed by the given samples.
     *
     * @param width          Width of the image.
     * @param height         Height of the image.
     * @param componentOrder Order of the samples in the given array.
     * @param samples        Array of samples (one byte per sample).
     * @return               New image backed by the given samples with no
     *                       copying.
     */
    public static BufferedImage newImage(final int width,
                                         final int height,
                                         byte[] samples,
                                         final ComponentOrder componentOrder) {
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ColorModel colorModel;
        SampleModel sampleModel;
        if (componentOrder.hasAlpha()) {
            // Create a ColorModel
            colorModel = new ComponentColorModel(
                    colorSpace, new int[] {8, 8, 8, 8}, true, false, 1, 0);
            // Create a SampleModel
            int[] bandOffsets;
            switch (componentOrder) {
                case ABGR -> bandOffsets = new int[] {3, 2, 1, 0};
                default   -> bandOffsets = new int[] {0, 1, 2, 3};
            }
            sampleModel = new PixelInterleavedSampleModel(
                    DataBuffer.TYPE_BYTE, width, height,
                    4, 4 * width, bandOffsets);
        } else {
            // Create a ColorModel
            colorModel = new ComponentColorModel(
                    colorSpace, new int[] {8, 8, 8}, false, false, 1, 0);
            // Create a SampleModel
            int[] bandOffsets;
            switch (componentOrder) {
                case BGR -> bandOffsets = new int[] {2, 1, 0};
                default  -> bandOffsets = new int[] {0, 1, 2};
            }
            sampleModel = new PixelInterleavedSampleModel(
                    DataBuffer.TYPE_BYTE, width, height,
                    3, 3 * width, bandOffsets);
        }
        // Create the image
        DataBuffer dataBuffer = new DataBufferByte(samples, samples.length);
        WritableRaster raster = WritableRaster.createWritableRaster(
                sampleModel, dataBuffer, null);
        return new BufferedImage(colorModel, raster, false, null);
    }

    /**
     * Generates a new test pattern image consisting of red, green, blue, and
     * alpha (if supported by the given type) bars of equal width.
     *
     * @param width  Width.
     * @param height Height.
     * @param type   One of the {@link BufferedImage#getType()} constant
     *               values.
     * @return       New image.
     */
    public static BufferedImage newTestPatternImage(int width,
                                                    int height,
                                                    int type) {
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D g2d      = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);

        if (image.getColorModel().hasAlpha()) {
            final int oneFourthWidth = image.getWidth() / 4;
            g2d.setColor(java.awt.Color.RED);
            g2d.fill(new Rectangle(0, 0, oneFourthWidth, image.getHeight()));
            g2d.setColor(java.awt.Color.GREEN);
            g2d.fill(new Rectangle(oneFourthWidth, 0, oneFourthWidth, image.getHeight()));
            g2d.setColor(java.awt.Color.BLUE);
            g2d.fill(new Rectangle(oneFourthWidth * 2, 0, oneFourthWidth, image.getHeight()));
        } else {
            final int oneThirdWidth = image.getWidth() / 3;
            g2d.setColor(java.awt.Color.RED);
            g2d.fill(new Rectangle(0, 0, oneThirdWidth, image.getHeight()));
            g2d.setColor(java.awt.Color.GREEN);
            g2d.fill(new Rectangle(oneThirdWidth, 0, oneThirdWidth, image.getHeight()));
            g2d.setColor(java.awt.Color.BLUE);
            g2d.fill(new Rectangle(oneThirdWidth * 2, 0, oneThirdWidth, image.getHeight()));
        }
        g2d.dispose();
        return image;
    }

    /**
     * Reduces an image's sample/component size to 8 bits if greater. This
     * involves copying it into a new {@link BufferedImage}, which is expensive.
     *
     * @param inImage Image to reduce.
     * @return        Reduced image, or the input image if it already is 8 bits
     *                or less.
     */
    static BufferedImage reduceTo8Bits(final BufferedImage inImage) {
        BufferedImage outImage = inImage;
        final ColorModel inColorModel = inImage.getColorModel();

        if (inColorModel.getComponentSize(0) > 8) {
            final Stopwatch watch = new Stopwatch();

            int type;
            if (inColorModel.getNumComponents() > 3) {
                type = BufferedImage.TYPE_4BYTE_ABGR;
            } else if (inColorModel.getNumComponents() > 1) {
                type = BufferedImage.TYPE_3BYTE_BGR;
            } else {
                type = BufferedImage.TYPE_BYTE_GRAY;
            }
            outImage = new BufferedImage(
                    inImage.getWidth(), inImage.getHeight(), type);
            final ColorConvertOp op = new ColorConvertOp(
                    inColorModel.getColorSpace(),
                    outImage.getColorModel().getColorSpace(), null);
            outImage.createGraphics().drawImage(inImage, op, 0, 0);
            LOGGER.trace("reduceTo8Bits(): executed in {}", watch);
        }
        return outImage;
    }

    /**
     * Creates a new image without alpha, paints the given background color
     * into it, draws the RGB channels of the given image into it, and returns
     * it.
     *
     * @param inImage Image with alpha.
     * @return        Flattened image, or the input image if it has no alpha.
     */
    public static BufferedImage removeAlpha(final BufferedImage inImage,
                                            final Color bgColor) {
        BufferedImage outImage = inImage;
        if (inImage.getColorModel().hasAlpha()) {
            final Stopwatch watch = new Stopwatch();

            outImage = new BufferedImage(inImage.getWidth(),
                    inImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g2d = outImage.createGraphics();

            if (bgColor != null) {
                g2d.setBackground(bgColor.toAWTColor());
                g2d.clearRect(0, 0, inImage.getWidth(), inImage.getHeight());
            }

            g2d.drawImage(inImage, 0, 0, null);
            g2d.dispose();
            LOGGER.trace("removeAlpha(): executed in {}", watch);
        }
        return outImage;
    }

    /**
     * Creates a new image without alpha, paints the given background color
     * into it, draws the RGB channels of the given image into it, and returns
     * it.
     *
     * @param inImage Image with alpha.
     * @return        Flattened image, or the input image if it has no alpha.
     */
    public static RenderedImage removeAlpha(final RenderedImage inImage,
                                            final Color bgColor) {
        RenderedImage outImage = inImage;
        if (inImage.getColorModel().hasAlpha()) {
            final Stopwatch watch = new Stopwatch();

            outImage = new BufferedImage(inImage.getWidth(),
                    inImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g2d = ((BufferedImage) outImage).createGraphics();

            if (bgColor != null) {
                g2d.setBackground(bgColor.toAWTColor());
                g2d.clearRect(0, 0, inImage.getWidth(), inImage.getHeight());
            }

            g2d.drawRenderedImage(inImage, null);
            g2d.dispose();
            LOGGER.trace("removeAlpha(): executed in {}", watch);
        }
        return outImage;
    }

    /**
     * Alternative to {@link #rotate(BufferedImage, Rotate)} for orientations
     * other than {@link Orientation#ROTATE_0} when there is no
     * {@link Rotate} operation present.
     *
     * @param inImage     Image to rotate.
     * @param orientation Orientation.
     * @return            Rotated image, or the input image if the given
     *                    orientation is a no-op.
     */
    static BufferedImage rotate(final BufferedImage inImage,
                                final Orientation orientation) {
        return rotate(inImage, new Rotate(orientation.degrees()));
    }

    /**
     * @param inImage Image to rotate.
     * @param rotate  Rotate operation.
     * @return        Rotated image, or the input image if the given
     *                rotation is a no-op.  The image has alpha.
     */
    static BufferedImage rotate(final BufferedImage inImage,
                                final Rotate rotate) {
        BufferedImage outImage = inImage;
        if (rotate.hasEffect()) {
            final Stopwatch watch = new Stopwatch();
            final double radians = Math.toRadians(rotate.getDegrees());
            final int sourceWidth = inImage.getWidth();
            final int sourceHeight = inImage.getHeight();
            final int canvasWidth = (int) Math.round(Math.abs(sourceWidth *
                    Math.cos(radians)) + Math.abs(sourceHeight *
                    Math.sin(radians)));
            final int canvasHeight = (int) Math.round(Math.abs(sourceHeight *
                    Math.cos(radians)) + Math.abs(sourceWidth *
                    Math.sin(radians)));

            // N.B.: Operations happen in reverse order of declaration
            AffineTransform tx = new AffineTransform();
            tx.translate(canvasWidth / 2.0, canvasHeight / 2.0);
            tx.rotate(radians);
            tx.translate(-sourceWidth / 2.0, -sourceHeight / 2.0);

            final int compSize      = inImage.getColorModel().getComponentSize(0);
            final int numComponents = inImage.getColorModel().getNumComponents();

            // AffineTransformOp should be faster, but the G2D drawing method
            // is more compatible.
            if (compSize > 8 || numComponents < 3) {
                if (compSize > 8) {
                    outImage = newImage(canvasWidth, canvasHeight,
                            inImage.getColorModel(), true);
                } else {
                    outImage = new BufferedImage(
                            canvasWidth, canvasHeight,
                            BufferedImage.TYPE_INT_ARGB);
                }

                final Graphics2D g2d = outImage.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_SPEED);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                g2d.drawImage(inImage, tx, null);
                g2d.dispose();
            } else {
                AffineTransformOp op = new AffineTransformOp(
                        tx, AffineTransformOp.TYPE_BILINEAR);
                outImage = new BufferedImage(canvasWidth, canvasHeight,
                        BufferedImage.TYPE_INT_ARGB);
                op.filter(inImage, outImage);
            }
            LOGGER.trace("rotate(): executed in {}", watch);
        }
        return outImage;
    }

    /**
     * <p>Scales an image, taking an already-applied reduction factor into
     * account. In other words, the dimensions of the input image have already
     * been halved {@link ReductionFactor#factor} times but the given scale is
     * relative to the full-sized image.</p>
     *
     * <p>If one or both target dimensions would end up being less than three
     * pixels, an empty image (with the correct dimensions) is returned.</p>
     *
     * @param inImage         Image to scale.
     * @param scale           Requested size ignoring any reduction factor. If
     *                        no resample filter is set, a default will be
     *                        used. {@link
     *                        Operation#hasEffect(Size, OperationList)}
     *                        should be called before invoking.
     * @param scaleConstraint Scale constraint.
     * @param reductionFactor Reduction factor that has already been applied to
     *                        {@literal inImage}.
     * @param isLinear        Whether the provided image is in a linear color
     *                        space.
     * @return                Scaled image, or the input image if the given
     *                        arguments would result in a no-op.
     */
    public static BufferedImage scale(BufferedImage inImage,
                                      final Scale scale,
                                      final ScaleConstraint scaleConstraint,
                                      final ReductionFactor reductionFactor,
                                      final boolean isLinear) {
        final Size sourceSize = new Size(
                inImage.getWidth(), inImage.getHeight());
        final Size targetSize = scale.getResultingSize(
                sourceSize, reductionFactor, scaleConstraint);

        // ResampleFilter requires both target dimensions to be at least 3
        // pixels. (OpenSeadragon has been known to request smaller.)
        // If one or both are less than that, then given that the result is
        // virtually guaranteed to end up unrecognizable anyway, we will skip
        // the scaling step and return a fake image with the target dimensions.
        // The only alternatives would be to use a different resampler, set a
        // 3x3 floor, or error out.
        BufferedImage scaledImage = inImage;
        if (sourceSize.intWidth() >= 3 && sourceSize.intHeight() >= 3 &&
                targetSize.intWidth() >= 3 && targetSize.intHeight() >= 3) {
            if (!targetSize.equals(sourceSize)) {
                final Stopwatch watch = new Stopwatch();

                final ResampleOp resampleOp = new ResampleOp(
                        targetSize.intWidth(), targetSize.intHeight(),
                        isLinear);

                // Try to use the requested resample filter.
                ResampleFilter filter = null;
                if (scale.getFilter() != null) {
                    filter = scale.getFilter().toResampleFilter();
                }
                // No particular filter requested, so select a default.
                if (filter == null) {
                    if (targetSize.width() < sourceSize.width() ||
                            targetSize.height() < sourceSize.height()) {
                        filter = DEFAULT_DOWNSCALE_FILTER.toResampleFilter();
                    } else {
                        filter = DEFAULT_UPSCALE_FILTER.toResampleFilter();
                    }
                }
                resampleOp.setFilter(filter);

                scaledImage = resampleOp.filter(inImage, null);

                LOGGER.trace("scale(): scaled {}x{} image to {}x{} using a " +
                                "{} in {}",
                        sourceSize.intWidth(), sourceSize.intHeight(),
                        targetSize.intWidth(), targetSize.intHeight(),
                        filter.getClass().getSimpleName(), watch);
            }
        } else {
            // Dummy image.
            scaledImage = new BufferedImage(
                    targetSize.intWidth(),
                    targetSize.intHeight(),
                    BufferedImage.TYPE_4BYTE_ABGR);
        }
        return scaledImage;
    }

    /**
     * @param inImage Image to sharpen.
     * @param sharpen Sharpen operation.
     * @return        Sharpened image.
     */
    static BufferedImage sharpen(final BufferedImage inImage,
                                 final Sharpen sharpen) {
        BufferedImage sharpenedImage = inImage;
        if (sharpen.hasEffect()) {
            if (inImage.getWidth() > 2 && inImage.getHeight() > 2) {
                final Stopwatch watch = new Stopwatch();

                final ResampleOp resampleOp = new ResampleOp(
                        inImage.getWidth(), inImage.getHeight(), false);
                resampleOp.setUnsharpenMask((float) sharpen.getAmount());
                sharpenedImage = resampleOp.filter(inImage, null);

                LOGGER.trace("sharpen(): sharpened by {} in {}",
                        sharpen.getAmount(), watch);
            } else {
                LOGGER.trace("sharpen(): image must be at least 3 " +
                        "pixels on a side; skipping");
            }
        }
        return sharpenedImage;
    }

    /**
     * @param inImage        Image to filter.
     * @param colorTransform Operation to apply.
     * @return               Filtered image, or the input image if the given
     *                       operation is a no-op.
     */
    static BufferedImage transformColor(final BufferedImage inImage,
                                        final ColorTransform colorTransform) {
        BufferedImage outImage = inImage;
        final Stopwatch watch = new Stopwatch();

        switch (colorTransform) {
            case GRAY:
                outImage = convertIndexedToARGB(outImage);
                grayscale(outImage);
                break;
            case BITONAL:
                outImage = convertIndexedToARGB(outImage);
                binarize(outImage);
                break;
        }
        if (outImage != inImage) {
            LOGGER.trace("transformColor(): transformed {}x{} image in {}",
                    inImage.getWidth(), inImage.getHeight(), watch);
        }
        return outImage;
    }

    /**
     * Grayscales the given image's pixels.
     */
    private static void grayscale(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb  = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xff;
                int red   = (argb >> 16) & 0xff;
                int green = (argb >> 8) & 0xff;
                int blue  = argb & 0xff;
                int luma  = (int) (0.21 * red + 0.71 * green + 0.07 * blue);
                argb      = (alpha << 24) | (luma << 16) | (luma << 8) | luma;
                image.setRGB(x, y, argb);
            }
        }
    }

    /**
     * Binarizes the given image's pixels.
     *
     * @see <a href="https://bostjan-cigan.com/java-image-binarization-using-otsus-algorithm/">
     *     Java Image Binarization Using Otsu's Algorithm</a>
     */
    private static void binarize(BufferedImage image) {
        int red;
        int newPixel;
        int threshold = otsuThreshold(image);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                red = new Color(image.getRGB(x, y)).getRed();
                int alpha = new Color(image.getRGB(x, y)).getAlpha();
                if (red > threshold) {
                    newPixel = 255;
                } else {
                    newPixel = 0;
                }
                Color color = new Color(newPixel, newPixel, newPixel, alpha);
                image.setRGB(x, y, color.getARGB());
            }
        }
    }

    /**
     * @return Histogram of a grayscale image.
     */
    private static int[] histogram(BufferedImage input) {
        int[] histogram = new int[256];
        for (int x = 0; x < input.getWidth(); x++) {
            for (int y = 0; y < input.getHeight(); y++) {
                int red = new Color(input.getRGB(x, y)).getRed();
                histogram[red]++;
            }
        }
        return histogram;
    }

    /**
     * @return Binary threshold using Otsu's method.
     */
    private static int otsuThreshold(BufferedImage image) {
        int[] histogram = histogram(image);
        int total = image.getHeight() * image.getWidth();

        float sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += i * histogram[i];
        }

        float sumB = 0;
        int wB = 0, wF;

        float varMax = 0;
        int threshold = 0;

        for (int i = 0; i < 256; i++) {
            wB += histogram[i];
            if (wB == 0) {
                continue;
            }
            wF = total - wB;

            if (wF == 0) {
                break;
            }

            sumB += (float) (i * histogram[i]);
            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;

            float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);

            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = i;
            }
        }
        return threshold;
    }

    /**
     * @param inImage   Image to transpose.
     * @param transpose Operation to apply.
     * @return          Transposed image.
     */
    static BufferedImage transpose(final BufferedImage inImage,
                                   final Transpose transpose) {
        final Stopwatch watch = new Stopwatch();
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        switch (transpose) {
            case HORIZONTAL:
                tx.translate(-inImage.getWidth(null), 0);
                break;
            case VERTICAL:
                tx.translate(0, -inImage.getHeight(null));
                break;
        }
        AffineTransformOp op = new AffineTransformOp(tx,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        BufferedImage outImage = op.filter(inImage, null);

        LOGGER.trace("transpose(): executed in {}", watch);
        return outImage;
    }

    private Java2DUtils() {}

}
