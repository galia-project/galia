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

import is.galia.config.Configuration;
import is.galia.config.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class CacheWorkerRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CacheWorkerRunner.class);

    private static final int DELAY = 60;

    private static CacheWorkerRunner instance;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> future;

    /**
     * For testing only!
     */
    static synchronized void clearInstance() {
        instance = null;
    }

    /**
     * @return Singleton instance.
     */
    public static synchronized CacheWorkerRunner getInstance() {
        if (instance == null) {
            instance = new CacheWorkerRunner();
        }
        return instance;
    }

    public synchronized void start() {
        final Configuration config = Configuration.forApplication();
        final int interval = config.getInt(Key.CACHE_WORKER_INTERVAL, -1);

        LOGGER.debug("Starting the cache worker with {} second delay, {} second interval",
                DELAY, interval);

        executorService = Executors.newSingleThreadScheduledExecutor();
        future = executorService.scheduleWithFixedDelay(
                new CacheWorker(interval),
                DELAY,
                interval,
                TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        LOGGER.debug("Stopping the cache worker...");

        if (future != null) {
            future.cancel(true);
            future = null;
        }
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }

    private CacheWorkerRunner() {}

}
