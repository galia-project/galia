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

package is.galia.resource.iiif.v2;

import is.galia.delegate.Delegate;
import is.galia.http.Query;
import is.galia.http.Reference;
import is.galia.image.MetaIdentifier;
import is.galia.operation.Encode;
import is.galia.operation.OperationList;
import is.galia.resource.IllegalClientArgumentException;
import is.galia.resource.iiif.FormatException;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Encapsulates the parameters of a request.
 *
 * @see <a href="http://iiif.io/api/request/2.0/#request-request-parameters">IIIF
 *      Image API 2.0</a>
 * @see <a href="http://iiif.io/api/request/2.1/#request-request-parameters">IIIF
 *      Image API 2.1</a>
 */
final class Parameters {

    private String identifier;
    private Region region;
    private Size size;
    private Rotation rotation;
    private Quality quality;
    private OutputFormat outputFormat;
    private Query query = new Query();

    /**
     * @param paramsStr URI path fragment beginning from the identifier onward.
     * @throws IllegalClientArgumentException if the argument is not in the
     *         correct format.
     */
    public static Parameters fromURI(String paramsStr) {
        Parameters params = new Parameters();
        String[] parts = StringUtils.split(paramsStr, "/");
        if (parts.length == 5) {
            params.setIdentifier(Reference.decode(parts[0]));
            params.setRegion(Region.fromURI(parts[1]));
            params.setSize(Size.fromURI(parts[2]));
            params.setRotation(Rotation.fromURI(parts[3]));
            String[] subparts = StringUtils.split(parts[4], ".");
            if (subparts.length == 2) {
                params.setQuality(Quality.valueOf(subparts[0].toUpperCase()));
                params.setOutputFormat(new OutputFormat(subparts[1].toLowerCase()));
            } else {
                throw new IllegalClientArgumentException("Invalid parameters format");
            }
        } else {
            throw new IllegalClientArgumentException("Invalid parameters format");
        }
        return params;
    }

    /**
     * No-op constructor.
     */
    Parameters() {}

    /**
     * Copy constructor.
     */
    Parameters(Parameters params) {
        setIdentifier(params.getIdentifier());
        setRegion(params.getRegion());
        setSize(params.getSize());
        setRotation(params.getRotation());
        setQuality(params.getQuality());
        setOutputFormat(params.getOutputFormat());
        setQuery(params.getQuery());
    }

    /**
     * @param identifier Decoded identifier.
     * @param region     From URI.
     * @param size       From URI.
     * @param rotation   From URI.
     * @param quality    From URI.
     * @param format     From URI.
     * @throws FormatException if the {@literal format} argument is invalid.
     * @throws IllegalClientArgumentException if any of the other arguments are
     *         invalid.
     */
    Parameters(String identifier,
               String region,
               String size,
               String rotation,
               String quality,
               String format) {
        setIdentifier(identifier);
        setRegion(Region.fromURI(region));
        setSize(Size.fromURI(size));
        setRotation(Rotation.fromURI(rotation));
        try {
            setQuality(Quality.valueOf(quality.toUpperCase()));
        } catch (IllegalArgumentException e) {
            String message = "Unsupported quality. Available qualities are: " +
                    Arrays.stream(Quality.values())
                            .map(Quality::getURIValue)
                            .collect(Collectors.joining(", ")) + ".";
            throw new IllegalClientArgumentException(message, e);
        }
        try {
            setOutputFormat(new OutputFormat(format.toLowerCase()));
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new FormatException(format);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Parameters) {
            return obj.toString().equals(toString());
        }
        return false;
    }

    public String getIdentifier() {
        return identifier;
    }

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    public Quality getQuality() {
        return quality;
    }

    /**
     * @return The URI query. This enables processors to support options and
     *         operations not available in the parameters. Query keys and
     *         values are not sanitized.
     */
    public Query getQuery() {
        return query;
    }

    public Region getRegion() {
        return region;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public Size getSize() {
        return size;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
    }

    public void setQuality(Quality quality) {
        this.quality = quality;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public void setRotation(Rotation rotation) {
        this.rotation = rotation;
    }

    public void setSize(Size size) {
        this.size = size;
    }

    /**
     * @return Analog of the request parameters for processing, excluding any
     *         additional operations that may need to be performed, such as
     *         overlays, etc.
     */
    OperationList toOperationList(Delegate delegate) {
        final OperationList ops = new OperationList(
                MetaIdentifier.fromString(getIdentifier(), delegate));
        if (!Region.Type.FULL.equals(getRegion().getType())) {
            ops.add(getRegion().toCrop());
        }
        if (!Size.ScaleMode.FULL.equals(getSize().getScaleMode()) &&
                !Size.ScaleMode.MAX.equals(getSize().getScaleMode())) {
            ops.add(getSize().toScale());
        }
        ops.add(getRotation().toTranspose());
        if (getRotation().getDegrees() != 0) {
            ops.add(getRotation().toRotate());
        }
        ops.add(getQuality().toColorTransform());
        ops.add(new Encode(getOutputFormat().toFormat()));
        return ops;
    }

    /**
     * @return URI parameters with no leading slash.
     * @see #toCanonicalString(is.galia.image.Size)
     * @see <a href="https://iiif.io/api/image/2.1/#image-request-uri-syntax">
     *     Image Request URI Syntax</a>
     */
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append(getIdentifier());
        b.append("/");
        b.append(getRegion());
        b.append("/");
        b.append(getSize());
        b.append("/");
        b.append(getRotation());
        b.append("/");
        b.append(getQuality().toString().toLowerCase());
        b.append(".");
        b.append(getOutputFormat());
        if (!getQuery().isEmpty()) {
            b.append("?");
            b.append(getQuery().toString());
        }
        return b.toString();
    }

    /**
     * @param fullSize Full source image dimensions.
     * @return         Canonicalized URI parameters with no leading slash.
     * @see            #toString()
     * @see            <a href="https://iiif.io/api/image/2.1/#canonical-uri-syntax">
     *                 Canonical URI Syntax</a>
     */
    String toCanonicalString(is.galia.image.Size fullSize) {
        final StringBuilder b = new StringBuilder();
        b.append(getIdentifier());
        b.append("/");
        b.append(getRegion().toCanonicalString(fullSize));
        b.append("/");
        b.append(getSize().toCanonicalString(fullSize));
        b.append("/");
        b.append(getRotation().toCanonicalString());
        b.append("/");
        b.append(getQuality().toString().toLowerCase());
        b.append(".");
        b.append(getOutputFormat());
        if (!getQuery().isEmpty()) {
            b.append("?");
            b.append(getQuery().toString());
        }
        return b.toString();
    }

}
