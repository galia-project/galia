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

import is.galia.codec.AbstractEncoderTest;
import is.galia.codec.Decoder;
import is.galia.codec.png.PNGDecoder;
import is.galia.image.Format;
import is.galia.operation.Encode;
import is.galia.codec.BufferedImageSequence;
import is.galia.stream.ByteArrayImageInputStream;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.w3c.dom.NamedNodeMap;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

class GIFEncoderTest extends AbstractEncoderTest {

    private static final boolean SAVE_IMAGES = false;

    private BufferedImage bufferedImage;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        Path fixture = TestUtils.getSampleImage("png/rgb-64x56x8.png");
        try (PNGDecoder decoder = new PNGDecoder()) {
            decoder.setSource(fixture);
            bufferedImage = decoder.decode(0);
        }
    }

    @Override
    protected GIFEncoder newInstance() {
        GIFEncoder encoder = new GIFEncoder();
        Encode encode      = new Encode(Format.get("gif"));
        encoder.setEncode(encode);
        return encoder;
    }

    /* getPreferredIIOImplementations() */

    @Test
    void getPreferredIIOImplementations() {
        String[] impls = ((GIFEncoder) instance).getPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.gif.GIFImageWriter", impls[0]);
    }

    /* write() */

    @Test
    void encodeWithBufferedImage() throws Exception {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            instance.encode(bufferedImage, os);
            byte[] imageBytes = os.toByteArray();
            if (SAVE_IMAGES) TestUtils.save(imageBytes, "gif");
        }
    }

    @Test
    @Disabled // this encoder doesn't support XMP metadata.
    void encodeWithBufferedImageWritesXMPMetadata()  throws Exception {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            Encode encode = new Encode(Format.get("gif"));
            encode.setXMP("<rdf:RDF></rdf:RDF>");
            instance.setEncode(encode);
            instance.encode(bufferedImage, os);
            byte[] imageBytes = os.toByteArray();
            if (SAVE_IMAGES) TestUtils.save(imageBytes, "gif");
            checkForMetadata(imageBytes);
        }
    }

    @Test
    void encodeWithSequence() throws Exception {
        Path fixture = TestUtils.getSampleImage("gif/animated-looping.gif");
        try (Decoder decoder = new GIFDecoder()) {
            decoder.setSource(fixture);
            BufferedImageSequence sequence = decoder.decodeSequence();

            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                instance.encode(sequence, os);
                byte[] imageBytes = os.toByteArray();
                if (SAVE_IMAGES) TestUtils.save(imageBytes, "gif");

                try (ImageInputStream is = new ByteArrayImageInputStream(imageBytes);
                     Decoder decoder2 = new GIFDecoder()) {
                    decoder2.setSource(is);
                    assertEquals(2, decoder2.getNumImages());
                }
            }
        }
    }

    private void checkForMetadata(byte[] imageBytes) throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("GIF");
        final ImageReader reader = readers.next();
        try (ImageInputStream iis = new ByteArrayImageInputStream(imageBytes)) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final IIOMetadataNode tree = (IIOMetadataNode)
                    metadata.getAsTree(metadata.getNativeMetadataFormatName());

            final NamedNodeMap attrs =
                    tree.getElementsByTagName("ApplicationExtensions").item(0).
                            getChildNodes().item(0).getAttributes();
            assertEquals("XMP Data", attrs.getNamedItem("applicationID").getNodeValue());
            assertEquals("XMP", attrs.getNamedItem("authenticationCode").getNodeValue());
        } finally {
            reader.dispose();
        }
    }

}
