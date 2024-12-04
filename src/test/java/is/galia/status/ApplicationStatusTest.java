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

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicationStatusTest extends BaseTest {

    private ApplicationStatus instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new ApplicationStatus();
    }

    @Test
    void getHeapInfoCacheMaxSizeWithInfoCacheDisabled() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, false);
        assertEquals(0, instance.getHeapInfoCacheMaxSize());
    }

    @Test
    void getHeapInfoCacheMaxSizeWithInfoCacheEnabled() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEAP_INFO_CACHE_ENABLED, true);
        assertTrue(instance.getHeapInfoCacheMaxSize() > 100);
    }

    @Test
    void getHeapInfoCacheSize() {
        assertEquals(0, instance.getHeapInfoCacheSize());
    }

    @Test
    void getNumProcessors() {
        assertTrue(instance.getNumProcessors() >= 1);
    }

    @Test
    void getVMFreeHeap() {
        assertTrue(instance.getVMFreeHeap() > 1000);
    }

    @Test
    void getVMInfo() {
        assertNotNull(instance.getVMInfo());
    }

    @Test
    void getVMMaxHeap() {
        assertTrue(instance.getVMMaxHeap() > 1000);
    }

    @Test
    void getVMName() {
        assertNotNull(instance.getVMName());
    }

    @Test
    void getVMPID() {
        assertTrue(instance.getVMPID() > 1);
    }

    @Test
    void getVMTotalHeap() {
        assertTrue(instance.getVMTotalHeap() > 1000);
    }

    @Test
    void getVMUptime() {
        assertTrue(instance.getVMUptime() > 10);
    }

    @Test
    void getVMUsedHeap() {
        assertTrue(instance.getVMUsedHeap() > 1000);
    }

    @Test
    void getVMVendor() {
        assertNotNull(instance.getVMVendor());
    }

    @Test
    void getVMVersion() {
        assertNotNull(instance.getVMVersion());
    }

    @Test
    void toMap() {
        Map<String,Object> map = instance.toMap();
        assertEquals(3, map.size());
    }

}