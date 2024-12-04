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

package is.galia.resource.iiif.v3;

import is.galia.operation.Rotate;
import is.galia.operation.Transpose;
import is.galia.resource.IllegalClientArgumentException;
import is.galia.util.StringUtils;

/**
 * Encapsulates the rotation component of a URI.
 *
 * @see <a href="https://iiif.io/api/image/3.0/#43-rotation">IIIF Image API
 * 3.0: Rotation</a>
 */
class Rotation {

    private static final float DELTA = 0.00000001f;

    private float degrees;
    private boolean mirror;

    /**
     * @param rotationUri Rotation URI component.
     * @return Instance corresponding to the given argument.
     * @throws IllegalClientArgumentException if the argument is invalid.
     */
    static Rotation fromURI(String rotationUri) {
        Rotation rotation = new Rotation();
        try {
            if (rotationUri.startsWith("!")) {
                rotation.setMirror(true);
                rotation.setDegrees(Float.parseFloat(rotationUri.substring(1)));
            } else {
                rotation.setMirror(false);
                rotation.setDegrees(Float.parseFloat(rotationUri));
            }
        } catch (NumberFormatException e) {
            throw new IllegalClientArgumentException("Invalid rotation");
        } catch (IllegalArgumentException e) {
            throw new IllegalClientArgumentException(e.getMessage(), e);
        }
        return rotation;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Rotation other) {
            return mirror == other.mirror &&
                    Math.abs(degrees - other.degrees) < DELTA;
        }
        return super.equals(obj);
    }

    float getDegrees() {
        return degrees;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * @return Whether the instance describes a zero rotation. (This would be
     *         a no-op if {@link #shouldMirror()} returns {@code false}.
     */
    boolean isZero() {
        return Math.abs(degrees) < DELTA || Math.abs(degrees - 360) < DELTA;
    }

    /**
     * @param degrees Degrees of rotation between 0 and 360.
     * @throws IllegalClientArgumentException if the argument is less than zero
     *         or greater than or equal to 360.
     */
    void setDegrees(float degrees) {
        if (degrees < 0 || degrees > 360) {
            throw new IllegalClientArgumentException(
                    "Degrees must be greater than or equal to 0 and less than 360");
        }
        this.degrees = degrees;
    }

    /**
     * @param mirror Whether the image should be mirrored before being rotated.
     */
    void setMirror(boolean mirror) {
        this.mirror = mirror;
    }

    /**
     * @return Whether the image should be mirrored before being rotated.
     */
    boolean shouldMirror() {
        return mirror;
    }

    Rotate toRotate() {
        return new Rotate(getDegrees());
    }

    /**
     * @return Transpose, or null if there is no transposition.
     */
    Transpose toTranspose() {
        if (shouldMirror()) {
            return Transpose.HORIZONTAL;
        }
        return null;
    }

    /**
     * @return Value compatible with the rotation component of an IIIF URI.
     */
    @Override
    public String toString() {
        String str = "";
        if (shouldMirror()) {
            str += "!";
        }
        str += StringUtils.removeTrailingZeroes(getDegrees());
        return str;
    }

    /**
     * @return Canonical value compatible with the rotation component of an
     *         IIIF URI.
     * @see    #toString()
     */
    String toCanonicalString() {
        return toString();
    }

}
