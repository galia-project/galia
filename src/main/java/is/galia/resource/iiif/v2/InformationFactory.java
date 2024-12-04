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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds new information instances.
 */
final class InformationFactory {

    /**
     * Will be used if {@link Key#IIIF_MIN_SIZE} is not set.
     */
    private static final int DEFAULT_MIN_SIZE = 64;

    /**
     * Will be used if {@link Key#IIIF_MIN_TILE_SIZE} is not set.
     */
    private static final int DEFAULT_MIN_TILE_SIZE = 512;

    private static final Set<ServiceFeature> SUPPORTED_SERVICE_FEATURES =
            Collections.unmodifiableSet(
                    EnumSet.of(ServiceFeature.SIZE_BY_WHITELISTED,
                            ServiceFeature.BASE_URI_REDIRECT,
                            ServiceFeature.CANONICAL_LINK_HEADER,
                            ServiceFeature.CORS,
                            ServiceFeature.JSON_LD_MEDIA_TYPE,
                            ServiceFeature.PROFILE_LINK_HEADER));

    private double maxScale;
    private int maxPixels, minSize, minTileSize;

    InformationFactory() {
        var config  = Configuration.forApplication();
        maxPixels   = config.getInt(Key.MAX_PIXELS, 0);
        maxScale    = config.getDouble(Key.MAX_SCALE, Double.MAX_VALUE);
        minSize     = config.getInt(Key.IIIF_MIN_SIZE, DEFAULT_MIN_SIZE);
        minTileSize = config.getInt(Key.IIIF_MIN_TILE_SIZE, DEFAULT_MIN_TILE_SIZE);
    }

    /**
     * @param imageURI        May be {@code null}.
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
        // Ideally we would make this an array in order to include a URI for
        // our locally-defined custom keys, like in the 3.0 endpoint, but the
        // 2.1.1 spec says, "This must be the URI:
        // http://iiif.io/api/image/2/context.json for version 2.1 of the IIIF
        // Image API."
        responseInfo.put("@context", "http://iiif.io/api/image/2/context.json");
        responseInfo.put("@id", imageURI);
        responseInfo.put("protocol", "http://iiif.io/api/image");
        responseInfo.put("width", virtualSize.longWidth());
        responseInfo.put("height", virtualSize.longHeight());

        // sizes -- this will be a 2^n series that will work for both multi-
        // and monoresolution images.
        final List<Map<String,Long>> sizes = getSizes(virtualSize);
        responseInfo.put("sizes", sizes);

        // The max reduction factor is the maximum number of times the full
        // image size can be halved until it's smaller than minSize.
        final int maxReductionFactor =
                ImageInfoUtils.maxReductionFactor(virtualSize, minSize);

        // tiles -- this is not a canonical listing of tiles that are
        // actually encoded in the image, but rather a hint to the client as
        // to what is efficient to deliver.
        final Set<Size> uniqueTileSizes = new HashSet<>();

        // Find a tile width and height. If the image is not tiled,
        // calculate a tile size close to minTileSize pixels.
        // Otherwise, use the smallest multiple of the tile size above that
        // of image resolution 0.
        final List<Map<String,Object>> tiles = new ArrayList<>();
        responseInfo.put("tiles", tiles);

        final Size finalVirtualSize = virtualSize;
        info.getImages().forEach(image ->
                uniqueTileSizes.add(ImageInfoUtils.getTileSize(
                        finalVirtualSize,
                        orientation.adjustedSize(image.getTileSize()),
                        minTileSize)));

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

        { // pageCount (local key, not an IIIF key)
            // It would be nice to make this a "pages" key with links to all
            // pages, but we can't really know the page URIs from the server
            // side, as their identifiers may be supplied by an upstream proxy
            // via X-Forwarded-ID headers.
            responseInfo.put("pageCount", info.getNumPages());
        }

        final List<Object> profile = new ArrayList<>(2);
        responseInfo.put("profile", profile);

        final String complianceUri = ComplianceLevel.getLevel(
                SUPPORTED_SERVICE_FEATURES,
                EncoderFactory.getAllSupportedFormats()).getURI();
        profile.add(complianceUri);

        final Map<String, Object> profileMap = new HashMap<>();
        { // formats
            Set<String> formatStrings = new HashSet<>();
            for (Format format : EncoderFactory.getAllSupportedFormats()) {
                formatStrings.add(format.getPreferredExtension());
            }
            profileMap.put("formats", formatStrings);
            profile.add(profileMap);
        }
        { // maxArea
            // N.B.: maxWidth and maxHeight are not supported as maxArea more
            // succinctly fulfills the "emergency brake" role.
            final int effectiveMaxPixels = getEffectiveMaxPixels(virtualSize);
            if (effectiveMaxPixels > 0) {
                profileMap.put("maxArea", effectiveMaxPixels);
            }
        }
        { // qualities
            final Set<String> qualityStrings = new HashSet<>();
            for (Quality quality : Quality.values()) {
                qualityStrings.add(quality.toString().toLowerCase());
            }
            profileMap.put("qualities", qualityStrings);
        }
        { // supports
            final Set<String> featureStrings = new HashSet<>();
            for (Feature pFeature : ProcessorFeature.values()) {
                // sizeAboveFull should not be available if the info is being used
                // for a virtual scale-constrained version, or if upscaling is
                // disallowed in the configuration.
                if (ProcessorFeature.SIZE_ABOVE_FULL.equals(pFeature) &&
                        (scaleConstraint.hasEffect() || maxScale <= 1)) {
                    continue;
                }
                featureStrings.add(pFeature.getName());
            }
            for (Feature sFeature : SUPPORTED_SERVICE_FEATURES) {
                featureStrings.add(sFeature.getName());
            }
            profileMap.put("supports", featureStrings);
        }
        return responseInfo;
    }

    /**
     * @param virtualSize Orientation-aware and {@link ScaleConstraint
     *                    scale-constrained} full size.
     */
    List<Map<String,Long>> getSizes(Size virtualSize) {
        // This will be a 2^n series that will work for both multi- and
        // monoresolution images.
        final List<Map<String,Long>> sizes = new ArrayList<>();

        // The min reduction factor is the smallest number of reductions that
        // are required in order to fit within maxPixels.
        final int effectiveMaxPixels = getEffectiveMaxPixels(virtualSize);
        final int minReductionFactor = (effectiveMaxPixels > 0) ?
                ImageInfoUtils.minReductionFactor(virtualSize, effectiveMaxPixels) : 0;
        // The max reduction factor is the number of times the full image
        // dimensions can be halved until they're smaller than minSize.
        final int maxReductionFactor =
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
     * @param fullSize Full source image size.
     * @return         The smaller of {@link #maxPixels} or the area at {@link
     *                 #maxScale}.
     */
    private int getEffectiveMaxPixels(Size fullSize) {
        return (int) Math.min(fullSize.area() * maxScale, maxPixels);
    }

    /**
     * @param maxPixels Maximum number of pixels that will be used in {@code
     *                  sizes} keys.
     */
    void setMaxPixels(int maxPixels) {
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
