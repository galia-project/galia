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

import is.galia.codec.AbstractEncoderTest;
import is.galia.config.Key;
import is.galia.image.Format;
import is.galia.image.Metadata;
import is.galia.operation.Encode;
import is.galia.stream.ByteArrayImageInputStream;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Test;
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

class JPEGEncoderTest extends AbstractEncoderTest {

    private static final boolean SAVE_IMAGES = false;

    @Override
    protected JPEGEncoder newInstance() {
        JPEGEncoder encoder = new JPEGEncoder();
        encoder.setEncode(new Encode(Format.get("jpg")));
        return encoder;
    }

    /* getPreferredIIOImplementations() */

    @Test
    void getPreferredIIOImplementations() {
        String[] impls = ((JPEGEncoder) instance).getPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.jpeg.JPEGImageWriter", impls[0]);
    }

    /* write() */

    @Test
    void encode() throws Exception {
        try (JPEGDecoder decoder = new JPEGDecoder();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            decoder.setSource(TestUtils.getSampleImage("jpg/xmp.jpg"));

            final Metadata metadata   = decoder.readMetadata(0);
            final BufferedImage image = decoder.decode(0);
            final Encode encode       = new Encode(Format.get("jpg"));
            encode.setXMP(metadata.getXMP().orElseThrow());
            instance.setEncode(encode);
            instance.encode(image, os);
            byte[] bytes = os.toByteArray();

            if (SAVE_IMAGES) TestUtils.save(bytes, "jpg");
        }
    }

    @Test
    void encodeWithXMPMetadata() throws Exception {
        try (JPEGDecoder decoder = new JPEGDecoder();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            decoder.setSource(TestUtils.getSampleImage("jpg/xmp.jpg"));

            final Metadata metadata   = decoder.readMetadata(0);
            final BufferedImage image = decoder.decode(0);
            final Encode encode       = new Encode(Format.get("jpg"));
            encode.setXMP(metadata.getXMP().orElseThrow());
            instance.setEncode(encode);
            instance.encode(image, os);
            byte[] bytes = os.toByteArray();

            if (SAVE_IMAGES) TestUtils.save(bytes, "jpg");
            checkForXMPMetadata(bytes);
        }
    }

    @Test
    void encodeRespectsQFactorOption() throws Exception {
        // Write two different images with two different Q factors and
        // assert that the one with the larger Q factor is larger than the
        // other. (This is easier than reconstructing the Q factors from
        // the quantization tables.)
        try (JPEGDecoder decoder = new JPEGDecoder();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            decoder.setSource(TestUtils.getSampleImage("jpg/jpg.jpg"));
            final BufferedImage image = decoder.decode(0);

            // Write the first image with Q factor 95
            final Encode encode = new Encode(Format.get("jpg"));
            encode.setOption(Key.ENCODER_JPEGENCODER_QUALITY.key(), 95);

            instance.setEncode(encode);
            instance.encode(image, os);
            byte[] image1Bytes = os.toByteArray();
            if (SAVE_IMAGES) TestUtils.save(image1Bytes, "jpg");

            // Write the second image with Q factor 25
            try (ByteArrayOutputStream os2 = new ByteArrayOutputStream()) {
                encode.setOption(Key.ENCODER_JPEGENCODER_QUALITY.key(), 25);
                instance.encode(image, os2);

                byte[] image2Bytes = os2.toByteArray();

                assertTrue(image1Bytes.length - image2Bytes.length > 1000);
            }
        }
    }

    @Test
    void encodeRespectsProgressiveOptionWhenFalse() throws Exception {
        try (JPEGDecoder decoder = new JPEGDecoder();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            decoder.setSource(TestUtils.getSampleImage("jpg/jpg.jpg"));
            final BufferedImage image = decoder.decode(0);
            final Encode encode       = new Encode(Format.get("jpg"));
            encode.setOption(Key.ENCODER_JPEGENCODER_PROGRESSIVE.key(), false);

            instance.setEncode(encode);
            instance.encode(image, os);
            byte[] imageBytes = os.toByteArray();
            if (SAVE_IMAGES) TestUtils.save(imageBytes, "jpg");

            try (ImageInputStream inputStream = new ByteArrayImageInputStream(imageBytes)) {
                JPEGMetadataReader reader = new JPEGMetadataReader();
                reader.setSource(inputStream);
                assertFalse(reader.isProgressive());
            }
        }
    }

    @Test
    void encodeRespectsProgressiveOptionWhenTrue() throws Exception {
        try (JPEGDecoder decoder = new JPEGDecoder();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            decoder.setSource(TestUtils.getSampleImage("jpg/jpg.jpg"));
            final BufferedImage image = decoder.decode(0);
            final Encode encode       = new Encode(Format.get("jpg"));
            encode.setOption(Key.ENCODER_JPEGENCODER_PROGRESSIVE.key(), true);

            instance.setEncode(encode);
            instance.encode(image, os);
            byte[] imageBytes = os.toByteArray();
            if (SAVE_IMAGES) TestUtils.save(imageBytes, "jpg");

            try (ImageInputStream inputStream = new ByteArrayImageInputStream(imageBytes)) {
                JPEGMetadataReader reader = new JPEGMetadataReader();
                reader.setSource(inputStream);
                assertTrue(reader.isProgressive());
            }
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
            final NodeList unknowns = tree.getElementsByTagName("unknown");
            for (int i = 0; i < unknowns.getLength(); i++) {
                if ("225".equals(unknowns.item(i).getAttributes().
                        getNamedItem("MarkerTag").getNodeValue())) {
                    found = true;
                }
            }
            assertTrue(found);
        } finally {
            reader.dispose();
        }
    }

    private ImageReader getIIOReader() {
        try (final JPEGDecoder decoder = new JPEGDecoder()) {
            final String preferredImpl =
                    decoder.getPreferredIIOImplementations()[0];
            final Iterator<ImageReader> readers =
                    ImageIO.getImageReadersByFormatName("JPEG");
            while (readers.hasNext()) {
                ImageReader reader = readers.next();
                if (reader.getClass().getName().equals(preferredImpl)) {
                    return reader;
                }
            }
        }
        return null;
    }

}
