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

package is.galia.operation.overlay;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.image.Size;
import is.galia.test.BaseTest;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class BasicImageOverlayServiceTest extends BaseTest {

    private BasicImageOverlayService instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.forApplication();
        config.setProperty(Key.OVERLAY_ENABLED, true);
        config.setProperty(Key.OVERLAY_STRATEGY, "BasicStrategy");
        config.setProperty(Key.OVERLAY_TYPE, "image");
        config.setProperty(Key.OVERLAY_INSET, 10);
        config.setProperty(Key.OVERLAY_POSITION, "top left");
        config.setProperty(Key.OVERLAY_IMAGE, "/dev/null");

        instance = new BasicImageOverlayService();
    }

    @Test
    void testGetOverlay() throws Exception {
        final ImageOverlay overlay = instance.newOverlay();
        if (SystemUtils.IS_OS_WINDOWS) {
            assertEquals(new URI("file:///C:/dev/null"), overlay.getURI());
        } else {
            assertEquals(new URI("file:///dev/null"), overlay.getURI());
        }
        assertEquals((long) 10, overlay.getInset());
        assertEquals(Position.TOP_LEFT, overlay.getPosition());
    }

    @Test
    void testShouldApplyToImage() {
        Configuration config = Configuration.forApplication();
        config.clear();

        final Size imageSize = new Size(100, 100);

        // image width > width threshold, image height > height threshold
        config.setProperty(Key.OVERLAY_OUTPUT_WIDTH_THRESHOLD, 50);
        config.setProperty(Key.OVERLAY_OUTPUT_HEIGHT_THRESHOLD, 50);
        assertTrue(BasicImageOverlayService.shouldApplyToImage(imageSize));

        // image width < width threshold, image height < height threshold
        config.setProperty(Key.OVERLAY_OUTPUT_WIDTH_THRESHOLD, 200);
        config.setProperty(Key.OVERLAY_OUTPUT_HEIGHT_THRESHOLD, 200);
        assertFalse(BasicImageOverlayService.shouldApplyToImage(imageSize));

        // image width < width threshold, image height > height threshold
        config.setProperty(Key.OVERLAY_OUTPUT_WIDTH_THRESHOLD, 200);
        config.setProperty(Key.OVERLAY_OUTPUT_HEIGHT_THRESHOLD, 50);
        assertFalse(BasicImageOverlayService.shouldApplyToImage(imageSize));

        // image width > width threshold, image height < height threshold
        config.setProperty(Key.OVERLAY_OUTPUT_WIDTH_THRESHOLD, 50);
        config.setProperty(Key.OVERLAY_OUTPUT_HEIGHT_THRESHOLD, 200);
        assertFalse(BasicImageOverlayService.shouldApplyToImage(imageSize));
    }

}
