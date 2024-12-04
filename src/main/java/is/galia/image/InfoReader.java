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

package is.galia.image;

import is.galia.codec.Decoder;
import is.galia.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Reads image characteristics.
 */
public class InfoReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(InfoReader.class);

    private Decoder decoder;
    private Format format;

    /**
     * @param decoder Instance from which to read.
     */
    public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    /**
     * @param format Format of the image data supplied by the instance passed
     *               to {@link #setDecoder(Decoder)}.
     */
    public void setFormat(Format format) {
        this.format = format;
    }

    /**
     * @return New instance describing the image that the decoder has been
     *         initialized to read.
     * @throws IOException if there is an issue accessing the source image.
     * @throws IllegalStateException if a decoder or format have not been set.
     */
    public Info read() throws IOException {
        validateState();

        final Stopwatch watch = new Stopwatch();
        final Info info = new Info();
        info.getImages().clear();
        info.setSourceFormat(format);
        info.setNumResolutions(decoder.getNumResolutions());

        for (int i = 0, numImages = decoder.getNumImages(); i < numImages; i++) {
            Info.Image image = new Info.Image();
            Size size = decoder.getSize(i);
            image.setSize(size);
            Size tileSize = decoder.getTileSize(i);
            // JP2 tile dimensions are inverted, so un-invert them
            long width      = size.longWidth();
            long height     = size.longHeight();
            long tileWidth  = tileSize.longWidth();
            long tileHeight = tileSize.longHeight();
            if ((width > height && tileWidth < tileHeight) ||
                    (width < height && tileWidth > tileHeight)) {
                tileSize = tileSize.inverted();
            }
            image.setTileSize(tileSize);
            image.setMetadata(decoder.readMetadata(i));
            info.getImages().add(image);
        }
        LOGGER.trace("read(): completed in {}", watch);
        return info;
    }

    private void validateState() {
        if (decoder == null) {
            throw new IllegalStateException(
                    "Decoder is null (was setDecoder() called?)");
        } else if (format == null) {
            throw new IllegalStateException(
                    "Format is null (was setFormat() called?)");
        }
    }

}
