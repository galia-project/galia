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

import com.fasterxml.jackson.core.JsonParseException;
import is.galia.async.VirtualThreadPool;
import is.galia.codec.Decoder;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.image.Info;
import is.galia.image.InfoReader;
import is.galia.image.StatResult;
import is.galia.operation.OperationList;
import is.galia.stream.CompletableOutputStream;
import is.galia.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Simplified interface to the caching architecture.
 */
public final class CacheFacade {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(CacheFacade.class);

    /**
     * Cleans up all active caches.
     *
     * @see Cache#cleanUp
     */
    public void cleanUp() throws IOException {
        // N.B.: the heap info cache can't be cleaned up.
        // Clean up the info cache
        Optional<InfoCache> optInfoCache = getInfoCache();
        if (optInfoCache.isPresent()) {
            optInfoCache.get().cleanUp();
        }
        // Clean up the variant cache
        Optional<VariantCache> optVariantCache = getVariantCache();
        if (optVariantCache.isPresent()) {
            optVariantCache.get().cleanUp();
        }
    }

    /**
     * Evicts all cached items associated with a source image from all caches.
     *
     * @see Cache#evict(Identifier)
     */
    public void evict(Identifier identifier) throws IOException {
        // Purge it from the heap info cache.
        getHeapInfoCache().ifPresent(c -> c.evict(identifier));
        // Purge it from the info cache.
        Optional<InfoCache> optInfoCache = getInfoCache();
        if (optInfoCache.isPresent()) {
            optInfoCache.get().evict(identifier);
        }
        // Purge it from the variant cache.
        Optional<VariantCache> optVariantCache = getVariantCache();
        if (optVariantCache.isPresent()) {
            optVariantCache.get().evict(identifier);
        }
    }

    /**
     * Evicts the given variant image from the variant cache.
     *
     * @see VariantCache#evict(OperationList)
     */
    public void evict(OperationList opList) throws IOException {
        Optional<VariantCache> optCache = getVariantCache();
        if (optCache.isPresent()) {
            optCache.get().evict(opList);
        }
    }

    /**
     * Invokes {@link #evict(Identifier)} asynchronously.
     */
    public void evictAsync(Identifier identifier) {
        VirtualThreadPool.getInstance().submit(() -> {
            try {
                evict(identifier);
            } catch (IOException e) {
                LOGGER.error("evictAsync(): {}", e.getMessage());
            }
            return null;
        });
    }

    /**
     * Purges all infos from the {@link InfoCache} and {@link HeapInfoCache}.
     *
     * @see InfoCache#evictInfos()
     */
    public void evictInfos() throws IOException {
        // Purge from the heap info cache.
        getHeapInfoCache().ifPresent(HeapInfoCache::purge);
        // Purge from the info cache.
        Optional<InfoCache> optCache = getInfoCache();
        if (optCache.isPresent()) {
            optCache.get().evictInfos();
        }
    }

    /**
     * Purges all invalid content from all caches.
     *
     * @see Cache#evictInvalid
     */
    public void evictInvalid() throws IOException {
        // Purge the info cache.
        Optional<InfoCache> optInfoCache = getInfoCache();
        if (optInfoCache.isPresent()) {
            optInfoCache.get().evictInvalid();
        }
        // Purge the variant cache.
        Optional<VariantCache> optVariantCache = getVariantCache();
        if (optVariantCache.isPresent()) {
            optVariantCache.get().evictInvalid();
        }
    }

    /**
     * <p>Returns the {@link Info} of the source image corresponding to the
     * given identifier.</p>
     *
     * <p>The following caches are consulted in order of preference:</p>
     *
     * <ol>
     *     <li>The {@link HeapInfoCache}, if enabled by the {@link
     *     Key#HEAP_INFO_CACHE_ENABLED} configuration key</li>
     *     <li>The instance returned by {@link CacheFactory#getInfoCache()}</li>
     * </ol>
     *
     * <p>If an info exists in a cache but is corrupt, the error is swallowed
     * and logged and an empty instance is returned.</p>
     *
     * @param identifier   Identifier of the source image for which to retrieve
     *                     the info.
     * @return             Info for the image with the given identifier.
     * @throws IOException If there is an error reading or writing to or from
     *                     the cache.
     * @see #fetchOrReadInfo(Identifier, Format, Decoder)
     */
    public Optional<Info> fetchInfo(Identifier identifier) throws IOException {
        // Check the heap info cache.
        HeapInfoCache heapInfoCache = getHeapInfoCache().orElse(null);
        if (heapInfoCache != null) {
            Info info = heapInfoCache.get(identifier);
            if (info != null) {
                LOGGER.trace("fetchInfo(): retrieved from {}: {}",
                        heapInfoCache.getClass().getSimpleName(), identifier);
                return Optional.of(info);
            }
        }
        // Check the info cache.
        final InfoCache infoCache = getInfoCache().orElse(null);
        if (infoCache != null) {
            Stopwatch timer = new Stopwatch();
            try {
                Optional<Info> optInfo = infoCache.fetchInfo(identifier);
                if (optInfo.isPresent()) {
                    LOGGER.trace("fetchInfo(): retrieved info of {} from {} in {}",
                            identifier,
                            infoCache.getClass().getSimpleName(),
                            timer);
                    if (heapInfoCache != null) {
                        heapInfoCache.put(identifier, optInfo.get());
                    }
                }
                return optInfo;
            } catch (JsonParseException e) {
                LOGGER.warn("fetchInfo(): {}", e.getMessage());
            }
        }
        return Optional.empty();
    }

    /**
     * <p>Returns the {@link Info} of the source image corresponding to the
     * given identifier.</p>
     *
     * <p>The following sources are consulted in order of preference:</p>
     *
     * <ol>
     *     <li>The {@link HeapInfoCache}, if enabled by the {@link
     *     Key#HEAP_INFO_CACHE_ENABLED} configuration key;</li>
     *     <li>The instance returned by {@link
     *     CacheFactory#getInfoCache()};</li>
     *     <li>The given processor. If this is the case, it will also be cached
     *     in whichever of the above caches are available. (This may happen
     *     asynchronously.)</li>
     * </ol>
     *
     * <p>If an info exists in a cache but is corrupt, the error is swallowed
     * and logged.</p>
     *
     * @param identifier Identifier of the source image for which to retrieve
     *                   the info.
     * @param format     Source image format.
     * @param decoder    Instance to use to read the info if necessary.
     * @return           Info for the image with the given identifier.
     * @throws IOException if there is an error reading or writing to or from
     *                     the cache.
     * @see #fetchInfo(Identifier)
     */
    public Optional<Info> fetchOrReadInfo(
            final Identifier identifier,
            final Format format,
            final Decoder decoder) throws IOException {
        Optional<Info> optInfo = Optional.empty();
        // Try to retrieve it from a cache. In the (hopefully rare) event that
        // it is corrupt and cannot be deserialized, log the problem and fall
        // back to retrieving it from an InfoReader.
        try {
            optInfo = fetchInfo(identifier);
        } catch (IOException e) {
            LOGGER.warn("fetchOrReadInfo(): {}", e.getMessage());
        }
        if (optInfo.isEmpty()) {
            // Read it from the processor and then add it to the info and heap
            // info caches (if enabled).
            final Info info = readInfo(identifier, format, decoder);
            putAsync(identifier, info);
            optInfo = Optional.of(info);
        }
        return optInfo;
    }

    /**
     * @return The {@link HeapInfoCache}, if it is enabled.
     */
    public Optional<HeapInfoCache> getHeapInfoCache() {
        if (isHeapInfoCacheEnabled()) {
            return Optional.of(HeapInfoCache.getInstance());
        }
        return Optional.empty();
    }

    /**
     * @return Number of instances contained in the {@link HeapInfoCache}, if
     *         enabled, or else {@code 0}.
     */
    public long getHeapInfoCacheSize() {
        return getHeapInfoCache().map(HeapInfoCache::size).orElse(0L);
    }

    /**
     * @return The active {@link InfoCache}, if one is enabled.
     * @see CacheFactory#getInfoCache
     */
    public Optional<InfoCache> getInfoCache() {
        return CacheFactory.getInfoCache();
    }

    /**
     * @return The active {@link VariantCache}, if one is enabled.
     * @see CacheFactory#getVariantCache
     */
    public Optional<VariantCache> getVariantCache() {
        return CacheFactory.getVariantCache();
    }

    public boolean isHeapInfoCacheEnabled() {
        return Configuration.forApplication().
                getBoolean(Key.HEAP_INFO_CACHE_ENABLED, false);
    }

    public boolean isInfoCacheEnabled() {
        return getInfoCache().isPresent();
    }

    public boolean isVariantCacheEnabled() {
        return getVariantCache().isPresent();
    }

    /**
     * @see VariantCache#newVariantImageInputStream(OperationList)
     */
    public InputStream newVariantImageInputStream(
            OperationList opList) throws IOException {
        Optional<VariantCache> optCache = getVariantCache();
        if (optCache.isPresent()) {
            return optCache.get().newVariantImageInputStream(opList);
        }
        return null;
    }

    /**
     * @see VariantCache#newVariantImageInputStream(OperationList, StatResult)
     */
    public InputStream newVariantImageInputStream(
            OperationList opList,
            StatResult statResult) throws IOException {
        Optional<VariantCache> optCache = getVariantCache();
        if (optCache.isPresent()) {
            return optCache.get().newVariantImageInputStream(opList, statResult);
        }
        return null;
    }

    /**
     * @see VariantCache#newVariantImageOutputStream(OperationList)
     */
    public CompletableOutputStream newVariantImageOutputStream(
            OperationList opList) throws IOException {
        Optional<VariantCache> optCache = getVariantCache();
        if (optCache.isPresent()) {
            return optCache.get().newVariantImageOutputStream(opList);
        }
        return null;
    }

    /**
     * Purges all information from all caches.
     *
     * @see Cache#purge()
     */
    public void purge() throws IOException {
        // Purge the heap info cache (even if disabled).
        HeapInfoCache.getInstance().purge();
        // Purge the info cache.
        Optional<InfoCache> optInfoCache = getInfoCache();
        if (optInfoCache.isPresent()) {
            optInfoCache.get().purge();
        }
        // Purge the variant cache.
        Optional<VariantCache> optVariantCache = getVariantCache();
        if (optVariantCache.isPresent()) {
            optVariantCache.get().purge();
        }
    }

    /**
     * Adds an info to the heap info and info caches asynchronously.
     */
    private void putAsync(Identifier identifier, Info info) {
        VirtualThreadPool.getInstance().submit(() -> {
            // Add to the heap info cache.
            getHeapInfoCache().ifPresent(c -> c.put(identifier, info));
            // Add to the info cache.
            Optional<InfoCache> optInfoCache = getInfoCache();
            if (optInfoCache.isPresent()) {
                try {
                    optInfoCache.get().put(identifier, info);
                } catch (IOException e) {
                    LOGGER.error("putAsync(): {}", e.getMessage());
                }
            }
            return null;
        });
    }

    /**
     * Reads information about a source image from the given {@link Decoder}.
     */
    private Info readInfo(final Identifier identifier,
                          final Format format,
                          final Decoder decoder) throws IOException {
        final Stopwatch timer = new Stopwatch();

        InfoReader reader = new InfoReader();
        reader.setDecoder(decoder);
        reader.setFormat(format);
        Info info = reader.read();

        LOGGER.trace("readInfo(): read {} from {} in {}",
                identifier, decoder.getClass().getSimpleName(), timer);
        info.setIdentifier(identifier);
        return info;
    }

}
