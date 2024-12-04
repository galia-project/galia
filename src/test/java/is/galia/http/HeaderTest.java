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

import static org.junit.jupiter.api.Assertions.*;

class HeaderTest extends BaseTest {

    private Header instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Header("name", "value");
    }

    @Test
    void copyConstructor() {
        Header other = new Header(instance);
        assertEquals("name", other.name());
        assertEquals("value", other.value());
    }

    @Test
    void constructorWithNullNameArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new Header(null, "value"));
    }

    @Test
    void constructorWithNullValueArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new Header("name", null));
    }

    @Test
    void constructorWithZeroLengthNameArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new Header("", "value"));
    }

    @Test
    void constructorWithZeroLengthValueArgument() {
        instance = new Header("name", "");
        assertEquals("", instance.value());
    }

    @Test
    void testToString() {
        assertEquals(instance.name() + ": " + instance.value(),
                instance.toString());
    }

}
