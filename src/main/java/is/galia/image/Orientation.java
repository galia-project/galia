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
 * <p>Image orientation.</p>
 *
 * <p>This is used to support images with orientations that differ from those
 * of their pixel data. It aligns with the TIFF/EXIF {@code Orientation} tag,
 * but it currently supports only rotation and not flipping.</p>
 */
public enum Orientation {

    /**
     * No rotation. (TIFF {@code Orientation} value 1)
     */
    ROTATE_0(1, 0),

    /**
     * The image is rotated counter-clockwise (or, the view is rotated
     * clockwise) 90 degrees. Orienting it will require rotating it 90 degrees
     * clockwise. (TIFF {@code Orientation} value 6; "right top")
     */
    ROTATE_90(6, 90),

    /**
     * The image is rotated 180 degrees. (TIFF {@code Orientation} value 3;
     * "bottom right")
     */
    ROTATE_180(3, 180),

    /**
     * The image is rotated counter-clockwise (or, the view is rotated
     * clockwise) 270 degrees. Orienting it will require rotating it 90 degrees
     * counter-clockwise. (TIFF Orientation value 8; "left bottom")
     */
    ROTATE_270(8, 270);

    private final int degrees, exifValue;

    /**
     * @param value TIFF/EXIF {@code Orientation} tag value.
     * @return Orientation corresponding to the given tag value.
     * @throws IllegalArgumentException if the value is not supported.
     */
    public static Orientation forTIFFOrientation(int value) {
        for (Orientation orientation : values()) {
            if (orientation.tiffValue() == value) {
                return orientation;
            }
        }
        throw new IllegalArgumentException("Orientation value " + value +
                " is not supported.");
    }

    Orientation(int tiffValue, int degrees) {
        this.exifValue = tiffValue;
        this.degrees   = degrees;
    }

    /**
     * @return Orientation-adjusted size.
     */
    public Size adjustedSize(Size size) {
        if (ROTATE_90.equals(this) || ROTATE_270.equals(this)) {
            return size.inverted();
        }
        return size;
    }

    public int degrees() {
        return degrees;
    }

    public int tiffValue() {
        return exifValue;
    }

}
