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

import is.galia.operation.ColorTransform;

/**
 * Encapsulates the rotation component of a URI.
 *
 * @see <a href="https://iiif.io/api/image/3.0/#44-quality">IIIF Image API
 * 3.0: Quality</a>
 */
enum Quality {

    BITONAL("bitonal"),
    COLOR("color"),
    DEFAULT("default"),
    GRAY("gray");

    private final String uriValue;

    Quality(String uriValue) {
        this.uriValue = uriValue;
    }

    public String getURIValue() {
        return uriValue;
    }

    public ColorTransform toColorTransform() {
        return switch (this) {
            case BITONAL -> ColorTransform.BITONAL;
            case GRAY    -> ColorTransform.GRAY;
            default      -> null;
        };
    }

}
