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

package is.galia.cache;

import is.galia.image.Identifier;
import is.galia.image.Info;
import is.galia.operation.OperationList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <p>Stores and retrieves unique images corresponding to {@link OperationList}
 * instances, as well as {@link Info} instances corresponding to {@link
 * Identifier} instances.</p>
 *
 * <p>Implementations typically use a least-recently-used (LRU) strategy, but
 * may also use a least-frequently-used (LFU) or some other strategy.</p>
 *
 * <p>Implementations are shared across threads and must be thread-safe.</p>
 */
public sealed interface Cache permits VariantCache, InfoCache {

    Logger LOGGER = LoggerFactory.getLogger(Cache.class);

    /**
     * @param observer Instance to add. A weak reference to it will be
     *                 maintained, so there will be no need to remove it later.
     */
    void addObserver(CacheObserver observer);

    /**
     * <p>Cleans up the cache.</p>
     *
     * <p>This method should <strong>not</strong> duplicate the behavior of any
     * of the purging-related methods. Other than that, implementations may use
     * their own interpretation of "clean up"&mdash;ideally, they will not need
     * to do anything at all.</p>
     *
     * <p>The frequency with which this method will be called may vary. It may
     * never be called. Implementations should try to keep themselves clean
     * without relying on this method.</p>
     *
     * <p>The default implementation does nothing.</p>
     *
     * @throws IOException Upon fatal error. Implementations should swallow and
     *         log non-fatal errors.
     * @see #shutdown()
     */
    default void cleanUp() throws IOException {}

    /**
     * Deletes all cached content (source image, variant image(s), and info)
     * corresponding to the image with the given identifier.
     *
     * @param identifier Identifier of the image whose content to evict.
     * @throws IOException Upon fatal error. Implementations should swallow and
     *         log non-fatal errors.
     */
    void evict(Identifier identifier) throws IOException;

    /**
     * Deletes invalid images and dimensions from the cache.
     *
     * @throws IOException Upon fatal error. Implementations should swallow and
     *         log non-fatal errors.
     */
    void evictInvalid() throws IOException;

    /**
     * <p>Implementations should perform all necessary initialization in this
     * method rather than a constructor or static initializer.</p>
     *
     * <p>The default implementation does nothing.</p>
     */
    default void initialize() {}

    /**
     * Called by {@link CacheWorker} during its shifts. This default
     * implementation calls {@link #evictInvalid()} and {@link #cleanUp()}.
     * If an implementation has anything else to do, it should override and
     * call {@code super}.
     */
    default void onCacheWorker() {
        CacheFacade cacheFacade = new CacheFacade();

        // Purge invalid content.
        try {
            cacheFacade.evictInvalid();
        } catch (IOException e) {
            LOGGER.error("onCacheWorker: {}", e.getMessage(), e);
        }

        // Clean up.
        try {
            cacheFacade.cleanUp();
        } catch (IOException e) {
            LOGGER.error("onCacheWorker: {}", e.getMessage(), e);
        }
    }

    /**
     * Deletes the entire cache contents.
     *
     * @throws IOException Upon fatal error. Implementations should swallow and
     *         log non-fatal errors.
     */
    void purge() throws IOException;

    /**
     * <p>Shuts down the instance, freeing any resource handles, stopping any
     * worker threads, etc.</p>
     *
     * <p>The default implementation does nothing.</p>
     *
     * @see #cleanUp()
     */
    default void shutdown() {}

}
