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
import is.galia.image.Size;

abstract class BasicOverlayService {

    private int inset;
    private Position position;

    /**
     * @return Whether an overlay should be applied to an output image with
     * the given dimensions.
     */
    static boolean shouldApplyToImage(Size outputImageSize) {
        final Configuration config = Configuration.forApplication();
        final int minOutputWidth =
                config.getInt(Key.OVERLAY_OUTPUT_WIDTH_THRESHOLD, 0);
        final int minOutputHeight =
                config.getInt(Key.OVERLAY_OUTPUT_HEIGHT_THRESHOLD, 0);
        return (outputImageSize.width() >= minOutputWidth &&
                outputImageSize.height() >= minOutputHeight);
    }

    BasicOverlayService() {
        readPosition();
        readInset();
    }

    /**
     * @return Overlay inset.
     */
    protected int getInset() {
        return inset;
    }

    /**
     * @return Overlay position.
     */
    protected Position getPosition() {
        return position;
    }

    public boolean isAvailable() {
        return Configuration.forApplication().
                getBoolean(Key.OVERLAY_ENABLED, false);
    }

    private void readInset() {
        inset = Configuration.forApplication().getInt(Key.OVERLAY_INSET, 0);
    }

    private void readPosition() {
        final Configuration config = Configuration.forApplication();
        final String configValue = config.getString(Key.OVERLAY_POSITION, "");
        if (!configValue.isEmpty()) {
            try {
                position = Position.fromString(configValue);
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException("Invalid " +
                        Key.OVERLAY_POSITION + " value: " + configValue);
            }
        } else {
            throw new ConfigurationException(Key.OVERLAY_POSITION +
                    " is not set.");
        }
    }

}
