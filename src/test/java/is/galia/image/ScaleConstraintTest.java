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

package is.galia.image;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScaleConstraintTest extends BaseTest {

    private static final double DELTA = 0.00000001;

    private ScaleConstraint instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new ScaleConstraint(2, 3);
    }

    /* ScaleConstraint() */

    @Test
    void constructor1() {
        assertEquals(new ScaleConstraint(1, 1), new ScaleConstraint());
    }

    /* ScaleConstraint(long, long) */

    @Test
    void constructor2WithLargerNumeratorThanDenominator() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScaleConstraint(3, 2));
    }

    @Test
    void constructor2WithNegativeNumerator() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScaleConstraint(-1, 2));
    }

    @Test
    void constructor2WithNegativeDenominator() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScaleConstraint(1, -2));
    }

    @Test
    void constructor2WithZeroNumerator() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScaleConstraint(0, 2));
    }

    @Test
    void constructor2WithZeroDenominator() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScaleConstraint(1, 0));
    }

    /* getConstrainedSize() */

    @Test
    void getConstrainedSize() {
        Size fullSize = new Size(900, 600);
        Size actual = instance.getConstrainedSize(fullSize);
        assertEquals(600, actual.width(), DELTA);
        assertEquals(400, actual.height(), DELTA);
    }

    /* getResultingSize() */

    @Test
    void getResultingSize() {
        Size fullSize = new Size(1500, 1200);
        Size actual = instance.getResultingSize(fullSize);
        assertEquals(new Size(1000, 800), actual);
    }

    /* hasEffect() */

    @Test
    void hasEffect() {
        assertTrue(instance.hasEffect());
        instance = new ScaleConstraint(2, 2);
        assertFalse(instance.hasEffect());
    }

    /* reduced() */

    @Test
    void reduced() {
        assertEquals(instance, instance.reduced());
        assertEquals(new ScaleConstraint(23, 27),
                new ScaleConstraint(92, 108).reduced());
    }

    /* toMap() */

    @Test
    void toMap() {
        Map<String,Long> actual = instance.toMap();
        assertEquals(2, actual.size());
        assertEquals(2, (long) actual.get("numerator"));
        assertEquals(3, (long) actual.get("denominator"));
    }

    /* toString() */

    @Test
    void testToString() {
        assertEquals("2:3", instance.toString());
    }

}