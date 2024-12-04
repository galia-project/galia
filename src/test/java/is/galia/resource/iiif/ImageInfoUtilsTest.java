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
import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ImageInfoUtilsTest extends BaseTest {

    @Test
    void testMaxReductionFactor() {
        Size fullSize = new Size(1024, 1024);
        int minDimension = 100;
        assertEquals(3, ImageInfoUtils.maxReductionFactor(fullSize, minDimension));

        fullSize = new Size(1024, 512);
        minDimension = 100;
        assertEquals(2, ImageInfoUtils.maxReductionFactor(fullSize, minDimension));
    }

    @Test
    void testMaxReductionFactorWithZeroMinDimension() {
        Size fullSize = new Size(1024, 1024);
        int minDimension = 0;
        assertThrows(IllegalArgumentException.class,
                () -> ImageInfoUtils.maxReductionFactor(fullSize, minDimension));
    }

    @Test
    void testMinReductionFactor() {
        Size fullSize = new Size(50, 50);
        int maxPixels = 10000;
        assertEquals(0, ImageInfoUtils.minReductionFactor(fullSize, maxPixels));

        fullSize = new Size(100, 100);
        maxPixels = 10000;
        assertEquals(0, ImageInfoUtils.minReductionFactor(fullSize, maxPixels));

        fullSize = new Size(200, 100);
        maxPixels = 10000;
        assertEquals(1, ImageInfoUtils.minReductionFactor(fullSize, maxPixels));

        fullSize = new Size(300, 300);
        maxPixels = 10000;
        assertEquals(2, ImageInfoUtils.minReductionFactor(fullSize, maxPixels));
    }

    @Test
    void testMinReductionFactorWithZeroMaxPixels() {
        Size fullSize = new Size(50, 50);
        int maxPixels = 0;
        assertThrows(IllegalArgumentException.class,
                () -> ImageInfoUtils.minReductionFactor(fullSize, maxPixels));
    }

    @Test
    void testGetTileSizeWithTiledImage() {
        // full size > tile size > min tile size
        Size fullSize = new Size(1024, 1024);
        Size tileSize = new Size(512, 512);
        int minTileSize = 128;
        assertEquals(new Size(512, 512),
                ImageInfoUtils.getTileSize(fullSize, tileSize, minTileSize));

        // full size > min tile size > tile size
        fullSize = new Size(1024, 1024);
        tileSize = new Size(128, 100);
        minTileSize = 512;
        assertEquals(new Size(1024, 800),
                ImageInfoUtils.getTileSize(fullSize, tileSize, minTileSize));

        // min tile size > full size > tile size
        fullSize = new Size(512, 512);
        tileSize = new Size(128, 128);
        minTileSize = 768;
        assertEquals(new Size(512, 512),
                ImageInfoUtils.getTileSize(fullSize, tileSize, minTileSize));
    }

    @Test
    void testGetTileSizeWithUntiledImage() {
        // full size > min tile size
        Size fullSize = new Size(1024, 1024);
        Size tileSize = new Size(fullSize);
        int minTileSize = 128;
        assertEquals(new Size(minTileSize, minTileSize),
                ImageInfoUtils.getTileSize(fullSize, tileSize, minTileSize));

        // full size < min tile size
        fullSize = new Size(512, 512);
        tileSize = new Size(fullSize);
        minTileSize = 768;
        assertEquals(new Size(512, 512),
                ImageInfoUtils.getTileSize(fullSize, tileSize, minTileSize));
    }

}
