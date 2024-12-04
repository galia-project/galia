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

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.image.Format;
import is.galia.operation.Color;
import is.galia.operation.Encode;
import is.galia.processor.Java2DUtils;
import is.galia.codec.AbstractImageIOEncoder;
import is.galia.codec.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation wrapping the default JDK Image I/O JPEG {@link
 * javax.imageio.ImageWriter}.
 */
public final class JPEGEncoder extends AbstractImageIOEncoder
        implements Encoder {

    /**
     * Wraps an {@link OutputStream}, injecting an {@literal APP1} segment
     * into it at the appropriate position if there is any metadata to write.
     * This works around an apparent bug in the Sun JPEG writer (see {@link
     * #addMetadata(IIOMetadataNode)}).
     */
    private static class SegmentInjectingOutputStream
            extends FilterOutputStream {
        private byte[] app1;

        SegmentInjectingOutputStream(String xmp, OutputStream os) {
            super(os);
            app1 = JPEGUtils.assembleAPP1Segment(xmp);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (app1 != null) {
                out.write(Arrays.copyOfRange(b, off, 20));
                out.write(app1);
                out.write(Arrays.copyOfRange(b, 20, len));
                app1 = null;
            } else {
                out.write(b, off, len);
            }
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JPEGEncoder.class);

    private static final Color DEFAULT_BACKGROUND_COLOR = Color.WHITE;
    private static final int DEFAULT_QUALITY = 80;

    /**
     * <p>When an {@literal unknown} node representing an {@literal APPn}
     * segment is appended to the {@literal markerSegment} node per the "Native
     * Metadata Format Tree Structure and Editing" section of
     * <a href="http://docs.oracle.com/javase/7/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html">
     * JPEG Metadata Format Specification and Usage Notes</a>, the Sun JPEG
     * writer writes that segment before the {@literal APP0} segment, producing
     * a corrupt image (that resilient readers can nevertheless still
     * read).</p>
     *
     * <p>To avoid that, this method does nothing and an alternative metadata-
     * writing technique involving {@link SegmentInjectingOutputStream} is
     * used instead.</p>
     */
    @Override
    protected void addMetadata(final IIOMetadataNode baseTree) {
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected String[] getPreferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.jpeg.JPEGImageWriter" };
    }

    private ImageWriteParam getWriteParam() {
        final ImageWriteParam writeParam = iioWriter.getDefaultWriteParam();
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionType("JPEG");

        final Configuration options = encode.getOptions();
        // Quality
        final int quality = options.getInt(
                Key.ENCODER_JPEGENCODER_QUALITY, DEFAULT_QUALITY);
        writeParam.setCompressionQuality(quality * 0.01f);

        // Interlacing
        final boolean progressive =
                options.getBoolean(Key.ENCODER_JPEGENCODER_PROGRESSIVE, false);
        writeParam.setProgressiveMode(progressive ?
                ImageWriteParam.MODE_DEFAULT : ImageWriteParam.MODE_DISABLED);
        return writeParam;
    }

    /**
     * Removes the alpha channel from the given image, taking the return value
     * of the operation list's {@link Encode#getBackgroundColor()} method into
     * account, if available.
     *
     * @param image Image to remove alpha from.
     * @return      Flattened image.
     */
    private BufferedImage removeAlpha(BufferedImage image) {
        Color bgColor = encode.getBackgroundColor();
        if (bgColor == null) {
            bgColor = DEFAULT_BACKGROUND_COLOR;
        }
        return Java2DUtils.removeAlpha(image, bgColor);
    }

    @Override
    public Set<Format> getSupportedFormats() {
        return Set.of(JPEGDecoder.FORMAT);
    }

    /**
     * Writes the given image to the given output stream.
     *
     * @param image        Image to write.
     * @param outputStream Stream to write the image to.
     */
    @Override
    public void encode(RenderedImage image,
                       OutputStream outputStream) throws IOException {
        if (image instanceof BufferedImage) {
            encode((BufferedImage) image, outputStream);
        } else {
            throw new IllegalArgumentException(
                    "image must be either a BufferedImage or PlanarImage.");
        }
    }

    /**
     * Writes a Java 2D {@link BufferedImage} to the given output stream.
     *
     * @param image        Image to write
     * @param outputStream Stream to write the image to
     */
    private void encode(BufferedImage image,
                        OutputStream outputStream) throws IOException {
        final Optional<String> xmp = encode.getXMP();
        if (xmp.isPresent()) {
            outputStream = new SegmentInjectingOutputStream(
                    xmp.get(), outputStream);
        }

        // JPEG doesn't support alpha, so convert to RGB or else the
        // client will interpret as CMYK
        image = removeAlpha(image);
        final ImageWriteParam writeParam = getWriteParam();
        final IIOMetadata iioMetadata = getMetadata(writeParam, image);
        final IIOImage iioImage = new IIOImage(image, null, iioMetadata);

        // N.B.: the wrapping stream will be closed, but the wrapped one won't,
        // which is intended.
        try (ImageOutputStream os =
                     new MemoryCacheImageOutputStream(outputStream)) {
            iioWriter.setOutput(os);
            iioWriter.write(null, iioImage, writeParam);
            os.flush();
        }
    }

}
