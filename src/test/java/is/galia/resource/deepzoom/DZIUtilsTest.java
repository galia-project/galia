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
import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DZIUtilsTest extends BaseTest {

    /* computeFullSizes() */

    @Test
    void computeFullSizes() {
        Info info        = Info.builder().withSize(8848, 6928).build();
        Info.Image image = info.getImages().getFirst();
        List<Size> sizes = DZIUtils.computeFullSizes(image);
        assertEquals(15, sizes.size());
        // Exactness is crucial!
        // Level 0
        Size size = sizes.getFirst();
        assertEquals(1, size.intWidth());
        assertEquals(1, size.intHeight());
        // Level 1
        size = sizes.get(1);
        assertEquals(2, size.intWidth());
        assertEquals(1, size.intHeight());
        // Level 2
        size = sizes.get(2);
        assertEquals(3, size.intWidth());
        assertEquals(2, size.intHeight());
        // Level 3
        size = sizes.get(3);
        assertEquals(5, size.intWidth());
        assertEquals(4, size.intHeight());
        // Level 4
        size = sizes.get(4);
        assertEquals(9, size.intWidth());
        assertEquals(7, size.intHeight());
        // Level 5
        size = sizes.get(5);
        assertEquals(18, size.intWidth());
        assertEquals(14, size.intHeight());
        // Level 6
        size = sizes.get(6);
        assertEquals(35, size.intWidth());
        assertEquals(28, size.intHeight());
        // Level 7
        size = sizes.get(7);
        assertEquals(70, size.intWidth());
        assertEquals(55, size.intHeight());
        // Level 8
        size = sizes.get(8);
        assertEquals(139, size.intWidth());
        assertEquals(109, size.intHeight());
        // Level 9
        size = sizes.get(9);
        assertEquals(277, size.intWidth());
        assertEquals(217, size.intHeight());
        // Level 10
        size = sizes.get(10);
        assertEquals(553, size.intWidth());
        assertEquals(433, size.intHeight());
        // Level 11
        size = sizes.get(11);
        assertEquals(1106, size.intWidth());
        assertEquals(866, size.intHeight());
        // Level 12
        size = sizes.get(12);
        assertEquals(2212, size.intWidth());
        assertEquals(1732, size.intHeight());
        // Level 13
        size = sizes.get(13);
        assertEquals(4424, size.intWidth());
        assertEquals(3464, size.intHeight());
        // Level 14
        size = sizes.get(14);
        assertEquals(8848, size.intWidth());
        assertEquals(6928, size.intHeight());
    }

    @Test
    void computeFullSizesOrdersFromSmallestToLargest() {
        Info info        = Info.builder().withSize(8848, 6928).build();
        Info.Image image = info.getImages().getFirst();
        List<Size> sizes = DZIUtils.computeFullSizes(image);
        Size lastSize    = new Size(0, 0);
        for (Size size : sizes) {
            assertTrue(size.width() > lastSize.width());
            assertTrue(size.height() > lastSize.height());
        }
    }

    /* getTileSize() */

    @Test
    void getTileSizeUsesImageTileSizeWhenLargerThanConfigurationValue() {
        Configuration.forApplication().setProperty(Key.DEEPZOOM_MIN_TILE_SIZE, 128);
        Info info = Info.builder()
                .withSize(1024, 1024)
                .withTileSize(256, 256)
                .build();
        Info.Image image = info.getImages().getFirst();
        Size tileSize = DZIUtils.getTileSize(image);
        assertEquals(256, tileSize.intWidth());
        assertEquals(256, tileSize.intHeight());
    }

    @Test
    void getTileSizeUsesConfigurationTileSizeWhenActualIsSmaller() {
        Configuration.forApplication().setProperty(Key.DEEPZOOM_MIN_TILE_SIZE, 512);
        Info info = Info.builder()
                .withSize(1024, 1024)
                .withTileSize(256, 256)
                .build();
        Info.Image image = info.getImages().getFirst();
        Size tileSize    = DZIUtils.getTileSize(image);
        assertEquals(512, tileSize.intWidth());
        assertEquals(512, tileSize.intHeight());
    }

    @Test
    void getTileSizeUsesDefaultTileSizeForUntiledImagesWhenConfigurationIsNotSet() {
        Configuration.forApplication().clearProperty(Key.DEEPZOOM_MIN_TILE_SIZE);
        Info info = Info.builder()
                .withSize(1024, 1024)
                .withTileSize(1024, 1024)
                .build();
        Info.Image image = info.getImages().getFirst();
        Size tileSize    = DZIUtils.getTileSize(image);
        assertEquals(DZIUtils.DEFAULT_MIN_TILE_SIZE, tileSize.intWidth());
        assertEquals(DZIUtils.DEFAULT_MIN_TILE_SIZE, tileSize.intHeight());
    }

}


