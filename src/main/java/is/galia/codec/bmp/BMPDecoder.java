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

package is.galia.codec.bmp;

import is.galia.codec.Decoder;
import is.galia.image.EmptyMetadata;
import is.galia.image.Format;
import is.galia.image.MediaType;
import is.galia.image.Metadata;
import is.galia.codec.AbstractImageIODecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Implementation wrapping the default JDK Image I/O BMP {@link
 * javax.imageio.ImageReader}.
 */
public final class BMPDecoder extends AbstractImageIODecoder
        implements Decoder {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(BMPDecoder.class);

    private static final Format FORMAT = new Format(
            "bmp",                                            // key
            "BMP",                                            // name
            List.of(new MediaType("image", "bmp"),
                    new MediaType("image", "x-bmp"),
                    new MediaType("image", "x-ms-bmp"),       // media types
                    new MediaType("image", "x-windows-bmp")), // media types
            List.of("bmp", "dib"),                            // extensions
            true,                                             // isRaster
            false,                                            // isVideo
            true);                                            // supportsTransparency

    @Override
    public boolean canSeek() {
        return false;
    }

    @Override
    public Format detectFormat() throws IOException {
        initializeInputStream();
        inputStream.seek(0);
        byte[] magicBytes = new byte[2];
        int b, i = 0;
        while ((b = inputStream.read()) != -1 && i < magicBytes.length) {
            magicBytes[i] = (byte) b;
            i++;
        }
        inputStream.seek(0);
        if (magicBytes[0] == 0x42 && magicBytes[1] == 0x4d) {
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
        return new String[] { "com.sun.imageio.plugins.bmp.BMPImageReader" };
    }

    @Override
    public Set<Format> getSupportedFormats() {
        return Set.of(FORMAT);
    }

    @Override
    public Metadata readMetadata(int imageIndex) throws IOException {
        // Throw any contract-required exceptions.
        getSize(imageIndex);
        return new EmptyMetadata();
    }

}
