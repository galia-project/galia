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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class MockNativeMetadata implements NativeMetadata {

    @JsonProperty
    int number;

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof MockNativeMetadata other) {
            return obj.hashCode() == other.hashCode();
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(number);
    }

    @Override
    public Map<String, ?> toMap() {
        return Map.of("number", number);
    }

    @Override
    public String toString() {
        return "" + number;
    }

}
