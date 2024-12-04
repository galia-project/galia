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

package is.galia.codec;

import is.galia.image.Format;
import is.galia.plugin.Plugin;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;

/**
 * Tries to detect a file's format by reading its signature, a.k.a. magic
 * bytes, and consulting {@link DecoderFactory#getAllDecoders() every available
 * decoder} to find out whether it can {@link Decoder#detectFormat infer a
 * format} from those bytes.
 */
public final class FormatDetector {

    /**
     * Number of bytes that are recommended to read when reading may be
     * expensive.
     */
    public static final int RECOMMENDED_READ_LENGTH = 32;

    /**
     * @param inputStream Stream to probe.
     * @return Format of the given magic bytes, if the format is supported;
     *         otherwise {@link Format#UNKNOWN}.
     */
    public static Format detect(ImageInputStream inputStream)
            throws IOException {
        ByteOrder initialByteOrder = inputStream.getByteOrder();
        for (Decoder decoder : DecoderFactory.getAllDecoders()) {
            try (decoder) {
                if (decoder instanceof Plugin plugin) {
                    plugin.initializePlugin();
                }
                inputStream.setByteOrder(initialByteOrder);
                inputStream.seek(0);
                decoder.setSource(inputStream);
                Format format = decoder.detectFormat();
                if (!Format.UNKNOWN.equals(format)) {
                    return format;
                }
            }
        }
        return Format.UNKNOWN;
    }

    /**
     * @param path File to probe.
     * @return Format of the given magic bytes, if the format is supported;
     *         otherwise {@link Format#UNKNOWN}.
     */
    public static Format detect(Path path) throws IOException {
        for (Decoder decoder : DecoderFactory.getAllDecoders()) {
            try (decoder) {
                if (decoder instanceof Plugin plugin) {
                    plugin.initializePlugin();
                }
                decoder.setSource(path);
                Format format = decoder.detectFormat();
                if (!Format.UNKNOWN.equals(format)) {
                    return format;
                }
            }
        }
        return Format.UNKNOWN;
    }

    private FormatDetector() {}

}
