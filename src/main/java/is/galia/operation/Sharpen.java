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

public class Sharpen implements Operation {

    private static final double DELTA = 0.00000001;

    private double amount;
    private boolean isFrozen;

    /**
     * No-op constructor.
     */
    public Sharpen() {}

    /**
     * @param amount Amount to sharpen.
     */
    public Sharpen(double amount) {
        setAmount(amount);
    }

    @Override
    public void freeze() {
        isFrozen = true;
    }

    @Override
    public boolean hasEffect() {
        return (getAmount() > DELTA);
    }

    @Override
    public boolean hasEffect(Size fullSize, OperationList opList) {
        return hasEffect();
    }

    public double getAmount() {
        return amount;
    }

    /**
     * @param amount Amount to sharpen.
     * @throws IllegalArgumentException if the supplied amount is less than
     *                                  zero.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setAmount(double amount) throws IllegalArgumentException {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be >= 0.");
        }
        this.amount = amount;
    }

    /**
     * @return Map with an {@literal amount} key corresponding to the amount.
     */
    @Override
    public Map<String,Object> toMap(Size fullSize,
                                    ScaleConstraint scaleConstraint) {
        return Map.of(
                "class", getClass().getSimpleName(),
                "amount", getAmount());
    }

    /**
     * @return String representation of the instance, guaranteed to represent
     * the instance, but not guaranteed to have any particular format.
     */
    @Override
    public String toString() {
        return String.valueOf(getAmount());
    }
}
