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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import is.galia.Application;
import is.galia.cache.InfoCache;
import is.galia.codec.Decoder;
import is.galia.util.SoftwareVersion;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p>Contains JSON-serializable information about an image file, including its
 * format, dimensions, embedded metadata, subimages, and tile sizes&mdash;
 * essentially a superset of characteristics of all {@link Format formats}
 * supported by the application.</p>
 *
 * <p>Instances are format-, codec-, and endpoint-agnostic.</p>
 *
 * <p>All sizes are raw pixel data sizes, disregarding orientation.</p>
 *
 * <p>The current class design supports multiple physical subimages, but not
 * nested ones. The first subimage is considered the primary image and
 * additional images are siblings. &quot;Child&quot; images are not
 * supported.</p>
 *
 * <p>Instances ultimately originate from {@link InfoReader}, but subsequently
 * they can be cached in an {@link InfoCache} and/or a {@link
 * is.galia.cache.HeapInfoCache}, perhaps for a very long time. When an
 * instance is needed, it may be preferentially acquired from a cache, with an
 * {@link InfoReader} being consulted only as a last resort (see {@link
 * is.galia.cache.CacheFacade#fetchOrReadInfo(Identifier, Format, Decoder)}).
 * As a result, changes to the class definition must be implemented carefully
 * so that {@link InfoDeserializer older serializations remain readable}.
 * (Otherwise, users might have to purge their {@link InfoCache} whenever the
 * class changes.)</p>
 *
 * <h2>History</h2>
 *
 * <p>See {@link Serialization}.</p>
 */
@JsonSerialize(using = InfoSerializer.class)
@JsonDeserialize(using = InfoDeserializer.class)
public final class Info {

    public static final class Builder {

        private final Info info;

        Builder(Info info) {
            this.info = info;
        }

        public Info build() {
            return info;
        }

        public Builder withFormat(Format format) {
            info.setSourceFormat(format);
            return this;
        }

        public Builder withIdentifier(Identifier identifier) {
            info.setIdentifier(identifier);
            return this;
        }

        public Builder withMetadata(Metadata metadata) {
            info.getImages().getFirst().setMetadata(metadata);
            return this;
        }

        public Builder withNumResolutions(int numResolutions) {
            info.setNumResolutions(numResolutions);
            return this;
        }

        public Builder withSize(Size size) {
            info.getImages().getFirst().setSize(size);
            return this;
        }

        public Builder withSize(int width, int height) {
            return withSize(new Size(width, height));
        }

        public Builder withTileSize(Size size) {
            info.getImages().getFirst().setTileSize(size);
            return this;
        }

        public Builder withTileSize(int width, int height) {
            return withTileSize(new Size(width, height));
        }

    }

    /**
     * Represents an embedded subimage within a container stream. This is a
     * physical image such as an embedded EXIF thumbnail or embedded TIFF page.
     */
    @JsonPropertyOrder({ "width", "height", "tileWidth", "tileHeight" })
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Image {

        @JsonProperty
        private long width, height;
        @JsonProperty
        private Long tileWidth, tileHeight;
        @JsonProperty
        private Metadata metadata = new MutableMetadata();

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof Image other) {
                return (width == other.width &&
                        height == other.height &&
                        Objects.equals(tileWidth, other.tileWidth) &&
                        Objects.equals(tileHeight, other.tileHeight));
            }
            return false;
        }

        public Metadata getMetadata() {
            return metadata;
        }

        /**
         * @return Physical image size.
         */
        @JsonIgnore
        public Size getSize() {
            return new Size(width, height);
        }

        /**
         * @return Physical tile size.
         */
        @JsonIgnore
        public Size getTileSize() {
            if (tileWidth != null && tileHeight != null) {
                return new Size(tileWidth, tileHeight);
            }
            return new Size(width, height);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(
                    new long[] { width, height, tileWidth, tileHeight });
        }

        public void setMetadata(Metadata metadata) {
            this.metadata = metadata;
        }

        /**
         * @param size Physical image size.
         */
        public void setSize(Size size) {
            width  = size.intWidth();
            height = size.intHeight();
        }

        /**
         * @param tileSize Physical image tile size.
         */
        public void setTileSize(Size tileSize) {
            tileWidth  = tileSize.longWidth();
            tileHeight = tileSize.longHeight();
        }

    }

    public enum Serialization {

        /**
         * <p>Introduced in application version 1.0.</p>
         */
        VERSION_1(1),

        /**
         * The next version not introduced yet, and may never be.
         */
        VERSION_2(2);

        private final int version;

        static final Serialization CURRENT = VERSION_1;

        Serialization(int version) {
            this.version = version;
        }

        int getVersion() {
            return version;
        }
    }

    private SoftwareVersion appVersion      = Application.getVersion();
    private Identifier identifier;
    private MediaType mediaType;
    private Serialization serialization     = Serialization.CURRENT;
    private Instant serializationTimestamp;

    /**
     * Ordered list of physical images. The main image is at index {@code 0}.
     */
    private final List<Image> images = new ArrayList<>(8);

    /**
     * Number of resolutions available in the image. This applies to images
     * that don't have {@link #images literal embedded subimages}, but can
     * still be decoded at reduced scale factors.
     */
    private int numResolutions = -1;

    public static Builder builder() {
        return new Builder(new Info());
    }

    public static Info fromJSON(Path jsonFile) throws IOException {
        return newMapper().readValue(jsonFile.toFile(), Info.class);
    }

    public static Info fromJSON(InputStream jsonStream) throws IOException {
        return newMapper().readValue(jsonStream, Info.class);
    }

    public static Info fromJSON(String json) throws IOException {
        return newMapper().readValue(json, Info.class);
    }

    private static ObjectMapper newMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // This module obscures Optionals from the serialization (e.g.
        // Optional.empty() maps to null rather than { isPresent: false })
        mapper.registerModule(new Jdk8Module());
        return mapper;
    }

    public Info() {
        images.add(new Image());
    }

    /**
     * N.B.: the {@link #getSerializationTimestamp() serialization timestamp}
     * is not considered.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Info other) {
            return Objects.equals(other.getApplicationVersion(), getApplicationVersion()) &&
                    Objects.equals(other.getSerialization(), getSerialization()) &&
                    Objects.equals(other.getIdentifier(), getIdentifier()) &&
                    Objects.equals(other.getMetadata(), getMetadata()) &&
                    Objects.equals(other.getSourceFormat(), getSourceFormat()) &&
                    other.getNumResolutions() == getNumResolutions() &&
                    other.getImages().equals(getImages());
        }
        return false;
    }

    /**
     * @return Version of the application with which the instance was or will
     *         be serialized.
     * @see #getSerialization
     */
    public SoftwareVersion getApplicationVersion() {
        return appVersion;
    }

    /**
     * @return Image identifier.
     */
    public Identifier getIdentifier() {
        return identifier;
    }

    public List<Image> getImages() {
        return images;
    }

    /**
     * For convenient serialization.
     *
     * @see #getSourceFormat()
     */
    public MediaType getMediaType() {
        return mediaType;
    }

    public Metadata getMetadata() {
        return getMetadata(0);
    }

    public Metadata getMetadata(int imageIndex) {
        return images.get(imageIndex).getMetadata();
    }

    /**
     * Returns the number of "pages" contained in the image. If the {@link
     * #getImages() images} appear to comprise a pyramid, this is {@code 1}.
     * Otherwise, it is equal to their count.
     *
     * @return Page count.
     */
    public int getNumPages() {
        return isPyramid() ? 1 : getImages().size();
    }

    /**
     * <p>Returns the number of resolutions contained in the image.</p>
     *
     * <ul>
     *     <li>For formats like multi-resolution TIFF, this will match the size
     *     of {@link #getImages()}.</li>
     *     <li>For formats like JPEG2000, it will be {@code (number of
     *     decomposition levels) + 1}.</li>
     *     <li>For more conventional formats like JPEG, PNG, BMP, etc., it will
     *     be {@code 1}.</li>
     * </ul>
     *
     * @return Number of resolutions contained in the image.
     */
    public int getNumResolutions() {
        return numResolutions;
    }

    /**
     * @see #getApplicationVersion
     */
    public Serialization getSerialization() {
        return serialization;
    }

    /**
     * N.B.: Although it would be possible for most {@link InfoCache}s to
     * obtain a serialization timestamp by other means, such as e.g. filesystem
     * attributes, storing it within the serialized instance makes a separate
     * I/O call unnecessary.
     *
     * @return Timestamp that the instance was serialized.
     */
    public Instant getSerializationTimestamp() {
        return serializationTimestamp;
    }

    /**
     * @return Size of the main image.
     */
    public Size getSize() {
        return getSize(0);
    }

    /**
     * @return Size of the image at the given index.
     */
    public Size getSize(int imageIndex) {
        return images.get(imageIndex).getSize();
    }

    /**
     * @return Source format of the image, or {@link Format#UNKNOWN} if
     *         unknown.
     */
    public Format getSourceFormat() {
        if (mediaType != null) {
            return mediaType.toFormat();
        }
        return Format.UNKNOWN;
    }

    @Override
    public int hashCode() {
        int[] codes = new int[7];
        codes[0] = getApplicationVersion().hashCode();
        codes[1] = getSerialization().hashCode();
        codes[2] = getIdentifier().hashCode();
        codes[3] = getImages().hashCode();
        codes[4] = getSourceFormat().hashCode();
        codes[5] = getNumResolutions();
        if (getMetadata() != null) {
            codes[6] = getMetadata().hashCode();
        }
        return Arrays.hashCode(codes);
    }

    /**
     * @return Whether the {@link #getImages() images} appear to comprise a
     *         pyramid.
     */
    boolean isPyramid() {
        List<Size> sizes = getImages()
                .stream()
                .map(Image::getSize)
                .toList();
        return Size.isPyramid(sizes);
    }

    /**
     * @param version Application version. This value is not serialized
     *                (the {@link Application#getVersion() current application
     *                version} is instead).
     */
    void setApplicationVersion(SoftwareVersion version) {
        this.appVersion = version;
    }

    /**
     * @param identifier Identifier of the image described by the instance.
     */
    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }

    /**
     * For convenient deserialization.
     *
     * @see #setSourceFormat(Format)
     */
    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    /**
     * @param numResolutions Number of resolutions contained in the image.
     */
    public void setNumResolutions(int numResolutions) {
        this.numResolutions = numResolutions;
    }

    /**
     * @param timestamp Time at which the instance is serialized.
     */
    public void setSerializationTimestamp(Instant timestamp) {
        this.serializationTimestamp = timestamp;
    }

    /**
     * @param version One of the {@link Serialization} versions. This value is
     *                not serialized (the current serialization version is
     *                instead).
     */
    void setSerializationVersion(int version) {
        this.serialization = Arrays
                .stream(Serialization.values())
                .filter(sv -> sv.getVersion() == version)
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    public void setSourceFormat(Format sourceFormat) {
        if (sourceFormat == null) {
            mediaType = null;
        } else {
            mediaType = sourceFormat.getPreferredMediaType();
        }
    }

    /**
     * @return JSON representation of the instance.
     */
    public String toJSON() throws JsonProcessingException {
        return newMapper().writer().writeValueAsString(this);
    }

    @Override
    public String toString() {
        try {
            return toJSON();
        } catch (JsonProcessingException e) { // this should never happen
            return super.toString();
        }
    }

    /**
     * @param os Output stream to write to.
     */
    public void writeAsJSON(OutputStream os) throws IOException {
        newMapper().writer().writeValue(os, this);
    }

}
