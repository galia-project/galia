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

import is.galia.util.Rational;
import is.galia.operation.Scale;

import java.util.Map;

/**
 * <p>A scale-constrained image can be thought of as a virtually downscaled
 * source image. In contrast to a {@link Scale}, the dimensions of a
 * scale-constrained image appear (to the client) to be the dimensions
 * resulting from the constraint, so any scaling-related operations expressed
 * by the client are relative to the scale-constrained image.</p>
 *
 * <p>Scale constraints are used for providing access to limited-resolution
 * versions of images depending on authorization status. An unauthorized client
 * may only be able to access a 1/2 scaled image, for example.</p>
 *
 * <p>Scale constraints are expressed as fractions, mainly in order to avoid
 * repeating decimals, which would make for URIs that are hard to
 * canonicalize.</p>
 */
public record ScaleConstraint(Rational rational) {

    public ScaleConstraint {
        if (rational.numerator() > rational.denominator()) {
            throw new IllegalArgumentException(
                    "Numerator must be less than or equal to denominator");
        } else if (rational.numerator() < 1) {
            throw new IllegalArgumentException(
                    "Numerator and denominator must both be positive");
        }
    }

    /**
     * Initializes a no-op (1:1) constraint.
     */
    public ScaleConstraint() {
        this(1, 1);
    }

    /**
     * @param numerator   Scale numerator.
     * @param denominator Scale denominator.
     * @throws IllegalArgumentException if the numerator or denominator is not
     *         positive or if the numerator is greater than the denominator.
     */
    public ScaleConstraint(long numerator, long denominator) {
        this(new Rational(numerator, denominator));
    }

    /**
     * @param fullSize Full image dimensions.
     * @return         Virtual full size after applying the constraint
     *                 described by the instance.
     */
    public Size getConstrainedSize(Size fullSize) {
        final double factor = rational.doubleValue();
        return new Size(
                fullSize.width() * factor,
                fullSize.height() * factor);
    }

    /**
     * @return Instance reduced to lowest terms.
     */
    public ScaleConstraint reduced() {
        var reduced = rational().reduced();
        return new ScaleConstraint(reduced.numerator(), reduced.denominator());
    }

    /**
     * @param fullSize Full source image size.
     * @return New downscaled instance.
     */
    public Size getResultingSize(Size fullSize) {
        return fullSize.scaled(rational().doubleValue());
    }

    /**
     * @return Whether the instance's {@link Rational#numerator() numerator}
     *         and {@link Rational#denominator() denominator} are unequal.
     */
    public boolean hasEffect() {
        return (rational.numerator() != rational.denominator());
    }

    /**
     * @return Map with {@code numerator} and {@code denominator} keys.
     */
    public Map<String,Long> toMap() {
        return Map.of(
                "numerator", rational.numerator(),
                "denominator", rational.denominator());
    }

    @Override
    public String toString() {
        return rational.numerator() + ":" + rational.denominator();
    }

}
