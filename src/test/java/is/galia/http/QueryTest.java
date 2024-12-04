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

package is.galia.http;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class QueryTest extends BaseTest {

    private Query instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Query("key1=value1&key2=value2");
    }

    @Test
    void stringConstructorWithEmptyString() {
        instance = new Query("");
        assertTrue(instance.isEmpty());
    }

    @Test
    void stringConstructorWithNonEmptyString() {
        assertEquals("value1", instance.getFirstValue("key1"));
        assertEquals("value2", instance.getFirstValue("key2"));
    }

    @Test
    void stringConstructorWithEncodedCharacters() {
        instance = new Query("key1%60=value1%60&key2%60");
        assertEquals("value1`", instance.getFirstValue("key1`"));
        assertNull(instance.getFirstValue("key2`"));
    }

    @Test
    void copyConstructor() {
        Query q2 = new Query(instance);
        assertEquals(q2, instance);
    }

    @Test
    void add1() {
        instance.add("key1");
        assertEquals(2, instance.getAll("key1").size());
    }

    @Test
    void add2() {
        instance.add("key1", "dogs");
        assertEquals(2, instance.getAll("key1").size());
    }

    @Test
    void clear() {
        instance.clear();
        assertTrue(instance.isEmpty());
    }

    @Test
    void equalsWithEqualObjects() {
        Query instance2 = new Query("key1=value1&key2=value2");
        assertEquals(instance, instance2);
    }

    @Test
    void equalsWithUnequalObjects() {
        Query instance2 = new Query("key1=value1&key3=value3");
        assertNotEquals(instance, instance2);
    }

    @Test
    void getAll() {
        List<KeyValuePair> actual = instance.getAll();
        assertEquals(2, actual.size());
    }

    @Test
    void getAllWithString() {
        List<KeyValuePair> actual = instance.getAll("key1");
        assertEquals(1, actual.size());
        assertEquals("key1", actual.getFirst().key());
        assertEquals("value1", actual.getFirst().value());
    }

    @Test
    void getFirstValue() {
        assertEquals("value1", instance.getFirstValue("key1"));
        assertNull(instance.getFirstValue("bogus"));
    }

    @Test
    void getFirstValueWithNullValue() {
        instance = new Query("key1=value1&key2");
        assertNull(instance.getFirstValue("key2"));
    }

    @Test
    void getFirstValueWithDefaultValue() {
        assertEquals("value1", instance.getFirstValue("key1", "default"));
        assertEquals("default", instance.getFirstValue("bogus", "default"));
    }

    @Test
    void testHashCode() {
        assertEquals(Objects.hashCode(instance.getAll().hashCode()),
                instance.hashCode());
    }

    @Test
    void isEmpty() {
        assertFalse(instance.isEmpty());
        instance.removeAll("key1");
        instance.removeAll("key2");
        assertTrue(instance.isEmpty());
    }

    @Test
    void iterator() {
        Iterator<KeyValuePair> it = instance.iterator();
        assertNotNull(it.next());
        assertNotNull(it.next());

        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void removeOnNonExistingKey() {
        int size = instance.size();
        instance.remove("bogus");
        assertEquals(size, instance.size());
    }

    @Test
    void removeOnExistingKey() {
        int size = instance.size();
        instance.remove("key1");
        assertEquals(size - 1, instance.size());
    }

    @Test
    void removeAll() {
        instance.removeAll("key1");
        assertNull(instance.getFirstValue("key1"));
    }

    @Test
    void set() {
        assertNull(instance.getFirstValue("test"));
        instance.set("test", "cats");
        assertEquals("cats", instance.getFirstValue("test"));
    }

    @Test
    void size() {
        assertEquals(2, instance.size());
    }

    @Test
    void stream() {
        assertNotNull(instance.stream());
    }

    @Test
    void toMap() {
        Map<String,String> actual = instance.toMap();
        assertEquals(2, actual.size());
        assertEquals("value1", actual.get("key1"));
        assertEquals("value2", actual.get("key2"));
    }

    @Test
    void testToString() {
        assertEquals("key1=value1&key2=value2", instance.toString());
    }

    @Test
    void toStringWithEmptyInstance() {
        instance = new Query();
        assertEquals("", instance.toString());
    }

}