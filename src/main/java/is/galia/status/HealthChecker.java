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

package is.galia.status;

import is.galia.async.VirtualThreadPool;
import is.galia.cache.CacheFacade;
import is.galia.cache.InfoCache;
import is.galia.cache.VariantCache;
import is.galia.image.Identifier;
import is.galia.image.Info;
import is.galia.operation.OperationList;
import is.galia.source.Source;
import is.galia.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <p>Checks various aspects of the application to verify that they are
 * functioning correctly:</p>
 *
 * <dl>
 *     <dt>Source I/O</dt>
 *     <dd>When an image endpoint successfully completes a request, it calls
 *     {@link #addSourceUsage(Source)} to register the source it used to do so.
 *     This class tests the I/O of each unique source.</dd>
 *     <dt>The variant cache</dt>
 *     <dd>An image is written to the variant cache (if available) and read
 *     back.</dd>
 *     <dt>The info cache</dt>
 *     <dd>An info is written to the info cache (if available) and read
 *     back.</dd>
 * </dl>
 */
public final class HealthChecker {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HealthChecker.class);

    private static final Set<SourceUsage> SOURCE_USAGES =
            ConcurrentHashMap.newKeySet();

    /**
     * Can be set during testing to cause {@link #checkSerially()} to return a
     * custom instance.
     */
    private static Health overriddenHealth;

    /**
     * <p>Informs the class of a {@link Source} that has just been used
     * successfully, and could be used again in the course of a health check.
     * Should be called by image processing endpoints after processing has
     * completed successfully.</p>
     *
     * <p>This method is thread-safe.</p>
     */
    public static void addSourceUsage(Source source) {
        final SourceUsage usage = new SourceUsage(source);
        // The pair is configured to read a specific image. We want to remove
        // any older "equal" (see this ivar's equals() method!) instance before
        // adding the current one because the older one is more likely to be
        // stale (no longer accessible), in light of the possibility that the
        // application has been running for a while.
        SOURCE_USAGES.remove(usage);
        SOURCE_USAGES.add(usage);
    }

    /**
     * For testing only!
     */
    public static Set<SourceUsage> getSourceUsages() {
        return SOURCE_USAGES;
    }

    /**
     * For testing only!
     *
     * @param health Custom instance that will be returned by {@link
     *               #checkSerially()}. Supply {@code null} to clear the
     *               override.
     */
    public static synchronized void overrideHealth(Health health) {
        overriddenHealth = health;
    }

    /**
     * <p>Checks the functionality of every {@link #addSourceUsage(Source)
     * known source}.</p>
     *
     * <p>The individual checks are done concurrently in as many threads as
     * there are unique pairs.</p>
     */
    private static synchronized void checkSources(Health health) {
        // Make a local copy to ensure that another thread doesn't change it
        // underneath us.
        final Set<SourceUsage> localUsages = new HashSet<>(SOURCE_USAGES);
        final int numUsages                = localUsages.size();

        final CountDownLatch latch = new CountDownLatch(numUsages);
        localUsages.forEach(usage -> {
            VirtualThreadPool.getInstance().submit(() -> {
                final Source source = usage.getSource();
                LOGGER.trace("Exercising source I/O for {}", usage);
                try {
                    source.stat();
                } catch (IOException e) {
                    health.setMinColor(Health.Color.RED);
                    health.setMessage(String.format("%s (%s)",
                            e.getMessage(), usage));
                } finally {
                    latch.countDown();
                }
            });
        });

        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            health.setMinColor(Health.Color.YELLOW);
            health.setMessage(e.getMessage());
        }
    }

    /**
     * Checks the reading and writing functionality of the variant cache.
     */
    private static synchronized void checkVariantCache(Health health) {
        final CacheFacade cacheFacade = new CacheFacade();
        final Optional<VariantCache> optVariantCache =
                cacheFacade.getVariantCache();
        if (optVariantCache.isPresent()) {
            VariantCache variantCache = optVariantCache.get();
            LOGGER.trace("Exercising the variant cache: {}", variantCache);
            final Identifier identifier =
                    new Identifier("HealthCheck-" + UUID.randomUUID());
            final OperationList opList = OperationList.builder()
                    .withIdentifier(identifier)
                    .build();
            try {
                // Write to the cache.
                try (OutputStream os = variantCache.newVariantImageOutputStream(opList)) {
                    os.write(opList.toString().getBytes(StandardCharsets.UTF_8));
                }
                // Read it back in.
                try (InputStream is = variantCache.newVariantImageInputStream(opList)) {
                    is.readAllBytes();
                }
                // Delete it.
                variantCache.evict(identifier);
            } catch (Throwable t) {
                health.setMinColor(Health.Color.RED);
                String message = String.format("%s: %s",
                        variantCache.getClass().getSimpleName(),
                        t.getMessage());
                health.setMessage(message);
            }
        }
    }

    /**
     * Checks the reading and writing functionality of the variant cache.
     */
    private static synchronized void checkInfoCache(Health health) {
        final CacheFacade cacheFacade          = new CacheFacade();
        final Optional<InfoCache> optInfoCache = cacheFacade.getInfoCache();
        if (optInfoCache.isPresent()) {
            InfoCache infoCache = optInfoCache.get();
            LOGGER.trace("Exercising the info cache: {}", infoCache);
            final Identifier identifier =
                    new Identifier("HealthCheck-" + UUID.randomUUID());
            try {
                // Write to the cache.
                infoCache.put(identifier, new Info());
                // Read it back.
                infoCache.fetchInfo(identifier);
                // Delete it.
                infoCache.evict(identifier);
            } catch (Throwable t) {
                health.setMinColor(Health.Color.RED);
                String message = String.format("%s: %s",
                        infoCache.getClass().getSimpleName(),
                        t.getMessage());
                health.setMessage(message);
            }
        }
    }

    /**
     * <p>Performs a health check as explained in the class documentation.
     * Each group of checks (variant cache, source I/O, etc.) is performed
     * sequentially. If any check fails, all remaining checks are skipped.</p>
     *
     * <p>N.B.: it may be faster to {@link #checkConcurrently() do all of the
     * checks concurrently}; but this could also result in having to do more
     * checks than necessary, since, if a single check fails, none of the
     * others matter.</p>
     *
     * @return Instance reflecting the application health.
     * @see #checkConcurrently()
     */
    public Health checkSerially() {
        LOGGER.trace("Initiating a health check");

        if (overriddenHealth != null) {
            return overriddenHealth;
        }

        final Stopwatch watch = new Stopwatch();
        final Health health   = new Health();

        // Check source input.
        if (!Health.Color.RED.equals(health.getColor())) {
            checkSources(health);
            LOGGER.trace("Source I/O check completed in {}; health so far is {}",
                    watch, health.getColor());
        }

        // Check the variant cache.
        if (!Health.Color.RED.equals(health.getColor())) {
            checkVariantCache(health);
            LOGGER.trace("Variant cache check completed in {}; health so far is {}",
                    watch, health.getColor());
        }

        // Check the variant cache.
        if (!Health.Color.RED.equals(health.getColor())) {
            checkInfoCache(health);
            LOGGER.trace("Info cache check completed in {}; health so far is {}",
                    watch, health.getColor());
        }

        // Log the final status.
        LOGGER.trace("Sequential health check completed in {}: {}",
                watch, health);
        return health;
    }

    /**
     * <p>Performs a health check as explained in the class documentation.
     * Each group of checks (variant cache, source I/O, etc.) is performed
     * concurrently. This may cause the check to complete faster than the one
     * via {@link #checkSerially()} but it also may result in performing more
     * checks than necessary.</p>
     *
     * @return Instance reflecting the application health.
     * @see #checkSerially()
     */
    public Health checkConcurrently() {
        LOGGER.trace("Initiating a health check");

        if (overriddenHealth != null) {
            return overriddenHealth;
        }

        final Stopwatch watch        = new Stopwatch();
        final Health health          = new Health();
        final VirtualThreadPool pool = VirtualThreadPool.getInstance();
        final CountDownLatch latch   = new CountDownLatch(3);

        // Check source I/O.
        pool.submit(() -> {
            try {
                checkSources(health);
                LOGGER.trace("Source I/O check completed in {}; health so far is {}",
                        watch, health.getColor());
            } finally {
                latch.countDown();
            }
        });

        // Check the variant cache.
        pool.submit(() -> {
            try {
                checkVariantCache(health);
                LOGGER.trace("Variant cache check completed in {}; health so far is {}",
                        watch, health.getColor());
            } finally {
                latch.countDown();
            }
        });

        // Check the info cache.
        pool.submit(() -> {
            try {
                checkInfoCache(health);
                LOGGER.trace("Info cache check completed in {}; health so far is {}",
                        watch, health.getColor());
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
        }

        // Log the final status.
        LOGGER.trace("Concurrent health check completed in {}: {}",
                watch, health);
        return health;
    }

}
