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

package is.galia.resource.iiif.v3;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.operation.Scale;
import is.galia.operation.ScaleByPercent;
import is.galia.operation.ScaleByPixels;
import is.galia.resource.IllegalClientArgumentException;
import is.galia.util.StringUtils;

import java.util.Objects;

/**
 * Encapsulates the size component of a URI.
 *
 * @see <a href="https://iiif.io/api/image/3.0/#42-size">IIIF Image API 3.0:
 * Size</a>
 */
final class Size {

    /**
     * <p>Type of size specification, corresponding to the options available
     * in <a href="https://iiif.io/api/image/3.0/#42-size">IIIF Image API 3.0:
     * Size</a>.</p>
     *
     * <p>This does not take into account the leading carat indicating
     * "upscaling allowed;" that is handled by {@link #isUpscalingAllowed()}
     * which can work in conjunction with any of these.</p>
     */
    enum Type {

        /**
         * Represents a {@code max} size argument.
         */
        MAX,

        /**
         * Represents a size argument in {@code w,} format.
         */
        ASPECT_FIT_WIDTH,

        /**
         * Represents a size argument in {@code ,h} format.
         */
        ASPECT_FIT_HEIGHT,

        /**
         * Represents a size argument in {@code w,h} format.
         */
        NON_ASPECT_FILL,

        /**
         * Represents a size argument in {@code !w,h} format.
         */
        ASPECT_FIT_INSIDE

    }

    private static final double DELTA            = 0.00000001;
    private static final String MAX_SIZE_KEYWORD = "max";
    private static final String PERCENT_KEYWORD  = "pct";

    private Integer width, height;
    private Float percent;
    private Type type;
    private boolean isExact, isUpscalingAllowed;

    /**
     * @param uriSize The {@literal size} component of a URI.
     * @return        Size corresponding to the argument.
     * @throws IllegalClientArgumentException if the argument is invalid.
     */
    static Size fromURI(String uriSize) {
        Size size = new Size();

        // Decode the path component.
        uriSize = uriSize.replace("%5E", "^");
        if (uriSize.startsWith("^")) {
            if (!uriSize.startsWith("^!") && !uriSize.contains("max")) {
                size.setExact(true);
            }
            size.setUpscalingAllowed(true);
            uriSize = uriSize.substring(1);
        }
        try {
            if (MAX_SIZE_KEYWORD.equals(uriSize)) {
                size.setType(Type.MAX);
            } else {
                if (uriSize.endsWith(",")) {
                    size.setType(Type.ASPECT_FIT_WIDTH);
                    size.setWidth(Integer.parseInt(StringUtils.stripEnd(uriSize, ",")));
                    size.setExact(true);
                } else if (uriSize.startsWith(",")) {
                    size.setType(Type.ASPECT_FIT_HEIGHT);
                    size.setHeight(Integer.parseInt(StringUtils.stripStart(uriSize, ",")));
                    size.setExact(true);
                } else if (uriSize.startsWith("pct:")) {
                    size.setType(Type.ASPECT_FIT_INSIDE);
                    size.setPercent(Float.parseFloat(StringUtils.stripStart(uriSize, PERCENT_KEYWORD + ":")));
                    size.setExact(true);
                } else if (uriSize.startsWith("!")) {
                    size.setType(Type.ASPECT_FIT_INSIDE);
                    String[] parts = StringUtils.stripStart(uriSize, "!").split(",");
                    if (parts.length == 2) {
                        size.setWidth(Integer.parseInt(parts[0]));
                        size.setHeight(Integer.parseInt(parts[1]));
                    }
                } else {
                    size.setType(Type.NON_ASPECT_FILL);
                    size.setExact(true);
                    String[] parts = uriSize.split(",");
                    if (parts.length == 2) {
                        size.setWidth(Integer.parseInt(parts[0]));
                        size.setHeight(Integer.parseInt(parts[1]));
                    } else {
                        throw new IllegalClientArgumentException("Invalid size");
                    }
                }
            }
        } catch (IllegalClientArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalClientArgumentException("Invalid size");
        }
        return size;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Size other) {
            return toString().equals(other.toString());
        }
        return false;
    }

    /**
     * @return Height. May be {@code null}.
     */
    Integer getHeight() {
        return height;
    }

    /**
     * @return Float between {@code 0} and {@code 100. May be {@code null}.
     */
    Float getPercent() {
        return percent;
    }

    Type getType() {
        return type;
    }

    /**
     * @return Width. May be {@code null}.
     */
    Integer getWidth() {
        return width;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, percent, type, isUpscalingAllowed);
    }

    /**
     * @return Whether the dimension(s) are exact values.
     */
    boolean isExact() {
        return isExact;
    }

    /**
     * @return Whether upscaling can be performed if necessary.
     */
    boolean isUpscalingAllowed() {
        return isUpscalingAllowed;
    }

    void setExact(boolean exact) {
        this.isExact = exact;
    }

    /**
     * @throws IllegalClientArgumentException if the given height is {@code
     *         null} or not positive.
     */
    void setHeight(Integer height) {
        if (height != null && height <= 0) {
            throw new IllegalClientArgumentException(
                    "Height must be a positive integer");
        }
        this.height = height;
    }

    /**
     * @param percent Float between {@code 0} and {@code 100}.
     * @throws IllegalClientArgumentException if the given percent is {@code
     *         null} or not positive.
     */
    void setPercent(Float percent) {
        if (percent != null && percent <= 0) {
            throw new IllegalClientArgumentException("Percent must be positive");
        }
        this.percent = percent;
    }

    void setType(Type type) {
        this.type = type;
    }

    void setUpscalingAllowed(boolean isUpscalingAllowed) {
        this.isUpscalingAllowed = isUpscalingAllowed;
    }

    /**
     * @throws IllegalClientArgumentException if the given width is {@code
     *         null} or not positive.
     */
    void setWidth(Integer width) {
        if (width != null && width <= 0) {
            throw new IllegalClientArgumentException(
                    "Width must be a positive integer");
        }
        this.width = width;
    }

    /**
     * @param maxScale Maximum scale allowed by the application configuration.
     */
    Scale toScale(double maxScale) {
        if (getPercent() != null) {
            return new ScaleByPercent(getPercent() / 100.0);
        }
        switch (getType()) {
            case MAX:
                if (maxScale > DELTA) {
                    return new ScaleByPercent(isUpscalingAllowed() ? maxScale : 1);
                } else {
                    Configuration config = Configuration.forApplication();
                    final long maxPixels = config.getLong(Key.MAX_PIXELS, 0);
                    if (maxPixels > 0) {
                        // Using the square root of max_pixels is not ideal,
                        // but we don't yet know the source image dimensions in
                        // order to compute a more accurate size.
                        final int dims = (int) Math.floor(Math.sqrt(maxPixels));
                        return new ScaleByPixels(dims, dims, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
                    } else {
                        return new ScaleByPercent(1);
                    }
                }
            case ASPECT_FIT_WIDTH:
                return new ScaleByPixels(
                        getWidth(), null, ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
            case ASPECT_FIT_HEIGHT:
                return new ScaleByPixels(
                        null, getHeight(), ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
            case ASPECT_FIT_INSIDE:
                return new ScaleByPixels(
                        getWidth(), getHeight(), ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
            case NON_ASPECT_FILL:
                return new ScaleByPixels(
                        getWidth(), getHeight(), ScaleByPixels.Mode.NON_ASPECT_FILL);
            default:
                throw new IllegalArgumentException(
                        "Unknown scale mode. This is probably a bug.");
        }
    }

    /**
     * @return Value compatible with the size component of a URI.
     */
    @Override
    public String toString() {
        String str = "";
        if (isUpscalingAllowed()) {
            str += "^";
        }
        if (Type.MAX.equals(getType())) {
            str += MAX_SIZE_KEYWORD;
        } else if (getPercent() != null) {
            str += PERCENT_KEYWORD + ":" +
                    StringUtils.removeTrailingZeroes(getPercent());
        } else {
            if (Type.ASPECT_FIT_INSIDE.equals(getType())) {
                str += "!";
            }
            if (getWidth() != null && getWidth() > 0) {
                str += getWidth();
            }
            str += ",";
            if (getHeight() != null && getHeight() > 0) {
                str += getHeight();
            }
        }
        return str;
    }

    /**
     * @param fullSize Full source image dimensions.
     * @return         Canonical value compatible with the size component of a
     *                 URI.
     * @see            #toString()
     */
    String toCanonicalString(is.galia.image.Size fullSize) {
        if (Type.MAX.equals(getType())) {
            return toString();
        }

        StringBuilder b = new StringBuilder();
        long w, h;
        boolean isUp;

        if (getPercent() != null) {
            isUp = (getPercent() > 100);
            w = Math.round(fullSize.width() * getPercent() / 100.0);
            h = Math.round(fullSize.height() * getPercent() / 100.0);
        } else {
            if (getWidth() == null || getWidth() == 0) {
                double scale = getHeight() / fullSize.height();
                w = Math.round(fullSize.width() * scale);
            } else {
                w = getWidth();
            }
            if (getHeight() == null || getHeight() == 0) {
                double scale = w / fullSize.width();
                h = Math.round(fullSize.height() * scale);
            } else {
                h = getHeight();
            }
            isUp = (w > fullSize.width() || h > fullSize.height());
        }
        if (isUp) {
            b.append("^");
        }
        b.append(w);
        b.append(",");
        b.append(h);
        return b.toString();
    }

}
