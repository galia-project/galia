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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import is.galia.util.SoftwareVersion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;

/**
 * Deserializes an {@link Info}.
 */
final class InfoDeserializer extends JsonDeserializer<Info> {

    @Override
    public Info deserialize(JsonParser parser,
                            DeserializationContext deserializationContext) throws IOException {
        // N.B.: keys may or may not exist in different serializations,
        // documented inline. Even keys that are supposed to always exist may
        // not exist in tests, so we have to check for them anyway.
        final Info info     = new Info();
        final JsonNode node = parser.getCodec().readTree(parser);
        { // serializationTimestamp
            JsonNode timestampNode = node.get(InfoSerializer.SERIALIZATION_TIMESTAMP_KEY);
            if (timestampNode != null) {
                info.setSerializationTimestamp(Instant.parse(timestampNode.textValue()));
            }
        }
        { // applicationVersion
            JsonNode appVersionNode = node.get(InfoSerializer.APPLICATION_VERSION_KEY);
            if (appVersionNode != null) {
                info.setApplicationVersion(SoftwareVersion.parse(appVersionNode.textValue()));
            }
        }
        { // serializationVersion
            JsonNode serialVersionNode = node.get(InfoSerializer.SERIALIZATION_VERSION_KEY);
            if (serialVersionNode != null) {
                info.setSerializationVersion(serialVersionNode.intValue());
            }
        }
        { // identifier
            JsonNode identifierNode = node.get(InfoSerializer.IDENTIFIER_KEY);
            if (identifierNode != null) {
                info.setIdentifier(new Identifier(identifierNode.textValue()));
            }
        }
        { // mediaType
            JsonNode mediaTypeNode = node.get(InfoSerializer.MEDIA_TYPE_KEY);
            if (mediaTypeNode != null) {
                info.setMediaType(MediaType.fromString(mediaTypeNode.textValue()));
            }
        }
        { // numResolutions
            JsonNode numResolutionsNode = node.get(InfoSerializer.NUM_RESOLUTIONS_KEY);
            if (numResolutionsNode != null) {
                info.setNumResolutions(numResolutionsNode.intValue());
            }
        }
        { // images
            info.getImages().clear();
            node.get(InfoSerializer.IMAGES_KEY).elements().forEachRemaining(imageNode -> {
                try {
                    Info.Image image = new ObjectMapper().readValue(
                            imageNode.toString(), Info.Image.class);
                    info.getImages().add(image);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        return info;
    }

}
