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

package is.galia.resource.iiif.v1;

import is.galia.operation.Rotate;
import is.galia.resource.IllegalClientArgumentException;
import is.galia.util.StringUtils;

/**
 * Encapsulates the "rotation" component of a URI.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#parameters-rotation">IIIF Image
 * API 1.1</a>
 */
class Rotation {

    private static final float DELTA = 0.00000001f;

    private float degrees = 0f;

    /**
     * @param rotationUri Rotation component of a URI.
     * @return            Instance corresponding to the given argument.
     * @throws IllegalClientArgumentException if the argument is invalid.
     */
    public static Rotation fromURI(String rotationUri) {
        Rotation rotation = new Rotation();
        try {
            rotation.setDegrees(Float.parseFloat(rotationUri));
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
            return Math.abs(degrees - other.degrees) < DELTA;
        }
        return super.equals(obj);
    }

    public float getDegrees() {
        return degrees;
    }

    @Override
    public int hashCode() {
        return Float.valueOf(degrees).hashCode();
    }

    /**
     * @param degrees Degrees of rotation between 0 and 360
     * @throws IllegalClientArgumentException if the argument is invalid.
     */
    public void setDegrees(float degrees) {
        if (degrees < 0 || degrees > 360) {
            throw new IllegalClientArgumentException(
                    "Degrees must be between 0 and 360");
        }
        this.degrees = degrees;
    }

    public Rotate toRotate() {
        return new Rotate(getDegrees());
    }

    /**
     * @return Value compatible with the rotation component of a URI.
     */
    public String toString() {
        return StringUtils.removeTrailingZeroes(getDegrees());
    }

}
