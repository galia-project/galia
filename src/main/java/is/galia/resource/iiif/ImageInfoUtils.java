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

package is.galia.resource.iiif;

import is.galia.image.Size;

public final class ImageInfoUtils {

    /**
     * @param fullSize     Full size of the source image.
     * @param minDimension Minimum allowed dimension.
     * @return             Maximum reduction factor to be able to fit above the
     *                     minimum allowed dimension.
     */
    public static int maxReductionFactor(final Size fullSize,
                                         final int minDimension) {
        if (minDimension <= 0) {
            throw new IllegalArgumentException("minDimension must be a positive number.");
        }
        double nextDimension = Math.min(fullSize.width(), fullSize.height());
        int factor = -1;
        for (int i = 0; i < 9999; i++) {
            nextDimension /= 2.0;
            if (nextDimension < minDimension) {
                factor = i;
                break;
            }
        }
        return factor;
    }

    /**
     * @param fullSize  Full size of the source image.
     * @param maxPixels Maximum allowed number of pixels.
     * @return          Minimum reduction factor to be able to fit below the
     *                  maximum allowed number of pixels.
     */
    public static int minReductionFactor(final Size fullSize,
                                         final long maxPixels) {
        if (maxPixels <= 0) {
            throw new IllegalArgumentException("maxPixels must be a positive number.");
        }
        int factor = 0;
        Size nextSize = fullSize;
        while (nextSize.area() > maxPixels) {
            nextSize = nextSize.scaled(0.5);
            factor++;
        }
        return factor;
    }

    /**
     * Calculates an optimal information tile size based on a given physical
     * image tile size.
     *
     * @param fullSize         Size of the full source image.
     * @param physicalTileSize Size of the source image's tiles. If the source
     *                         image is not natively tiled, this should be
     *                         equal to {@literal fullSize}.
     * @param minSize          Minimum allowed dimension.
     * @return                 Information tile size.
     */
    public static Size getTileSize(final Size fullSize,
                                   final Size physicalTileSize,
                                   final int minSize) {
        final double minW = Math.min(minSize, fullSize.width());
        final double minH = Math.min(minSize, fullSize.height());

        // If true, the image is not natively tiled. Use minSize as the tile
        // size.
        if (physicalTileSize.longWidth() == fullSize.longWidth()) {
            return new Size(minW, minH);
        }

        Size infoTileSize = new Size(physicalTileSize);
        while (infoTileSize.width() < minW || infoTileSize.height() < minH) {
            infoTileSize = infoTileSize.scaled(2);
        }

        // Limit tile dimensions to the full image size.
        if (infoTileSize.width() > fullSize.width() ||
                infoTileSize.height() > fullSize.height()) {
            infoTileSize = new Size(fullSize.width(), fullSize.height());
        }
        return infoTileSize;
    }

    private ImageInfoUtils() {}

}
