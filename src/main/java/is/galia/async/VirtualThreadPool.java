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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Global virtual thread pool Singleton.
 */
public final class VirtualThreadPool {

    private static VirtualThreadPool instance;

    private boolean isShutdown;
    private final ExecutorService executorService =
            Executors.newVirtualThreadPerTaskExecutor();

    /**
     * @return Shared instance.
     */
    public static synchronized VirtualThreadPool getInstance() {
        if (instance == null || instance.isShutdown()) {
            instance = new VirtualThreadPool();
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

    private VirtualThreadPool() {}

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public void shutdown() {
        executorService.shutdownNow();
        isShutdown = true;
    }

    /**
     * Submits a task for immediate execution.
     */
    public Future<?> submit(Callable<?> task) {
        return executorService.submit(task);
    }

    /**
     * Submits a task for immediate execution.
     */
    public Future<?> submit(Runnable task) {
        return executorService.submit(task);
    }

}
