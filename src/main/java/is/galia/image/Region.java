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

/**
 * <p>Region on a plane with origin and dimensions.</p>
 *
 * <p>A negative origin is allowed. Zero-dimensions are allowed, but not
 * negative ones.</p>
 *
 * @param x      X origin.
 * @param y      Y origin.
 * @param size   Size.
 * @param isFull When {@code true}, indicates a "full region" which renders
 *               its coordinate properties irrelevant.
 */
public record Region(double x, double y, Size size, boolean isFull) {

    private static final double DELTA = 0.00000001;

    public Region(double x, double y, Size size) {
        this(x, y, size, false);
    }

    public Region(double x, double y, Size size, boolean isFull) {
        this.x      = x;
        this.y      = y;
        this.size   = size;
        this.isFull = isFull;
    }

    public Region(double x, double y, double width, double height) {
        this(x, y, width, height, false);
    }

    public Region(double x, double y, double width, double height,
                  boolean isFull) {
        this(x, y, new Size(width, height), isFull);
    }

    public Region(long x, long y, long width, long height) {
        this (x, y, new Size(width, height), false);
    }

    public Region(long x, long y, long width, long height, boolean isFull) {
        this (x, y, new Size(width, height), isFull);
    }

    /**
     * Copy constructor.
     */
    public Region(Region other) {
        this(other.x(), other.y(), other.width(), other.height(),
                other.isFull());
    }

    /**
     * @return Whether the given rectangle is entirely contained within the
     *         instance.
     */
    public boolean contains(Region other) {
        return (other.x() > x || x - other.x() < DELTA) &&
                (other.y() > y || y - other.y() < DELTA) &&
                ((other.x() + other.width() <= x + width()) ||
                        (other.x() + other.width() - x - width() < DELTA)) &&
                ((other.y() + other.height() <= y + height()) ||
                        (other.y() + other.height() - y - height() < DELTA));
    }

    /**
     * Override that allows a small differences between float values.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Region other) {
            return (Math.abs(other.x() - x()) < DELTA &&
                    Math.abs(other.y() - y()) < DELTA &&
                    Math.abs(other.width() - width()) < DELTA &&
                    Math.abs(other.height() - height()) < DELTA);
        }
        return false;
    }

    /**
     * @return New instance with the smaller of its current size or the given
     *         size.
     */
    public Region clippedTo(Size size) {
        return new Region(x, y, Math.min(width(), size.width()),
                Math.min(height(), size.height()));
    }

    public double width() {
        return size.width();
    }

    public double height() {
        return size.height();
    }

    /**
     * @return Rounded value of {@link #x()}.
     */
    public int intX() {
        return (int) Math.round(x);
    }

    /**
     * @return Rounded value of {@link #y()}.
     */
    public int intY() {
        return (int) Math.round(y);
    }

    /**
     * @return Rounded value of {@link #width()}.
     */
    public int intWidth() {
        return size.intWidth();
    }

    /**
     * @return Rounded value of {@link #height()}.
     */
    public int intHeight() {
        return size.intHeight();
    }

    public boolean intersects(Region other) {
        final double x2      = x() + width();
        final double y2      = y() + height();
        final double otherX2 = other.x() + other.width();
        final double otherY2 = other.y() + other.height();
        return other.x() < x2 && other.y() < y2 &&
                otherX2 > x() && otherY2 > y();
    }

    /**
     * @return Whether the instance has zero dimensions and is not {@link
     *         #isFull() full}.
     */
    public boolean isEmpty() {
        return !isFull && size.isEmpty();
    }

    /**
     * @return Rounded value of {@link #x()}.
     */
    public long longX() {
        return Math.round(x);
    }

    /**
     * @return Rounded value of {@link #y()}.
     */
    public long longY() {
        return Math.round(y);
    }

    /**
     * @return Rounded value of {@link #width()}.
     */
    public long longWidth() {
        return size.longWidth();
    }

    /**
     * @return Rounded value of {@link #height()}.
     */
    public long longHeight() {
        return size.longHeight();
    }

    public Region moved(double x, double y) {
        return new Region(this.x + x, this.y + y, this.size, false);
    }

    /**
     * @param fullSize    Raw source image dimensions.
     * @param orientation Orientation to apply.
     * @return            New oriented instance derived from the current one.
     */
    public Region oriented(Size fullSize, Orientation orientation) {
        return switch (orientation) {
            case ROTATE_90 -> { // image is rotated counterclockwise 90 degrees
                if (isFull) {
                    yield new Region(x, y, size.inverted(), true);
                }
                double newX = y;
                double newY = fullSize.height() - x - size.width();
                newY = Math.max(newY, 0);
                double newWidth = size.height();
                double newHeight = Math.min(fullSize.intHeight() - x, size.width());
                yield new Region(newX, newY, newWidth, newHeight, isFull);
            }
            case ROTATE_180 -> {
                if (isFull) {
                    yield this;
                }
                double newX = fullSize.width() - x - size.width();
                double newY = fullSize.height() - y - size.height();
                newX = Math.max(newX, 0);
                newY = Math.max(newY, 0);
                yield new Region(newX, newY, size, isFull);
            }
            case ROTATE_270 -> { // image is rotated clockwise 90 degrees
                if (isFull) {
                    yield new Region(x, y, size.inverted(), true);
                }
                double newY     = x;
                double newX = fullSize.width() - y - size.height();
                newX = Math.max(newX, 0);
                double newWidth = size.height();
                double newHeight = Math.min(size.width(), fullSize.height() - newY);
                yield new Region(newX, newY, newWidth, newHeight, isFull);
            }
            default -> this;
        };
    }

    /**
     * @return New instance with the same origin and the given size.
     */
    public Region resized(double width, double height) {
        return new Region(x, y, width, height, isFull);
    }

    /**
     * @return New instance with origin and size scaled by the given amount.
     */
    public Region scaled(double amount) {
        return scaled(amount, amount);
    }

    /**
     * @return New instance with origin and dimensions scaled by the given
     *         amounts.
     */
    public Region scaled(double xAmount, double yAmount) {
        return new Region(x * xAmount, y * yAmount,
                width() * xAmount, height() * yAmount, isFull);
    }

    public Size size() {
        return size;
    }

    /**
     * <strong>Warning: {@link java.awt.Rectangle} does not support coordinates
     * larger than {@link Integer#MAX_VALUE},</strong>
     */
    public java.awt.Rectangle toAWTRectangle() {
        return new java.awt.Rectangle(intX(), intY(), intWidth(), intHeight());
    }

    @Override
    public String toString() {
        return StringUtils.removeTrailingZeroes(x()) + "," +
                StringUtils.removeTrailingZeroes(y()) + "/" +
                size.toString();
    }

}
