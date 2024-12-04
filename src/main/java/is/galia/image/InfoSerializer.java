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
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import is.galia.Application;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;

/**
 * Serializes an {@link Info}.
 */
final class InfoSerializer extends JsonSerializer<Info> {

    static final String APPLICATION_VERSION_KEY     = "applicationVersion";
    static final String IDENTIFIER_KEY              = "identifier";
    static final String IMAGES_KEY                  = "images";
    static final String MEDIA_TYPE_KEY              = "mediaType";
    static final String NUM_RESOLUTIONS_KEY         = "numResolutions";
    static final String SERIALIZATION_TIMESTAMP_KEY = "serializationTimestamp";
    static final String SERIALIZATION_VERSION_KEY   = "serializationVersion";

    @Override
    public void serialize(Info info,
                          JsonGenerator generator,
                          SerializerProvider serializerProvider) throws IOException {
        generator.writeStartObject();
        // application version
        generator.writeStringField(APPLICATION_VERSION_KEY,
                Application.getVersion().toString());
        // serialization version
        generator.writeNumberField(SERIALIZATION_VERSION_KEY,
                Info.Serialization.CURRENT.getVersion());
        // serialization timestamp
        generator.writeStringField(SERIALIZATION_TIMESTAMP_KEY,
                Instant.now().toString());
        // identifier
        if (info.getIdentifier() != null) {
            generator.writeStringField(IDENTIFIER_KEY,
                    info.getIdentifier().toString());
        }
        // mediaType
        if (info.getMediaType() != null) {
            generator.writeStringField(MEDIA_TYPE_KEY,
                    info.getMediaType().toString());
        }
        // numResolutions
        generator.writeNumberField(NUM_RESOLUTIONS_KEY,
                info.getNumResolutions());
        // images
        generator.writeArrayFieldStart(IMAGES_KEY);
        info.getImages().forEach(image -> {
            try {
                generator.writeObject(image);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        generator.writeEndArray();
        generator.writeEndObject();
    }

}
