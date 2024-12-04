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

package is.galia.codec.png;

import is.galia.codec.Encoder;
import is.galia.image.Format;
import is.galia.codec.AbstractImageIOEncoder;
import is.galia.image.NativeMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import javax.imageio.IIOImage;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation wrapping the default JDK Image I/O PNG {@link
 * javax.imageio.ImageWriter}.
 *
 * @see <a href="http://libpng.org/pub/png/spec/1.2/PNG-Contents.html">
 *     PNG Specification, Version 1.2</a>
 */
public final class PNGEncoder extends AbstractImageIOEncoder
        implements Encoder {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PNGEncoder.class);

    @Override
    protected void addMetadata(final IIOMetadataNode baseTree) {
        Optional<NativeMetadata> optMD = encode.getNativeMetadata();
        optMD.ifPresent(metadata -> {
            if (metadata instanceof PNGNativeMetadata nativeMetadata) {
                // Get the /tEXt node, creating it if it does not already exist.
                final NodeList textNodes = baseTree.getElementsByTagName("tEXt");
                IIOMetadataNode textNode;
                if (textNodes.getLength() > 0) {
                    textNode = (IIOMetadataNode) textNodes.item(0);
                } else {
                    textNode = new IIOMetadataNode("tEXt");
                    baseTree.appendChild(textNode);
                }
                // Append the metadata.
                nativeMetadata.forEach((key, value) -> {
                    IIOMetadataNode node = new IIOMetadataNode("tEXtEntry");
                    node.setAttribute("keyword", key);
                    node.setAttribute("value", value);
                    textNode.appendChild(node);
                });
            }
        });

        // Add XMP metadata, if available.
        encode.getXMP().ifPresent(xmp -> {
            // Get the /iTXt node, creating it if it does not already exist.
            final NodeList itxtNodes = baseTree.getElementsByTagName("iTXt");
            IIOMetadataNode itxtNode;
            if (itxtNodes.getLength() > 0) {
                itxtNode = (IIOMetadataNode) itxtNodes.item(0);
            } else {
                itxtNode = new IIOMetadataNode("iTXt");
                baseTree.appendChild(itxtNode);
            }
            // Append the XMP.
            final IIOMetadataNode xmpNode = new IIOMetadataNode("iTXtEntry");
            xmpNode.setAttribute("keyword", "XML:com.adobe.xmp");
            xmpNode.setAttribute("compressionFlag", "FALSE");
            xmpNode.setAttribute("compressionMethod", "0");
            xmpNode.setAttribute("languageTag", "");
            xmpNode.setAttribute("translatedKeyword", "");
            xmpNode.setAttribute("text", xmp);
            itxtNode.appendChild(xmpNode);
        });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected String[] getPreferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.png.PNGImageWriter" };
    }

    @Override
    public Set<Format> getSupportedFormats() {
        return Set.of(PNGDecoder.FORMAT);
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
        final IIOMetadata metadata = getMetadata(
                iioWriter.getDefaultWriteParam(), image);
        final IIOImage iioImage = new IIOImage(image, null, metadata);

        // N.B.: the wrapping stream will be closed, but the wrapped one won't,
        // which is intended.
        try (ImageOutputStream os =
                     new MemoryCacheImageOutputStream(outputStream)) {
            iioWriter.setOutput(os);
            iioWriter.write(iioImage);
        }
    }

}
