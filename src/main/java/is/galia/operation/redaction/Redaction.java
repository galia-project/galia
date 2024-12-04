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

package is.galia.operation.redaction;

import is.galia.image.Size;
import is.galia.image.Region;
import is.galia.image.ScaleConstraint;
import is.galia.operation.Color;
import is.galia.operation.Crop;
import is.galia.operation.Operation;
import is.galia.operation.OperationList;

import java.util.Map;
import java.util.Objects;

/**
 * <p>Encapsulates a rectangle overlaid onto an image.</p>
 *
 * <p>Instances should be obtained from the {@link RedactionService}.</p>
 */
public class Redaction implements Operation {

    private boolean isFrozen;
    private Region region;
    private Color color = Color.BLACK;

    /**
     * No-op constructor.
     */
    public Redaction() {}

    public Redaction(Region region, Color color) {
        setRegion(region);
        setColor(color);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Redaction other) {
            return Objects.equals(other.getRegion(), getRegion()) &&
                    Objects.equals(other.getColor(), getColor());
        }
        return super.equals(obj);
    }

    @Override
    public void freeze() {
        isFrozen = true;
    }

    /**
     * @return Color with alpha.
     */
    public Color getColor() {
        return color;
    }

    /**
     * @return Redacted region in source image pixel coordinates.
     */
    public Region getRegion() {
        return region;
    }

    /**
     * @param fullSize        Size of the source image.
     * @param scaleConstraint Scale constraint.
     * @param appliedCrop     Crop that has been applied to the source image.
     * @return                Region of the cropped image to be redacted, or an
     *                        empty rectangle if none.
     */
    public Region getResultingRegion(final Size fullSize,
                                     final ScaleConstraint scaleConstraint,
                                     final Crop appliedCrop) {
        final Region cropRegion = appliedCrop.getRegion(
                fullSize, scaleConstraint);
        final Region thisRegion = getRegion();

        if (thisRegion.intersects(cropRegion)) {
            return thisRegion.moved(-cropRegion.x(), -cropRegion.y());
        }
        return new Region(0, 0, 0, 0);
    }

    @Override
    public boolean hasEffect() {
        return region.width() > 0 && region.height() > 0 &&
                color.getAlpha() > 0;
    }

    @Override
    public boolean hasEffect(Size fullSize, OperationList opList) {
        if (!hasEffect()) {
            return false;
        }
        Region resultingImage;
        Crop crop = (Crop) opList.getFirst(Crop.class);
        if (crop != null) {
            resultingImage = crop.getRegion(
                    fullSize, opList.getScaleConstraint());
        } else {
            resultingImage = new Region(
                    0, 0, fullSize.width(), fullSize.height());
        }
        return getRegion().intersects(resultingImage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRegion(), getColor());
    }

    public void setColor(Color color) {
        checkFrozen();
        Objects.requireNonNull(color);
        this.color = color;
    }

    /**
     * @param region Redacted region in source image pixel coordinates.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setRegion(Region region) {
        checkFrozen();
        Objects.requireNonNull(region);
        this.region = region;
    }

    private void checkFrozen() {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
    }

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return Map with {@literal x}, {@literal y}, {@literal width},
     *         and {@literal height} keys.
     */
    @Override
    public Map<String, Object> toMap(Size fullSize,
                                     ScaleConstraint scaleConstraint) {
        return Map.of(
                "class", getClass().getSimpleName(),
                "x", getRegion().longX(),
                "y", getRegion().longY(),
                "width", getRegion().longWidth(),
                "height", getRegion().longHeight(),
                "color", getColor().toRGBAHex());
    }

    /**
     * @return String representation of the instance, in the format
     *         {@literal [x],[y]/[width]x[height]}.
     */
    @Override
    public String toString() {
        return String.format("%d,%d/%dx%d/%s",
                getRegion().longX(),
                getRegion().longY(),
                getRegion().longWidth(),
                getRegion().longHeight(),
                getColor().toRGBAHex());
    }

}
