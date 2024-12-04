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

import is.galia.delegate.Delegate;
import is.galia.delegate.DelegateException;
import is.galia.delegate.DelegateFactory;
import is.galia.operation.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

final class DelegateOverlayService implements OverlayService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DelegateOverlayService.class);

    private final Delegate delegate;

    DelegateOverlayService(Delegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isAvailable() {
        return DelegateFactory.isDelegateAvailable();
    }

    /**
     * @return Map with {@code inset}, {@code position}, and {@code pathname}
     *         or {@code string} keys; or {@code null}
     */
    @Override
    public Overlay newOverlay() throws DelegateException {
        final Map<String,Object> defs = overlayProperties(delegate);
        if (defs != null) {
            final int inset = ((Number) defs.get("inset")).intValue();
            final Position position = (Position) defs.get("position");
            final String location = (String) defs.get("image");
            if (location != null) {
                try {
                    URI overlayURI;
                    // If the location in the configuration starts with a
                    // supported URI scheme, create a new URI for it.
                    // Otherwise, get its absolute path and convert that to a
                    // file: URI.
                    if (ImageOverlay.SUPPORTED_URI_SCHEMES.stream().anyMatch(location::startsWith)) {
                        overlayURI = new URI(location);
                    } else {
                        overlayURI = Paths.get(location).toUri();
                    }
                    return new ImageOverlay(overlayURI, position, inset);
                } catch (URISyntaxException e) {
                    LOGGER.error("newOverlay(): {}", e.getMessage());
                    return null;
                }
            } else {
                String string = (String) defs.get("string");

                Map<TextAttribute, Object> attributes = Map.of(
                        TextAttribute.FAMILY, defs.get("font"),
                        TextAttribute.SIZE, defs.get("font_size"),
                        TextAttribute.WEIGHT, defs.get("font_weight"),
                        TextAttribute.TRACKING, defs.get("glyph_spacing"));
                Font font = Font.getFont(attributes);

                Color backgroundColor =
                        Color.fromString((String) defs.get("background_color"));
                Color color =
                        Color.fromString((String) defs.get("color"));
                int minSize =
                        ((Number) defs.get("font_min_size")).intValue();
                Color strokeColor =
                        Color.fromString((String) defs.get("stroke_color"));
                float strokeWidth =
                        Float.parseFloat(defs.get("stroke_width").toString());
                boolean wordWrap = (boolean) defs.get("word_wrap");

                return new StringOverlay(string, position, inset, font, minSize,
                        color, backgroundColor, strokeColor, strokeWidth,
                        wordWrap);
            }
        }
        return null;
    }

    /**
     * Invokes the overlay delegate method to retrieve overlay properties.
     *
     * <p>The returned map is the same as that of {@link
     * Delegate#getOverlayProperties()}.</p>
     *
     * @param delegate
     * @return Map with one of the above structures, or {@code null} for no
     *         overlay.
     */
    private Map<String,Object> overlayProperties(Delegate delegate)
            throws DelegateException {
        final Map<String,Object> resultMap = delegate.getOverlayProperties();

        if (resultMap.isEmpty()) {
            return null;
        }

        // Copy the map into a new one that we can tweak before returning.
        final Map<String,Object> props = new HashMap<>(resultMap);
        if (props.get("pathname") != null) {
            props.put("pathname", new File((String) props.get("pathname")));
        }
        props.put("position",
                Position.fromString((String) props.get("position")));
        return props;
    }

}
