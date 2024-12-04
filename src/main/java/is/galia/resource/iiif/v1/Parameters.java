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

package is.galia.resource.iiif.v1;

import is.galia.delegate.Delegate;
import is.galia.http.Reference;
import is.galia.image.Format;
import is.galia.image.MetaIdentifier;
import is.galia.operation.Encode;
import is.galia.operation.OperationList;
import is.galia.resource.IllegalClientArgumentException;
import is.galia.resource.iiif.FormatException;
import org.apache.commons.lang3.StringUtils;

/**
 * Encapsulates the parameters of a request URI.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#parameters">IIIF Image API
 * 1.1</a>
 */
final class Parameters {

    private String identifier;
    private Format outputFormat;
    private Quality quality;
    private Region region;
    private Rotation rotation;
    private Size size;

    /**
     * @param paramsStr URI path fragment beginning from the identifier onward
     * @throws IllegalClientArgumentException if the argument does not have the
     *         correct format, or any of its components are invalid.
     */
    public static Parameters fromURI(String paramsStr) {
        Parameters params = new Parameters();
        String[] parts = StringUtils.split(paramsStr, "/");
        try {
            if (parts.length == 5) {
                params.setIdentifier(Reference.decode(parts[0]));
                params.setRegion(Region.fromURI(parts[1]));
                params.setSize(Size.fromURI(parts[2]));
                params.setRotation(Rotation.fromURI(parts[3]));
                String[] subparts = StringUtils.split(parts[4], ".");
                if (subparts.length == 2) {
                    params.setQuality(Quality.valueOf(subparts[0].toUpperCase()));
                    params.setOutputFormat(Format.withExtension(subparts[1]));
                } else {
                    throw new IllegalClientArgumentException("Invalid parameters format");
                }
            } else {
                throw new IllegalClientArgumentException("Invalid parameters format");
            }
        } catch (IllegalClientArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalClientArgumentException(e.getMessage(), e);
        }
        return params;
    }

    /**
     * No-op constructor.
     */
    Parameters() {}

    /**
     * @param identifier Decoded identifier.
     * @param region     From URI
     * @param size       From URI
     * @param rotation   From URI
     * @param quality    From URI
     * @param format     From URI
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
            throw new IllegalClientArgumentException(e.getMessage(), e);
        }
        Format f = Format.withExtension(format);
        if (f != null) {
            setOutputFormat(f);
        } else {
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
        return super.equals(obj);
    }

    public String getIdentifier() {
        return identifier;
    }

    public Format getOutputFormat() {
        return outputFormat;
    }

    public Quality getQuality() {
        return quality;
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

    public void setOutputFormat(Format outputFormat) {
        this.outputFormat = outputFormat;
    }

    public void setQuality(Quality quality) {
        this.quality = quality;
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
        if (!getRegion().isFull()) {
            ops.add(getRegion().toCrop());
        }
        if (!Size.ScaleMode.FULL.equals(getSize().getScaleMode())) {
            ops.add(getSize().toScale());
        }
        if (getRotation().getDegrees() != 0) {
            ops.add(getRotation().toRotate());
        }
        ops.add(getQuality().toColorTransform());
        ops.add(new Encode(getOutputFormat()));
        return ops;
    }

    /**
     * @return URI parameters with no leading slash.
     */
    public String toString() {
        return String.format("%s/%s/%s/%s/%s.%s", getIdentifier(), getRegion(),
                getSize(), getRotation(), getQuality().toString().toLowerCase(),
                getOutputFormat());
    }

}
