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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

final class TaskRunner implements Runnable {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TaskRunner.class);

    private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();

    /**
     * @return Unmodifiable list of all queued tasks, including the one
     *         currently running, if any. Completed tasks are not included.
     *         Tasks may change from moment to moment, but the returned list
     *         is fixed.
     */
    List<Object> queuedTasks() {
        Object[] tasks = new Object[] {};
        return List.of(queue.toArray(tasks));
    }

    @Override
    public void run() {
        while (true) {
            Object object;
            try {
                object = queue.take();

                LOGGER.debug("run(): running {}", object);
                if (object instanceof Runnable runnable) {
                    runnable.run();
                } else if (object instanceof Callable<?> callable) {
                    callable.call();
                }
            } catch (InterruptedException e) { // happens at shutdown
                LOGGER.debug("Interrupted");
                return;
            } catch (Exception e) {
                LOGGER.error("run(): {}", e.getMessage(), e);
            }
        }
    }

    /**
     * @param callable Object to submit to the queue.
     * @throws IllegalStateException If the queue is full.
     */
    boolean submit(Callable<?> callable) {
        LOGGER.debug("submit(): {} (queue size: {})", callable, queue.size());
        return queue.add(callable);
    }

    /**
     * @param runnable Object to submit to the queue.
     * @throws IllegalStateException If the queue is full.
     */
    boolean submit(Runnable runnable) {
        LOGGER.debug("submit(): {} (queue size: {})", runnable, queue.size());
        final boolean result = queue.add(runnable);

        if (runnable instanceof AuditableFutureTask<?> aTask) {
            aTask.setStatus(TaskStatus.QUEUED);
            aTask.setInstantQueued(Instant.now());
        }
        return result;
    }

}
