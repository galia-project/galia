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

import java.util.Map;

/**
 * <p>Format-native metadata.</p>
 */
public interface NativeMetadata {

    /**
     * @param other Object to compare.
     * @return Whether the given object is equal to the instance.
     */
    @Override
    boolean equals(Object other);

    /**
     * @return Suitable hash code.
     */
    @Override
    int hashCode();

    /**
     * @return Key-value representation, which must be Jackson-serializable.
     */
    Map<String,?> toMap();

    /**
     * @return Meaningful representation.
     */
    @Override
    String toString();

}
