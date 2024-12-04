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

import is.galia.image.Format;

import java.util.Objects;

/**
 * Legal Image API output format. This is used only within the context of
 * Image API request handling; {@link Format} is generally used elsewhere.
 *
 * @see <a href="https://iiif.io/api/image/3.0/#45-format">IIIF Image API 3.0:
 * Format</a>
 */
class OutputFormat {

    private final String name;

    /**
     * @param name Format name as it appears in a URI extension.
     */
    OutputFormat(String name) {
        Objects.requireNonNull(name);
        if (name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof OutputFormat other) {
            return name.equals(other.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * @return Equivalent {@link Format}.
     */
    public Format toFormat() {
        return Format.all().stream()
                .filter(f -> f.extensions().contains(name))
                .findAny()
                .orElse(null);
    }

    @Override
    public String toString() {
        return name;
    }

}
