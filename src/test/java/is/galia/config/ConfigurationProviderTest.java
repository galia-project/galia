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

package is.galia.config;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationProviderTest extends BaseTest {

    private static final double DELTA = 0.00000001;

    private ConfigurationProvider instance;
    private Configuration config1 = new MapConfiguration();
    private Configuration config2 = new MapConfiguration();

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new ConfigurationProvider(List.of(config1, config2));
    }

    @Test
    void clear() {
        config1.setProperty("key", "value");
        config2.setProperty("key", "value");

        instance.clear();

        assertFalse(config1.getKeys().hasNext());
        assertFalse(config2.getKeys().hasNext());
    }

    @Test
    void clearProperty() {
        config1.setProperty("key1", "value");
        config1.setProperty("key2", "value");
        config2.setProperty("key1", "value");
        config2.setProperty("key2", "value");

        instance.clearProperty("key1");

        assertNull(config1.getString("key1"));
        assertNotNull(config1.getString("key2"));
        assertNull(config2.getString("key1"));
        assertNotNull(config2.getString("key2"));
    }

    @Test
    void getBoolean1() {
        // true in config1
        config1.setProperty("key", true);
        assertTrue(instance.getBoolean("key"));

        // false in config1
        config1.setProperty("key", false);
        assertFalse(instance.getBoolean("key"));

        // not set in config1, true in config2
        config1.clear();
        config2.setProperty("key", true);
        assertTrue(instance.getBoolean("key"));

        // not set in either
        config1.clear();
        config2.clear();
        try {
            instance.getBoolean("key");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    @Test
    void getBoolean2() {
        // true in config1
        config1.setProperty("key", true);
        assertTrue(instance.getBoolean("key", false));

        // not set in config1, true in config2
        config1.clear();
        config2.setProperty("key", true);
        assertTrue(instance.getBoolean("key", false));

        // not set in either
        config1.clear();
        config2.clear();
        assertTrue(instance.getBoolean("key", true));
    }

    @Test
    void getDouble1() {
        // set in config1
        config1.setProperty("key", 1.0);
        assertEquals(1.0, instance.getDouble("key"), DELTA);

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", 1.0);
        assertEquals(1.0, instance.getDouble("key"), DELTA);

        // not set in either
        config1.clear();
        config2.clear();
        try {
            instance.getDouble("key");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    @Test
    void getDouble2() {
        // set in config1
        config1.setProperty("key", 1.0);
        assertEquals(1.0, instance.getDouble("key", 2.0), DELTA);

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", 1.0);
        assertEquals(1.0, instance.getDouble("key", 2.0), DELTA);

        // not set in either
        config1.clear();
        config2.clear();
        assertEquals(2.0, instance.getDouble("key", 2.0), DELTA);
    }

    @Test
    void getFile() {
        assertFalse(instance.getFile().isPresent());
    }

    @Test
    void getFloat1() {
        // set in config1
        config1.setProperty("key", 1f);
        assertEquals(1f, instance.getFloat("key"), DELTA);

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", 1f);
        assertEquals(1f, instance.getFloat("key"), DELTA);

        // not set in either
        config1.clear();
        config2.clear();
        try {
            instance.getFloat("key");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    @Test
    void getFloat2() {
        // set in config1
        config1.setProperty("key", 1f);
        assertEquals(1f, instance.getFloat("key", 2f), DELTA);

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", 1f);
        assertEquals(1f, instance.getFloat("key", 2f), DELTA);

        // not set in either
        config1.clear();
        config2.clear();
        assertEquals(2f, instance.getFloat("key", 2f), DELTA);
    }

    @Test
    void getInt1() {
        // set in config1
        config1.setProperty("key", 1);
        assertEquals(1, instance.getInt("key"));

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", 1);
        assertEquals(1, instance.getInt("key"));

        // not set in either
        config1.clear();
        config2.clear();
        try {
            instance.getInt("key");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    @Test
    void getInt2() {
        // set in config1
        config1.setProperty("key", 1);
        assertEquals(1, instance.getInt("key", 2), DELTA);

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", 1);
        assertEquals(1, instance.getInt("key", 2), DELTA);

        // not set in either
        config1.clear();
        config2.clear();
        assertEquals(2, instance.getInt("key", 2), DELTA);
    }

    @Test
    void getKeys() {
        Iterator<String> it = instance.getKeys();
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(0, count);

        config1.setProperty("cats", "cats");
        config2.setProperty("dogs", "dogs");
        it = instance.getKeys();
        count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    void getLong1() {
        // set in config1
        config1.setProperty("key", 1);
        assertEquals(1, instance.getLong("key"));

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", 1);
        assertEquals(1, instance.getLong("key"));

        // not set in either
        config1.clear();
        config2.clear();
        try {
            instance.getLong("key");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    @Test
    void getLong2() {
        // set in config1
        config1.setProperty("key", 1);
        assertEquals(1, instance.getLong("key", 2), DELTA);

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", 1);
        assertEquals(1, instance.getLong("key", 2), DELTA);

        // not set in either
        config1.clear();
        config2.clear();
        assertEquals(2, instance.getLong("key", 2), DELTA);
    }

    @Test
    void getProperty() {
        // set in config1
        config1.setProperty("key", "value");
        assertEquals("value", instance.getProperty("key"));

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", "value");
        assertEquals("value", instance.getProperty("key"));

        // not set in either
        config1.clear();
        config2.clear();
        assertNull(instance.getProperty("key"));
    }

    @Test
    void getString1() {
        // set in config1
        config1.setProperty("key", "value");
        assertEquals("value", instance.getString("key"));

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", "value");
        assertEquals("value", instance.getString("key"));

        // not set in either
        config1.clear();
        config2.clear();
        assertNull(instance.getString("key"));
    }

    @Test
    void getString2() {
        // set in config1
        config1.setProperty("key", "value");
        assertEquals("value", instance.getString("key", "default"));

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", "value");
        assertEquals("value", instance.getString("key", "default"));

        // not set in either
        config1.clear();
        config2.clear();
        assertEquals("default", instance.getString("key", "default"));
    }

    @Test
    void getWrappedConfigurations() {
        assertEquals(2, instance.getWrappedConfigurations().size());
    }

    @Test
    void reload() {
        // TODO: write this
    }

    @Test
    void save() {
        // TODO: write this
    }

    @Test
    void setProperty() {
        instance.setProperty("key", "cats");
        assertEquals("cats", config1.getProperty("key"));
        assertEquals("cats", config2.getProperty("key"));
    }

}
