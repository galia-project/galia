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
import is.galia.util.ObjectCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ObjectCache}-backed cache for {@link Info} instances.
 */
public final class HeapInfoCache {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HeapInfoCache.class);

    /**
     * This includes all {@link Info} properties including embedded metadata
     * (which will probably comprise most of this size, if present).
     */
    private static final int EXPECTED_AVERAGE_INFO_SIZE = 4096;

    /**
     * Cached infos will consume, at most, this much of max heap.
     */
    private static final double MAX_HEAP_PERCENT = 0.05;

    private static HeapInfoCache instance;

    private final ObjectCache<Identifier, Info> objectCache;

    static synchronized HeapInfoCache getInstance() {
        if (instance == null) {
            instance = new HeapInfoCache();
        }
        return instance;
    }

    HeapInfoCache() {
        final long maxByteSize =
                Math.round(Runtime.getRuntime().maxMemory() * MAX_HEAP_PERCENT);
        final long maxCount = Math.round(maxByteSize /
                (float) EXPECTED_AVERAGE_INFO_SIZE);

        LOGGER.info("Max {} capacity: {} ({}% max heap / {}-byte expected average info size)",
                HeapInfoCache.class.getSimpleName(),
                maxCount,
                Math.round(MAX_HEAP_PERCENT * 100),
                EXPECTED_AVERAGE_INFO_SIZE);
        objectCache = new ObjectCache<>(maxCount);
    }

    public void evict(Identifier identifier) {
        LOGGER.debug("purge(Identifier): purging {}", identifier);
        objectCache.remove(identifier);
    }

    /**
     * @param identifier Identifier of the source image for which to retrieve
     *                   the info.
     * @return           Info for the image with the given identifier.
     */
    public Info get(final Identifier identifier) {
        return objectCache.get(identifier);
    }

    public long maxSize() {
        return objectCache.maxSize();
    }

    public void purge() {
        LOGGER.debug("purge()");
        objectCache.removeAll();
    }

    /**
     * Adds an info.
     */
    public void put(Identifier identifier, Info info) {
        LOGGER.debug("put(): adding info: {} (new size: {})",
                identifier,
                objectCache.size() + 1);
        objectCache.put(identifier, info);
    }

    /**
     * @return Number of items in the cache.
     */
    public long size() {
        return objectCache.size();
    }

}
