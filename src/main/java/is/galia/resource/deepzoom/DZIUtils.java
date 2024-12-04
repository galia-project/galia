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

package is.galia.resource.deepzoom;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.image.Info;
import is.galia.image.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

final class DZIUtils {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DZIUtils.class);

    static final int DEFAULT_MIN_TILE_SIZE = 512;

    /**
     * Computes the full image size at every resolution level.
     *
     * @return Sizes in order from smallest to largest.
     */
    static List<Size> computeFullSizes(Info.Image image) {
        final List<Size> sizes = new LinkedList<>();
        final Size fullSize    = image.getSize();
        double width           = fullSize.width();
        double height          = fullSize.height();
        while (true) {
            sizes.addFirst(new Size(width, height));
            if (width <= 1 && height <= 1) {
                break;
            }
            width  = Math.ceil(width / 2.0);
            height = Math.ceil(height / 2.0);
        }

        // Log the sizes (separately so the levels aren't reversed)
        for (int level = 0, count = sizes.size(); level < count; level++) {
            Size size = sizes.get(level);
            LOGGER.trace("Level {}: {}x{}",
                    level, size.intWidth(), size.intHeight());
        }
        return sizes;
    }

    /**
     * <ul>
     *     <li>If the image is natively tiled:
     *         <ul>
     *             <li>If the image's tile size is smaller than the value of
     *             {@link Key#DEEPZOOM_MIN_TILE_SIZE}, the value of {@link
     *             Key#DEEPZOOM_MIN_TILE_SIZE} is returned.</li>
     *             <li>Otherwise, the native tile size is returned.</li>
     *         </ul>
     *     </li>
     *     <li>If the image is not natively tiled, the value of {@link
     *     Key#DEEPZOOM_MIN_TILE_SIZE} is returned.</li>
     * </ul>
     *
     * @return Effective tile size for the given instance.
     */
    static Size getTileSize(Info.Image image) {
        Size size       = image.getSize();
        Size tileSize   = image.getTileSize();
        int minTileSize = Configuration.forApplication().getInt(
                Key.DEEPZOOM_MIN_TILE_SIZE, DEFAULT_MIN_TILE_SIZE);
        if (tileSize.equals(size) || tileSize.width() < minTileSize ||
                tileSize.height() < minTileSize) {
            tileSize = new Size(minTileSize, minTileSize);
        }
        return tileSize;
    }

    private DZIUtils() {}

}
