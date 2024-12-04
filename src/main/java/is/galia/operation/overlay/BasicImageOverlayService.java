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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

/**
 * Used to acquire overlay images when using BasicStrategy for overlays.
 */
class BasicImageOverlayService extends BasicOverlayService
        implements OverlayService {

    private URI overlayURI;

    BasicImageOverlayService() {
        super();
        readLocation();
    }

    /**
     * @return Overlay image corresponding to the application configuration.
     */
    @Override
    public ImageOverlay newOverlay() {
        return new ImageOverlay(overlayURI, getPosition(), getInset());
    }

    private void readLocation() {
        final Configuration config = Configuration.forApplication();
        final String location = config.getString(Key.OVERLAY_IMAGE, "");
        if (location.isBlank()) {
            throw new ConfigurationException(Key.OVERLAY_IMAGE + " is not set.");
        }
        try {
            // If the location in the configuration starts with a supported URI
            // scheme, create a new URI for it. Otherwise, get its absolute path
            // and convert that to a file: URI.
            if (ImageOverlay.SUPPORTED_URI_SCHEMES.stream().anyMatch(location::startsWith)) {
                overlayURI = new URI(location);
            } else {
                overlayURI = Paths.get(location).toUri();
            }
        } catch (URISyntaxException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

}
