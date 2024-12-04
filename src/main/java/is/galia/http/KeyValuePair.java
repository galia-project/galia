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

public record KeyValuePair(String key, String value) {

    public KeyValuePair {
        validateKey(key);
    }

    private void validateKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Illegal key: " + key);
        }
    }

    /**
     * @return URL-encoded key-value pair.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (key != null && !key.isBlank()) {
            builder.append(Reference.encode(key));
        }
        if (value != null && !value.isBlank()) {
            builder.append("=");
            builder.append(Reference.encode(value));
        }
        return !builder.isEmpty() ? builder.toString() : "(empty)";
    }

}
