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

import is.galia.util.StringUtils;

import java.util.List;

/**
 * <p>Two-dimensional area, typically an image area. Values are stored as
 * doubles.</p>
 *
 * <p>Zero-dimensions are allowed, but not negative ones.</p>
 *
 * @param width  Width in pixels.
 * @param height Height in pixels.
 */
public record Size(double width, double height) {

    /**
     * @param levels Resolution levels in order from largest to smallest.
     * @return       Whether the given dimensions appear to comprise a pyramid
     *               of successively half-scaled dimensions.
     */
    public static boolean isPyramid(List<Size> levels) {
        boolean isPyramid   = false;
        final int numImages = levels.size();
        if (numImages > 1) {
            isPyramid = true;
            double expectedWidth = 0, expectedHeight = 0;
            // Either dimension may deviate from expectations by this amount.
            final short tolerance = 2;
            for (int i = 0; i < numImages; i++) {
                Size size = levels.get(i);
                if (i > 0 && (Math.abs(size.width() - expectedWidth) > tolerance ||
                        Math.abs(size.height() - expectedHeight) > tolerance)) {
                    isPyramid = false;
                    break;
                }
                expectedWidth  = size.width() / 2.0;
                expectedHeight = size.height() / 2.0;
            }
        }
        return isPyramid;
    }

    /**
     * @param size       Pre-scaled size.
     * @param scaledArea Area to fill.
     * @return           Resulting dimensions when {@code size} is scaled to
     *                   fill {@code scaledArea}.
     */
    public static Size ofScaledArea(Size size, long scaledArea) {
        double aspectRatio = size.width() / size.height();
        double height      = Math.sqrt(scaledArea / aspectRatio);
        double width       = scaledArea / height;
        return new Size(width, height);
    }

    /**
     * Floating-point constructor.
     */
    public Size(double width, double height) {
        if (width < 0) {
            throw new IllegalArgumentException("Width must be >= 0");
        }
        this.width = width;

        if (height < 0) {
            throw new IllegalArgumentException("Height must be >= 0");
        }
        this.height = height;
    }

    /**
     * Integer constructor. The arguments will be converted to doubles for
     * storage.
     */
    public Size(long width, long height) {
        this((double) width, (double) height);
    }

    /**
     * Copy constructor.
     */
    public Size(Size size) {
        this(size.width(), size.height());
    }

    public double area() {
        return width * height;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public int intArea() {
        return (int) Math.round(area());
    }

    /**
     * @return Rounded width.
     */
    public int intWidth() {
        return (int) Math.round(width);
    }

    /**
     * @return Rounded height.
     */
    public int intHeight() {
        return (int) Math.round(height);
    }

    /**
     * @return New instance with swapped width and height.
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public Size inverted() {
        return new Size(height, width);
    }

    /**
     * @return {@literal true} if either dimension is &lt; {@literal 0.5}.
     */
    public boolean isEmpty() {
        return (width() < 0.5 || height() < 0.5);
    }

    public long longArea() {
        return Math.round(area());
    }

    /**
     * @return Rounded width.
     */
    public long longWidth() {
        return Math.round(width);
    }

    /**
     * @return Rounded height.
     */
    public long longHeight() {
        return Math.round(height);
    }

    /**
     * Rescales both dimensions by the given amount.
     *
     * @param amount Positive number with {@code 1} indicating no scale.
     * @return New instance.
     */
    public Size scaled(double amount) {
        return new Size(width * amount, height * amount);
    }

    @Override
    public String toString() {
        return StringUtils.removeTrailingZeroes(width()) + "x" +
                StringUtils.removeTrailingZeroes(height());
    }

}
