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

import is.galia.codec.Encoder;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.image.Format;
import is.galia.codec.AbstractImageIOEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.plugins.tiff.TIFFDirectory;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.plugins.tiff.TIFFTag;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation wrapping the default JDK Image I/O TIFF {@link
 * javax.imageio.ImageWriter}.
 *
 * @see <a href="http://download.java.net/media/jai-imageio/javadoc/1.1/com/sun/media/imageio/plugins/tiff/package-summary.html">
 *     ImageIO TIFF Plugin Documentation</a>
 */
public final class TIFFEncoder extends AbstractImageIOEncoder
        implements Encoder {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TIFFEncoder.class);

    /**
     * No-op.
     *
     * @see #addMetadata(IIOMetadata)
     */
    @Override
    protected void addMetadata(final IIOMetadataNode baseNode) {}

    /**
     * @param variantMetadata Metadata in which to embed the XMP metadata.
     * @return                New variant metadata.
     */
    private IIOMetadata addMetadata(IIOMetadata variantMetadata)
            throws IOException {
        final Optional<String> optXMP = encode.getXMP();
        if (optXMP.isPresent()) {
            final String xmp = optXMP.get();
            final TIFFDirectory destDir =
                    TIFFDirectory.createFromMetadata(variantMetadata);
            byte[] xmpBytes = xmp.getBytes(StandardCharsets.UTF_8);
            final TIFFTag xmpTag = new TIFFTag(
                    "XMP", TIFFMetadata.XMP_POINTER_TAG.id(),
                    1 << TIFFTag.TIFF_UNDEFINED);
            final TIFFField xmpField = new TIFFField(
                    xmpTag, TIFFTag.TIFF_UNDEFINED, xmpBytes.length, xmpBytes);
            destDir.addTIFFField(xmpField);

            variantMetadata = destDir.getAsMetadata();
        }
        return variantMetadata;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    /**
     * @param writeParam Write parameters on which to base the metadata.
     * @param image      Image to apply the metadata to.
     * @return           Image metadata with added metadata corresponding to any
     *                   writer-specific operations applied.
     */
    @Override
    protected IIOMetadata getMetadata(final ImageWriteParam writeParam,
                                      final RenderedImage image) throws IOException {
        IIOMetadata variantMetadata = iioWriter.getDefaultImageMetadata(
                ImageTypeSpecifier.createFromRenderedImage(image),
                writeParam);
        return addMetadata(variantMetadata);
    }

    @Override
    protected String[] getPreferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.tiff.TIFFImageWriter" };
    }

    @Override
    public Set<Format> getSupportedFormats() {
        return Set.of(TIFFDecoder.FORMAT);
    }

    /**
     * @return Write parameters respecting the operation list.
     */
    private ImageWriteParam getWriteParam() {
        final ImageWriteParam writeParam = iioWriter.getDefaultWriteParam();
        final Configuration config = encode.getOptions();
        final String compression = config.getString(
                Key.ENCODER_TIFFENCODER_COMPRESSION);
        if (compression != null) {
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionType(compression);
        }
        return writeParam;
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
        final ImageWriteParam writeParam = getWriteParam();
        final IIOMetadata metadata = getMetadata(writeParam, image);
        final IIOImage iioImage = new IIOImage(image, null, metadata);

        // N.B.: the wrapping stream will be closed, but the wrapped one won't,
        // which is intended.
        try (ImageOutputStream os =
                     new MemoryCacheImageOutputStream(outputStream)) {
            iioWriter.setOutput(os);
            iioWriter.write(metadata, iioImage, writeParam);
            os.flush(); // http://stackoverflow.com/a/14489406
        }
    }

}
