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

class KeyValuePairTest extends BaseTest {

    private KeyValuePair instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new KeyValuePair("key", "value");
    }

    @Test
    void testConstructorWithNullKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new KeyValuePair(null, "value"));
    }

    @Test
    void testConstructorWithEmptyKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new KeyValuePair("", "value"));
    }

    @Test
    void testToString() {
        assertEquals("key=value", instance.toString());
    }

    @Test
    void testToStringEncoding() {
        instance = new KeyValuePair("key`", "value`");
        assertEquals("key%60=value%60", instance.toString());
    }

}