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
import is.galia.delegate.Delegate;
import is.galia.http.Reference;
import is.galia.util.StringUtils;
import is.galia.resource.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <p>Immutable application-unique source image file/object identifier.</p>
 *
 * <p>This may be one component of a {@link MetaIdentifier}, which may include
 * additional information, such as a page number.</p>
 *
 * <h2>Input</h2>
 *
 * <p>When identifiers are supplied to the application via URIs, they must go
 * through some processing steps before they can be used (order is
 * important):</p>
 *
 * <ol>
 *     <li>URI decoding</li>
 *     <li>{@link StringUtils#decodeSlashes(String) slash decoding}</li>
 * </ol>
 *
 * <p>({@link Identifier#fromURI(String)} will handle all of
 * this.)</p>
 *
 * <h2>Output</h2>
 *
 * <p>The input steps must be reversed for output. Note that requests can
 * supply a {@link
 * AbstractResource#PUBLIC_IDENTIFIER_HEADER}
 * to suggest that the identifier supplied in a URI is different from the one
 * the user agent is seeing and supplying to a reverse proxy.</p>
 *
 * <p>So, the steps for output are:</p>
 *
 * <ol>
 *     <li>Replace the URI identifier with the one from {@link
 *     AbstractResource#PUBLIC_IDENTIFIER_HEADER},
 *     if present</li>
 *     <li>Encode slashes</li>
 *     <li>URI encoding</li>
 * </ol>
 *
 * @param value Value.
 * @see MetaIdentifier
 */
@JsonSerialize(using = Identifier.IdentifierSerializer.class)
@JsonDeserialize(using = Identifier.IdentifierDeserializer.class)
public record Identifier(String value) implements Comparable<Identifier> {

    /**
     * Deserializes a type/subtype string into an {@link Identifier}.
     */
    static class IdentifierDeserializer extends JsonDeserializer<Identifier> {
        @Override
        public Identifier deserialize(JsonParser jsonParser,
                                      DeserializationContext deserializationContext) throws IOException {
            return new Identifier(jsonParser.getValueAsString());
        }
    }

    /**
     * Serializes an {@link Identifier} as a string.
     */
    static class IdentifierSerializer extends JsonSerializer<Identifier> {
        @Override
        public void serialize(Identifier identifier,
                              JsonGenerator jsonGenerator,
                              SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeString(identifier.toString());
        }
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Identifier.class);

    /**
     * Translates the string in a raw URI path component or query into a new
     * instance.
     *
     * @param uriValue Raw URI path component or query.
     * @see MetaIdentifier#fromURI(String, Delegate)
     */
    public static Identifier fromURI(String uriValue) {
        // Decode entities.
        final String decodedComponent = Reference.decode(uriValue);
        // Decode slash substitutes.
        final String deSlashedComponent =
                StringUtils.decodeSlashes(decodedComponent);

        LOGGER.trace("Raw value: {} -> decoded: {} -> slashes substituted: {}",
                uriValue, decodedComponent, deSlashedComponent);
        return new Identifier(deSlashedComponent);
    }

    /**
     * @param value Identifier value.
     * @throws IllegalArgumentException If the given value is {@code null}.
     */
    public Identifier {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null.");
        }
    }

    @Override
    public int compareTo(Identifier identifier) {
        return toString().compareTo(identifier.toString());
    }


    /**
     * @return Value of the instance.
     */
    @Override
    public String toString() {
        return value;
    }

}
