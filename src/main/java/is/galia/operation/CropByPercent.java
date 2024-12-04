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
import is.galia.util.StringUtils;

public class CropByPercent extends Crop implements Operation {

    private double x = 0, y = 0, width = 1, height = 1;

    /**
     * Creates a no-op "100% crop" instance.
     */
    public CropByPercent() {
        super();
    }

    /**
     * @param x      X origin in the range {@literal 0 <= x < 1}.
     * @param y      Y origin in the range {@literal 0 <= y < 1}.
     * @param width  Width in the range {@literal 0 < width <= 1}.
     * @param height Height in the range {@literal 0 < height <= 1}.
     * @throws IllegalArgumentException if any of the arguments are invalid.
     */
    public CropByPercent(double x, double y, double width, double height) {
        super();
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
    }

    /**
     * @return The X origin of the operation, expressed in percent.
     */
    public double getX() {
        return x;
    }

    /**
     * @return The Y origin of the operation, expressed in percent.
     */
    public double getY() {
        return y;
    }

    /**
     * @return The width of the operation, expressed in percent.
     */
    public double getWidth() {
        return width;
    }

    /**
     * @return The height of the operation, expressed in percent.
     */
    public double getHeight() {
        return height;
    }

    /**
     * @param reducedSize     Size of the input image, which has been reduced
     *                        by {@code reductionFactor}.
     * @param reductionFactor Ignored.
     * @param scaleConstraint Ignored.
     * @return                Region relative to {@code reducedSize}.
     */
    @Override
    public Region getRegion(Size reducedSize,
                            ReductionFactor reductionFactor,
                            ScaleConstraint scaleConstraint) {
        double x      = getX() * reducedSize.width();
        double y      = getY() * reducedSize.height();
        double width  = getWidth() * reducedSize.width();
        double height = getHeight() * reducedSize.height();

        // Clip dimensions to the image bounds.
        if (x + width > reducedSize.width()) {
            width = reducedSize.width() - x;
        }
        if (y + height > reducedSize.height()) {
            height = reducedSize.height() - y;
        }

        boolean isFull = Math.abs(getWidth() - 1) < DELTA &&
                Math.abs(getHeight() - 1) < DELTA;
        return new Region(x, y, width, height, isFull);
    }

    /**
     * @return Whether the crop is not effectively a no-op.
     */
    @Override
    public boolean hasEffect() {
        return getX() > DELTA ||
                getY() > DELTA ||
                Math.abs(getWidth() - 1) > DELTA ||
                Math.abs(getHeight() - 1) > DELTA;
    }

    /**
     * Fulfills the {@link Operation} contract, but {@link #hasEffect()} can
     * be used safely instead.
     *
     * @param fullSize Ignored.
     * @param opList   Ignored.
     * @return         Whether the crop is not effectively a no-op.
     */
    @Override
    public boolean hasEffect(Size fullSize, OperationList opList) {
        return hasEffect();
    }

    /**
     * @param x X coordinate to set.
     * @throws IllegalArgumentException If the given X coordinate is invalid.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setX(double x) {
        checkFrozen();
        if (x < 0) {
            throw new IllegalArgumentException("X must be >= 0");
        } else if (x >= 1) {
            throw new IllegalArgumentException("X must be < 1");
        }
        this.x = x;
    }

    /**
     * @param y Y coordinate to set.
     * @throws IllegalArgumentException If the given Y coordinate is invalid.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setY(double y) {
        checkFrozen();
        if (y < 0) {
            throw new IllegalArgumentException("Y must be >= 0");
        } else if (y >= 1) {
            throw new IllegalArgumentException("Y must be < 1");
        }
        this.y = y;
    }

    /**
     * @param width Width to set.
     * @throws IllegalArgumentException if the given width is invalid.
     * @throws IllegalStateException    if the instance is frozen.
     */
    public void setWidth(double width) {
        checkFrozen();
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be > 0");
        } else if (width > 1) {
            throw new IllegalArgumentException("Width must be <= 1");
        }
        this.width = width;
    }

    /**
     * @param height Height to set.
     * @throws IllegalArgumentException if the given height is invalid.
     * @throws IllegalStateException    if the instance is frozen.
     */
    public void setHeight(double height) {
        checkFrozen();
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be > 0");
        } else if (height > 1) {
            throw new IllegalArgumentException("Height must be <= 1");
        }
        this.height = height;
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s",
                StringUtils.removeTrailingZeroes(getX()),
                StringUtils.removeTrailingZeroes(getY()),
                StringUtils.removeTrailingZeroes(getWidth()),
                StringUtils.removeTrailingZeroes(getHeight()));
    }

}
