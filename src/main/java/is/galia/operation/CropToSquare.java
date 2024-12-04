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
import is.galia.image.Region;
import is.galia.image.ReductionFactor;
import is.galia.image.ScaleConstraint;

public class CropToSquare extends Crop implements Operation {

    @Override
    public Region getRegion(Size reducedSize,
                            ReductionFactor reductionFactor,
                            ScaleConstraint scaleConstraint) {
        final double shortestSide = Math.min(
                reducedSize.width(), reducedSize.height());
        final double x = (reducedSize.width() - shortestSide) / 2.0;
        final double y = (reducedSize.height() - shortestSide) / 2.0;
        return new Region(x, y, shortestSide, shortestSide);
    }

    /**
     * May produce false positives. {@link #hasEffect(Size,
     * OperationList)} should be used instead where possible.
     *
     * @return Whether the crop is not effectively a no-op.
     */
    @Override
    public boolean hasEffect() {
        return true;
    }

    @Override
    public boolean hasEffect(Size fullSize, OperationList opList) {
        return fullSize.longWidth() != fullSize.longHeight();
    }

    @Override
    public String toString() {
        return "square";
    }

}
