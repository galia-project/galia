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

import is.galia.cache.MockBrokenInfoCache;
import is.galia.cache.MockBrokenVariantCache;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.source.MockFileSource;
import is.galia.source.MockStreamSource;
import is.galia.source.Source;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HealthCheckerTest extends BaseTest {

    private HealthChecker instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new HealthChecker();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        HealthChecker.getSourceUsages().clear();
    }

    /* addSourceUsage() */

    @Test
    void addSourceUsage() {
        assertTrue(HealthChecker.getSourceUsages().isEmpty());

        Source source = new MockStreamSource();
        HealthChecker.addSourceUsage(source);
        assertEquals(1, HealthChecker.getSourceUsages().size());

        // Add new source of the same class
        source = new MockStreamSource();
        HealthChecker.addSourceUsage(source);
        assertEquals(1, HealthChecker.getSourceUsages().size());

        // Add unique source
        source = new MockFileSource();
        HealthChecker.addSourceUsage(source);
        assertEquals(2, HealthChecker.getSourceUsages().size());
    }

    /* checkConcurrently() */

    @Test
    void checkConcurrentlySuccessfully() {
        Health health = instance.checkConcurrently();
        assertEquals(Health.Color.GREEN, health.getColor());
        assertNull(health.getMessage());
    }

    @Test
    void checkConcurrentlyWithVariantCacheFailure() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, "true");
        config.setProperty(Key.VARIANT_CACHE,
                MockBrokenVariantCache.class.getName());

        Health health = instance.checkConcurrently();
        assertEquals(Health.Color.RED, health.getColor());
        assertEquals(MockBrokenVariantCache.class.getSimpleName() + ": I'm broken",
                health.getMessage());
    }

    @Test
    void checkConcurrentlyWithInfoCacheFailure() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.INFO_CACHE_ENABLED, "true");
        config.setProperty(Key.INFO_CACHE, MockBrokenInfoCache.class.getName());

        Health health = instance.checkConcurrently();
        assertEquals(Health.Color.RED, health.getColor());
        assertEquals(MockBrokenInfoCache.class.getSimpleName() + ": I'm broken",
                health.getMessage());
    }

    /* checkSerially() */

    @Test
    void checkSeriallySuccessfully() {
        Health health = instance.checkSerially();
        assertEquals(Health.Color.GREEN, health.getColor());
        assertNull(health.getMessage());
    }

    @Test
    void checkSeriallyWithVariantCacheFailure() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, "true");
        config.setProperty(Key.VARIANT_CACHE,
                MockBrokenVariantCache.class.getName());

        Health health = instance.checkSerially();
        assertEquals(Health.Color.RED, health.getColor());
        assertEquals(MockBrokenVariantCache.class.getSimpleName() + ": I'm broken",
                health.getMessage());
    }

    @Test
    void checkSeriallyWithInfoCacheFailure() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.INFO_CACHE_ENABLED, "true");
        config.setProperty(Key.INFO_CACHE, MockBrokenInfoCache.class.getName());

        Health health = instance.checkSerially();
        assertEquals(Health.Color.RED, health.getColor());
        assertEquals(MockBrokenInfoCache.class.getSimpleName() + ": I'm broken",
                health.getMessage());
    }

}
