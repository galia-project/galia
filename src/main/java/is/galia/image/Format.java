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

import com.fasterxml.jackson.annotation.JsonProperty;
import is.galia.source.Source;

import java.util.List;
import java.util.Set;

/**
 * File format.
 *
 * @param key                  Unique format key, used internally to identify
 *                             formats but not relevant outside of the
 *                             application.
 * @param name                 Human-readable name.
 * @param mediaTypes           All media types associated with this format, in
 *                             descending preference order.
 * @param extensions           All extensions associated with this format, in
 *                             descending preference order.
 * @param isRaster             Whether the format is raster (pixel-based still
 *                             image).
 * @param isVideo              Whether the format is video (moving image).
 * @param supportsTransparency Whether the format supports transparency.
 */
public record Format(
        @JsonProperty("key") String key,
        @JsonProperty("name") String name,
        @JsonProperty("mediaTypes") List<MediaType> mediaTypes,
        @JsonProperty("extensions") List<String> extensions,
        @JsonProperty("raster") boolean isRaster,
        @JsonProperty("video") boolean isVideo,
        @JsonProperty("supportsTransparency") boolean supportsTransparency)
        implements Comparable<Format> {

    /**
     * Represents an unknown format.
     */
    public static final Format UNKNOWN = new Format(
            "unknown",
            "Unknown",
            List.of(new MediaType("unknown", "unknown")),
            List.of("unknown"),
            true,
            false,
            false);

    /**
     * Alias of {@link FormatRegistry#allFormats()}.
     *
     * @return All registered formats.
     */
    public static Set<Format> all() {
        return FormatRegistry.allFormats();
    }

    /**
     * Alias of {@link FormatRegistry#formatWithKey(String)}.
     *
     * @param key One of the keys in {@literal formats.yml}.
     * @return    Instance corresponding to the given argument, or {@code null}
     *            if no such format exists.
     */
    public static Format get(String key) {
        return FormatRegistry.formatWithKey(key);
    }

    /**
     * <p>Attempts to infer a format from the given identifier.</p>
     *
     * <p>It is usually more reliable (but also maybe more expensive) to
     * obtain this information from {@link Source#getFormatIterator()}.</p>
     *
     * @param identifier
     * @return The source format corresponding to the given identifier,
     *         assuming that its value will have a recognizable filename
     *         extension. If not, {@link #UNKNOWN} is returned.
     */
    public static Format inferFormat(Identifier identifier) {
        return inferFormat(identifier.toString());
    }

    /**
     * <p>Attempts to infer a format from the given pathname.</p>
     *
     * <p>It is usually more reliable (but also maybe more expensive) to
     * obtain this information from {@link Source#getFormatIterator()}.</p>
     *
     * @param pathname Full pathname of an image file.
     * @return The source format corresponding to the given identifier,
     *         assuming that its value will have a recognizable filename
     *         extension. If not, {@link #UNKNOWN} is returned.
     */
    public static Format inferFormat(String pathname) {
        String extension = null;
        int i = pathname.lastIndexOf('.');
        if (i > 0) {
            extension = pathname.substring(i + 1);
        }
        if (extension != null) {
            extension = extension.toLowerCase();
            for (Format format : Format.all()) {
                if (format.extensions().contains(extension)) {
                    return format;
                }
            }
        }
        return Format.UNKNOWN;
    }

    /**
     * @return Format in the {@link FormatRegistry registry} with the given
     *         extension.
     */
    public static Format withExtension(String extension) {
        if (extension.startsWith(".")) {
            extension = extension.substring(1);
        }
        final String lcext = extension.toLowerCase();
        return all()
                .stream()
                .filter(f -> f.extensions().contains(lcext))
                .findAny()
                .orElse(null);
    }

    /**
     * Compares by case-insensitive name.
     */
    @Override
    public int compareTo(Format o) {
        return name().compareToIgnoreCase(o.name());
    }

    /**
     * @return The most appropriate extension for the format.
     */
    public String getPreferredExtension() {
        return extensions().getFirst();
    }

    /**
     * @return The most appropriate media type for the format.
     */
    public MediaType getPreferredMediaType() {
        return mediaTypes().getFirst();
    }

    /**
     * @return Preferred extension.
     */
    @Override
    public String toString() {
        return getPreferredExtension();
    }

}
