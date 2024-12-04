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

import is.galia.codec.Decoder;
import is.galia.image.Format;
import is.galia.image.MediaType;
import is.galia.image.Metadata;
import is.galia.codec.AbstractImageIODecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOMetadata;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Implementation wrapping the default JDK Image I/O PNG {@link
 * javax.imageio.ImageReader}.
 */
public final class PNGDecoder extends AbstractImageIODecoder
        implements Decoder {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PNGDecoder.class);

    static final Format FORMAT = new Format(
            "png",                                    // key
            "PNG",                                    // name
            List.of(new MediaType("image", "png"),    // media types
                    new MediaType("image", "x-png")), // media types
            List.of("png", "apng"),                   // extensions
            true,                                     // isRaster
            false,                                    // isVideo
            true);                                    // supportsTransparency

    @Override
    public boolean canSeek() {
        return false;
    }

    @Override
    public Format detectFormat() throws IOException {
        initializeInputStream();
        inputStream.seek(0);
        byte[] magicBytes = new byte[8];
        int b, i = 0;
        while ((b = inputStream.read()) != -1 && i < magicBytes.length) {
            magicBytes[i] = (byte) b;
            i++;
        }
        inputStream.seek(0);
        if (magicBytes[0] == (byte) 0x89 &&
                magicBytes[1] == 0x50 &&
                magicBytes[2] == 0x4e &&
                magicBytes[3] == 0x47 &&
                magicBytes[4] == 0x0d &&
                magicBytes[5] == 0x0a &&
                magicBytes[6] == 0x1a &&
                magicBytes[7] == 0x0a) {
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
        return new String[] { "com.sun.imageio.plugins.png.PNGImageReader" };
    }

    @Override
    public Set<Format> getSupportedFormats() {
        return Set.of(FORMAT);
    }

    @Override
    public Metadata readMetadata(int imageIndex) throws IOException {
        // Throw any contract-required exceptions.
        getSize(imageIndex);
        final IIOMetadata metadata = iioReader.getImageMetadata(0);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new PNGMetadata(metadata, metadataFormat);
    }

}
