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

import static org.junit.jupiter.api.Assertions.*;

class CookiesTest extends BaseTest {

    private Cookies instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Cookies();
    }

    @Test
    void fromHeaderValue() {
        Cookies expected = new Cookies();
        expected.add("name1", "value1");
        expected.add("name2", "value2");
        Cookies actual = Cookies.fromHeaderValue("name1=value1;name2=value2");
        assertEquals(expected, actual);
    }

    @Test
    void fromHeaderValueWithNoName() {
        Cookies expected = new Cookies();
        expected.add("", "value1");
        Cookies actual = Cookies.fromHeaderValue("value1");
        assertEquals(expected, actual);
    }

    @Test
    void fromHeaderValuePermissiveness() {
        final String torture = "abcABC012 !#$%&'()*+-./:<>?@[]^_`{|}~ 简体中文网页";

        Cookies expected = new Cookies();
        expected.add("name1", torture);
        expected.add("name2", torture);
        Cookies actual = Cookies.fromHeaderValue(
                "name1=" + torture + "; name2=" + torture);
        assertEquals(expected, actual);
    }

    @Test
    void copyConstructor() {
        instance.add("cookie1", "cats");
        instance.add("cookie1", "dogs");
        instance.add("cookie2", "foxes");

        Cookies other = new Cookies(instance);
        assertEquals(3, other.size());
    }

    @Test
    void addWithCookie() {
        assertEquals(0, instance.size());
        instance.add(new Cookie("name", "value"));
        assertEquals(1, instance.size());
    }

    @Test
    void addWithStrings() {
        assertEquals(0, instance.size());
        instance.add("name", "value");
        assertEquals(1, instance.size());
    }

    @Test
    void addAll() {
        instance.add("name", "value");
        assertEquals(1, instance.size());

        Cookies other = new Cookies();
        other.add("name2", "value2");
        other.add("name3", "value3");

        instance.addAll(other);
        assertEquals(3, instance.size());
    }

    @Test
    void clear() {
        instance.add("name", "value");
        instance.add("name", "value");
        instance.clear();
        assertEquals(0, instance.size());
    }

    @Test
    void equalsWithEqualObjects() {
        Cookies h1 = new Cookies();
        Cookies h2 = new Cookies();
        assertEquals(h1, h2);

        h1.add("test", "cats1");
        h1.add("test", "cats2");
        h2.add("test", "cats1");
        h2.add("test", "cats2");
        assertEquals(h1, h2);
    }

    @Test
    void equalsWithUnequalObjects() {
        Cookies h1 = new Cookies();
        Cookies h2 = new Cookies();
        h1.add("test", "cats1");
        h1.add("test", "cats2");
        h2.add("test", "cats1");
        h2.add("test2", "cats2");
        assertNotEquals(h1, h2);
    }

    @Test
    void getAll() {
        assertEquals(0, instance.getAll().size());

        instance.add("name", "value");
        instance.add("name", "value");
        assertEquals(2, instance.getAll().size());
    }

    @Test
    void getAllWithString() {
        assertEquals(0, instance.getAll("name").size());

        instance.add("name", "value");
        instance.add("name", "value");
        assertEquals(2, instance.getAll("name").size());
    }

    @Test
    void getAllListIsUnmodifiable() {
        List<Cookie> cookies = instance.getAll();
        assertThrows(UnsupportedOperationException.class,
                () -> cookies.add(new Cookie("name", "value")));
    }

    @Test
    void getFirstValue() {
        assertNull(instance.getFirstValue("name"));

        instance.add("name", "value1");
        instance.add("name", "value2");
        assertEquals("value1", instance.getFirstValue("name"));
    }

    @Test
    void getFirstValueWithDefaultValue() {
        assertEquals("default value",
                instance.getFirstValue("name", "default value"));

        instance.add("name", "value1");
        instance.add("name", "value2");
        assertEquals("value1", instance.getFirstValue("name"));
    }

    @Test
    void hashCodeWithEqualObjects() {
        Cookies h1 = new Cookies();
        Cookies h2 = new Cookies();
        assertEquals(h1.hashCode(), h2.hashCode());

        h1.add("test", "cats1");
        h1.add("test", "cats2");
        h2.add("test", "cats1");
        h2.add("test", "cats2");
        assertEquals(h1.hashCode(), h2.hashCode());
    }

    @Test
    void hashCodeWithUnequalObjects() {
        Cookies h1 = new Cookies();
        Cookies h2 = new Cookies();
        h1.add("test", "cats1");
        h1.add("test", "cats2");
        h2.add("test", "cats1");
        h2.add("test2", "cats2");
        assertNotEquals(h1.hashCode(), h2.hashCode());
    }

    @Test
    void iterator() {
        instance.add("name", "value1");
        instance.add("name", "value2");

        Iterator<Cookie> it = instance.iterator();
        assertTrue(it.hasNext());
        it.next();
        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());
    }

    @Test
    void removeAll() {
        instance.add("name1", "value1");
        instance.add("name1", "value2");
        instance.add("name2", "value1");
        instance.removeAll("name1");
        assertEquals(1, instance.size());
        assertNotNull(instance.getFirstValue("name2"));
    }

    @Test
    void set() {
        instance.set("name1", "value1");
        instance.set("name2", "value1");
        instance.set("name2", "value2");
        assertEquals(1, instance.getAll("name2").size());
        assertEquals("value2", instance.getFirstValue("name2"));
    }

    @Test
    void size() {
        assertEquals(0, instance.size());
        instance.add("name", "value");
        instance.add("name", "value");
        assertEquals(2, instance.size());
    }

    @Test
    void stream() {
        instance.add("name", "value");
        instance.add("name", "value");
        assertEquals(2, instance.stream().count());
    }

    @Test
    void toMap() {
        instance.add("name1", "value1");
        instance.add("name1", "value2");
        instance.add("name2", "value1");
        Map<String, String> map = instance.toMap();
        assertEquals(2, map.size());
    }

}
