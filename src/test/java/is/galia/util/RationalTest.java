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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RationalTest extends BaseTest {

    private static final float FLOAT_DELTA   = 0.00001f;
    private static final double DOUBLE_DELTA = 0.00000000001;

    private Rational instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Rational(2, 3);
    }

    @Test
    void testConstructorWithZeroDenominator() {
        assertThrows(IllegalArgumentException.class, () -> new Rational(2, 0));
    }

    @Test
    void testDoubleValue() {
        assertTrue(Math.abs((2 / 3.0) - instance.doubleValue()) < DOUBLE_DELTA);
    }

    @Test
    void testFloatValue() {
        assertTrue(Math.abs((2 / 3.0) - instance.floatValue()) < FLOAT_DELTA);
    }

    @Test
    void testGetReduced() {
        assertSame(instance, instance.reduced());
        assertEquals(new Rational(23, 27), new Rational(92, 108).reduced());
    }

    @Test
    void testToMap() {
        Map<String,Long> expected = new LinkedHashMap<>(2);
        expected.put("numerator", 2L);
        expected.put("denominator", 3L);
        assertEquals(expected, instance.toMap());
    }

    @Test
    void testToString() {
        assertEquals("2:3", instance.toString());
    }

}
