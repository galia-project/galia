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

package is.galia.resource.iiif.v2;

import is.galia.image.Size;
import is.galia.operation.Crop;
import is.galia.operation.CropByPercent;
import is.galia.operation.CropByPixels;
import is.galia.operation.CropToSquare;
import is.galia.resource.IllegalClientArgumentException;
import is.galia.util.StringUtils;

/**
 * Encapsulates the "region" component of a URI.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#region">IIIF Image API 2.0</a>
 * @see <a href="http://iiif.io/api/image/2.1/#region">IIIF Image API 2.1</a>
 */
final class Region {

    enum Type {
        FULL, PERCENT, PIXELS, SQUARE
    }

    private static final double DELTA = 0.00000001;

    private Float x, y, width, height;
    private Type type;

    /**
     * @param uriRegion Region component of a URI.
     * @throws IllegalClientArgumentException if the argument is invalid.
     */
    public static Region fromURI(String uriRegion) {
        final Region region = new Region();
        switch (uriRegion) {
            case "full":
                region.setType(Type.FULL);
                break;
            case "square":
                region.setType(Type.SQUARE);
                break;
            default:
                String csv;
                if (uriRegion.startsWith("pct:")) {
                    region.setType(Type.PERCENT);
                    csv = uriRegion.substring(4);
                } else {
                    region.setType(Type.PIXELS);
                    csv = uriRegion;
                }
                String[] parts = csv.split(",");
                if (parts.length == 4) {
                    region.setX(Float.parseFloat(parts[0]));
                    region.setY(Float.parseFloat(parts[1]));
                    region.setWidth(Float.parseFloat(parts[2]));
                    region.setHeight(Float.parseFloat(parts[3]));
                } else {
                    throw new IllegalClientArgumentException("Invalid region");
                }
                break;
        }
        return region;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Region) {
            final Region region = (Region) obj;
            if (Type.FULL.equals(getType()) &&
                    Type.FULL.equals(region.getType())) {
                return true;
            } else if (Type.SQUARE.equals(getType()) &&
                    Type.SQUARE.equals(region.getType())) {
                return true;
            }
            return getType().equals(region.getType()) &&
                    Math.abs(getX() - region.getX()) < DELTA &&
                    Math.abs(getY() - region.getY()) < DELTA &&
                    Math.abs(getWidth() - region.getWidth()) < DELTA &&
                    Math.abs(getHeight() - region.getHeight()) < DELTA;
        }
        return super.equals(obj);
    }

    public Type getType() {
        return type;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setX(float x) {
        if (x < 0) {
            throw new IllegalClientArgumentException("X must be >= 0");
        }
        this.x = x;
    }

    public void setY(float y) {
        if (y < 0) {
            throw new IllegalClientArgumentException("Y must be >= 0");
        }
        this.y = y;
    }

    public void setWidth(float width) {
        if (width <= 0) {
            throw new IllegalClientArgumentException("Width must be > 0");
        }
        this.width = width;
    }

    public void setHeight(float height) {
        if (height <= 0) {
            throw new IllegalClientArgumentException("Height must be > 0");
        }
        this.height = height;
    }

    /**
     * @return Equivalent instance, or {@code null} if the {@link #getType()
     *         type} is {@link Type#FULL}.
     */
    Crop toCrop() {
        if (Type.FULL.equals(getType())) {
            return new CropByPercent(); // 100% crop
        } else if (Type.SQUARE.equals(getType())) {
            return new CropToSquare();
        } else if (Type.PERCENT.equals(getType())) {
            return new CropByPercent(
                    getX() / 100.0, getY() / 100.0,
                    getWidth() / 100.0, getHeight() / 100.0);
        } else {
            return new CropByPixels(
                    Math.round(getX()), Math.round(getY()),
                    Math.round(getWidth()), Math.round(getHeight()));
        }
    }

    /**
     * @return Value compatible with the region component of an IIIF URI.
     * @see    #toCanonicalString(Size)
     */
    @Override
    public String toString() {
        String str;
        switch (getType()) {
            case FULL:
                str = "full";
                break;
            case SQUARE:
                str = "square";
                break;
            default:
                String x, y;
                if (getType().equals(Type.PERCENT)) {
                    str = "pct:";
                    x = StringUtils.removeTrailingZeroes(getX());
                    y = StringUtils.removeTrailingZeroes(getY());
                } else {
                    str = "";
                    x = Integer.toString(Math.round(getX()));
                    y = Integer.toString(Math.round(getY()));
                }
                str += String.format("%s,%s,%s,%s", x, y,
                        StringUtils.removeTrailingZeroes(getWidth()),
                        StringUtils.removeTrailingZeroes(getHeight()));
                break;
        }
        return str;
    }

    /**
     * @param fullSize Full source image dimensions.
     * @return         Canonical value compatible with the region component of
     *                 an IIIF URI.
     * @see            #toString()
     */
    String toCanonicalString(Size fullSize) {
        if (Type.PERCENT.equals(getType())) {
            return String.format("%d,%d,%d,%d",
                    Math.round(getX() / 100.0 * fullSize.width()),
                    Math.round(getY() / 100.0 * fullSize.height()),
                    Math.round(getWidth() / 100.0 * fullSize.width()),
                    Math.round(getHeight() / 100.0 * fullSize.height()));
        }
        return toString();
    }

}
