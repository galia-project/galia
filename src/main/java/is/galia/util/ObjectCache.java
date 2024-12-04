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

package is.galia.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.ConcurrentMap;

/**
 * Size-bounded heap cache.
 */
public final class ObjectCache<K, V> {

    private final long maxSize;

    // This is thread-safe.
    private Cache<K, V> store;

    /**
     * Creates an instance with the given max size.
     */
    public ObjectCache(long maxSize) {
        this.maxSize = maxSize;
        store = Caffeine.newBuilder().softValues().maximumSize(maxSize).build();
    }

    public ConcurrentMap<K,V> asMap() {
        return store.asMap();
    }

    public void cleanUp() {
        store.cleanUp();
    }

    public V get(K key) {
        return store.getIfPresent(key);
    }

    public long maxSize() {
        return maxSize;
    }

    public void purge() {
        store.invalidateAll();
    }

    public void put(K key, V value) {
        store.put(key, value);
    }

    public void remove(K key) {
        store.invalidate(key);
    }

    public void removeAll() {
        store.invalidateAll();
    }

    public long size() {
        return store.estimatedSize();
    }

}
