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

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Base class for {@link Cache} implementations.
 */
public abstract class AbstractCache {

    private final Set<CacheObserver> observers =
            Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * @param observer Instance to add. A weak reference to it will be
     *                 maintained, so there will be no need to remove it later.
     */
    public synchronized void addObserver(CacheObserver observer) {
        observers.add(observer);
    }

    /**
     * @return Immutable copy of the internal instance. Use {@link
     *         #addObserver(CacheObserver)} to add observers.
     */
    protected synchronized Set<CacheObserver> getAllObservers() {
        return Set.copyOf(observers);
    }

}
