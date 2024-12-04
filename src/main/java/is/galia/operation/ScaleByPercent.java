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
import is.galia.image.ReductionFactor;
import is.galia.image.ScaleConstraint;
import is.galia.util.StringUtils;

import java.util.Objects;

/**
 * N.B.: {@link #getPercent()} should not be used when figuring out how to
 * apply an instance to an image. For that, {@link #getResultingSize} and
 * {@link #getResultingScales} should be used instead.
 */
public class ScaleByPercent extends Scale implements Operation {

    private double percent = 1;

    /**
     * No-op constructor for a 100% instance.
     */
    public ScaleByPercent() {
    }

    /**
     * Percent-based constructor.
     *
     * @param percent Value between {@literal 0} and {@literal 1} to represent
     *                a downscale, or above {@literal 1} to represent an
     *                upscale.
     */
    public ScaleByPercent(double percent) {
        setPercent(percent);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof ScaleByPercent other) {
            return Math.abs(getPercent() - other.getPercent()) < DELTA &&
                    Objects.equals(other.getFilter(), getFilter());
        }
        return super.equals(obj);
    }

    /**
     * @return Double from 0 to 1. May be null.
     */
    public double getPercent() {
        return percent;
    }

    @Override
    public ReductionFactor getReductionFactor(final Size reducedSize,
                                              final ScaleConstraint scaleConstraint,
                                              final int maxFactor) {
        final double scScale = scaleConstraint.rational().doubleValue();
        ReductionFactor rf = ReductionFactor.forScale(getPercent() * scScale);

        if (rf.factor > maxFactor) {
            rf.factor = maxFactor;
        }
        return rf;
    }

    @Override
    public double[] getResultingScales(Size fullSize,
                                       ScaleConstraint scaleConstraint) {
        final double[] result = new double[2];
        result[0] = result[1] = getPercent() *
                scaleConstraint.rational().doubleValue();
        return result;
    }

    @Override
    public Size getResultingSize(Size imageSize,
                                 ReductionFactor reductionFactor,
                                 ScaleConstraint scaleConstraint) {
        final double rfScale  = reductionFactor.getScale();
        final double scScale  = scaleConstraint.rational().doubleValue();
        final double scalePct = getPercent() * (scScale / rfScale);
        return imageSize.scaled(scalePct);
    }

    @Override
    public boolean hasEffect() {
        return Math.abs(getPercent() - 1) > DELTA;
    }

    @Override
    public boolean hasEffect(Size fullSize, OperationList opList) {
        if (opList.getScaleConstraint().hasEffect()) {
            return true;
        }
        return Math.abs(getPercent() - 1) > DELTA;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPercent(), getFilter());
    }

    /**
     * @param percent Double &gt; 0 and &le; 1.
     * @throws IllegalArgumentException If the given percent is invalid.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setPercent(double percent) {
        checkFrozen();
        if (percent < DELTA) {
            throw new IllegalArgumentException("Percent must be greater than zero");
        }
        this.percent = percent;
    }

    /**
     * <p>Returns a string representation of the instance, guaranteed to
     * uniquely represent the instance. The format is:</p>
     *
     * <dl>
     *     <dt>No-op</dt>
     *     <dd>{@literal none}</dd>
     *     <dt>Percent</dt>
     *     <dd>{@literal nnn%(,filter)}</dd>
     * </dl>
     *
     * @return String representation of the instance.
     */
    @Override
    public String toString() {
        var builder = new StringBuilder();
        builder.append(StringUtils.removeTrailingZeroes(getPercent() * 100));
        builder.append("%");
        if (getFilter() != null) {
            builder.append(",");
            builder.append(getFilter().name().toLowerCase());
        }
        return builder.toString();
    }

}
