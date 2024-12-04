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

package is.galia.operation.overlay;

import is.galia.image.Size;
import is.galia.operation.Operation;
import is.galia.operation.OperationList;

public abstract class Overlay implements Operation {

    boolean isFrozen;
    private int inset;
    private Position position;

    public Overlay(Position position, int inset) {
        this.setPosition(position);
        this.setInset(inset);
    }

    void checkFrozen() {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
    }

    @Override
    public void freeze() {
        isFrozen = true;
    }

    public int getInset() {
        return inset;
    }

    public Position getPosition() {
        return position;
    }

    public Size getResultingSize(Size fullSize) {
        return fullSize;
    }

    public boolean hasEffect() {
        return true;
    }

    @Override
    public boolean hasEffect(Size fullSize, OperationList opList) {
        return hasEffect();
    }

    /**
     * @param inset Inset to set.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setInset(int inset) {
        checkFrozen();
        this.inset = inset;
    }

    /**
     * @param position Position to set.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setPosition(Position position) {
        checkFrozen();
        this.position = position;
    }

}
