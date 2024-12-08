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

import is.galia.codec.EncoderFactory;
import is.galia.codec.VariantFormatException;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.delegate.Delegate;
import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.image.MutableMetadata;
import is.galia.image.Identifier;
import is.galia.image.Info;
import is.galia.image.MetaIdentifier;
import is.galia.image.Metadata;
import is.galia.image.ScaleConstraint;
import is.galia.operation.overlay.Overlay;
import is.galia.operation.overlay.OverlayFactory;
import is.galia.operation.redaction.Redaction;
import is.galia.operation.redaction.RedactionService;
import is.galia.util.StringUtils;
import is.galia.cache.Cache;
import is.galia.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>Normalized list of {@link Operation image transform operations}
 * associated with a source image that is identified by either a {@link
 * MetaIdentifier} or {@link Identifier}.</p>
 *
 * <p>This class has dual purposes:</p>
 *
 * <ol>
 *     <li>To describe a list of image transform operations;</li>
 *     <li>To uniquely identify a post-processed (&quot;variant&quot;) image
 *     created using the instance. For example, the return values of {@link
 *     #toString()} or {@link #toFilename()} may be used in cache keys.</li>
 * </ol>
 *
 * <p>Endpoints translate request arguments into instances of this class in
 * order to pass them off to {@link Processor processors} and {@link Cache
 * caches}.</p>
 *
 * <p>Processors should iterate the operations in the list and apply them
 * (generally in order) as best they can. They must take the {@link
 * #getScaleConstraint() scale constraint} into account when cropping and
 * scaling.</p>
 */
public final class OperationList implements Iterable<Operation> {

    public static final class Builder {

        private Identifier identifier;
        private MetaIdentifier metaIdentifier;
        private Operation[] operations;
        private Map<String,String> options;

        public Builder withIdentifier(Identifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withMetaIdentifier(MetaIdentifier metaIdentifier) {
            this.metaIdentifier = metaIdentifier;
            return this;
        }

        public Builder withOperations(Operation... operations) {
            this.operations = operations;
            return this;
        }

        public Builder withOptions(Map<String,String> options) {
            this.options = options;
            return this;
        }

        public OperationList build() {
            OperationList opList = new OperationList();
            opList.setIdentifier(identifier);
            opList.setMetaIdentifier(metaIdentifier);
            if (operations != null) {
                opList.operations.addAll(List.of(operations));
            }
            if (options != null) {
                opList.options.putAll(options);
            }
            return opList;
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(OperationList.class);

    private boolean isFrozen, haveAppliedNonEndpointMutations;
    private Identifier identifier;
    private MetaIdentifier metaIdentifier;
    private final List<Operation> operations = new ArrayList<>();
    private final Map<String,Object> options = new HashMap<>();

    public static OperationList.Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new empty instance.
     */
    public OperationList() {}

    public OperationList(Identifier identifier) {
        this();
        setIdentifier(identifier);
    }

    public OperationList(MetaIdentifier identifier) {
        this();
        setMetaIdentifier(identifier);
    }

    /**
     * Adds an operation to the end of the list.
     *
     * @param op Operation to add. {@code null} values are silently discarded.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void add(Operation op) {
        if (op != null) {
            checkFrozen();
            operations.add(op);
        }
    }

    /**
     * Inserts an operation at the given index in the list.
     *
     * @param op Operation to add. {@code null} values are silently discarded.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void add(int index, Operation op) {
        if (op != null) {
            checkFrozen();
            operations.add(index, op);
        }
    }

    /**
     * Adds an operation immediately after the last instance of the given
     * class in the list. If there are no such instances in the list, the
     * operation will be added to the end of the list.
     *
     * @param op         Operation to add. {@code null} values are silently
     *                   discarded.
     * @param afterClass The operation will be added after the last
     *                   instance of this class in the list.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void addAfter(Operation op,
                         Class<? extends Operation> afterClass) {
        if (op != null) {
            checkFrozen();
            final int index = lastIndexOf(afterClass);
            if (index >= 0) {
                operations.add(index + 1, op);
            } else {
                add(op);
            }
        }
    }

    /**
     * Adds an operation immediately before the first instance of the given
     * class in the list. If there are no such instances in the list, the
     * operation will be added to the end of the list.
     *
     * @param op          Operation to add.
     * @param beforeClass The operation will be added before the first
     *                    instance of this class in the list.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void addBefore(Operation op,
                          Class<? extends Operation> beforeClass) {
        if (op != null) {
            checkFrozen();
            int index = firstIndexOf(beforeClass);
            if (index >= 0) {
                operations.add(index, op);
            } else {
                add(op);
            }
        }
    }

    /**
     * <p>Most image processing operations (crop, scale, etc.) are supplied by
     * a client in a request to an endpoint. This method adds any other
     * operations or options that endpoints have nothing to do with, and also
     * adjusts existing operations according to either/both the application
     * configuration and delegate method return values.</p>
     *
     * <p>This method must be called <strong>after</strong> all endpoint
     * operations have been added, as it may modify them. The instance's
     * identifier must also be {@link #setIdentifier(Identifier) set}.</p>
     *
     * <p>Subsequent invocations will have no effect.</p>
     *
     * @param info     Source image info.
     * @param delegate Delegate for the current request.
     */
    public void applyNonEndpointMutations(final Info info,
                                          final Delegate delegate) {
        if (haveAppliedNonEndpointMutations) {
            return;
        }
        checkFrozen();
        haveAppliedNonEndpointMutations = true;

        final Configuration config      = Configuration.forApplication();
        final Size sourceImageSize = info.getSize();
        final Metadata srcMetadata      = info.getMetadata();

        { // If there is a scale constraint set, but no Scale operation, add one.
            if (getScaleConstraint().hasEffect()) {
                Scale scale = (Scale) getFirst(Scale.class);
                if (scale == null) {
                    scale = new ScaleByPercent();
                    int index = firstIndexOf(Crop.class);
                    if (index == -1) {
                        operations.addFirst(scale);
                    } else {
                        addAfter(scale, Crop.class);
                    }
                }
            }
        }
        { // Redactions
            if (delegate != null) {
                try {
                    final RedactionService service = new RedactionService();
                    List<Redaction> redactions = service.redactionsFor(delegate);
                    for (Redaction redaction : redactions) {
                        addBefore(redaction, Encode.class);
                    }
                } catch (Exception e) {
                    LOGGER.error("applyNonEndpointMutations(): {}",
                            e.getMessage(), e);
                }
            }
        }
        { // Scale customization
            final Scale scale = (Scale) getFirst(Scale.class);
            if (scale != null) {
                // Linear downscaling
                scale.setLinear(
                        config.getBoolean(Key.PROCESSOR_DOWNSCALE_LINEAR, false));
                // Filter
                double[] scales = scale.getResultingScales(
                        sourceImageSize, getScaleConstraint());
                double smallestScale = Arrays.stream(scales).min().orElse(1);

                final Key filterKey = (smallestScale > 1) ?
                        Key.PROCESSOR_UPSCALE_FILTER :
                        Key.PROCESSOR_DOWNSCALE_FILTER;
                try {
                    final String filterStr = config.getString(filterKey);
                    final Scale.Filter filter =
                            Scale.Filter.valueOf(filterStr.toUpperCase());
                    scale.setFilter(filter);
                } catch (Exception e) {
                    LOGGER.warn("applyNonEndpointMutations(): invalid value for {}",
                            filterKey);
                }
            }
        }
        { // Sharpening
            double sharpen = config.getDouble(Key.PROCESSOR_SHARPEN, 0);
            if (sharpen > 0.001) {
                addBefore(new Sharpen(sharpen), Encode.class);
            }
        }
        { // Overlay
            try {
                final OverlayFactory overlayFactory = new OverlayFactory();
                if (overlayFactory.shouldApplyToImage(getResultingSize(sourceImageSize))) {
                    final Optional<Overlay> overlay =
                            overlayFactory.newOverlay(delegate);
                    overlay.ifPresent(ov -> addBefore(ov, Encode.class));
                }
            } catch (Exception e) {
                LOGGER.error("applyNonEndpointMutations(): {}",
                        e.getMessage(), e);
            }
        }
        { // Encode customization
            final Encode encode = (Encode) getFirst(Encode.class);
            if (encode != null) {
                if (!encode.getFormat().supportsTransparency()) {
                    final String bgColor = config.getString(Key.PROCESSOR_BACKGROUND_COLOR);
                    if (bgColor != null) {
                        encode.setBackgroundColor(Color.fromString(bgColor));
                    }
                }
                // Copy relevant keys from the application configuration.
                Iterator<String> keyIterator = config.getKeys();
                while (keyIterator.hasNext()) {
                    String key = keyIterator.next();
                    if (key.startsWith(Encode.OPTION_PREFIX) &&
                            !Key.ENCODER_FORMATS.key().equals(key)) {
                        encode.setOption(key, config.getProperty(key));
                    }
                }

                // Add metadata.
                MutableMetadata metadata = null;
                // Add XMP metadata returned from a delegate method, if any.
                if (delegate != null) {
                    try {
                        final String xmp = delegate.getMetadata();
                        if (xmp != null) {
                            metadata = new MutableMetadata();
                            metadata.setXMP(xmp);
                        }
                    } catch (Exception e) {
                        LOGGER.error("applyNonEndpointMutations(): {}",
                                e.getMessage(), e);
                    }
                }
                // Source metadata may contain important native information like
                // e.g. delay time in the case of animated GIF, so that too must be
                // copied over.
                if (srcMetadata != null) {
                    if (metadata == null) {
                        metadata = new MutableMetadata();
                    }
                    metadata.setNativeMetadata(srcMetadata.getNativeMetadata().orElse(null));
                }
                if (metadata != null) {
                    metadata.getXMP().ifPresent(encode::setXMP);
                    metadata.getNativeMetadata().ifPresent(encode::setNativeMetadata);
                }
            }
        }
    }

    private void checkFrozen() {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
    }

    /**
     * @throws IllegalStateException If the instance is frozen.
     */
    public void clear() {
        checkFrozen();
        operations.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof OperationList) {
            return obj.toString().equals(this.toString());
        }
        return super.equals(obj);
    }

    /**
     * @param clazz Operation class.
     * @return Index of the first instance of the given class in the list, or
     *         {@literal -1} if no instance of the given class is present in
     *         the list.
     */
    private int firstIndexOf(Class<? extends Operation> clazz) {
        int index = 0;
        boolean found = false;
        for (Operation operation : operations) {
            if (clazz.isAssignableFrom(operation.getClass())) {
                found = true;
                break;
            }
            index++;
        }
        return (found) ? index : -1;
    }

    /**
     * "Freezes" the instance and all of its operations so that they can no
     * longer be mutated.
     */
    public void freeze() {
        isFrozen = true;
        for (Operation op : this) {
            op.freeze();
        }
    }

    /**
     * @param opClass Class to get the first instance of.
     * @return The first instance of {@literal opClass} in the list, or {@code
     *         null} if there is no operation of that class in the list.
     */
    public Operation getFirst(Class<? extends Operation> opClass) {
        for (Operation op : operations) {
            if (opClass.isAssignableFrom(op.getClass())) {
                return op;
            }
        }
        return null;
    }

    /**
     * @return The identifier ascribed to the instance, if set; otherwise the
     *         {@link #getMetaIdentifier() meta-identifier}'s identifier, if
     *         set; otherwise {@code null}.
     */
    public Identifier getIdentifier() {
        if (identifier != null) {
            return identifier;
        } else if (metaIdentifier != null) {
            return metaIdentifier.identifier();
        }
        return null;
    }

    public MetaIdentifier getMetaIdentifier() {
        return metaIdentifier;
    }

    /**
     * @return Unmodifiable instance.
     */
    public List<Operation> getOperations() {
        return Collections.unmodifiableList(operations);
    }

    /**
     * @return Map of auxiliary options separate from the basic
     *         crop/scale/etc., such as URI query arguments, etc. If the
     *         instance is frozen, the map is unmodifiable.
     */
    public Map<String,Object> getOptions() {
        if (isFrozen) {
            return Collections.unmodifiableMap(options);
        }
        return options;
    }

    /**
     * <p>Used for quickly checking the {@link Encode} operation's format.</p>
     *
     * <p>N.B.: {@link #applyNonEndpointMutations} may mutate the {@link
     * Encode} operation.</p>
     *
     * @return The output format.
     */
    public Format getOutputFormat() {
        Encode encode = (Encode) getFirst(Encode.class);
        return (encode != null) ? encode.getFormat() : null;
    }

    /**
     * Convenience method.
     *
     * @return the effective page index of the {@link #getMetaIdentifier()
     *         meta-identifier}, if set, or {@code 0} otherwise.
     */
    public int getPageIndex() {
        int pageIndex = 0;
        if (getMetaIdentifier() != null) {
            Integer tmp = getMetaIdentifier().pageNumber();
            if (tmp != null) {
                pageIndex = tmp - 1;
            }
        }
        return pageIndex;
    }

    /**
     * @param fullSize Full size of the source image to which the instance is
     *                 being applied.
     * @return         Resulting dimensions when all operations are applied in
     *                 sequence to an image of the given full size.
     */
    public Size getResultingSize(Size fullSize) {
        for (Operation op : this) {
            fullSize = op.getResultingSize(fullSize, getScaleConstraint());
        }
        return fullSize;
    }

    /**
     * Convenience method that returns the instance ascribed to the {@link
     * #getMetaIdentifier() meta-identifier}, if set, or a neutral instance
     * otherwise.
     */
    public ScaleConstraint getScaleConstraint() {
        ScaleConstraint scaleConstraint = null;
        if (getMetaIdentifier() != null) {
            scaleConstraint = getMetaIdentifier().scaleConstraint();
        }
        if (scaleConstraint == null) {
            scaleConstraint = new ScaleConstraint(1, 1);
        }
        return scaleConstraint;
    }

    /**
     * Determines whether the operations are effectively calling for the
     * unmodified source image, based on the given source format.
     *
     * @param fullSize Full size of the source image.
     * @param format   Source image format.
     * @return         Whether the operations are effectively calling for the
     *                 unmodified source image.
     */
    public boolean hasEffect(Size fullSize, Format format) {
        if (getMetaIdentifier() != null &&
                getMetaIdentifier().scaleConstraint() != null &&
                getMetaIdentifier().scaleConstraint().hasEffect()) {
            return true;
        }
        if (!format.equals(getOutputFormat())) {
            return true;
        }
        for (Operation op : this) {
            if (op.hasEffect(fullSize, this)) {
                // 1. Ignore overlays when the output format is PDF.
                // 2. Ignore Encodes when the given output format is the same
                //    as the instance's output format. (This helps enable
                //    streaming source images without re-encoding them.)
                if (op instanceof Overlay &&
                        getOutputFormat().equals(Format.get("pdf"))) { // (1)
                    continue;
                } else if (op instanceof Encode &&
                        format.equals(((Encode) op).getFormat())) { // (2)
                    continue;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * @return Iterator over the instance's operations. If the instance is
     *         frozen, {@link Iterator#remove()} will throw an
     *         {@link UnsupportedOperationException}.
     */
    @Override
    public Iterator<Operation> iterator() {
        if (isFrozen) {
            return Collections.unmodifiableList(operations).iterator();
        }
        return operations.iterator();
    }

    /**
     * @param clazz Operation class.
     * @return Index of the last instance of the given class in the list, or -1
     *         if no instance of the given class is present in the list.
     */
    private int lastIndexOf(Class<? extends Operation> clazz) {
        for (int i = operations.size() - 1; i >= 0; i--) {
            if (clazz.isAssignableFrom(operations.get(i).getClass())) {
                return i;
            }
        }
        return -1;
    }

    public void remove(Operation op) {
        checkFrozen();
        operations.remove(op);
    }

    public void replace(Operation op, Operation newOp) {
        checkFrozen();
        Objects.requireNonNull(op);
        Objects.requireNonNull(newOp);
        if (!operations.contains(op)) {
            throw new IllegalArgumentException(
                    "The given operation is not present in the instance.");
        } else if (op != newOp) {
            int index = operations.indexOf(op);
            operations.remove(op);
            operations.add(index, newOp);
        }
    }

    /**
     * @param identifier
     * @throws IllegalStateException if the instance is frozen.
     * @see #setMetaIdentifier(MetaIdentifier)
     */
    public void setIdentifier(Identifier identifier) {
        checkFrozen();
        this.identifier = identifier;
    }

    /**
     * <p>Sets the effective base scale of the source image upon which the
     * instance is to be applied.</p>
     *
     * @param metaIdentifier
     * @throws IllegalStateException if the instance is frozen.
     * @see #setIdentifier(Identifier)
     */
    public void setMetaIdentifier(MetaIdentifier metaIdentifier) {
        checkFrozen();
        this.metaIdentifier = metaIdentifier;
    }

    public Stream<Operation> stream() {
        return operations.stream();
    }

    /**
     * <p>Returns a filename-safe string guaranteed to uniquely represent the
     * instance. The filename is in the format:</p>
     *
     * <p>{@literal [hashed identifier]_[page number + hashed scale constraint + operation list + options list].[output format extension]}</p>
     *
     * @return Filename string.
     */
    public String toFilename() {
        final List<String> parts = new ArrayList<>();
        // Add page index if != 0
        int pageIndex = getPageIndex();
        if (pageIndex != 0) {
            parts.add("" + pageIndex);
        }
        // Add scale constraint
        if (getScaleConstraint().hasEffect()) {
            parts.add(getScaleConstraint().toString());
        }
        // Add operations
        parts.addAll(stream().
                filter(Operation::hasEffect).
                map(Operation::toString).
                toList());
        // Add options
        for (String key : getOptions().keySet()) {
            parts.add(key + ":" + this.getOptions().get(key));
        }

        String opsString = StringUtils.md5(String.join("_", parts));

        String idStr = "";
        Identifier identifier = getIdentifier();
        if (identifier != null) {
            idStr = identifier.toString();
        }
        String extension = "";
        Encode encode = (Encode) getFirst(Encode.class);
        if (encode != null) {
            extension = "." + encode.getFormat().getPreferredExtension();
        }
        return StringUtils.md5(idStr) + "_" + opsString + extension;
    }

    /**
     * <p>Returns a map representing the instance with the following format
     * (expressed in JSON, with {@link Map}s expressed as objects and
     * {@link List}s expressed as arrays):</p>
     *
     * <pre>{
     *     "identifier": "result of {@link Identifier#toString()}",
     *     "scale_constraint": result of {@link ScaleConstraint#toMap()}
     *     "operations": [
     *         result of {@link Operation#toMap}
     *     ],
     *     "options": {
     *         "key": value
     *     }
     * }</pre>
     *
     * @param fullSize Full size of the source image on which the instance is
     *                 being applied.
     * @return         Unmodifiable Map representation of the instance.
     */
    public Map<String,Object> toMap(Size fullSize) {
        final Map<String,Object> map = new HashMap<>();
        if (getIdentifier() != null) {
            map.put("identifier", getIdentifier().toString());
        }
        map.put("page_index", getPageIndex());
        map.put("scale_constraint", getScaleConstraint().toMap());
        if (fullSize != null) {
            map.put("operations", this.stream()
                    .filter(op -> op.hasEffect(fullSize, this))
                    .map(op -> op.toMap(fullSize, getScaleConstraint()))
                    .toList());
        } else {
            map.put("operations", List.of());
        }
        map.put("options", getOptions());
        return Collections.unmodifiableMap(map);
    }

    /**
     * @return String representation of the instance, guaranteed to uniquely
     *         represent the instance, but not guaranteed to have any particular
     *         format.
     */
    @Override
    public String toString() {
        final List<String> parts = new ArrayList<>();
        if (getIdentifier() != null) {
            parts.add(getIdentifier().toString());
        }
        parts.add("" + getPageIndex());
        parts.add(getScaleConstraint().toString());
        for (Operation op : this) {
            if (op.hasEffect()) {
                final String opName = op.getClass().getSimpleName().toLowerCase();
                parts.add(opName + ":" + op.toString());
            }
        }
        for (String key : getOptions().keySet()) {
            parts.add(key + ":" + getOptions().get(key));
        }
        return String.join("_", parts);
    }

    /**
     * <ol>
     *     <li>Checks that an {@link #setIdentifier(Identifier) identifier is
     *     set}</li>
     *     <li>Checks that an {@link Encode} is present</li>
     *     <li>Calls {@link Operation#validate} on each {@link Operation}</li>
     *     <li>Checks that the resulting scale is not larger than allowed by
     *     the {@link #getScaleConstraint() scale constraint}</li>
     *     <li>Checks that the resulting pixel area is greater than zero and
     *     less than or equal to {@link Key#MAX_PIXELS} (if set)</li>
     * </ol>
     *
     * These are all general validations that are not endpoint-specific.
     *
     * @param fullSize     Full size of the source image on which the instance
     *                     is being applied.
     * @param sourceFormat Source image format.
     * @throws IllegalSizeException  if the resulting size exceeds {@link
     *         Key#MAX_PIXELS}.
     * @throws IllegalScaleException if the resulting scale exceeds that
     *         allowed by the {@link #getScaleConstraint() scale constraint},
     *         if set.
     * @throws OperationException if the instance is invalid in some other way.
     */
    public void validate(Size fullSize,
                         Format sourceFormat) throws VariantFormatException, OperationException {
        // Ensure that an identifier is set.
        if (getIdentifier() == null) {
            throw new OperationException("Identifier is not set.");
        }
        // Ensure that an Encode operation is present.
        if (getFirst(Encode.class) == null) {
            throw new OperationException(
                    "Missing " + Encode.class.getSimpleName() + " operation");
        }
        // Ensure that there is an Encoder available to handle the format
        // specified by the Encode operation.
        if (!EncoderFactory.getAllSupportedFormats().contains(getOutputFormat())) {
            throw new VariantFormatException(getOutputFormat());
        }
        // Validate each operation.
        for (Operation op : this) {
            op.validate(fullSize, getScaleConstraint());
        }

        Size resultingSize = getResultingSize(fullSize);

        // If there is a scale constraint set, ensure that the resulting scale
        // will not be greater than 100%.
        final ScaleConstraint scaleConstraint = getScaleConstraint();
        if (scaleConstraint.hasEffect()) {
            Scale scale = (Scale) getFirst(Scale.class);
            if (scale == null) {
                scale = new ScaleByPercent();
            }
            final double delta = Math.max(1 / fullSize.width(), 1 / fullSize.height());
            final double[] scales = scale.getResultingScales(fullSize, scaleConstraint);

            if (Arrays.stream(scales)
                    .filter(s -> s - delta > scaleConstraint.rational().doubleValue())
                    .findAny()
                    .isPresent()) {
                throw new IllegalScaleException();
            }
        }

        // Ensure that the resulting pixel area is positive.
        if (resultingSize.isEmpty()) {
            throw new OperationException("Resulting pixel area is empty.");
        }

        // Ensure that the resulting pixel area is less than or equal to the
        // max allowed area, unless the processing is a no-op.
        final long maxAllowedSize =
                Configuration.forApplication().getLong(Key.MAX_PIXELS, 0);
        if (maxAllowedSize > 0 && hasEffect(fullSize, sourceFormat) &&
                resultingSize.area() > maxAllowedSize) {
            throw new IllegalSizeException();
        }
    }

}
