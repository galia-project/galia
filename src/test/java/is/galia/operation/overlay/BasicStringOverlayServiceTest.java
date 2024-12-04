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
import is.galia.operation.Color;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.font.TextAttribute;

import static org.junit.jupiter.api.Assertions.*;

public class BasicStringOverlayServiceTest extends BaseTest {

    private BasicStringOverlayService instance;

    public static void setUpConfiguration() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.OVERLAY_ENABLED, true);
        config.setProperty(Key.OVERLAY_STRATEGY, "BasicStrategy");
        config.setProperty(Key.OVERLAY_TYPE, "string");
        config.setProperty(Key.OVERLAY_INSET, 10);
        config.setProperty(Key.OVERLAY_POSITION, "top left");
        config.setProperty(Key.OVERLAY_STRING_BACKGROUND_COLOR, "rgba(12, 23, 34, 45)");
        config.setProperty(Key.OVERLAY_STRING_COLOR, "red");
        config.setProperty(Key.OVERLAY_STRING_FONT, "SansSerif");
        config.setProperty(Key.OVERLAY_STRING_FONT_MIN_SIZE, 11);
        config.setProperty(Key.OVERLAY_STRING_FONT_SIZE, 14);
        config.setProperty(Key.OVERLAY_STRING_FONT_WEIGHT, 2f);
        config.setProperty(Key.OVERLAY_STRING_GLYPH_SPACING, 0.2f);
        config.setProperty(Key.OVERLAY_STRING_STRING, "cats");
        config.setProperty(Key.OVERLAY_STRING_STROKE_COLOR, "orange");
        config.setProperty(Key.OVERLAY_STRING_STROKE_WIDTH, 3);
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        setUpConfiguration();

        instance = new BasicStringOverlayService();
    }

    @Test
    void testGetOverlay() {
        final StringOverlay overlay = instance.newOverlay();
        assertEquals("cats", overlay.getString());
        Assertions.assertEquals(new Color(12, 23, 34, 45), overlay.getBackgroundColor());
        Assertions.assertEquals(Color.RED, overlay.getColor());
        assertEquals("SansSerif", overlay.getFont().getName());
        assertEquals((long) 10, overlay.getInset());
        assertEquals(Position.TOP_LEFT, overlay.getPosition());
        assertEquals(14, overlay.getFont().getSize());
        assertEquals(11, overlay.getMinSize());
        assertEquals(2f, overlay.getFont().getAttributes().get(TextAttribute.WEIGHT));
        assertEquals(0.2f, overlay.getFont().getAttributes().get(TextAttribute.TRACKING));
        Assertions.assertEquals(Color.ORANGE, overlay.getStrokeColor());
        assertEquals(3, overlay.getStrokeWidth(), 0.00001f);
    }

    @Test
    void testShouldApplyToImage() {
        Configuration config = Configuration.forApplication();
        config.clear();

        final Size imageSize = new Size(100, 100);

        // image width > width threshold, image height > height threshold
        config.setProperty(Key.OVERLAY_OUTPUT_WIDTH_THRESHOLD, 50);
        config.setProperty(Key.OVERLAY_OUTPUT_HEIGHT_THRESHOLD, 50);
        assertTrue(BasicStringOverlayService.shouldApplyToImage(imageSize));

        // image width < width threshold, image height < height threshold
        config.setProperty(Key.OVERLAY_OUTPUT_WIDTH_THRESHOLD, 200);
        config.setProperty(Key.OVERLAY_OUTPUT_HEIGHT_THRESHOLD, 200);
        assertFalse(BasicStringOverlayService.shouldApplyToImage(imageSize));

        // image width < width threshold, image height > height threshold
        config.setProperty(Key.OVERLAY_OUTPUT_WIDTH_THRESHOLD, 200);
        config.setProperty(Key.OVERLAY_OUTPUT_HEIGHT_THRESHOLD, 50);
        assertFalse(BasicStringOverlayService.shouldApplyToImage(imageSize));

        // image width > width threshold, image height < height threshold
        config.setProperty(Key.OVERLAY_OUTPUT_WIDTH_THRESHOLD, 50);
        config.setProperty(Key.OVERLAY_OUTPUT_HEIGHT_THRESHOLD, 200);
        assertFalse(BasicStringOverlayService.shouldApplyToImage(imageSize));
    }

}
