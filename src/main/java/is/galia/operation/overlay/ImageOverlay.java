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

import is.galia.image.Size;
import is.galia.image.ScaleConstraint;
import is.galia.operation.Operation;
import is.galia.stream.ByteArrayImageInputStream;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * <p>Encapsulates an image overlaid on top of another image.</p>
 *
 * <p>Instances should be obtained from the {@link OverlayFactory}.</p>
 */
public class ImageOverlay extends Overlay implements Operation {

    static final Set<String> SUPPORTED_URI_SCHEMES =
            Set.of("file", "http", "https");

    private static final ImageOverlayCache OVERLAY_CACHE =
            new ImageOverlayCache();

    private URI uri;

    /**
     * Constructor for images that reside on a web server.
     *
     * @param uri      Image URI, which may be a file URI.
     * @param position Position of the overlay.
     * @param inset    Inset in pixels.
     */
    public ImageOverlay(URI uri, Position position, int inset) {
        super(position, inset);
        setURI(uri);
    }

    /**
     * For reading the image, clients should use {@link #openStream()} instead.
     *
     * @return URI of the image.
     */
    public URI getURI() {
        return uri;
    }

    /**
     * @return Stream from which the image can be read.
     */
    public ImageInputStream openStream() throws IOException {
        byte[] bytes = OVERLAY_CACHE.putAndGet(getURI());
        return new ByteArrayImageInputStream(bytes);
    }

    /**
     * @param uri Image URI, which may be a file URI.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setURI(URI uri) {
        checkFrozen();
        this.uri = uri;
    }

    /**
     * @return Map with {@literal identifier}, {@literal position}, and
     *         {@literal inset} keys.
     */
    @Override
    public Map<String, Object> toMap(Size fullSize,
                                     ScaleConstraint scaleConstraint) {
        return Map.of(
                "class", getClass().getSimpleName(),
                "uri", getURI().toString(),
                "position", getPosition().toString(),
                "inset", getInset());
    }

    /**
     * @return String representation of the instance, in the format
     *         {@literal [URI]_[position]_[inset]}.
     */
    @Override
    public String toString() {
        return String.format("%s_%s_%d",
                getURI(), getPosition(), getInset());
    }

}
