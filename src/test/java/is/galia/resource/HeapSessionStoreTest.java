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

import is.galia.test.BaseTest;
import is.galia.util.StringUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class HeapSessionStoreTest extends BaseTest {

    static class TestSession implements Session {
        private final String id;
        private Instant createdAt;

        TestSession() {
            this.createdAt = Instant.now();
            this.id        = StringUtils.randomAlphanumeric(8);
        }

        @Override
        public Instant getCreatedAt() {
            return createdAt;
        }

        @Override
        public String getID() {
            return id;
        }
    }

    private static final Duration SESSION_DURATION = Duration.ofSeconds(1);

    private final HeapSessionStore<TestSession> instance =
            new HeapSessionStore<>(SESSION_DURATION);

    /* evictExpired() */

    @Test
    void evictExpired() {
        TestSession session1 = new TestSession();
        TestSession session2 = new TestSession();
        TestSession session3 = new TestSession();
        TestSession session4 = new TestSession();
        instance.put(session1);
        instance.put(session2);
        instance.put(session3);
        instance.put(session4);
        session3.createdAt = Instant.EPOCH;
        session4.createdAt = Instant.EPOCH;
        instance.evictExpired();
        assertTrue(instance.get(session1.getID()).isPresent());
        assertTrue(instance.get(session2.getID()).isPresent());
        assertFalse(instance.get(session3.getID()).isPresent());
        assertFalse(instance.get(session4.getID()).isPresent());
    }

    /* get(String) */

    @Test
    void getWithStoredValidSession() {
        TestSession session = new TestSession();
        instance.put(session);
        assertSame(session, instance.get(session.getID()).orElse(null));
    }

    @Test
    void getWithStoredInvalidSession() {
        TestSession session = new TestSession();
        instance.put(session);
        session.createdAt = Instant.EPOCH;
        assertFalse(instance.get(session.getID()).isPresent());
    }

    @Test
    void getWithUnstoredSession() {
        assertFalse(instance.get("bogus").isPresent());
    }

    /* put() */

    @Test
    void put() {
        TestSession session = new TestSession();
        instance.put(session);
        assertNotNull(instance.get(session.getID()));
    }

    @Test
    void putWithExpiredInstance() {
        TestSession session = new TestSession();
        session.createdAt = Instant.EPOCH;
        instance.put(session);
        assertFalse(instance.get(session.getID()).isPresent());
    }

}
