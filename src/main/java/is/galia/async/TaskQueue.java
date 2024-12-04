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

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Queue of serial tasks, backed internally by {@link ThreadPool}. Should be
 * preferred over {@link ThreadPool} for tasks that are resource-intensive but
 * not time-sensitive.
 */
public final class TaskQueue {

    private static TaskQueue instance = new TaskQueue();

    private final TaskRunner runner;

    /**
     * For testing only.
     */
    static synchronized void clearInstance() {
        instance = new TaskQueue();
    }

    /**
     * @return Singleton instance.
     */
    public static synchronized TaskQueue getInstance() {
        return instance;
    }

    private TaskQueue() {
        runner = new TaskRunner();
        ThreadPool.getInstance().submit(runner);
    }

    /**
     * @return Unmodifiable list of all queued tasks, including the one
     *         currently running, if any. Completed tasks are not included.
     *         Tasks may change from moment to moment, but the returned list
     *         is fixed and immutable.
     */
    List<Object> queuedTasks() {
        return runner.queuedTasks();
    }

    /**
     * Adds a task to the queue.
     */
    public void submit(Callable<?> callable) {
        runner.submit(callable);
    }

    /**
     * Adds a task to the queue.
     */
    public void submit(Runnable runnable) {
        runner.submit(runnable);
    }

}
