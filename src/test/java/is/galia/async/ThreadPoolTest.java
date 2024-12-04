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

package is.galia.async;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ThreadPoolTest extends BaseTest {

    private static final int ASYNC_WAIT = 50;

    private ThreadPool instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = ThreadPool.getInstance();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        ThreadPool.clearInstance();
    }

    @Test
    void isShutdown() {
        assertFalse(instance.isShutdown());
        instance.shutdown();
        assertTrue(instance.isShutdown());
    }

    @Test
    void shutdown() {
        assertFalse(instance.isShutdown());
        instance.shutdown();
        assertTrue(instance.isShutdown());
    }

    @Test
    void submitWithCallable() throws Exception {
        final AtomicInteger atomicInt = new AtomicInteger(0);
        instance.submit(() -> {
            atomicInt.incrementAndGet();
            return null;
        });
        Thread.sleep(ASYNC_WAIT);
        assertEquals(1, atomicInt.get());
    }

    @Test
    void submitWithCallableWithPriority() throws Exception {
        final AtomicInteger atomicInt = new AtomicInteger(0);
        instance.submit(() -> {
            atomicInt.incrementAndGet();
            return null;
        }, ThreadPool.Priority.HIGH);
        Thread.sleep(ASYNC_WAIT);
        assertEquals(1, atomicInt.get());
    }

    @Test
    void submitWithRunnable() throws Exception {
        final AtomicInteger atomicInt = new AtomicInteger(0);
        instance.submit(atomicInt::incrementAndGet);
        Thread.sleep(ASYNC_WAIT);
        assertEquals(1, atomicInt.get());
    }

    @Test
    void submitWithRunnableWithPriority() throws Exception {
        final AtomicInteger atomicInt = new AtomicInteger(0);
        instance.submit(atomicInt::incrementAndGet, ThreadPool.Priority.HIGH);
        Thread.sleep(ASYNC_WAIT);
        assertEquals(1, atomicInt.get());
    }

}
