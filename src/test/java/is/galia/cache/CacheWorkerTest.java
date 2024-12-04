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

import is.galia.config.Key;
import is.galia.test.BaseTest;
import is.galia.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CacheWorkerTest extends BaseTest {

    private CacheWorker instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.forApplication();
        config.setProperty(Key.VARIANT_CACHE_ENABLED, true);
        config.setProperty(Key.VARIANT_CACHE, MockCache.class.getName());

        instance = new CacheWorker(-1); // we aren't using the interval
    }

    @Test
    void runCallsCleanUp() {
        MockCache cache = (MockCache) CacheFactory.getVariantCache().get();
        instance.run();
        assertTrue(cache.isCleanUpCalled());
    }

    @Test
    void runCallsEvictInvalid() {
        MockCache cache = (MockCache) CacheFactory.getVariantCache().get();
        instance.run();
        assertTrue(cache.isPurgeInvalidCalled());
    }

    @Test
    void runCallsOnCacheWorkerCallback() {
        MockCache cache = (MockCache) CacheFactory.getVariantCache().get();
        instance.run();
        assertTrue(cache.isOnCacheWorkerCalled());
    }

}
