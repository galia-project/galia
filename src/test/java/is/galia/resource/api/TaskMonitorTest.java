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

package is.galia.resource.api;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

class TaskMonitorTest extends BaseTest {

    private TaskMonitor instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = TaskMonitor.getInstance();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.setUp();
        instance = null;
        TaskMonitor.clearInstance();
    }

    /* add() */

    @Test
    void add() {
        Callable<?> callable = new EvictInfosFromCacheCommand<>();
        APITask<?> message   = new APITask<>(callable);
        assertNull(instance.get(message.getUUID()));
        instance.add(message);
        assertNotNull(instance.get(message.getUUID()));
    }

    /* get() */

    @Test
    void get() {
        Callable<?> callable = new EvictInfosFromCacheCommand<>();
        APITask<?> message   = new APITask<>(callable);
        instance.add(message);
        assertNotNull(instance.get(message.getUUID()));
    }

    /* getAll() */

    @Test
    void getAll() {
        Callable<?> callable = new EvictInfosFromCacheCommand<>();
        APITask<?> message   = new APITask<>(callable);
        instance.add(message);
        assertFalse(instance.getAll().isEmpty());
    }

}
