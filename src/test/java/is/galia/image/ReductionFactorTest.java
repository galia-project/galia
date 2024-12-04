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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReductionFactorTest extends BaseTest {

    private static final double DELTA = 0.0000001;

    @Test
    void forScale1() {
        assertEquals(new ReductionFactor(0), ReductionFactor.forScale(2));
        assertEquals(new ReductionFactor(0), ReductionFactor.forScale(1.5));
        assertEquals(new ReductionFactor(0), ReductionFactor.forScale(1));
        assertEquals(new ReductionFactor(0), ReductionFactor.forScale(0.75));
        assertEquals(new ReductionFactor(1), ReductionFactor.forScale(0.5));
        assertEquals(new ReductionFactor(1), ReductionFactor.forScale(0.45));
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.25));
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.2));
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.1251));
        assertEquals(new ReductionFactor(3), ReductionFactor.forScale(0.125001));
        assertEquals(new ReductionFactor(3), ReductionFactor.forScale(0.125));
        assertEquals(new ReductionFactor(3), ReductionFactor.forScale(0.1249999));
        assertEquals(new ReductionFactor(4), ReductionFactor.forScale(0.0625));
        assertEquals(new ReductionFactor(5), ReductionFactor.forScale(0.03125));
    }

    @Test
    void forScale2() {
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.25001, 0.001));
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.25, 0.001));
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.24999, 0.001));

        assertEquals(new ReductionFactor(1), ReductionFactor.forScale(0.2501, 0.00001));
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.25, 0.00001));
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.2499, 0.00001));
    }



    /* ReductionFactor(int) */

    @Test
    void constructor2() {
        ReductionFactor rf = new ReductionFactor(3);
        assertEquals(3, rf.factor);
    }

    @Test
    void constructor2WithNegativeArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReductionFactor(-3));
    }

    /* equals() */

    @Test
    void equalsWithSameInstance() {
        ReductionFactor rf = new ReductionFactor();
        assertEquals(rf, rf);
    }

    @Test
    void equalsWithEqualInstances() {
        ReductionFactor rf1 = new ReductionFactor(2);
        ReductionFactor rf2 = new ReductionFactor(2);
        assertEquals(rf1, rf2);
    }

    @Test
    void equalsWithUnequalInstances() {
        ReductionFactor rf1 = new ReductionFactor(2);
        ReductionFactor rf2 = new ReductionFactor(3);
        assertNotEquals(rf1, rf2);
    }

    /* findDifferentialScale() */

    @Test
    void findDifferentialScale() {
        assertEquals(0.2, new ReductionFactor(0).findDifferentialScale(0.2));
        assertEquals(0.4, new ReductionFactor(1).findDifferentialScale(0.2));
        assertEquals(0.8, new ReductionFactor(2).findDifferentialScale(0.2));

        assertEquals(1, new ReductionFactor(0).findDifferentialScale(1));
        assertEquals(1, new ReductionFactor(1).findDifferentialScale(0.5));
        assertEquals(1, new ReductionFactor(2).findDifferentialScale(0.25));
    }

    /* getScale() */

    @Test
    void getScale() {
        assertTrue(Math.abs(new ReductionFactor(0).getScale() - 1.0) < DELTA);
        assertTrue(Math.abs(new ReductionFactor(1).getScale() - 0.5) < DELTA);
        assertTrue(Math.abs(new ReductionFactor(2).getScale() - 0.25) < DELTA);
        assertTrue(Math.abs(new ReductionFactor(3).getScale() - 0.125) < DELTA);
        assertTrue(Math.abs(new ReductionFactor(4).getScale() - 0.0625) < DELTA);
        assertTrue(Math.abs(new ReductionFactor(5).getScale() - 0.03125) < DELTA);
    }

    /* toString() */

    @Test
    void testToString() {
        assertEquals("1", new ReductionFactor(1).toString());
    }

}
