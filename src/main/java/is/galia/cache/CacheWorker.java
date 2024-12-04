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

import is.galia.util.Stopwatch;
import is.galia.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs in a background thread to purge invalid items from the cache.
 */
final class CacheWorker implements Runnable {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(CacheWorker.class);

    private final int interval;

    /**
     * @param interval Shift interval in seconds.
     */
    CacheWorker(int interval) {
        this.interval = interval;
    }

    /**
     * Runs one sweep of the worker.
     */
    @Override
    public void run() {
        LOGGER.info("Working...");
        final Stopwatch timer = new Stopwatch();

        VariantCache dCache = CacheFactory.getVariantCache().orElse(null);
        if (dCache != null) {
            dCache.onCacheWorker();
        }
        LOGGER.info("Finished working in {}. Next shift starts in {} seconds.",
                TimeUtils.toHMS((int) Math.round(timer.timeElapsed() / 1000.0)),
                interval);
    }

}
