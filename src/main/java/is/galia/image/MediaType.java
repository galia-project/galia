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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;

/**
 * IANA media (a.k.a. MIME) type.
 *
 * @param type    Type, i.e. the part before the slash.
 * @param subtype Subtype, i.e. the part after the slash.
 * @see <a href="https://www.iana.org/assignments/media-types/media-types.xhtml">
 *     Media Types</a>
 */
@JsonSerialize(using = MediaType.MediaTypeSerializer.class)
@JsonDeserialize(using = MediaType.MediaTypeDeserializer.class)
public record MediaType(String type, String subtype) {

    /**
     * Deserializes a type/subtype string into a {@link MediaType}.
     */
    static class MediaTypeDeserializer extends JsonDeserializer<MediaType> {
        @Override
        public MediaType deserialize(
                JsonParser jsonParser,
                DeserializationContext deserializationContext) throws IOException {
            return MediaType.fromString(jsonParser.getValueAsString());
        }
    }

    /**
     * Serializes a {@link MediaType} as a type/subtype string.
     */
    static class MediaTypeSerializer extends JsonSerializer<MediaType> {
        @Override
        public void serialize(
                MediaType mediaType,
                JsonGenerator jsonGenerator,
                SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeString(mediaType.toString());
        }
    }

    public static final MediaType APPLICATION_JSON =
            new MediaType("application", "json");
    public static final MediaType TEXT_PLAIN =
            new MediaType("text", "plain");

    /**
     * @param contentType {@literal Content-Type} header value.
     * @return            Media type corresponding to the given header value.
     * @throws IllegalArgumentException if the format of the argument is
     *                    illegal.
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.5">RFC
     *      7231</a>
     */
    public static MediaType fromContentType(String contentType) {
        String[] parts = contentType.split(";");
        if (parts.length > 0) {
            return MediaType.fromString(parts[0].trim());
        }
        throw new IllegalArgumentException("Unrecognized Content-Type");
    }

    /**
     * @param mediaType
     * @throws IllegalArgumentException if the given string is not a media
     *                                  type.
     */
    public static MediaType fromString(String mediaType) {
        String[] parts = mediaType.split("/");
        if (parts.length == 2) {
            return new MediaType(parts[0], parts[1]);
        } else {
            throw new IllegalArgumentException("Invalid media type: " + mediaType);
        }
    }

    /**
     * @return Format corresponding to the instance.
     */
    public Format toFormat() {
        for (Format enumValue : Format.all()) {
            for (MediaType type : enumValue.mediaTypes()) {
                if (type.equals(this)) {
                    return enumValue;
                }
            }
        }
        return Format.UNKNOWN;
    }

    @Override
    public String toString() {
        return type + "/" + subtype;
    }

}
