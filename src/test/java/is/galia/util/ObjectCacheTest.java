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

package is.galia.util;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectCacheTest extends BaseTest {

    private static final long MAX_SIZE = 100;

    private ObjectCache<String,String> instance;

    @BeforeEach
    public void setUp() {
        instance = new ObjectCache<>(MAX_SIZE);
    }

    @Test
    void testCleanUp() {
        final long excessiveSize = MAX_SIZE + 1;
        for (long i = 0; i < excessiveSize; i++) {
            instance.put("" + i, "cats");
        }
        instance.cleanUp();
        assertEquals(MAX_SIZE, instance.size());
    }

    @Test
    void testGet() {
        final String key = "cats";
        final String value = "yes";
        instance.put(key, value);
        assertSame(value, instance.get(key));
    }

    @Test
    void testMaxSize() {
        assertEquals(MAX_SIZE, instance.maxSize());
    }

    @Test
    void testPurge() {
        instance.put("1", "1");
        instance.put("2", "2");
        instance.put("3", "3");
        assertEquals(3, instance.size());

        instance.purge();
        assertEquals(0, instance.size());
    }

    @Test
    void testPut() {
        final String key = "cats";
        final String value = "yes";
        instance.put(key, value);
        assertSame(value, instance.get(key));
    }

    @Test
    void testPutRespectsMaxSize() {
        for (int i = 0; i < MAX_SIZE * 2; i++) {
            instance.put("" + i, "cats");
        }
        instance.cleanUp();
        assertEquals(MAX_SIZE, instance.size());
    }

    @Test
    void testRemove() {
        instance.put("1", "1");
        instance.put("2", "2");
        instance.remove("1");
        assertNull(instance.get("1"));
        assertNotNull(instance.get("2"));
    }

    @Test
    void testRemoveAll() {
        instance.put("1", "1");
        instance.put("2", "2");
        instance.removeAll();
        assertEquals(0, instance.size());
    }

    @Test
    void testSize() {
        assertEquals(0, instance.size());
        instance.put("1", "1");
        instance.put("2", "2");
        instance.put("3", "3");
        assertEquals(3, instance.size());
    }

}
