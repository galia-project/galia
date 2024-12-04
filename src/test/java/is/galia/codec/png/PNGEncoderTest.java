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

import is.galia.image.Format;
import is.galia.image.Metadata;
import is.galia.operation.Encode;
import is.galia.codec.AbstractEncoderTest;
import is.galia.stream.ByteArrayImageInputStream;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

class PNGEncoderTest extends AbstractEncoderTest {

    private static final boolean SAVE_IMAGES = false;

    @Override
    protected PNGEncoder newInstance() {
        PNGEncoder encoder = new PNGEncoder();
        encoder.setEncode(new Encode(Format.get("png")));
        return encoder;
    }

    /* getPreferredIIOImplementations() */

    @Test
    public void getPreferredIIOImplementations() {
        String[] impls = ((PNGEncoder) instance).getPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.png.PNGImageWriter", impls[0]);
    }

    /* write() */

    @Test
    public void encodeWithBufferedImage() throws Exception {
        try (final PNGDecoder decoder = new PNGDecoder();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            decoder.setSource(TestUtils.getSampleImage("png/xmp.png"));
            final Metadata metadata = decoder.readMetadata(0);
            final BufferedImage image = decoder.decode(0);

            Encode encode = new Encode(Format.get("png"));
            encode.setXMP(metadata.getXMP().orElseThrow());
            instance.setEncode(encode);
            instance.encode(image, os);
            byte[] bytes = os.toByteArray();

            if (SAVE_IMAGES) TestUtils.save(bytes, "png");
        }
    }

    @Test
    public void encodeWithBufferedImageAndNativeMetadata()  throws Exception {
        try (final PNGDecoder decoder = new PNGDecoder();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            decoder.setSource(TestUtils.getSampleImage("png/nativemetadata.png"));
            final Metadata srcMetadata = decoder.readMetadata(0);
            final BufferedImage image = decoder.decode(0);

            instance.close();
            instance = newInstance();
            Encode encode = new Encode(Format.get("png"));
            encode.setNativeMetadata(srcMetadata.getNativeMetadata().orElseThrow());
            instance.setEncode(encode);
            instance.encode(image, os);
            byte[] bytes = os.toByteArray();

            checkForNativeMetadata(bytes);
            if (SAVE_IMAGES) TestUtils.save(bytes, "png");
        }
    }

    @Test
    public void encodeWithBufferedImageAndXMPMetadata()  throws Exception {
        try (final PNGDecoder decoder = new PNGDecoder();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            decoder.setSource(TestUtils.getSampleImage("png/xmp.png"));
            final Metadata metadata = decoder.readMetadata(0);
            final BufferedImage image = decoder.decode(0);

            instance.close();
            instance = newInstance();
            Encode encode = new Encode(Format.get("png"));
            encode.setXMP(metadata.getXMP().orElseThrow());
            instance.setEncode(encode);
            instance.encode(image, os);
            byte[] bytes = os.toByteArray();

            checkForXMPMetadata(bytes);
            if (SAVE_IMAGES) TestUtils.save(bytes, "png");
        }
    }

    private void checkForNativeMetadata(byte[] imageData) throws Exception {
        final ImageReader reader = getIIOReader();
        try (ImageInputStream iis = new ByteArrayImageInputStream(imageData)) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final IIOMetadataNode tree = (IIOMetadataNode)
                    metadata.getAsTree(metadata.getNativeMetadataFormatName());

            boolean found = false;
            final NodeList textNodes = tree.getElementsByTagName("tEXt").
                    item(0).getChildNodes();
            for (int i = 0; i < textNodes.getLength(); i++) {
                final Node keywordAttr = textNodes.item(i).getAttributes().
                        getNamedItem("keyword");
                if (keywordAttr != null) {
                    if ("Title".equals(keywordAttr.getNodeValue())) {
                        found = true;
                        break;
                    }
                }
            }
            assertTrue(found);
        } finally {
            reader.dispose();
        }
    }

    private void checkForXMPMetadata(byte[] imageData) throws Exception {
        final ImageReader reader = getIIOReader();
        try (ImageInputStream iis = new ByteArrayImageInputStream(imageData)) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final IIOMetadataNode tree = (IIOMetadataNode)
                    metadata.getAsTree(metadata.getNativeMetadataFormatName());

            boolean found = false;
            final NodeList textNodes = tree.getElementsByTagName("iTXt").
                    item(0).getChildNodes();
            for (int i = 0; i < textNodes.getLength(); i++) {
                final Node keywordAttr = textNodes.item(i).getAttributes().
                        getNamedItem("keyword");
                if (keywordAttr != null) {
                    if ("XML:com.adobe.xmp".equals(keywordAttr.getNodeValue())) {
                        found = true;
                        break;
                    }
                }
            }
            assertTrue(found);
        } finally {
            reader.dispose();
        }
    }

    private ImageReader getIIOReader() {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("PNG");
        while (readers.hasNext()) {
            ImageReader reader = readers.next();
            String readerName = reader.getClass().getName();
            try (PNGDecoder decoder = new PNGDecoder()) {
                String preferredName = decoder.
                        getPreferredIIOImplementations()[0];
                if (readerName.equals(preferredName)) {
                    return reader;
                }
            }
        }
        return null;
    }

}
