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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.*;

class TaskQueueTest extends BaseTest {

    private TaskQueue instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        TaskQueue.clearInstance();
        instance = TaskQueue.getInstance();
    }

    /* queuedTasks() */

    @Test
    void queuedTasks() throws Exception {
        for (int i = 0; i < 3; i++) {
            instance.submit(new FutureTask<>(
                    new MockCallable<>(Duration.ofMillis(100))));
        }

        Thread.sleep(50);

        // The first has been taken out and is running.
        assertEquals(2, instance.queuedTasks().size());
    }

    /* submit(Callable<?>) */

    @Test
    void submitWithCallable() throws Exception {
        MockCallable<?> callable1 = new MockCallable<>(Duration.ofMillis(100));
        MockCallable<?> callable2 = new MockCallable<>(Duration.ofMillis(100));
        MockCallable<?> callable3 = new MockCallable<>(Duration.ofMillis(100));

        instance.submit(callable1);
        instance.submit(callable2);
        instance.submit(callable3);

        Thread.sleep(50);

        // The first has been taken out and is running.
        assertEquals(2, instance.queuedTasks().size());

        Thread.sleep(300); // wait for them all to complete
        assertTrue(callable1.hasRun());
        assertTrue(callable2.hasRun());
        assertTrue(callable3.hasRun());
    }

    /* submit(Runnable) */

    @Test
    void submitWithRunnable() throws Exception {
        MockRunnable runnable1 = new MockRunnable(Duration.ofMillis(100));
        MockRunnable runnable2 = new MockRunnable(Duration.ofMillis(100));
        MockRunnable runnable3 = new MockRunnable(Duration.ofMillis(100));

        instance.submit(runnable1);
        instance.submit(runnable2);
        instance.submit(runnable3);

        Thread.sleep(50);

        // The first has been taken out and is running.
        assertEquals(2, instance.queuedTasks().size());

        Thread.sleep(300); // wait for them all to complete
        assertTrue(runnable1.hasRun());
        assertTrue(runnable2.hasRun());
        assertTrue(runnable3.hasRun());
    }

    /* submit(Runnable) with AuditableFutureTask */

    @Test
    void submitWithAuditableFutureTaskSetsQueuedTaskStatus() throws Exception {
        AuditableFutureTask<?> future1 =
                new AuditableFutureTask<>(new MockCallable<>(Duration.ofMillis(100)));
        assertEquals(TaskStatus.NEW, future1.getStatus());
        AuditableFutureTask<?> future2 =
                new AuditableFutureTask<>(new MockCallable<>(Duration.ofMillis(100)));
        assertEquals(TaskStatus.NEW, future2.getStatus());

        instance.submit(future1);
        instance.submit(future2);

        Thread.sleep(50);

        assertEquals(TaskStatus.RUNNING, future1.getStatus());
        assertEquals(TaskStatus.QUEUED, future2.getStatus());
    }

    @Disabled // TODO: this fails in CI often
    @Test
    void submitWithAuditableFutureTaskSetsRunningTaskStatus() throws Exception {
        MockCallable<?> task = new MockCallable<>(Duration.ofMillis(100));
        AuditableFutureTask<?> future = new AuditableFutureTask<>(task);
        assertEquals(TaskStatus.NEW, future.getStatus());

        instance.submit(future);

        // This test is timing-sensitive. We will check it periodically for X
        // amount of time.
        final int stepMsec    = 50;
        final int maxWaitMsec = 5000;
        for (int elapsedMsec = 0; elapsedMsec < maxWaitMsec; elapsedMsec += stepMsec) {
            Thread.sleep(stepMsec);
            if (TaskStatus.RUNNING.equals(future.getStatus())) {
                return;
            }
        }
        fail("Task was not marked running");
    }

    @Test
    void submitWithAuditableFutureTaskSetsInstantQueued() {
        MockCallable<?> task = new MockCallable<>();
        AuditableFutureTask<?> future = new AuditableFutureTask<>(task);
        instance.submit(future);
        assertNotNull(future.getInstantQueued());
    }

    @Test
    void submitWithAuditableFutureTaskSetsInstantStarted() throws Exception {
        MockCallable<?> task = new MockCallable<>(Duration.ofMillis(100));
        AuditableFutureTask<?> future = new AuditableFutureTask<>(task);
        instance.submit(future);
        Thread.sleep(10);
        assertNotNull(future.getInstantStarted());
    }

    @Test
    void submitWithAuditableFutureTaskSetsInstantStopped() throws Exception {
        MockCallable<?> task = new MockCallable<>();
        AuditableFutureTask<?> future = new AuditableFutureTask<>(task);
        instance.submit(future);
        Thread.sleep(100);
        assertNotNull(future.getInstantStopped());
    }

    @Disabled // TODO: this fails in CI often
    @Test
    void submitWithAuditableFutureTaskSetsSuccessfulTaskStatus()
            throws Exception {
        MockCallable<?> task = new MockCallable<>();
        AuditableFutureTask<?> future = new AuditableFutureTask<>(task);
        assertEquals(TaskStatus.NEW, future.getStatus());

        instance.submit(future);

        // This test is timing-sensitive. We will check it periodically for X
        // amount of time.
        final int stepMsec    = 50;
        final int maxWaitMsec = 5000;
        for (int elapsedMsec = 0; elapsedMsec < maxWaitMsec; elapsedMsec += stepMsec) {
            Thread.sleep(stepMsec);
            if (TaskStatus.SUCCEEDED.equals(future.getStatus())) {
                return;
            }
        }
        fail("Task was not marked succeeded");
    }

    @Disabled // TODO: this fails in CI often
    @Test
    void submitWithAuditableFutureTaskSetsFailedTaskStatus() throws Exception {
        MockFailingCallable<?> task   = new MockFailingCallable<>();
        AuditableFutureTask<?> future = new AuditableFutureTask<>(task);
        assertEquals(TaskStatus.NEW, future.getStatus());

        instance.submit(future);

        // This test is timing-sensitive. We will check it periodically for X
        // amount of time.
        final int stepMsec    = 50;
        final int maxWaitMsec = 5000;
        for (int elapsedMsec = 0; elapsedMsec < maxWaitMsec; elapsedMsec += stepMsec) {
            Thread.sleep(stepMsec);
            if (TaskStatus.FAILED.equals(future.getStatus())) {
                assertEquals("Failed, as requested",
                        future.getException().getMessage());
                return;
            }
        }
        fail("Task was not marked failed");
    }

}
