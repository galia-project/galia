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

/**
 * Processor-dependent feature.
 */
enum ProcessorFeature implements Feature {

    /**
     * The image may be rotated around the vertical axis, resulting in a
     * left-to-right mirroring of the content.
     */
    MIRRORING("mirroring"),

    /**
     * Regions of images may be requested by percentage.
     */
    REGION_BY_PERCENT("regionByPct"),

    /**
     * Regions of images may be requested by pixel dimensions.
     */
    REGION_BY_PIXELS("regionByPx"),

    /**
     * <p>A square region where the width and height are equal to the shorter
     * dimension of the complete image content.</p>
     *
     * <p>New in Image API 2.1.</p>
     */
    REGION_SQUARE("regionSquare"),

    /**
     * Rotation of images may be requested by degrees other than multiples of
     * 90.
     */
    ROTATION_ARBITRARY("rotationArbitrary"),

    /**
     * Rotation of images may be requested by degrees in multiples of 90.
     */
    ROTATION_BY_90S("rotationBy90s"),

    /**
     * Sizes larger than the full size may be requested.
     */
    SIZE_ABOVE_FULL("sizeAboveFull"),

    /**
     * <p>Size of images may be requested in the form {@literal !w,h}.</p>
     *
     * <p>New in Image API 2.1.</p>
     */
    SIZE_BY_CONFINED_WIDTH_HEIGHT("sizeByConfinedWh"),

    /**
     * <p>Size of images may be requested in the form {@literal w,h}, including
     * sizes that would distort the image.</p>
     *
     * <p>New in Image API 2.1.</p>
     * */
    SIZE_BY_DISTORTED_WIDTH_HEIGHT("sizeByDistortedWh"),

    /**
     * Deprecated in Image API 2.1; to be replaced with {@link
     * #SIZE_BY_CONFINED_WIDTH_HEIGHT}.
     */
    SIZE_BY_FORCED_WIDTH_HEIGHT("sizeByForcedWh"),

    /**
     * Size of images may be requested in the form {@literal ,h}.
     */
    SIZE_BY_HEIGHT("sizeByH"),

    /**
     * Size of images may be requested in the form {@literal pct:n}.
     */
    SIZE_BY_PERCENT("sizeByPct"),

    /**
     * Size of images may be requested in the form {@literal w,}.
     */
    SIZE_BY_WIDTH("sizeByW"),

    /**
     * Size of images may be requested in the form {@literal w,h} where the
     * supplied {@literal w} and {@literal h} preserve the aspect ratio.
     */
    SIZE_BY_WIDTH_HEIGHT("sizeByWh");

    private final String name;

    ProcessorFeature(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    /**
     * @return The name.
     */
    public String toString() {
        return this.getName();
    }

}
