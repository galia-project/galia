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

package is.galia.http;

/**
 * Immutable class encapsulating an HTTP header.
 */
public record Header(String name, String value) {

    public Header(String name, String value) {
        validateName(name);
        this.name = name;
        validateValue(value);
        this.value = value;
    }

    /**
     * Copy constructor.
     */
    public Header(Header header) {
        this(header.name, header.value);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is blank");
        }
    }

    private void validateValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Illegal header value for header name: " + name);
        }
    }

    @Override
    public String toString() {
        return name + ": " + value;
    }

}
