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
import is.galia.config.ConfigurationException;
import is.galia.config.Key;
import is.galia.operation.Color;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.Map;

class BasicStringOverlayService extends BasicOverlayService
        implements OverlayService {

    private Color backgroundColor;
    private Color color;
    private Font font;
    private int minSize;
    private String string;
    private Color strokeColor;
    private float strokeWidth;

    BasicStringOverlayService() {
        super();
        readConfig();
    }

    @Override
    public StringOverlay newOverlay() {
        return new StringOverlay(string, getPosition(), getInset(), font,
                minSize, color, backgroundColor, strokeColor, strokeWidth,
                false);
    }

    private void readConfig() {
        final Configuration config = Configuration.forApplication();

        // Background color
        backgroundColor = Color.fromString(
                config.getString(Key.OVERLAY_STRING_BACKGROUND_COLOR));

        // Fill color
        color = Color.fromString(config.getString(Key.OVERLAY_STRING_COLOR));

        // Font
        final Map<TextAttribute, Object> attributes = Map.of(
                TextAttribute.FAMILY,
                config.getString(Key.OVERLAY_STRING_FONT, "SansSerif"),
                TextAttribute.SIZE,
                config.getInt(Key.OVERLAY_STRING_FONT_SIZE, 18),
                TextAttribute.WEIGHT,
                config.getFloat(Key.OVERLAY_STRING_FONT_WEIGHT, 1f),
                TextAttribute.TRACKING,
                config.getFloat(Key.OVERLAY_STRING_GLYPH_SPACING, 0f));
        font = Font.getFont(attributes);

        // Min size
        minSize = config.getInt(Key.OVERLAY_STRING_FONT_MIN_SIZE, 14);

        // String
        string = config.getString(Key.OVERLAY_STRING_STRING, "");

        // Stroke color
        strokeColor = Color.fromString(
                config.getString(Key.OVERLAY_STRING_STROKE_COLOR, "black"));

        // Stroke width
        strokeWidth = config.getFloat(Key.OVERLAY_STRING_STROKE_WIDTH, 2f);
    }

}
