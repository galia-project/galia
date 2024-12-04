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

package is.galia.operation;

import is.galia.image.Size;
import is.galia.image.ScaleConstraint;

import java.util.Map;

/**
 * Encapsulates a color transform operation.
 */
public enum ColorTransform implements Operation {

    BITONAL, GRAY;

    /**
     * Does nothing.
     */
    @Override
    public void freeze() {
        // no-op
    }

    @Override
    public boolean hasEffect() {
        return true;
    }

    @Override
    public boolean hasEffect(Size fullSize, OperationList opList) {
        return hasEffect();
    }

    /**
     * @return Map with a {@literal type} key corresponding to the lowercase
     *         enum name.
     */
    @Override
    public Map<String,Object> toMap(Size fullSize,
                                    ScaleConstraint scaleConstraint) {
        return Map.of(
                "class", getClass().getSimpleName(),
                "type", name().toLowerCase());
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}
