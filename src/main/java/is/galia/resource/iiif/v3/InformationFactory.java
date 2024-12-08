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

import is.galia.codec.EncoderFactory;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.image.Info;
import is.galia.image.Metadata;
import is.galia.image.Orientation;
import is.galia.image.ScaleConstraint;
import is.galia.resource.iiif.ImageInfoUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds new information instances.
 */
final class InformationFactory {

    private static final String IIIF_CONTEXT =
            "http://iiif.io/api/image/3/context.json";
    private static final String LOCAL_CONTEXT =
            "http://galia.is/ns/iiif/image/3/context.json";

    private static final double DELTA = 0.00000001;

    /**
     * Used when {@link Key#IIIF_MIN_SIZE} is not set.
     */
    private static final int DEFAULT_MIN_SIZE = 64;

    /**
     * Used when {@link Key#IIIF_MIN_TILE_SIZE} is not set.
     */
    private static final int DEFAULT_MIN_TILE_SIZE = 512;

    private static final String PROFILE  = "level2";
    private static final String PROTOCOL = "http://iiif.io/api/image";
    private static final String TYPE     = "ImageService3";

    private double maxScale;
    private long maxPixels;
    private int minSize, minTileSize;

    InformationFactory() {
        var config  = Configuration.forApplication();
        maxPixels   = config.getInt(Key.MAX_PIXELS, 0);
        maxScale    = config.getDouble(Key.MAX_SCALE, Double.MAX_VALUE);
        minSize     = config.getInt(Key.IIIF_MIN_SIZE, DEFAULT_MIN_SIZE);
        minTileSize = config.getInt(Key.IIIF_MIN_TILE_SIZE, DEFAULT_MIN_TILE_SIZE);
    }

    /**
     * @param info            Instance describing the image.
     * @param infoImageIndex  Index of the full/main image in the {@link Info}
     *                        argument's {@link Info#getImages()} list.
     * @param scaleConstraint May be {@code null}.
     */
    Map<String,Object> newImageInfo(final String imageURI,
                                    final Info info,
                                    final int infoImageIndex,
                                    ScaleConstraint scaleConstraint) {
        if (scaleConstraint == null) {
            scaleConstraint = new ScaleConstraint(1, 1);
        }
        // We want to use the orientation-aware full size, which takes the
        // embedded orientation into account.
        final Metadata metadata = info.getMetadata();
        final Orientation orientation = (metadata != null) ?
                metadata.getOrientation() : Orientation.ROTATE_0;
        Size virtualSize = orientation.adjustedSize(info.getSize(infoImageIndex));
        virtualSize = virtualSize.scaled(scaleConstraint.rational().doubleValue());

        // Create a Map instance, which will eventually be serialized to JSON
        // and returned in the response body. LinkedHashMap preserves insertion
        // order.
        final Map<String,Object> responseInfo = new LinkedHashMap<>();
        // "The @context property should appear as the very first key-value
        // pair of the JSON representation. Its value must be either the URI
        // http://iiif.io/api/image/3/context.json or a JSON array with [that
        // URI] as the last item. ...
        // If extensions are used then their context definitions should be
        // included in this top-level @context property.
        responseInfo.put("@context", List.of(LOCAL_CONTEXT, IIIF_CONTEXT));
        responseInfo.put("id", imageURI);
        responseInfo.put("type", TYPE);
        responseInfo.put("protocol", PROTOCOL);
        responseInfo.put("profile", PROFILE);
        responseInfo.put("width", virtualSize.longWidth());
        responseInfo.put("height", virtualSize.longHeight());

        { // maxArea
            // N.B.: maxWidth and maxHeight are not supported as maxArea more
            // succinctly fulfills the "emergency brake" role.
            long effectiveMaxPixels = getEffectiveMaxPixels(virtualSize);
            if (effectiveMaxPixels > 0) {
                responseInfo.put("maxArea", effectiveMaxPixels);
            }
        }
        { // sizes
            var sizes = getSizes(virtualSize);
            responseInfo.put("sizes", sizes);
        }
        { // tiles
            var tiles = getTiles(virtualSize, orientation, info.getImages());
            responseInfo.put("tiles", tiles);
        }
        { // pageCount (local key, not an IIIF key)
            // It would be nice to make this a "pages" key with links to all
            // pages, but we can't really know the page URIs from the server
            // side, as their identifiers may be supplied by an upstream proxy
            // via X-Forwarded-ID headers.
            responseInfo.put("pageCount", info.getNumPages());
        }
        { // extraQualities
            var qualityStrings = Arrays.stream(Quality.values())
                    .filter(q -> !Quality.DEFAULT.equals(q))
                    .map(q -> q.toString().toLowerCase())
                    .toList();
            responseInfo.put("extraQualities", qualityStrings);
        }
        { // extraFormats
            var formatStrings = EncoderFactory.getAllSupportedFormats()
                    .stream()
                    .filter(f -> !"jpg".equals(f.key()) && !"png".equals(f.key()))
                    .map(Format::getPreferredExtension)
                    .toList();
            responseInfo.put("extraFormats", formatStrings);
        }
        { // extraFeatures
            ScaleConstraint sc = scaleConstraint;
            var featureStrings = Arrays.stream(Feature.values())
                    // sizeUpscaling is not available if the info is being used
                    // for a virtual scale-constrained version, or if upscaling
                    // is disallowed in the configuration.
                    .filter(f -> !(Feature.SIZE_UPSCALING.equals(f) && (sc.hasEffect() || maxScale <= 1)))
                    .map(Feature::getName)
                    .toList();
            responseInfo.put("extraFeatures", featureStrings);
        }
        return responseInfo;
    }

    /**
     * Returns a 2^n series that will work for both multi-and monoresolution
     * images.
     *
     * @param virtualSize Orientation-aware and {@link ScaleConstraint
     *                    scale-constrained} full size.
     */
    List<Map<String,Long>> getSizes(Size virtualSize) {
        // This will be a 2^n series that will work for both multi- and
        // monoresolution images.
        final List<Map<String,Long>> sizes = new ArrayList<>();

        // The min reduction factor is the smallest number of reductions that
        // are required in order to fit within maxPixels.
        final long effectiveMaxPixels = getEffectiveMaxPixels(virtualSize);
        final long minReductionFactor  = (effectiveMaxPixels > 0) ?
                ImageInfoUtils.minReductionFactor(virtualSize, effectiveMaxPixels) : 0;
        // The max reduction factor is the number of times the full image
        // dimensions can be halved until they're smaller than minSize.
        final long maxReductionFactor =
                ImageInfoUtils.maxReductionFactor(virtualSize, minSize);

        for (double i = Math.pow(2, minReductionFactor);
             i <= Math.pow(2, maxReductionFactor);
             i *= 2) {
            Map<String,Long> size = new LinkedHashMap<>();
            size.put("width", Math.round(virtualSize.width() / i));
            size.put("height", Math.round(virtualSize.height() / i));
            sizes.addFirst(size);
        }
        return sizes;
    }

    /**
     * <p>Finds a tile width and height.</p>
     *
     * <p>If the image is not tiled, a tile size is chosen that is close to the
     * minimum allowed. Otherwise, the smallest multiple of the tile size above
     * that of image resolution 0 is used.</p>
     *
     * <p>This is not a canonical listing of tiles that are actually encoded in
     * the image, but instead a hint to the client as to what is efficient to
     * deliver (which may or may not match the physical tile size or a multiple
     * of it).</p>
     */
    List<Map<String,Object>> getTiles(Size virtualSize,
                                      Orientation orientation,
                                      List<Info.Image> images) {
        final List<Map<String,Object>> tiles = new ArrayList<>();
        final Set<Size> uniqueTileSizes      = new HashSet<>();

        images.forEach(image ->
                uniqueTileSizes.add(ImageInfoUtils.getTileSize(
                        virtualSize,
                        orientation.adjustedSize(image.getTileSize()),
                        minTileSize)));

        // The max reduction factor is the maximum number of times the full
        // image size can be halved until it's smaller than minSize.
        final int maxReductionFactor =
                ImageInfoUtils.maxReductionFactor(virtualSize, minSize);

        for (Size uniqueTileSize : uniqueTileSizes) {
            final Map<String,Object> tile = new LinkedHashMap<>();
            tile.put("width", (long) Math.ceil(uniqueTileSize.width()));
            tile.put("height", (long) Math.ceil(uniqueTileSize.height()));
            final List<Integer> scaleFactors = new ArrayList<>();
            tile.put("scaleFactors", scaleFactors);
            // Add every scale factor up to 2^RFmax.
            for (int i = 0; i <= maxReductionFactor; i++) {
                scaleFactors.add((int) Math.pow(2, i));
            }
            tiles.add(tile);
        }
        return tiles;
    }

    /**
     * @param fullSize Full source image size.
     * @return         The smaller of {@link #maxPixels} or the area at {@link
     *                 #maxScale}.
     */
    private long getEffectiveMaxPixels(Size fullSize) {
        final double area = fullSize.area();
        if (maxPixels == 0) {
            return Math.round(area * maxScale);
        } else if (maxScale < DELTA) {
            return maxPixels;
        }
        return (long) Math.min(area * maxScale, maxPixels);
    }

    /**
     * @param maxPixels Maximum number of pixels that will be used in {@code
     *                  sizes} keys.
     */
    void setMaxPixels(long maxPixels) {
        this.maxPixels = maxPixels;
    }

    /**
     * @param maxScale Maximum allowed scale.
     */
    void setMaxScale(double maxScale) {
        this.maxScale = maxScale;
    }

    /**
     * @param minSize Minimum size that will be used in {@code sizes} keys.
     */
    void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    /**
     * @param minTileSize Minimum size that will be used in a tile dimension.
     */
    void setMinTileSize(int minTileSize) {
        this.minTileSize = minTileSize;
    }

}