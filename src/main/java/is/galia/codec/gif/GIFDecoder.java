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

import is.galia.codec.Decoder;
import is.galia.codec.DecoderHint;
import is.galia.image.Format;
import is.galia.image.MediaType;
import is.galia.image.Metadata;
import is.galia.codec.AbstractImageIODecoder;
import is.galia.image.ReductionFactor;
import is.galia.image.Region;
import is.galia.processor.Java2DUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Implementation wrapping the default JDK Image I/O GIF {@link
 * javax.imageio.ImageReader}.
 */
public final class GIFDecoder extends AbstractImageIODecoder
        implements Decoder {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GIFDecoder.class);

    static final Format FORMAT = new Format(
            "gif",                                  // key
            "GIF",                                  // name
            List.of(new MediaType("image", "gif")), // media types
            List.of("gif"),                         // extensions
            true,                                   // isRaster
            false,                                  // isVideo
            true);                                  // supportsTransparency

    @Override
    public boolean canSeek() {
        return false;
    }

    @Override
    public Format detectFormat() throws IOException {
        initializeInputStream();
        inputStream.seek(0);
        byte[] magicBytes = new byte[6];
        int b, i = 0;
        while ((b = inputStream.read()) != -1 && i < magicBytes.length) {
            magicBytes[i] = (byte) b;
            i++;
        }
        inputStream.seek(0);
        if ((magicBytes[0] == 0x47 && magicBytes[1] == 0x49 &&
                magicBytes[2] == 0x46 && magicBytes[3] == 0x38 &&
                magicBytes[4] == 0x37 && magicBytes[5] == 0x61) || // 87a
                (magicBytes[0] == 0x47 && magicBytes[1] == 0x49 &&
                        magicBytes[2] == 0x46 && magicBytes[3] == 0x38 &&
                        magicBytes[4] == 0x39 && magicBytes[5] == 0x61)) { // 89a
            return FORMAT;
        }
        return Format.UNKNOWN;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected String[] getPreferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.gif.GIFImageReader" };
    }

    @Override
    public Set<Format> getSupportedFormats() {
        return Set.of(FORMAT);
    }

    @Override
    public BufferedImage decode(int imageIndex) throws IOException {
        BufferedImage image = super.decode(imageIndex);
        image = Java2DUtils.convertIndexedToARGB(image);
        return image;
    }

    @Override
    public BufferedImage decode(int imageIndex,
                                Region region,
                                double[] scales,
                                ReductionFactor reductionFactor,
                                double[] diffScales,
                                Set<DecoderHint> decoderHints) throws IOException {
        BufferedImage image = super.decode(imageIndex, region, scales,
                reductionFactor, diffScales, decoderHints);
        image = Java2DUtils.convertIndexedToARGB(image);
        return image;
    }

    @Override
    public Metadata readMetadata(int imageIndex) throws IOException {
        // Throw any contract-required exceptions.
        getSize(imageIndex);

        // The GIFMetadata is going to read from the GIFMetadataReader which is
        // going to read from inputStream. But, this reader isn't done reading
        // from inputStream. So, we need to reset the current position when
        // we're done.
        long pos = inputStream.getStreamPosition();
        inputStream.seek(0);

        GIFMetadataReader reader = new GIFMetadataReader();
        reader.setSource(inputStream);
        // Call one of GIFMetadataReader's reader methods to read from it...
        try {
            reader.getXMP();
        } catch (IOException e) {
            // If XMP-reading fails, we don't want the whole response to fail.
            LOGGER.info("readMetadata(): {}", e.getMessage());
        }
        // and then go back to where we were.
        inputStream.seek(pos);
        // This is horrible of course, but it'll work for now.

        return new GIFMetadata(reader);
    }

}
