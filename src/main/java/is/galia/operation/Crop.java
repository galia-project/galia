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

import is.galia.image.Size;
import is.galia.image.Region;
import is.galia.image.ReductionFactor;
import is.galia.image.ScaleConstraint;

import java.util.Map;

/**
 * <p>Abstract cropping operation.</p>
 *
 * <p>The basic idea is that {@link #getRegion} is used to compute a source
 * image region based on arguments supplied to it and to subclasses' mutator
 * methods.</p>
 */
public abstract class Crop implements Operation {

    static final double DELTA = 0.00000001;

    private boolean isFrozen;

    void checkFrozen() {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Crop) {
            return obj.toString().equals(toString());
        }
        return false;
    }

    @Override
    public void freeze() {
        isFrozen = true;
    }

    /**
     * @param fullSize Full-sized image dimensions.
     * @return         Region relative to the given full dimensions.
     */
    public Region getRegion(Size fullSize) {
        return getRegion(fullSize,
                new ReductionFactor(),
                new ScaleConstraint(1, 1));
    }

    /**
     * @param fullSize        Full-sized image dimensions.
     * @param scaleConstraint Scale constraint yet to be applied to the input
     *                        image. The instance is expressed relative to this
     *                        constraint rather than to {@code fullSize}.
     * @return                Region relative to the given full dimensions.
     */
    public Region getRegion(Size fullSize,
                            ScaleConstraint scaleConstraint) {
        return getRegion(fullSize, new ReductionFactor(), scaleConstraint);
    }

    /**
     * Computes an effective crop rectangle in source image coordinates.
     *
     * @param reducedSize     Size of the input image, reduced by {@code
     *                        reductionFactor}.
     * @param reductionFactor Factor by which the full-sized image has been
     *                        reduced to become {@code reducedSize}.
     * @param scaleConstraint Scale constraint yet to be applied to the input
     *                        image. The instance is expressed relative to this
     *                        constraint rather than to {@code reducedSize}
     *                        or the full image size.
     * @return                Region relative to the given reduced
     *                        dimensions.
     */
    abstract public Region getRegion(Size reducedSize,
                                     ReductionFactor reductionFactor,
                                     ScaleConstraint scaleConstraint);

    @Override
    public Size getResultingSize(Size fullSize,
                                 ScaleConstraint scaleConstraint) {
        return getRegion(fullSize, scaleConstraint).size();
    }

    /**
     * This method may produce false positives. {@link #hasEffect(Size,
     * OperationList)} should be used instead where possible, unless overrides
     * mention otherwise.
     *
     * @return Whether the crop is not effectively a no-op.
     */
    @Override
    public abstract boolean hasEffect();

    /**
     * @return Whether the crop is not effectively a no-op.
     */
    @Override
    public abstract boolean hasEffect(Size fullSize,
                                      OperationList opList);

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return         Map with {@code class}, {@code x}, {@code y}, {@code
     *                 width}, and {@code height} keys and integer values
     *                 corresponding to the instance coordinates.
     */
    @Override
    public Map<String,Object> toMap(Size fullSize,
                                    ScaleConstraint scaleConstraint) {
        final Region region = getRegion(fullSize, scaleConstraint);
        return Map.of(
                "class", getClass().getSimpleName(),
                "x", region.longX(),
                "y", region.longY(),
                "width", region.longWidth(),
                "height", region.longHeight());
    }

    /**
     * <p>Checks the intersection and dimensions.</p>
     *
     * {@inheritDoc}
     */
    @Override
    public void validate(Size fullSize,
                         ScaleConstraint scaleConstraint)
            throws OperationException {
        try {
            Size resultingSize =
                    getResultingSize(fullSize, scaleConstraint);
            if (resultingSize.isEmpty()) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new OperationException(
                    "Crop area is outside the bounds of the source image.");
        }
    }

}
