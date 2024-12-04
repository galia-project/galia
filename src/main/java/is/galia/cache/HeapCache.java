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

import is.galia.async.ThreadPool;
import is.galia.config.Configuration;
import is.galia.config.ConfigurationException;
import is.galia.image.Identifier;
import is.galia.image.Info;
import is.galia.image.StatResult;
import is.galia.operation.OperationList;
import is.galia.stream.CompletableNullOutputStream;
import is.galia.stream.CompletableOutputStream;
import is.galia.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static is.galia.config.Key.HEAPCACHE_TARGET_SIZE;
import static is.galia.config.Key.INFO_CACHE_TTL;
import static is.galia.config.Key.VARIANT_CACHE_TTL;

/**
 * <p>Heap-based LRU cache.</p>
 *
 * <p>This implementation can be size-limited in addition to time-limited. When
 * the target size ({@link is.galia.config.Key#HEAPCACHE_TARGET_SIZE}) has been
 * exceeded, the minimum number of least-recently-accessed items are purged
 * that will reduce it back down to this size. (The configured target size may
 * be safely changed while the application is running.)</p>
 */
public final class HeapCache extends AbstractCache
        implements InfoCache, VariantCache {

    /**
     * Cached item key. There are different constructors depending on what the
     * instance is intended to point to.
     */
    public static class CacheKey {

        private final String identifier;
        private String opList;

        /**
         * Info constructor.
         *
         * @param identifier Identifier of the image described by the info.
         */
        CacheKey(Identifier identifier) {
            this.identifier = identifier.toString();
        }

        /**
         * Variant image constructor.
         *
         * @param opList Instance describing the variant image.
         */
        CacheKey(OperationList opList) {
            this(Objects.requireNonNull(opList.getIdentifier()));
            this.opList = opList.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof CacheKey other) {
                return toString().equals(other.toString());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (identifier != null) ?
                    Objects.hashCode(identifier) : Objects.hashCode(opList);
        }

        @Override
        public String toString() {
            return (opList != null) ? "op:" + opList : "id:" + identifier;
        }

    }

    public abstract static class CacheValue {

        final long lastModifiedMSec = Instant.now().toEpochMilli();
        long lastAccessedMSec       = lastModifiedMSec;
        private final byte[] data;

        CacheValue(byte[] data) {
            this.data = data;
        }

        public byte[] getData() {
            return data;
        }

        public Instant getLastAccessed() {
            return Instant.ofEpochMilli(lastAccessedMSec);
        }

        public Instant getLastModified() {
            return Instant.ofEpochMilli(lastModifiedMSec);
        }

        public abstract boolean isValid();

        /**
         * Updates the last-accessed time.
         */
        synchronized void touch() {
            lastAccessedMSec = Instant.now().toEpochMilli();
        }
    }

    public static class CachedInfo extends CacheValue {

        CachedInfo(byte[] data) {
            super(data);
        }

        @Override
        public boolean isValid() {
            return getLastAccessed().isAfter(earliestValidInfoAccessTime());
        }

    }

    public static class CachedVariant extends CacheValue {

        CachedVariant(byte[] data) {
            super(data);
        }

        @Override
        public boolean isValid() {
            return getLastAccessed().isAfter(earliestValidVariantAccessTime());
        }

    }

    /**
     * Buffers written data and adds it to the cache upon closure.
     */
    private class HeapCacheOutputStream extends CompletableOutputStream {

        private final OperationList opList;
        private final ByteArrayOutputStream wrappedStream =
                new ByteArrayOutputStream();

        HeapCacheOutputStream(OperationList opList) {
            this.opList = opList;
        }

        @Override
        public void close() throws IOException {
            LOGGER.debug("Closing stream for {}", opList);
            if (isComplete()) {
                CacheKey key        = new CacheKey(opList);
                CachedVariant value = new CachedVariant(wrappedStream.toByteArray());
                cache.put(key, value);
                getAllObservers().forEach(o -> o.onImageWritten(opList));
            }
            try {
                super.close();
            } finally {
                wrappedStream.close();
            }
        }

        @Override
        public void flush() throws IOException {
            wrappedStream.flush();
        }

        @Override
        public void write(int b) {
            wrappedStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            wrappedStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            wrappedStream.write(b, off, len);
        }

    }

    /**
     * Periodically purges excess contents from the cache.
     */
    private class Worker implements Runnable {

        private static final int INTERVAL_SECONDS = 10;

        private final Logger logger = LoggerFactory.getLogger(Worker.class);

        @Override
        public void run() {
            while (workerShouldWork.get()) {
                try {
                    // We only evict excess content. Invalid content is
                    // CacheWorker's job.
                    evictExcess();
                    logger.trace("Cache size: {} items ({} bytes)",
                            size(), getByteSize());
                    Thread.sleep(INTERVAL_SECONDS * 1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HeapCache.class);

    private final ConcurrentMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();
    private final AtomicBoolean workerShouldWork = new AtomicBoolean(true);

    //region Helper methods

    private static long getInfoCacheTTL() {
        Configuration config = Configuration.forApplication();
        long ttl = config.getLong(INFO_CACHE_TTL, 0);
        return (ttl > 0) ? ttl : Duration.ofDays(3650).toMillis();
    }

    private static long getVariantCacheTTL() {
        Configuration config = Configuration.forApplication();
        long ttl = config.getLong(VARIANT_CACHE_TTL, 0);
        return (ttl > 0) ? ttl : Duration.ofDays(3650).toMillis();
    }

    private static Instant earliestValidInfoAccessTime() {
        return Instant.now().minus(getInfoCacheTTL(), ChronoUnit.SECONDS);
    }

    private static Instant earliestValidVariantAccessTime() {
        return Instant.now().minus(getVariantCacheTTL(), ChronoUnit.SECONDS);
    }

    /**
     * <p>Returns the item corresponding to the given key, updating its last-
     * accessed time before returning it.</p>
     *
     * <p>If the item is {@link CacheValue#isValid() invalid}, it is
     * evicted and {@code null} is returned.</p>
     *
     * <p><strong>All cache map retrievals should use this method.</strong></p>
     *
     * @param key Key to access.
     * @return Item corresponding to the given key. May be {@code null}.
     */
    private CacheValue get(CacheKey key) {
        CacheValue item = cache.get(key);
        if (item != null) {
            if (item.isValid()) {
                item.touch();
            } else {
                cache.remove(key);
                item = null;
            }
        }
        return item;
    }

    /**
     * @return Current size of the contents in bytes.
     */
    long getByteSize() {
        return cache.values().stream()
                .mapToLong(t -> t.getData().length)
                .sum();
    }

    private List<Map.Entry<CacheKey, CacheValue>> getEntriesSortedByLastAccessedTime() {
        List<Map.Entry<CacheKey, CacheValue>> sortedEntries =
                new ArrayList<>(cache.entrySet());
        sortedEntries.sort((Map.Entry<CacheKey, CacheValue> e1,
                            Map.Entry<CacheKey, CacheValue> e2) -> {
            CacheValue value1 = e1.getValue();
            CacheValue value2 = e2.getValue();
            if (value1.getLastAccessed().equals(value2.getLastAccessed())) {
                return 0;
            }
            return value1.getLastAccessed().isBefore(value2.getLastAccessed()) ?
                    -1 : 1;
        });
        return sortedEntries;
    }

    /**
     * @return Capacity of the instance based on the application configuration.
     * @throws ConfigurationException If the capacity in the configuration is
     *                                invalid.
     */
    static long getTargetByteSize() {
        final Configuration config = Configuration.forApplication();
        String humanSize           = config.getString(HEAPCACHE_TARGET_SIZE);
        if (humanSize != null && !humanSize.isEmpty()) {
            long size = StringUtils.toByteSize(humanSize);
            if (size <= 0) {
                throw new ConfigurationException(HEAPCACHE_TARGET_SIZE +
                        " must be greater than zero.");
            }
            return size;
        }
        throw new ConfigurationException(HEAPCACHE_TARGET_SIZE + " is null");
    }

    /**
     * Purges as much content as needed to reduce the current size below the
     * target size, starting with the least-recently-used first.
     */
    void evictExcess() {
        synchronized (Worker.class) {
            final long size       = getByteSize();
            final long targetSize = getTargetByteSize();
            long excess           = size - targetSize;
            excess                = (excess < 0) ? 0 : excess;
            LOGGER.debug("purgeExcess(): cache size: {}; target: {}; excess: {}",
                    size, targetSize, excess);
            if (excess > 0) {
                long purgedItems = 0, purgedSize = 0;
                for (Map.Entry<CacheKey, CacheValue> entry : getEntriesSortedByLastAccessedTime()) {
                    cache.remove(entry.getKey());
                    purgedItems++;
                    purgedSize += entry.getValue().getData().length;
                    if (purgedSize >= excess) {
                        break;
                    }
                }
                LOGGER.debug("purgeExcess(): purged {} items ({} bytes)",
                        purgedItems, purgedSize);
            }
        }
    }

    //endregion
    //region Cache methods

    @Override
    public void evict(Identifier identifier) {
        LOGGER.debug("evict(Identifier): evicting {}...", identifier);
        cache.keySet().removeIf(k -> k.identifier.equals(identifier.toString()));
    }

    @Override
    public void initialize() {
        // Start a worker thread to manage the size.
        try {
            ThreadPool.getInstance().submit(new Worker());
        } catch (RejectedExecutionException e) {
            LOGGER.error("initialize(): {}", e.getMessage());
        }
    }

    @Override
    public void purge() {
        LOGGER.debug("purge(): purging {} items", cache.size());
        cache.clear();
    }

    /**
     * Evicts expired cache entries.
     */
    @Override
    public void evictInvalid() {
        for (Map.Entry<CacheKey, CacheValue> entry : cache.entrySet()) {
            CacheValue value = entry.getValue();
            if (!value.isValid()) {
                cache.remove(entry.getKey());
            }
        }
    }

    @Override
    public void shutdown() {
        workerShouldWork.set(false);
    }

    //endregion
    //region InfoCache methods

    @Override
    public void evictInfos() {
        cache.entrySet().removeIf(entry -> entry.getKey().opList == null);
    }

    @Override
    public Optional<Info> fetchInfo(Identifier identifier) throws IOException {
        CacheValue item = get(new CacheKey(identifier));
        if (item != null) {
            LOGGER.debug("getInfo(): hit for {}", identifier);
            Info info = Info.fromJSON(new String(item.getData(), StandardCharsets.UTF_8));
            return Optional.of(info);
        }
        return Optional.empty();
    }

    @Override
    public void put(Identifier identifier, Info info) throws IOException {
        put(identifier, info.toJSON());
    }

    @Override
    public void put(Identifier identifier, String info) throws IOException {
        LOGGER.debug("put(): caching info for {}", identifier);
        CacheKey key     = new CacheKey(identifier);
        CacheValue value = new CachedInfo(info.getBytes(StandardCharsets.UTF_8));
        cache.put(key, value);
    }

    //endregion
    //region VariantCache methods

    @Override
    public void evict(OperationList opList) {
        LOGGER.debug("evict(OperationList): evicting {}...", opList.toString());
        cache.remove(new CacheKey(opList));
    }

    @Override
    public InputStream newVariantImageInputStream(OperationList opList,
                                                  StatResult statResult) {
        CacheKey key     = new CacheKey(opList);
        CacheValue value = get(key);
        if (value != null) {
            statResult.setLastModified(value.getLastModified());
            return new ByteArrayInputStream(value.getData());
        }
        return null;
    }

    @Override
    public CompletableOutputStream newVariantImageOutputStream(
            OperationList opList) {
        final CacheKey key     = new CacheKey(opList);
        final CacheValue value = get(key);
        if (value != null) {
            LOGGER.debug("newVariantImageOutputStream(): hit for {}",
                    opList);
            value.touch();
            return new CompletableNullOutputStream();
        } else {
            LOGGER.debug("newVariantImageOutputStream(): miss; caching {}",
                    opList);
            return new HeapCacheOutputStream(opList);
        }
    }

    //endregion
    //region HeapCache methods

    public Map<CacheKey,CacheValue> map() {
        return cache;
    }

    public long getNumVariantImages() {
        return cache.keySet()
                .stream()
                .filter(k -> k.opList != null)
                .count();
    }

    public long getNumInfos() {
        return cache.keySet()
                .stream()
                .filter(k -> k.opList == null)
                .count();
    }

    /**
     * @return Number of cached items of any kind.
     */
    public long size() {
        return cache.size();
    }

}
