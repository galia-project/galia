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
import is.galia.image.ScaleConstraint;
import is.galia.util.StringUtils;

import java.util.Map;

/**
 * Encapsulates a rotation operation.
 */
public class Rotate implements Operation {

    private static final double DELTA = 0.00000001;

    private double degrees;
    private boolean isFrozen;

    /**
     * No-op constructor.
     */
    public Rotate() {}

    /**
     * @param degrees Degrees of rotation between 0 and 360.
     */
    public Rotate(double degrees) {
        this();
        setDegrees(degrees);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Rotate other) {
            return (Math.abs(getDegrees() - other.getDegrees()) < DELTA);
        }
        return false;
    }

    @Override
    public void freeze() {
        isFrozen = true;
    }

    /**
     * @return Degrees.
     */
    public double getDegrees() {
        return degrees;
    }

    /**
     * @return Resulting dimensions when the scale is applied to the given full
     *         size.
     */
    @Override
    public Size getResultingSize(Size fullSize,
                                 ScaleConstraint scaleConstraint) {
        final double radians = Math.toRadians(getDegrees());
        final double sin     = Math.sin(radians);
        final double cos     = Math.cos(radians);

        final double width   = Math.abs(fullSize.width() * cos) +
                Math.abs(fullSize.height() * sin);
        final double height  = Math.abs(fullSize.height() * cos) +
                Math.abs(fullSize.width() * sin);
        return new Size(width, height);
    }

    @Override
    public boolean hasEffect() {
        return (Math.abs(getDegrees()) > 0.0001f);
    }

    @Override
    public boolean hasEffect(Size fullSize, OperationList opList) {
        return hasEffect();
    }

    @Override
    public int hashCode() {
        return Double.hashCode(degrees);
    }

    /**
     * @param degrees Degrees of rotation between 0 and 360
     * @throws IllegalArgumentException If the given degrees are invalid.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setDegrees(double degrees) {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
        if (degrees < 0 || degrees >= 360) {
            throw new IllegalArgumentException("Degrees must be between 0 and 360");
        }
        this.degrees = degrees;
    }

    /**
     * <p>Returns a map in the following format:</p>
     *
     * <pre>{
     *     class: "Rotate",
     *     degrees: double
     * }</pre>
     *
     * @return See above.
     */
    @Override
    public Map<String,Object> toMap(Size fullSize,
                                    ScaleConstraint scaleConstraint) {
        return Map.of(
                "class", getClass().getSimpleName(),
                "degrees", getDegrees());
    }

    /**
     * @return String representation of the instance, guaranteed to uniquely
     *         represent it.
     */
    @Override
    public String toString() {
        return StringUtils.removeTrailingZeroes(getDegrees());
    }

}
