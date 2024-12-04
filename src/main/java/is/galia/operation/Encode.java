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

package is.galia.operation;

import is.galia.config.Configuration;
import is.galia.config.MapConfiguration;
import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.image.NativeMetadata;
import is.galia.image.ScaleConstraint;
import is.galia.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * <p>Encapsulates an image encoding operation.</p>
 *
 * <p>Typically appears at the very end of an {@link OperationList}.</p>
 */
public class Encode implements Operation {

    /**
     * Any key supplied to {@link #setOption(String, Object)} must start with
     * this string.
     */
    public static final String OPTION_PREFIX = "encoder.";

    private Color backgroundColor;
    private final Map<String,Object> encoderOptions = new TreeMap<>(); // sorted
    private Format format                           = Format.UNKNOWN;
    private boolean isFrozen;
    private int maxComponentSize                    = 8;
    private String xmp;
    private NativeMetadata nativeMetadata;

    public Encode(Format format) {
        setFormat(format);
    }

    @Override
    public void freeze() {
        isFrozen = true;
    }

    /**
     * @return Color with which to fill the empty portions of the image when
     *         {@link #getFormat()} returns a format that does not
     *         support transparency and when either rotating by a non-90-degree
     *         multiple, or when flattening an image with alpha. May be
     *         <code>null</code>.
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Format getFormat() {
        return format;
    }

    /**
     * @return Maximum sample size to encode. May be {@link Integer#MAX_VALUE}
     *         indicating no max.
     */
    public int getMaxComponentSize() {
        return maxComponentSize;
    }

    /**
     * @return Native metadata.
     */
    public Optional<NativeMetadata> getNativeMetadata() {
        return Optional.ofNullable(nativeMetadata);
    }

    /**
     * @return Unmodifiable options.
     * @see #setOption(String, Object)
     */
    public Configuration getOptions() {
        return new MapConfiguration(Collections.unmodifiableMap(encoderOptions));
    }

    /**
     * @return Embeddable XMP metadata. {@code rdf:RDF} is the enclosing tag.
     */
    public Optional<String> getXMP() {
        return Optional.ofNullable(xmp);
    }

    /**
     * @return {@literal true}.
     */
    @Override
    public boolean hasEffect() {
        return true;
    }

    /**
     *
     * @param fullSize Full size of the source image.
     * @param opList   Operation list of which the operation may or may not be
     *                 a member.
     * @return         {@literal true}.
     */
    @Override
    public boolean hasEffect(Size fullSize, OperationList opList) {
        return hasEffect();
    }

    /**
     * @throws IllegalStateException if the instance is frozen.
     */
    public void removeOption(String key) {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
        encoderOptions.remove(key);
    }

    /**
     * @param color Background color.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setBackgroundColor(Color color) {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
        this.backgroundColor = color;
    }

    /**
     * @param format Format to set.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setFormat(Format format) {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
        if (format == null) {
            format = Format.UNKNOWN;
        }
        this.format = format;
    }

    /**
     * @param depth Maximum sample size to encode. Supply {@code 0} to
     *              indicate no max.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setMaxComponentSize(int depth) {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
        this.maxComponentSize = (depth == 0) ? Integer.MAX_VALUE : depth;
    }

    /**
     * @param metadata Native metadata.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setNativeMetadata(NativeMetadata metadata) {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
        this.nativeMetadata = metadata;
    }

    /**
     * <p>Some encoders may support their own custom encoding options. Rather
     * than forcing them to draw this information directly from the application
     * configuration, which would make it invisible in the operation pipeline
     * (and therefore not considered in e.g. variant image cache keys), these
     * options can be supplied here, where they will affect the output of
     * {@link #toString()} and {@link #toMap}.</p>
     *
     * @param key   Option key, which must start with {@link #OPTION_PREFIX}.
     *              No other constraints are imposed, but as keys are drawn
     *              from the application configuration, they should align with
     *              that syntax.
     * @param value Option value.
     * @throws IllegalArgumentException if the key does not conform to the
     *         required syntax.
     * @throws IllegalStateException if the instance is frozen.
     * @see #getOptions()
     */
    public void setOption(String key, Object value) {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        } else if (!key.startsWith(OPTION_PREFIX)) {
            throw new IllegalArgumentException(
                    "Key must start with \"" + OPTION_PREFIX + "\"");
        }
        encoderOptions.put(key, value);
    }

    /**
     * @param xmp Embeddable XMP metadata. {@code rdf:RDF} is the enclosing
     *            tag.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setXMP(String xmp) {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        } else if (!xmp.startsWith("<rdf:RDF")) {
            throw new IllegalArgumentException(
                    "XMP string must start with an <rdf:RDF> tag.");
        }
        this.xmp = xmp;
    }

    /**
     * <p>Returns a map in the following format:</p>
     *
     * <pre>{
     *     class: "Encode"
     *     background_color: Hexadecimal string
     *     format: Media type string
     *     max_sample_size: Integer
     *     native_metadata: See {@link NativeMetadata#toMap()}
     *     xmp: String
     *     options: {
     *         key1: value
     *         key2: value
     *     }
     * }</pre>
     *
     * @return Map representation of the instance.
     */
    @Override
    public Map<String,Object> toMap(Size fullSize,
                                    ScaleConstraint scaleConstraint) {
        final Map<String,Object> map = new HashMap<>(6);
        map.put("class", getClass().getSimpleName());
        if (getBackgroundColor() != null) {
            map.put("background_color", getBackgroundColor().toRGBHex());
        }
        map.put("format", getFormat().getPreferredMediaType());
        map.put("max_sample_size", getMaxComponentSize());
        getXMP().ifPresent(xmp -> map.put("xmp", xmp));
        getNativeMetadata().ifPresent(m ->
                map.put("native_metadata", m.toMap()));

        if (!encoderOptions.isEmpty()) {
            map.put("options", Collections.unmodifiableMap(encoderOptions));
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * @return String representation of the instance, guaranteed to uniquely
     *         represent it.
     */
    @Override
    public String toString() {
        final List<String> parts = new ArrayList<>(5);
        if (getFormat() != null) {
            parts.add(getFormat().getPreferredExtension());
        }
        if (getBackgroundColor() != null) {
            parts.add(getBackgroundColor().toRGBHex());
        }
        if (getMaxComponentSize() != Integer.MAX_VALUE) {
            parts.add(getMaxComponentSize() + "");
        }
        getXMP().ifPresent(xmp -> parts.add(StringUtils.md5(xmp)));
        getNativeMetadata().ifPresent(m ->
                parts.add(StringUtils.md5(m.toString())));

        if (!encoderOptions.isEmpty()) {
            String str = encoderOptions.entrySet()
                    .stream()
                    .map((e) -> e.getKey() + ":" + e.getValue())
                    .collect(Collectors.joining(";"));
            parts.add(str);
        }
        return String.join("_", parts);
    }

}
