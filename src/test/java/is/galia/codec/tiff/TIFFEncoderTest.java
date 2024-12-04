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

import is.galia.codec.AbstractEncoderTest;
import is.galia.config.Key;
import is.galia.image.Format;
import is.galia.image.Metadata;
import is.galia.operation.Encode;
import is.galia.stream.ByteArrayImageInputStream;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class TIFFEncoderTest extends AbstractEncoderTest {

    private static final boolean SAVE_IMAGES = false;

    @Override
    public TIFFEncoder newInstance() {
        Encode encode = new Encode(Format.get("tif"));
        encode.removeOption(Key.ENCODER_TIFFENCODER_COMPRESSION.key());

        TIFFEncoder encoder = new TIFFEncoder();
        encoder.setEncode(encode);
        return encoder;
    }

    /* getPreferredIIOImplementations() */

    @Test
    void getPreferredIIOImplementations() {
        String[] impls = ((TIFFEncoder) instance).getPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.tiff.TIFFImageWriter", impls[0]);
    }

    /* write() */

    @Test
    void encodeWithDeflateCompression() throws Exception {
        Encode encode = new Encode(Format.get("tif"));
        encode.setOption(Key.ENCODER_TIFFENCODER_COMPRESSION.key(), "Deflate");

        try (TIFFDecoder decoder = new TIFFDecoder();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            decoder.setSource(TestUtils.getSampleImage("tif/xmp.tif"));
            final BufferedImage image = decoder.decode(0);
            instance.setEncode(encode);
            instance.encode(image, os);
            byte[] bytes = os.toByteArray();
            if (SAVE_IMAGES) TestUtils.save(bytes, "tif");
        }
    }

    @Test
    void encodeWithLZWCompression() throws Exception {
        Encode encode = new Encode(Format.get("tif"));
        encode.setOption(Key.ENCODER_TIFFENCODER_COMPRESSION.key(), "LZW");

        try (TIFFDecoder decoder = new TIFFDecoder();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            decoder.setSource(TestUtils.getSampleImage("tif/xmp.tif"));
            final BufferedImage image = decoder.decode(0);
            instance.setEncode(encode);
            instance.encode(image, os);
            byte[] bytes = os.toByteArray();
            if (SAVE_IMAGES) TestUtils.save(bytes, "tif");
        }
    }

    @Test
    void encodeWithNoCompression() throws Exception {
        Encode encode = new Encode(Format.get("tif"));
        encode.removeOption(Key.ENCODER_TIFFENCODER_COMPRESSION.key());

        try (TIFFDecoder decoder = new TIFFDecoder();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            decoder.setSource(TestUtils.getSampleImage("tif/xmp.tif"));
            final BufferedImage image = decoder.decode(0);
            instance.setEncode(encode);
            instance.encode(image, os);
            byte[] bytes = os.toByteArray();
            if (SAVE_IMAGES) TestUtils.save(bytes, "tif");
        }
    }

    @Test
    void encodeWithPackBitsCompression() throws Exception {
        Encode encode = new Encode(Format.get("tif"));
        encode.setOption(Key.ENCODER_TIFFENCODER_COMPRESSION.key(), "PackBits");

        try (TIFFDecoder decoder = new TIFFDecoder();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            decoder.setSource(TestUtils.getSampleImage("tif/xmp.tif"));
            final BufferedImage image = decoder.decode(0);
            instance.setEncode(encode);
            instance.encode(image, os);
            byte[] bytes = os.toByteArray();
            if (SAVE_IMAGES) TestUtils.save(bytes, "tif");
        }
    }

    @Test
    void encodeWithZLibCompression() throws Exception {
        Encode encode = new Encode(Format.get("tif"));
        encode.setOption(Key.ENCODER_TIFFENCODER_COMPRESSION.key(), "ZLib");

        try (TIFFDecoder decoder = new TIFFDecoder();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            decoder.setSource(TestUtils.getSampleImage("tif/xmp.tif"));
            final BufferedImage image = decoder.decode(0);
            instance.setEncode(encode);
            instance.encode(image, os);
            byte[] bytes = os.toByteArray();
            if (SAVE_IMAGES) TestUtils.save(bytes, "tif");
        }
    }

    @Test
    void encodeWithXMPMetadata() throws Exception {
        try (TIFFDecoder decoder = new TIFFDecoder();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            decoder.setSource(TestUtils.getSampleImage("tif/xmp.tif"));
            final Metadata metadata   = decoder.readMetadata(0);
            final BufferedImage image = decoder.decode(0);
            Encode encode             = new Encode(Format.get("tif"));
            encode.setXMP(metadata.getXMP().orElseThrow());
            instance.setEncode(encode);
            instance.encode(image, os);
            byte[] bytes = os.toByteArray();
            if (SAVE_IMAGES) TestUtils.save(bytes, "tif");
            checkForXMPMetadata(bytes);
        }
    }

    private void checkForXMPMetadata(byte[] imageData) throws Exception {
        final DirectoryReader reader = new DirectoryReader();
        final Tag xmpTag = new Tag(700, "XMP", false);
        try (ImageInputStream iis = new ByteArrayImageInputStream(imageData)) {
            reader.setSource(iis);
            TagSet tagSet = new EXIFBaselineTIFFTagSet();
            tagSet.addTag(xmpTag);
            reader.addTagSet(tagSet);
            DirectoryIterator it = reader.iterator();
            Directory dir        = it.next();
            assertNotNull(dir.getField(xmpTag));
        }
    }

}
