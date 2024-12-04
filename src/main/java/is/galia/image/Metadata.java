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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import is.galia.codec.iptc.DataSet;
import is.galia.codec.tiff.Directory;
import org.apache.jena.rdf.model.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>Physical embedded image metadata.</p>
 *
 * <p>For image files with multiple embedded subimages, an instance applies
 * only to a single subimage, i.e. each subimage will have its own
 * instance.</p>
 *
 * @see MutableMetadata
 */
@JsonDeserialize(as = MutableMetadata.class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface Metadata {

    /**
     * @return EXIF data.
     */
    @JsonProperty
    Optional<Directory> getEXIF();

    /**
     * @return List of IPTC IIM data sets.
     */
    @JsonProperty
    List<DataSet> getIPTC();

    /**
     * @return Format-native metadata.
     */
    @JsonProperty("native")
    Optional<NativeMetadata> getNativeMetadata();

    /**
     * <p>Reads the orientation from the {@code Orientation} tag in {@link
     * #getEXIF() EXIF data}, falling back to the XMP orientation triple in
     * {@link #getXMP() XMP data}.</p>
     *
     * @return Image orientation, or {@link Orientation#ROTATE_0} if
     *         orientation is not specified in EXIF or XMP data.
     */
    @JsonIgnore
    Orientation getOrientation();

    /**
     * @return RDF/XML string in UTF-8 encoding. The root element is {@code
     *         rdf:RDF}, and there is no packet wrapper.
     * @see <a href="https://wwwimages2.adobe.com/content/dam/acom/en/devnet/xmp/pdfs/XMP%20SDK%20Release%20cc-2016-08/XMPSpecificationPart1.pdf">
     *     XMP Specification Part 1: Data Model, Serialization, and Core
     *     Properties</a>
     */
    @JsonProperty
    Optional<String> getXMP();

    /**
     * @return Map of elements found in the XMP data. If none are found, the
     *         map is empty.
     */
    @JsonIgnore
    Map<String,Object> getXMPElements();

    /**
     * @return XMP model backed by the contents of {@link #getXMP()}.
     */
    @JsonIgnore
    Optional<Model> getXMPModel();

    /**
     * <p>Returns a map with the following structure:</p>
     *
     * <pre>{@code
     * {
     *     "exif": See {@link Directory#toMap()},
     *     "iptc": See {@link DataSet#toMap()},
     *     "xmp_string": "&lt;rdf:RDF&gt;...&lt;/rdf:RDF&gt;",
     *     "xmp_model": [Jena model],
     *     "xmp_elements": {@link Map}
     *     "native": {@link Map}
     * }}</pre>
     *
     * <p>This default implementation will return such a map, but subclasses
     * that use the {@code native} key should override and set its value to a
     * {@link Map} rather than a string.</p>
     *
     * @return Map representation of the instance.
     */
    default Map<String,Object> toMap() {
        final Map<String,Object> map = new HashMap<>(6);
        // EXIF
        getEXIF().ifPresent(dir -> {
            List<Map<String,Object>> dirs = new ArrayList<>();
            dirs.add(dir.toMap());
            map.put("exif", dirs);
        });
        // IPTC
        if (!getIPTC().isEmpty()) {
            map.put("iptc", getIPTC().stream()
                    .map(DataSet::toMap)
                    .collect(Collectors.toList()));
        };
        // XMP
        getXMP().ifPresent(xmp -> map.put("xmp_string", xmp));
        getXMPModel().ifPresent(model -> map.put("xmp_model", model));
        map.put("xmp_elements", getXMPElements());
        // Native metadata
        getNativeMetadata().ifPresent(nm -> map.put("native", nm.toMap()));
        return Collections.unmodifiableMap(map);
    }

}
