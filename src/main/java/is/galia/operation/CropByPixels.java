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

public class CropByPixels extends Crop implements Operation {

    private int x, y, width, height;

    /**
     * @param x      X origin in the range {@literal 0 <= x}.
     * @param y      Y origin in the range {@literal 0 <= y}.
     * @param width  Width in the range {@literal 0 < width}.
     * @param height Height in the range {@literal 0 < height}.
     * @throws IllegalArgumentException if any of the arguments are invalid.
     */
    public CropByPixels(int x, int y, int width, int height) {
        super();
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
    }

    /**
     * @return The X origin of the operation, expressed in pixels.
     */
    public int getX() {
        return x;
    }

    /**
     * @return The Y origin of the operation, expressed in pixels.
     */
    public int getY() {
        return y;
    }

    /**
     * @return The width of the operation, expressed in pixels.
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return The height of the operation, expressed in pixels.
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param reducedSize     Size of the input image, reduced by {@code
     *                        reductionFactor}.
     * @param reductionFactor Factor by which the full-sized image has been
     *                        reduced to become {@code reducedSize}.
     * @param scaleConstraint Scale constraint yet to be applied to the input
     *                        image. The instance is expressed relative to this
     *                        constraint rather than to {@code reducedSize} or
     *                        the full image size.
     * @return                Region relative to the given reduced
     *                        dimensions.
     */
    @Override
    public Region getRegion(Size reducedSize,
                            ReductionFactor reductionFactor,
                            ScaleConstraint scaleConstraint) {
        final double rfScale = reductionFactor.getScale();
        final double scScale = scaleConstraint.rational().doubleValue();
        final double scale   = rfScale / scScale;

        double x      = getX() * scale;
        double y      = getY() * scale;
        double width  = getWidth() * scale;
        double height = getHeight() * scale;

        // Clip dimensions to the image bounds.
        width  = (x + width > reducedSize.width()) ?
                reducedSize.width() - x : width;
        height = (y + height > reducedSize.height()) ?
                reducedSize.height() - y : height;
        return new Region(x, y, width, height);
    }

    /**
     * May produce false positives. {@link #hasEffect(Size,
     * OperationList)} should be used instead where possible.
     *
     * @return Whether the crop is not effectively a no-op.
     */
    @Override
    public boolean hasEffect() {
        return true;
    }

    @Override
    public boolean hasEffect(Size fullSize, OperationList opList) {
        if (!hasEffect()) {
            return false;
        } else {
            if (getX() > 0 || getY() > 0) {
                return true;
            } else if ((Math.abs(fullSize.width() - getWidth()) > DELTA ||
                    Math.abs(fullSize.height() - getHeight()) > DELTA) &&
                    (getWidth() < fullSize.width() || getHeight() < fullSize.height())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param x X coordinate to set.
     * @throws IllegalArgumentException If the given X coordinate is invalid.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setX(int x) {
        checkFrozen();
        if (x < 0) {
            throw new IllegalArgumentException("X must >= 0");
        }
        this.x = x;
    }

    /**
     * @param y Y coordinate to set.
     * @throws IllegalArgumentException If the given Y coordinate is invalid.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setY(int y) {
        checkFrozen();
        if (y < 0) {
            throw new IllegalArgumentException("Y must be >= 0");
        }
        this.y = y;
    }

    /**
     * @param width Width to set.
     * @throws IllegalArgumentException if the given width is invalid.
     * @throws IllegalStateException    if the instance is frozen.
     */
    public void setWidth(int width) {
        checkFrozen();
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be > 0");
        }
        this.width = width;
    }

    /**
     * @param height Height to set.
     * @throws IllegalArgumentException if the given height is invalid.
     * @throws IllegalStateException    if the instance is frozen.
     */
    public void setHeight(int height) {
        checkFrozen();
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be > 0");
        }
        this.height = height;
    }

    @Override
    public String toString() {
        return String.format("%d,%d,%s,%s",
                getX(), getY(),
                StringUtils.removeTrailingZeroes(getWidth()),
                StringUtils.removeTrailingZeroes(getHeight()));
    }

}
