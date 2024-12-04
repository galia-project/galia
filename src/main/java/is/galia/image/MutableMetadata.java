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
import com.fasterxml.jackson.annotation.JsonProperty;
import is.galia.codec.iptc.DataSet;
import is.galia.codec.tiff.Directory;
import is.galia.codec.tiff.EXIFBaselineTIFFTagSet;
import is.galia.codec.tiff.Field;
import is.galia.codec.tiff.Tag;
import is.galia.codec.xmp.MapReader;
import is.galia.codec.xmp.XMPUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.riot.RIOT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Basic mutable implementation.
 */
public class MutableMetadata implements Metadata {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Metadata.class);

    private static final String XMP_ORIENTATION_PREDICATE =
            "http://ns.adobe.com/tiff/1.0/Orientation";

    protected Directory exifIFD;
    protected List<DataSet> iptcDataSets = new ArrayList<>();
    protected String xmp;
    protected NativeMetadata nativeMetadata;

    /**
     * Cached by {@link #loadXMP()}.
     */
    private transient Model xmpModel;

    /**
     * Cached by {@link #getOrientation()}.
     */
    private transient Orientation orientation;

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Metadata other) {
            return Objects.equals(other.getEXIF(), getEXIF()) &&
                    Objects.equals(other.getIPTC(), getIPTC()) &&
                    Objects.equals(other.getXMP(), getXMP()) &&
                    Objects.equals(other.getNativeMetadata(), getNativeMetadata());
        }
        return super.equals(obj);
    }

    /**
     * @return EXIF data.
     */
    @JsonProperty
    @Override
    public Optional<Directory> getEXIF() {
        return Optional.ofNullable(exifIFD);
    }

    /**
     * @return List of IPTC IIM data sets.
     */
    @JsonProperty
    @Override
    public List<DataSet> getIPTC() {
        return iptcDataSets;
    }

    /**
     * @return Format-native metadata, or {@code null} if none is present.
     */
    @JsonProperty("native")
    @Override
    public Optional<NativeMetadata> getNativeMetadata() {
        return Optional.ofNullable(nativeMetadata);
    }

    /**
     * <p>Reads the orientation from the {@code Orientation} tag in {@link
     * #getEXIF() EXIF data}, falling back to the {@link
     * #XMP_ORIENTATION_PREDICATE XMP orientation triple} in {@link #getXMP()
     * XMP data}. The result is cached.</p>
     *
     * @return Image orientation. Will be {@link Orientation#ROTATE_0} if
     *         orientation is not contained in EXIF or XMP data or cannot be
     *         read.
     */
    @JsonIgnore
    @Override
    public Orientation getOrientation() {
        if (orientation == null) {
            try {
                readOrientationFromEXIF();

                if (orientation == null) {
                    getXMP().ifPresent(xmp -> readOrientationFromXMP());
                }
                if (orientation == null) {
                    orientation = Orientation.ROTATE_0;
                }
            } catch (IllegalArgumentException e) {
                LOGGER.info("getOrientation(): {}", e.getMessage());
                orientation = Orientation.ROTATE_0;
            }
        }
        return orientation;
    }

    private void readOrientationFromEXIF() {
        if (getEXIF().isEmpty()) {
            return;
        }
        Tag tag     = EXIFBaselineTIFFTagSet.ORIENTATION;
        Field field = getEXIF().get().getField(tag);
        if (field == null) {
            return;
        }
        Object value = field.getValues().getFirst();
        if (value == null) {
            return;
        }
        // Orientation is supposed to be of type SHORT, but is incorrectly
        // stored in some images as type LONG.
        // At least, that is the long and the short of it.
        switch (field.getDataType()) {
            case LONG ->
                orientation = Orientation.forTIFFOrientation(Math.toIntExact((long) value));
            case SHORT ->
                orientation = Orientation.forTIFFOrientation((int) value);
        }
    }

    private void readOrientationFromXMP() {
        getXMPModel().ifPresent(model -> {
            final NodeIterator it = model.listObjectsOfProperty(
                    model.createProperty(XMP_ORIENTATION_PREDICATE));
            if (it.hasNext()) {
                final int value = it.next().asLiteral().getInt();
                orientation = Orientation.forTIFFOrientation(value);
            }
        });
    }

    /**
     * @return RDF/XML string in UTF-8 encoding. The root element is {@literal
     *         rdf:RDF}, and there is no packet wrapper.
     */
    @JsonProperty
    @Override
    public Optional<String> getXMP() {
        return Optional.ofNullable(xmp);
    }

    /**
     * @return Map of elements found in the XMP data. If none are found, the
     *         map is empty.
     */
    @JsonIgnore
    @Override
    public Map<String,Object> getXMPElements() {
        loadXMP();
        if (xmpModel != null) {
            try {
                MapReader reader = new MapReader(xmpModel);
                return reader.readElements();
            } catch (IOException e) {
                LOGGER.warn("getXMPElements(): {}", e.getMessage());
            }
        }
        return Collections.emptyMap();
    }

    /**
     * @return XMP model backed by the contents of {@link #getXMP()}.
     */
    @JsonIgnore
    @Override
    public Optional<Model> getXMPModel() {
        loadXMP();
        return Optional.ofNullable(xmpModel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exifIFD, iptcDataSets, xmp, nativeMetadata);
    }

    /**
     * Reads {@link #xmp} into {@link #xmpModel}.
     */
    private void loadXMP() {
        final Optional<String> xmp = getXMP();
        if (xmpModel == null && xmp.isPresent()) {
            RIOT.init();

            xmpModel = ModelFactory.createDefaultModel();

            try (StringReader reader = new StringReader(xmp.get())) {
                xmpModel.read(reader, "", "RDF/XML");
            } catch (Exception e) {
                LOGGER.info("loadXMP(): {}", e.getMessage());
            }
        }
    }

    /**
     * @param exifIFD EXIF directory (IFD). Not {@code null}.
     */
    public void setEXIF(Directory exifIFD) {
        this.exifIFD     = exifIFD;
        this.orientation = null;
    }

    /**
     * @param dataSets IPTC IIM data sets. May be {@code null}.
     */
    public void setIPTC(List<DataSet> dataSets) {
        this.iptcDataSets = dataSets;
    }

    /**
     * @param nativeMetadata Format-native metadata. May be {@code null}.
     */
    public void setNativeMetadata(NativeMetadata nativeMetadata) {
        this.nativeMetadata = nativeMetadata;
    }

    /**
     * @param xmp UTF-8 bytes. May be {@code null}.
     */
    public void setXMP(byte[] xmp) {
        if (xmp != null) {
            setXMP(new String(xmp, StandardCharsets.UTF_8));
        } else {
            this.xmp         = null;
            this.xmpModel = null;
            this.orientation = null;
        }
    }

    /**
     * @param xmp UTF-8 string. May be {@code null}.
     */
    public void setXMP(String xmp) {
        if (xmp != null) {
            this.xmp = XMPUtils.trimXMP(xmp);
        } else {
            this.xmp         = null;
            this.xmpModel = null;
            this.orientation = null;
        }
    }

}
