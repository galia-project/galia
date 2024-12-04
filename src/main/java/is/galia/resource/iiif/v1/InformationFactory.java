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

package is.galia.resource.iiif.v1;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds new information instances.
 */
final class InformationFactory {

    private static final int MIN_SIZE = 64;

    /**
     * Will be used if {@link Key#IIIF_MIN_TILE_SIZE} is not set.
     */
    private static final int DEFAULT_MIN_TILE_SIZE = 512;

    Map<String,Object> newImageInfo(final String imageURI,
                                    final Info info,
                                    final int imageIndex,
                                    ScaleConstraint scaleConstraint) {
        if (scaleConstraint == null) {
            scaleConstraint = new ScaleConstraint(1, 1);
        }
        final ComplianceLevel complianceLevel = ComplianceLevel.getLevel(
                EncoderFactory.getAllSupportedFormats());

        final int minTileSize = Configuration.forApplication().
                getInt(Key.IIIF_MIN_TILE_SIZE, DEFAULT_MIN_TILE_SIZE);

        // Find a tile width and height. If the image is not tiled,
        // calculate a tile size close to Key.IIIF_MIN_TILE_SIZE pixels.
        // Otherwise, use the smallest multiple of the tile size above
        // Key.IIIF_MIN_TILE_SIZE of image resolution 0.
        final Metadata metadata = info.getMetadata();
        final Orientation orientation = (metadata != null) ?
                metadata.getOrientation() : Orientation.ROTATE_0;
        Size virtualSize = orientation.adjustedSize(info.getSize(imageIndex));
        final double scScale = scaleConstraint.rational().doubleValue();
        virtualSize = virtualSize.scaled(scScale);
        Size virtualTileSize = orientation.adjustedSize(
                info.getImages().get(imageIndex).getTileSize());
        virtualTileSize = virtualTileSize.scaled(scScale);

        if (info.getNumResolutions() > 0) {
            if (!virtualTileSize.equals(virtualSize)) {
                virtualTileSize = ImageInfoUtils.getTileSize(
                        virtualSize, virtualTileSize, minTileSize);
            }
        }

        // Create an Info instance, which will eventually be serialized
        // to JSON and sent as the response body.
        final Map<String,Object> imageInfo = new LinkedHashMap<>();
        imageInfo.put("@context", "http://library.stanford.edu/iiif/image-api/1.1/context.json");
        imageInfo.put("@id",  imageURI);
        imageInfo.put("width", virtualSize.longWidth());
        imageInfo.put("height", virtualSize.longHeight());

        // scale factors
        List<Integer> scaleFactors = new ArrayList<>();
        imageInfo.put("scale_factors", scaleFactors);
        int maxReductionFactor =
                ImageInfoUtils.maxReductionFactor(virtualSize, MIN_SIZE);
        for (int i = 0; i <= maxReductionFactor; i++) {
            scaleFactors.add((int) Math.pow(2, i));
        }

        // Round up to prevent clients from requesting narrow edge tiles.
        imageInfo.put("tile_width", (long) Math.ceil(virtualTileSize.width()));
        imageInfo.put("tile_height", (long) Math.ceil(virtualTileSize.height()));

        // formats
        List<String> formats = new ArrayList<>();
        imageInfo.put("formats", formats);
        for (Format format : EncoderFactory.getAllSupportedFormats()) {
            formats.add(format.getPreferredExtension());
        }

        // qualities
        List<String> qualities = new ArrayList<>();
        imageInfo.put("qualities", qualities);
        for (Quality quality : Quality.values()) {
            qualities.add(quality.toString().toLowerCase());
        }

        imageInfo.put("profile", complianceLevel.getURI());

        // This is our own custom key, not defined by the spec--but the spec
        // doesn't seem to forbid it.
        imageInfo.put("page_count", info.getNumPages());

        return imageInfo;
    }

}
