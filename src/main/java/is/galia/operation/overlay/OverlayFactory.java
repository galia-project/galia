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
import is.galia.delegate.Delegate;

import java.util.Optional;

/**
 * Provides access to {@link Overlay} instances.
 */
public final class OverlayFactory {

    enum Strategy {

        /**
         * Gets global overlay properties from configuration keys.
         */
        BASIC,

        /**
         * Uses the result of a delegate method to get overlay properties
         * per-request.
         */
        DELEGATE_METHOD

    }

    private Strategy strategy;

    public OverlayFactory() {
        readStrategy();
    }

    private OverlayService newOverlayService(Delegate delegate) {
        final Configuration config = Configuration.forApplication();
        OverlayService instance = null;
        instance = switch (getStrategy()) {
            case BASIC -> switch (config.getString(Key.OVERLAY_TYPE, "")) {
                case "image"  -> new BasicImageOverlayService();
                case "string" -> new BasicStringOverlayService();
                default       -> instance;
            };
            case DELEGATE_METHOD -> new DelegateOverlayService(delegate);
        };
        return instance;
    }

    /**
     * Factory method that returns a new {@link Overlay} based on either the
     * configuration, or the delegate method return value, depending on the
     * setting of {@link Key#OVERLAY_STRATEGY}.
     *
     * @param delegate Required when {@link #getStrategy()} returns {@link
     *                 Strategy#DELEGATE_METHOD}. May be {@code null}
     *                 otherwise.
     * @return         Instance respecting the overlay strategy and given
     *                 arguments.
     */
    public Optional<Overlay> newOverlay(Delegate delegate) throws Exception {
        OverlayService service = newOverlayService(delegate);
        if (service != null && service.isAvailable()) {
            return Optional.ofNullable(service.newOverlay());
        }
        return Optional.empty();
    }

    Strategy getStrategy() {
        return strategy;
    }

    private void readStrategy() {
        final Configuration config = Configuration.forApplication();
        final String configValue   = config.getString(
                Key.OVERLAY_STRATEGY, "BasicStrategy");
        switch (configValue) {
            case "DelegateStrategy" -> setStrategy(Strategy.DELEGATE_METHOD);
            case "BasicStrategy"    -> setStrategy(Strategy.BASIC);
            default                 -> throw new ConfigurationException(
                    "Unsupported value for " + Key.OVERLAY_STRATEGY);
        }
    }

    /**
     * @param strategy Overlay strategy to use.
     */
    void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    /**
     * @param outputImageSize
     * @return Whether an overlay should be applied to an output image with
     *         the given dimensions.
     */
    public boolean shouldApplyToImage(Size outputImageSize) {
        return switch (strategy) {
            case BASIC ->
                    BasicOverlayService.shouldApplyToImage(outputImageSize);
            default ->
                    // The delegate method will decide.
                    true;
        };
    }

}
