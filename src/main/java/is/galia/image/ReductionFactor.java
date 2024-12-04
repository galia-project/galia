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

/**
 * <p>Represents a scale-reduction multiple with a corresponding scale of
 * {@code (1/2)^rf}. For example, a factor of 0 represents full scale; 1
 * represents 50% reduction; 2 represents 75% reduction; etc.</p>
 *
 * <p>This class is mutable, as one of its uses is to be passed to a {@link
 * is.galia.codec.Decoder}, which may change it.</p>
 */
public final class ReductionFactor {

    /**
     * Set to a value that will allow one pixel of leeway in an image up to
     * 99999x99999.
     */
    public static final double DEFAULT_TOLERANCE = 0.000001;

    public int factor = 0;

    /**
     * Factory method. Uses {@link #DEFAULT_TOLERANCE}.
     *
     * @param scalePercent Scale percentage.
     * @return             Instance corresponding to the given scale.
     */
    public static ReductionFactor forScale(final double scalePercent) {
        return forScale(scalePercent, DEFAULT_TOLERANCE);
    }

    /**
     * Factory method.
     *
     * @param scalePercent Scale percentage.
     * @param tolerance    Leeway that could allow a larger factor to be
     *                     selected.
     * @return             Instance corresponding to the given scale. If the
     *                     given scale is greater than 1, the factor will be
     *                     {@code 0}.
     */
    public static ReductionFactor forScale(final double scalePercent,
                                           final double tolerance) {
        int factor = 0;

        if (scalePercent < 1.0) {
            final int maxFactor = 99;
            double nextPct = 0.5;
            while (scalePercent <= nextPct + tolerance && factor < maxFactor) {
                nextPct /= 2.0;
                factor++;
            }
        }
        return new ReductionFactor(factor);
    }

    /**
     * No-op constructor.
     */
    public ReductionFactor() {}

    public ReductionFactor(int factor) {
        if (factor < 0) {
            throw new IllegalArgumentException(
                    "Factor must be greater than or equal to 0");
        }
        this.factor = factor;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof ReductionFactor other) {
            return other.factor == factor;
        }
        return false;
    }

    /**
     * @param scale Amount to scale an image (or one of its axes).
     * @return Amount that the image will need to be scaled relative to the
     *         instance's {@link #factor}.
     */
    public double findDifferentialScale(double scale) {
        return scale / getScale();
    }

    /**
     * @return Scale corresponding to the instance.
     */
    public double getScale() {
        return Math.pow(0.5, this.factor);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(factor);
    }

    @Override
    public String toString() {
        return factor + "";
    }

}
