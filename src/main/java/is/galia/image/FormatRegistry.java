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

import is.galia.codec.DecoderFactory;
import is.galia.codec.EncoderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Provides access to the master registry of {@link Format}s.</p>
 *
 * <p>The registry is seeded early in the application lifecycle by {@link
 * is.galia.codec.DecoderFactory} with the supported formats of all codecs.</p>
 *
 * @see is.galia.codec.FormatDetector
 */
public final class FormatRegistry {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(FormatRegistry.class);

    private static final Map<String, Format> FORMATS =
            new ConcurrentHashMap<>();

    private static final AtomicBoolean scannedForFormats = new AtomicBoolean();

    public static void addFormat(Format format) {
        LOGGER.debug("Adding format: {}", format);
        FORMATS.put(format.key(), format);
    }

    /**
     * @return Unmodifiable set of all formats.
     */
    public static Set<Format> allFormats() {
        scanForFormats();
        return Set.copyOf(FORMATS.values());
    }

    /**
     * @param mediaType Media type.
     * @return          Whether any formats in the registry have the given
     *                  media type.
     */
    public static boolean containsMediaType(MediaType mediaType) {
        return FORMATS.values().stream()
                .anyMatch(f -> f.mediaTypes().contains(mediaType));
    }

    /**
     * @param key Format {@link Format#key() key}.
     * @return    Format with the given key, or {@code null} if no such format
     *            is {@link #allFormats() registered}.
     */
    public static Format formatWithKey(String key) {
        scanForFormats();
        return FORMATS.get(key);
    }

    /**
     * Adds all formats for which support has been declared by any decoder or
     * encoder.
     */
    private static void scanForFormats() {
        if (!scannedForFormats.getAndSet(true)) {
            // Add all formats supported by any decoder.
            DecoderFactory.getAllDecoders().forEach(d ->
                    d.getSupportedFormats().forEach(FormatRegistry::addFormat));
            // Add all formats supported by any encoder.
            EncoderFactory.getAllEncoders().forEach(e ->
                    e.getSupportedFormats().forEach(FormatRegistry::addFormat));
        }
    }

    /**
     * For testing only.
     */
    static void reset() {
        FORMATS.clear();
        scannedForFormats.set(false);
    }

    private FormatRegistry() {}

}
