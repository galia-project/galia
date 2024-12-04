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

import is.galia.util.StringUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Global application thread pool Singleton.
 */
public final class ThreadPool {

    public enum Priority {
        LOW, NORMAL, HIGH
    }

    private static abstract class AbstractThreadFactory {

        abstract String getThreadNamePrefix();

        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName(getThreadNamePrefix() + "-" +
                    StringUtils.randomAlphanumeric(6));
            thread.setDaemon(true);
            return thread;
        }
    }

    private static class LowPriorityThreadFactory
            extends AbstractThreadFactory implements ThreadFactory {
        @Override
        String getThreadNamePrefix() {
            return "ptpl";
        }
    }

    private static class NormalPriorityThreadFactory
            extends AbstractThreadFactory implements ThreadFactory {
        @Override
        String getThreadNamePrefix() {
            return "ptpn";
        }
    }

    private static class HighPriorityThreadFactory
            extends AbstractThreadFactory implements ThreadFactory {
        @Override
        String getThreadNamePrefix() {
            return "ptph";
        }
    }

    private static ThreadPool instance;

    private boolean isShutdown;
    private final ExecutorService lowPriorityPool =
            Executors.newCachedThreadPool(new LowPriorityThreadFactory());
    private final ExecutorService normalPriorityPool =
            Executors.newCachedThreadPool(new NormalPriorityThreadFactory());
    private final ExecutorService highPriorityPool =
            Executors.newCachedThreadPool(new HighPriorityThreadFactory());

    /**
     * @return Shared instance.
     */
    public static synchronized ThreadPool getInstance() {
        if (instance == null || instance.isShutdown()) {
            instance = new ThreadPool();
        }
        return instance;
    }

    /**
     * For testing.
     */
    static synchronized void clearInstance() {
        instance.shutdown();
        instance = null;
    }

    private ThreadPool() {
    }

    public ExecutorService getLowPriorityPool() {
        return lowPriorityPool;
    }

    public ExecutorService getNormalPriorityPool() {
        return normalPriorityPool;
    }

    public ExecutorService getHighPriorityPool() {
        return highPriorityPool;
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public void shutdown() {
        lowPriorityPool.shutdownNow();
        normalPriorityPool.shutdownNow();
        highPriorityPool.shutdownNow();
        isShutdown = true;
    }

    /**
     * Submits a task for immediate execution.
     */
    public Future<?> submit(Callable<?> task) {
        return submit(task, Priority.NORMAL);
    }

    /**
     * Submits a task for immediate execution.
     */
    public Future<?> submit(Callable<?> task, Priority priority) {
        return switch (priority) {
            case LOW  -> lowPriorityPool.submit(task);
            case HIGH -> highPriorityPool.submit(task);
            default   -> normalPriorityPool.submit(task);
        };
    }

    /**
     * Submits a task for immediate execution.
     */
    public Future<?> submit(Runnable task) {
        return submit(task, Priority.NORMAL);
    }

    /**
     * Submits a task for immediate execution.
     */
    public Future<?> submit(Runnable task, Priority priority) {
        return switch (priority) {
            case LOW  -> lowPriorityPool.submit(task);
            case HIGH -> highPriorityPool.submit(task);
            default   -> normalPriorityPool.submit(task);
        };
    }

}
