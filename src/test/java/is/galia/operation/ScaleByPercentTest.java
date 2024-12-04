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
import is.galia.image.MetaIdentifier;
import is.galia.image.ReductionFactor;
import is.galia.image.ScaleConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScaleByPercentTest extends ScaleTest {

    private ScaleByPercent instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @Override
    ScaleByPercent newInstance() {
        ScaleByPercent instance = new ScaleByPercent();
        instance.setFilter(Scale.Filter.BOX);
        return instance;
    }

    @Test
    void noOpConstructor() {
        assertEquals(1, instance.getPercent());
    }

    @Test
    void doubleConstructor() {
        instance = new ScaleByPercent(0.45);
        assertEquals(0.45, instance.getPercent());
    }

    @Test
    void equalsWithEqualInstances() {
        ScaleByPercent expected = new ScaleByPercent(1);
        expected.setFilter(Scale.Filter.BOX);
        assertEquals(expected, instance);
    }

    @Test
    void equalsWithUnequalPercents() {
        ScaleByPercent expected = new ScaleByPercent(0.45);
        expected.setFilter(Scale.Filter.BOX);
        assertNotEquals(expected, instance);
    }

    @Test
    void equalsWithUnequalFilters() {
        ScaleByPercent expected = new ScaleByPercent(1);
        expected.setFilter(Scale.Filter.BICUBIC);
        assertNotEquals(expected, instance);
    }

    @Test
    void getReductionFactor() {
        Size size          = new Size(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance = new ScaleByPercent(0.45);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);

        instance.setPercent(0.2);
        assertEquals(2, instance.getReductionFactor(size, sc, 999).factor);
        assertEquals(1, instance.getReductionFactor(size, sc, 1).factor);
    }

    @Test
    void getResultingScales() {
        final Size fullSize = new Size(300, 200);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance = new ScaleByPercent(0.5);

        assertArrayEquals(new double[] { 0.5 * 1 / 3.0, 0.5 * 1 / 3.0 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void getResultingSize1WithDownscaling() {
        final Size fullSize = new Size(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance = new ScaleByPercent(0.5);

        assertEquals(new Size(300, 200),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize1WithUpscaling() {
        final Size fullSize = new Size(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance = new ScaleByPercent(1.5);

        assertEquals(new Size(900, 600),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize2WithPercent() {
        final Size fullSize      = new Size(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance = new ScaleByPercent();
        // down
        instance.setPercent(0.5);
        assertEquals(new Size(300, 200),
                instance.getResultingSize(fullSize, rf, sc));
        // up
        instance.setPercent(1.5);
        assertEquals(new Size(900, 600),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void hasEffect() {
        assertFalse(instance.hasEffect());
        instance = new ScaleByPercent(0.5);
        assertTrue(instance.hasEffect());
    }

    @Test
    void hasEffectWithArguments() {
        final Size fullSize        = new Size(600, 400);
        final OperationList opList = OperationList.builder()
                .withOperations(new CropByPixels(0, 0, 300, 200))
                .build();
        assertFalse(instance.hasEffect(fullSize, opList));

        instance.setPercent(0.5);
        assertTrue(instance.hasEffect(fullSize, opList));
    }

    @Test
    void hasEffectWithScaleConstraint() {
        final Size fullSize = new Size(600, 400);
        final OperationList opList = new OperationList();
        opList.setMetaIdentifier(MetaIdentifier.builder()
                .withIdentifier("cats")
                .withScaleConstraint(1, 2)
                .build());

        assertTrue(instance.hasEffect(fullSize, opList));
    }

    @Test
    void hashCodeWithEqualInstances() {
        ScaleByPercent expected = new ScaleByPercent(1);
        expected.setFilter(Scale.Filter.BOX);
        assertEquals(expected.hashCode(), instance.hashCode());
    }

    @Test
    void hashCodeWithUnequalPercents() {
        ScaleByPercent expected = new ScaleByPercent(0.45);
        expected.setFilter(Scale.Filter.BOX);
        assertNotEquals(expected.hashCode(), instance.hashCode());
    }

    @Test
    void hashCodeWithUnequalFilters() {
        ScaleByPercent expected = new ScaleByPercent(1);
        expected.setFilter(Scale.Filter.BICUBIC);
        assertNotEquals(expected.hashCode(), instance.hashCode());
    }

    @Test
    void isHeightUp() {
        Size size = new Size(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance.setPercent(0.5); // down
        assertFalse(instance.isHeightUp(size, scaleConstraint));
        instance.setPercent(1.0); // even
        assertFalse(instance.isHeightUp(size, scaleConstraint));
        instance.setPercent(1.2); // up
        assertTrue(instance.isHeightUp(size, scaleConstraint));
    }

    @Test
    void isUp() {
        Size size = new Size(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance.setPercent(0.5); // down
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setPercent(1.0); // even
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setPercent(1.2); // up
        assertTrue(instance.isUp(size, scaleConstraint));
    }

    @Test
    void isWidthUp() {
        Size size = new Size(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance.setPercent(0.5); // down
        assertFalse(instance.isWidthUp(size, scaleConstraint));
        instance.setPercent(1.0); // even
        assertFalse(instance.isWidthUp(size, scaleConstraint));
        instance.setPercent(1.2); // up
        assertTrue(instance.isWidthUp(size, scaleConstraint));
    }

    @Test
    void setPercent() {
        double percent = 0.5;
        instance.setPercent(percent);
        assertEquals(percent, instance.getPercent());
    }

    @Test
    void setPercentWithNegativePercent() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setPercent(-0.5));
    }

    @Test
    void setPercentWithZeroPercent() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setPercent(0.0));
    }

    @Test
    void setPercentWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setPercent(0.5));
    }

    @Test
    void toMap() {
        Size fullSize = new Size(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Size resultingSize =
                instance.getResultingSize(fullSize, scaleConstraint);

        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(resultingSize.longWidth(), map.get("width"));
        assertEquals(resultingSize.longHeight(), map.get("height"));
    }

    @Test
    void testToString() {
        assertEquals("100%,box", instance.toString());
    }

}
