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

package is.galia.resource;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe session store using the Java heap.
 */
public final class HeapSessionStore<T extends Session> {

    private final Map<String, T> sessions = new ConcurrentHashMap<>();
    private final Duration sessionDuration;

    public HeapSessionStore(Duration sessionDuration) {
        this.sessionDuration = sessionDuration;
    }

    public void evictExpired() {
        sessions.forEach((key, value) -> {
            if (!isValid(value)) {
                sessions.remove(key);
            }
        });
    }

    /**
     * Returns the valid session in the store with the given ID. If such a
     * session does not exist in the store, an empty value is returned. If an
     * invalid session exists, it is removed from the store and an empty value
     * is returned.
     *
     * @param id Session ID.
     * @return Session in the store with the given ID, if it exists and is
     *         valid.
     */
    public Optional<T> get(String id) {
        T session = sessions.get(id);
        if (session != null) {
            if (isValid(session)) {
                return Optional.of(session);
            } else {
                sessions.remove(id);
            }
        }
        return Optional.empty();
    }

    /**
     * Adds the given instance to the store. If it is expired, it is silently
     * discarded instead of being added.
     *
     * @param session Session to add to the store.
     */
    public void put(T session) {
        if (isValid(session)) {
            sessions.put(session.getID(), session);
        }
    }

    private Instant earliestValidInstant() {
        return Instant.now().minus(sessionDuration);
    }

    private boolean isValid(T session) {
        return session.getCreatedAt().isAfter(earliestValidInstant());
    }

}
