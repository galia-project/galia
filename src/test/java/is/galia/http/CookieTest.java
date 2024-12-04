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

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class CookieTest extends BaseTest {

    private Cookie instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Cookie("name", "value", "example.org", "/path",
                Duration.ofDays(365), true, true);
    }

    @Test
    void constructorWithNullNameArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new Cookie(null, "value"));
    }

    @Test
    void constructorWithZeroLengthNameArgument() {
        instance = new Cookie("", "value");
        assertEquals("", instance.name());
    }

    @Test
    void constructorWithIllegalNameArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new Cookie("name;", "value"));
    }

    @Test
    void constructorWithNullValueArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new Cookie("name", null));
    }

    @Test
    void constructorWithZeroLengthValueArgument() {
        instance = new Cookie("name", "");
        assertEquals("", instance.value());
    }

    @Test
    void constructorWithIllegalValueArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new Cookie("name", "value;"));
    }

    @Test
    void copyConstructor() {
        Cookie other = new Cookie(instance);
        assertEquals("name", other.name());
        assertEquals("value", other.value());
    }

    /* toString() */

    @Test
    void testToString() {
        String expected = "name=value; Domain=example.org; Path=/path; " +
                "Max-Age=31536000; Secure; HttpOnly";
        assertEquals(expected, instance.toString());
    }

    @Test
    void testToStringWithRootPath() {
        instance = new Cookie("name", "value", "example.org", "/",
                Duration.ofDays(365), true, true);
        String expected = "name=value; Domain=example.org; " +
                "Max-Age=31536000; Secure; HttpOnly";
        assertEquals(expected, instance.toString());
    }

}
