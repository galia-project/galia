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

import is.galia.codec.AbstractImageIOEncoder;
import is.galia.codec.BufferedImageSequence;
import is.galia.codec.Encoder;
import is.galia.codec.xmp.XMPUtils;
import is.galia.image.Format;
import is.galia.image.NativeMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Implementation wrapping the default JDK Image I/O GIF {@link
 * javax.imageio.ImageWriter}.
 *
 * @see <a href="http://docs.oracle.com/javase/7/docs/api/javax/imageio/package-summary.html#gif_plugin_notes">
 *     Writing GIF Images</a>
 * @see <a href="http://justsolve.archiveteam.org/wiki/GIF">GIF</a>
 */
public final class GIFEncoder extends AbstractImageIOEncoder
        implements Encoder {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GIFEncoder.class);

    private static final String DEFAULT_IMAGEIO_WRITER =
            "com.sun.imageio.plugins.gif.GIFImageWriter";

    @Override
    protected void addMetadata(final IIOMetadataNode baseTree) {
        if (DEFAULT_IMAGEIO_WRITER.equals(iioWriter.getClass().getName())) {
            /* This writer doesn't support XMP metadata. From the Adobe XMP
            specification part 3:

            "GIF actually treats the Application Data as a series of GIF data
            sub-blocks. The first byte of each sub-block is the length of the
            sub-block’s content, not counting the first byte itself. ...
            Software that is unaware of XMP views packet data bytes as sub-
            block lengths."

            The net result is that the writer inserts sub-block length bytes
            where data bytes should go, corrupting it. */
            return;
        }
        encode.getXMP().ifPresent(xmp -> {
            xmp = XMPUtils.wrapInXPacket(xmp);

            // Get the /ApplicationExtensions node, creating it if necessary.
            final NodeList appExtensionsList =
                    baseTree.getElementsByTagName("ApplicationExtensions");
            IIOMetadataNode appExtensions;
            if (appExtensionsList.getLength() > 0) {
                appExtensions = (IIOMetadataNode) appExtensionsList.item(0);
            } else {
                appExtensions = new IIOMetadataNode("ApplicationExtensions");
                baseTree.appendChild(appExtensions);
            }

            // Create /ApplicationExtensions/ApplicationExtension
            final IIOMetadataNode appExtensionNode =
                    new IIOMetadataNode("ApplicationExtension");
            appExtensionNode.setAttribute("applicationID", "XMP Data");
            appExtensionNode.setAttribute("authenticationCode", "XMP");
            appExtensionNode.setUserObject(xmp.getBytes(StandardCharsets.UTF_8));
            appExtensions.appendChild(appExtensionNode);
        });
    }

    /**
     * {@link #addMetadata(IIOMetadataNode)} is used for copying descriptive
     * metadata; this method is used for copying over structural image
     * metadata.
     */
    private void addStructuralMetadata(final IIOMetadataNode baseTree) {
        NativeMetadata nativeMetadata = encode.getNativeMetadata().orElse(null);
        GIFNativeMetadata gifNativeMetadata = null;
        if (nativeMetadata instanceof GIFNativeMetadata) {
            gifNativeMetadata = (GIFNativeMetadata) nativeMetadata;
        }

        // Get the /ApplicationExtensions node, creating it if it does
        // not exist.
        final NodeList appExtensionsList =
                baseTree.getElementsByTagName("ApplicationExtensions");
        IIOMetadataNode appExtensions;
        if (appExtensionsList.getLength() > 0) {
            appExtensions = (IIOMetadataNode) appExtensionsList.item(0);
        } else {
            appExtensions = new IIOMetadataNode("ApplicationExtensions");
            baseTree.appendChild(appExtensions);
        }

        // Create /ApplicationExtensions/ApplicationExtension
        final IIOMetadataNode appExtensionNode =
                new IIOMetadataNode("ApplicationExtension");
        appExtensionNode.setAttribute("applicationID", "NETSCAPE");
        appExtensionNode.setAttribute("authenticationCode", "2.0");
        int loopCount = (gifNativeMetadata != null) ?
                gifNativeMetadata.getLoopCount() : 1;
        appExtensionNode.setUserObject(new byte[]{
                1,
                (byte) (loopCount & 0xFF),
                (byte) ((loopCount >> 8) & 0xFF)
        });
        appExtensions.appendChild(appExtensionNode);

        // Get the /GraphicControlExtension node, creating it if it
        // does not exist.
        final NodeList gcExtensionsList =
                baseTree.getElementsByTagName("GraphicControlExtension");
        IIOMetadataNode gcExtension;
        if (gcExtensionsList.getLength() > 0) {
            gcExtension = (IIOMetadataNode) gcExtensionsList.item(0);
        } else {
            gcExtension = new IIOMetadataNode("GraphicControlExtension");
            baseTree.appendChild(gcExtension);
        }

        // Set /GraphicControlExtension node attributes.
        gcExtension.setAttribute("disposalMethod", "restoreToBackgroundColor");
        gcExtension.setAttribute("userInputFlag", "FALSE");
        gcExtension.setAttribute("transparentColorFlag", "FALSE");
        if (gifNativeMetadata != null) {
            final int delayTime = gifNativeMetadata.getDelayTime();
            if (delayTime != 0) {
                gcExtension.setAttribute("delayTime", "" + delayTime);
            }
        }
        gcExtension.setAttribute("transparentColorIndex", "0");
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected String[] getPreferredIIOImplementations() {
        return new String[] {DEFAULT_IMAGEIO_WRITER};
    }

    @Override
    public Set<Format> getSupportedFormats() {
        return Set.of(GIFDecoder.FORMAT);
    }

    @Override
    public void encode(RenderedImage image,
                       OutputStream outputStream) throws IOException {
        final ImageWriteParam writeParam = iioWriter.getDefaultWriteParam();
        final IIOMetadata metadata = getMetadata(writeParam, image);
        final IIOImage iioImage = new IIOImage(image, null, metadata);

        try (ImageOutputStream os = ImageIO
                .createImageOutputStream(outputStream)) {
            iioWriter.setOutput(os);
            iioWriter.write(iioImage);
            os.flush(); // http://stackoverflow.com/a/14489406
        }
    }

    @Override
    public void encode(BufferedImageSequence sequence,
                       OutputStream outputStream) throws IOException {
        final ImageWriteParam writeParam = iioWriter.getDefaultWriteParam();
        final IIOMetadata metadata = getMetadata(writeParam, sequence.get(0));

        String metaFormatName = metadata.getNativeMetadataFormatName();

        IIOMetadataNode root = (IIOMetadataNode)
                metadata.getAsTree(metaFormatName);
        addStructuralMetadata(root);
        metadata.setFromTree(metaFormatName, root);

        try (ImageOutputStream os = ImageIO.
                createImageOutputStream(outputStream)) {
            iioWriter.setOutput(os);
            iioWriter.prepareWriteSequence(null);

            for (BufferedImage image : sequence) {
                final IIOImage iioImage = new IIOImage(image, null, metadata);
                iioWriter.writeToSequence(iioImage, writeParam);
            }
            iioWriter.endWriteSequence();
            os.flush();
        }
    }

}
