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

import is.galia.delegate.Delegate;
import is.galia.http.Reference;
import is.galia.util.StringUtils;
import is.galia.resource.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>An {@link Identifier} is used to uniquely identify a source image file
 * or object. But a client may require more specificity than this&mdash;for
 * example, to be able to retrieve a particular page from within a multi-page
 * image, and/or at a particular scale limit. Ideally these parameters would be
 * supplied as arguments to the image request API service, but said service may
 * not support them. (This is the case for the IIIF Image API, at least,
 * through version 3.0.)</p>
 *
 * <p>Thus this class, which joins meta-information about an identifier with
 * the identifier itself, for utilization by an image-request API that doesn't
 * natively support such information.</p>
 *
 * <h2>Input</h2>
 *
 * <p>When meta-identifiers are supplied to the application via URIs, they must
 * go through some processing steps before they can be used (order is
 * important):</p>
 *
 * <ol>
 *     <li>URI decoding</li>
 *     <li>{@link StringUtils#decodeSlashes(String) slash decoding}</li>
 * </ol>
 *
 * <p>({@link MetaIdentifier#fromURI(String, Delegate)} will
 * handle all of this.)</p>
 *
 * <h2>Output</h2>
 *
 * <p>The input steps must be reversed for output. Note that requests can
 * supply a {@link AbstractResource#PUBLIC_IDENTIFIER_HEADER} to suggest that
 * the meta-identifier supplied in a URI is different from the one the user
 * agent is seeing and supplying to a reverse proxy.</p>
 *
 * <p>So, the steps for output are:</p>
 *
 * <ol>
 *     <li>Replace the URI meta-identifier with the one from {@link
 *     AbstractResource#PUBLIC_IDENTIFIER_HEADER}, if present</li>
 *     <li>Encode slashes</li>
 *     <li>URI encoding</li>
 * </ol>
 *
 * @param identifier      Identifier component.
 * @param pageNumber      Page number component, which may be {@code null}.
 * @param scaleConstraint Scale constraint component, which may be {@code
 *                        null}.
 */
public record MetaIdentifier(Identifier identifier,
                             Integer pageNumber,
                             ScaleConstraint scaleConstraint) {

    /**
     * Builds new {@link MetaIdentifier} instances.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder {

        private Identifier identifier;
        private Integer pageNumber;
        private ScaleConstraint scaleConstraint;

        private Builder() {}

        private Builder(MetaIdentifier instance) {
            this.identifier      = instance.identifier;
            this.pageNumber      = instance.pageNumber;
            this.scaleConstraint = instance.scaleConstraint;
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = new Identifier(identifier);
            return this;
        }

        public Builder withIdentifier(Identifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withPageNumber(Integer pageNumber) {
            this.pageNumber = pageNumber;
            return this;
        }

        public Builder withScaleConstraint(int numerator, int denominator) {
            this.scaleConstraint = new ScaleConstraint(numerator, denominator);
            return this;
        }

        public Builder withScaleConstraint(ScaleConstraint scaleConstraint) {
            this.scaleConstraint = scaleConstraint;
            return this;
        }

        public MetaIdentifier build() {
            if (identifier == null) {
                throw new IllegalArgumentException("Identifier cannot be null");
            } else if (pageNumber != null && pageNumber < 1) {
                throw new IllegalArgumentException("Page number must be >= 1");
            }
            return new MetaIdentifier(identifier, pageNumber, scaleConstraint);
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MetaIdentifier.class);

    public static MetaIdentifier.Builder builder() {
        return new Builder();
    }

    /**
     * <p>Deserializes the given meta-identifier string using the {@link
     * MetaIdentifierTransformer} specified in the application
     * configuration.</p>
     *
     * <p>This is a shortcut to using {@link
     * MetaIdentifierTransformerFactory}.</p>
     *
     * @return New deserialized instance.
     */
    public static MetaIdentifier fromString(String string, Delegate delegate) {
        final MetaIdentifierTransformer xformer =
                new MetaIdentifierTransformerFactory().newInstance(delegate);
        return xformer.deserialize(string);
    }

    /**
     * Translates the string in a raw URI path component or query into a new
     * instance using the {@link MetaIdentifierTransformer} specified in the
     * application configuration.
     *
     * @param uriValue Raw URI value from the path or query.
     * @param delegate Delegate.
     * @see #forURI(Delegate)
     */
    public static MetaIdentifier fromURI(String uriValue,
                                         Delegate delegate) {
        // Decode entities.
        final String decodedComponent = Reference.decode(uriValue);
        // Decode slash substitutes.
        final String deSlashedComponent =
                StringUtils.decodeSlashes(decodedComponent);
        LOGGER.trace("Raw value: {} -> decoded: {} -> slashes substituted: {}",
                uriValue, decodedComponent, deSlashedComponent);
        return fromString(deSlashedComponent, delegate);
    }

    /**
     * Creates a minimal valid instance. For more options, use {@link Builder}.
     */
    public MetaIdentifier(String identifier) {
        this(new Identifier(identifier));
    }

    /**
     * Creates a minimal valid instance. For more options, use {@link Builder}.
     */
    public MetaIdentifier(Identifier identifier) {
        this(identifier, null, null);
    }

    /**
     * @param delegate Delegate.
     * @return Serialization suitable for insertion into a URI path or
     *         query and already encoded.
     * @see #fromURI(String, Delegate)
     */
    public String forURI(Delegate delegate) {
        MetaIdentifierTransformer xformer =
                new MetaIdentifierTransformerFactory().newInstance(delegate);
        String serializedComponent = xformer.serialize(this);
        // Encode slash substitutes.
        String slashedComponent = StringUtils.encodeSlashes(serializedComponent);
        // Encode entities.
        return Reference.encode(slashedComponent);
    }

    /**
     * @return {@link Reference.Builder} of a new instance based on this one.
     */
    public Builder rebuilder() {
        return new Builder(this);
    }

    @Override
    public String toString() {
        return new StandardMetaIdentifierTransformer().serialize(this);
    }

}
