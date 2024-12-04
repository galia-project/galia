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

package is.galia.operation;

import is.galia.image.Size;
import is.galia.image.Region;
import is.galia.image.ReductionFactor;
import is.galia.image.ScaleConstraint;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CropToSquareTest extends BaseTest {

    private CropToSquare instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new CropToSquare();
    }

    @Test
    void equalsWithEqualInstances() {
        CropToSquare crop1 = new CropToSquare();
        CropToSquare crop2 = new CropToSquare();
        assertEquals(crop1, crop2);
    }

    @Test
    void equalsWithUnequalInstances() {
        // All instances are equal.
    }

    @Test
    void getRegion1() {
        final Size fullSize = new Size(300, 200);
        final CropToSquare crop = new CropToSquare();
        assertEquals(new Region(50, 0, 200, 200),
                crop.getRegion(fullSize));
    }

    @Test
    void getRegion2() {
        final Size fullSize = new Size(300, 200);
        final CropToSquare crop = new CropToSquare();

        // scale constraint 1:1
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        assertEquals(new Region(50, 0, 200, 200),
                crop.getRegion(fullSize, scaleConstraint));

        // scale constraint 1:2
        scaleConstraint = new ScaleConstraint(1, 2);
        assertEquals(new Region(50, 0, 200, 200),
                crop.getRegion(fullSize, scaleConstraint));
    }

    @Test
    void getRegion3() {
        final ReductionFactor rf = new ReductionFactor(2);
        final Size reducedSize = new Size(300, 200);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 2);
        CropToSquare crop = new CropToSquare();
        assertEquals(new Region(50, 0, 200, 200),
                crop.getRegion(reducedSize, rf, scaleConstraint));
    }

    @Test
    void getResultingSize() {
        final Size fullSize = new Size(300, 200);
        final CropToSquare crop = new CropToSquare();

        // scale constraint 1:1
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        assertEquals(new Size(200, 200),
                crop.getResultingSize(fullSize, scaleConstraint));

        // scale constraint 1:2
        scaleConstraint = new ScaleConstraint(1, 2);
        assertEquals(new Size(200, 200),
                crop.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void hasEffect() {
        CropToSquare crop = new CropToSquare();
        assertTrue(crop.hasEffect());
    }

    @Test
    void hasEffectWithArguments() {
        // very different width & height
        Size fullSize = new Size(600, 400);
        OperationList opList = new OperationList();
        assertTrue(instance.hasEffect(fullSize, opList));

        // little bit different width & height
        fullSize = new Size(600.4, 600.3);
        opList = new OperationList();
        assertFalse(instance.hasEffect(fullSize, opList));

        // exact same width & height
        fullSize = new Size(600.00001, 600.00001);
        opList = new OperationList();
        assertFalse(instance.hasEffect(fullSize, opList));
    }

    @Test
    void testHashCode() {
        assertEquals(instance.toString().hashCode(), instance.hashCode());
    }

    @Test
    void toMap() {
        final CropToSquare crop = new CropToSquare();
        final Size fullSize = new Size(150, 100);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        Map<String,Object> map = crop.toMap(fullSize, scaleConstraint);
        assertEquals(crop.getClass().getSimpleName(), map.get("class"));
        assertEquals(25L, map.get("x"));
        assertEquals(0L, map.get("y"));
        assertEquals(100L, map.get("width"));
        assertEquals(100L, map.get("height"));
    }

    @Test
    void toMapReturnsUnmodifiableMap() {
        Size fullSize = new Size(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    @Test
    void testToString() {
        assertEquals("square", instance.toString());
    }

    @Test
    void validate() throws Exception {
        // All instances are valid.
        Size fullSize = new Size(1000, 1000);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.validate(fullSize, scaleConstraint);
    }

}
