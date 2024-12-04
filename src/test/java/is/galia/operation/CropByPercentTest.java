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

class CropByPercentTest extends BaseTest {

    static final double DELTA = 0.0000001;

    private CropByPercent instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new CropByPercent();
    }

    @Test
    void constructor1() {
        instance = new CropByPercent();
        assertEquals(0, instance.getX(), DELTA);
        assertEquals(0, instance.getY(), DELTA);
        assertEquals(1, instance.getWidth(), DELTA);
        assertEquals(1, instance.getHeight(), DELTA);
    }

    @Test
    void constructor2() {
        instance = new CropByPercent(0.02, 0.05, 0.5, 0.8);
        assertEquals(0.02, instance.getX(), DELTA);
        assertEquals(0.05, instance.getY(), DELTA);
        assertEquals(0.5, instance.getWidth(), DELTA);
        assertEquals(0.8, instance.getHeight(), DELTA);
    }

    @Test
    void equalsWithEqualInstances() {
        Crop crop1 = new CropByPercent(0.2, 0.2, 0.5, 0.5);
        Crop crop2 = new CropByPercent(0.2, 0.2, 0.5, 0.5);
        assertEquals(crop1, crop2);
    }

    @Test
    void equalsWithUnequalX() {
        Crop crop1 = new CropByPercent(0.2, 0.2, 0.5, 0.5);
        Crop crop2 = new CropByPercent(0.3, 0.2, 0.5, 0.5);
        assertNotEquals(crop1, crop2);
    }

    @Test
    void equalsWithUnequalY() {
        Crop crop1 = new CropByPercent(0.2, 0.2, 0.5, 0.5);
        Crop crop2 = new CropByPercent(0.2, 0.3, 0.5, 0.5);
        assertNotEquals(crop1, crop2);
    }

    @Test
    void equalsWithUnequalWidth() {
        Crop crop1 = new CropByPercent(0.2, 0.2, 0.5, 0.5);
        Crop crop2 = new CropByPercent(0.2, 0.2, 0.7, 0.5);
        assertNotEquals(crop1, crop2);
    }

    @Test
    void equalsWithUnequalHeight() {
        Crop crop1 = new CropByPercent(0.2, 0.2, 0.5, 0.5);
        Crop crop2 = new CropByPercent(0.2, 0.2, 0.5, 0.7);
        assertNotEquals(crop1, crop2);
    }

    @Test
    void getRegion1() {
        final Size fullSize = new Size(1000, 500);
        Crop crop                = new CropByPercent(0.2, 0.2, 0.5, 0.5);
        assertEquals(new Region(200, 100, 500, 250),
                crop.getRegion(fullSize));
    }

    @Test
    void getRegion1DoesNotExceedFullSizeBounds() {
        final Size fullSize = new Size(1000, 500);
        Crop crop                = new CropByPercent(0.8, 0.8, 0.5, 0.5);
        assertEquals(new Region(800, 400, 200, 100),
                crop.getRegion(fullSize));
    }

    @Test
    void getRegion2() {
        final Size fullSize = new Size(1000, 500);
        final Crop crop          = new CropByPercent(0.2, 0.2, 0.5, 0.5);

        // scale constraint 1:1
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        assertEquals(new Region(200, 100, 500, 250),
                crop.getRegion(fullSize, sc));

        // scale constraint 1:2
        sc = new ScaleConstraint(1, 2);
        assertEquals(new Region(200, 100, 500, 250),
                crop.getRegion(fullSize, sc));
    }

    @Test
    void getRegion2DoesNotExceedFullSizeBounds() {
        final Size fullSize = new Size(1000, 500);
        final Crop crop = new CropByPercent(0.8, 0.8, 0.5, 0.5);

        // scale constraint 1:1
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        assertEquals(new Region(800, 400, 200, 100),
                crop.getRegion(fullSize, sc));

        // scale constraint 1:2
        sc = new ScaleConstraint(1, 2);
        assertEquals(new Region(800, 400, 200, 100),
                crop.getRegion(fullSize, sc));
    }

    @Test
    void getRegion3WithFullSize() {
        final Size reducedSize = new Size(300, 200);
        final ReductionFactor rf    = new ReductionFactor(2);
        final ScaleConstraint sc    = new ScaleConstraint(1, 2);
        final Crop crop             = new CropByPercent(0, 0, 1, 1);

        assertEquals(new Region(0, 0, 300, 200, true),
                crop.getRegion(reducedSize, rf, sc));
    }

    @Test
    void getRegion3WithLargerReductionThanConstraint() {
        final Size reducedSize = new Size(300, 200);
        final ReductionFactor rf    = new ReductionFactor(2);
        final ScaleConstraint sc    = new ScaleConstraint(1, 2);
        final Crop crop             = new CropByPercent(0.2, 0.2, 0.5, 0.5);

        assertEquals(new Region(60, 40, 150, 100),
                crop.getRegion(reducedSize, rf, sc));
    }

    @Test
    void getRegion3WithSmallerReductionThanConstraint() {
        final Size reducedSize = new Size(300, 200);
        final ReductionFactor rf    = new ReductionFactor(1);
        final ScaleConstraint sc    = new ScaleConstraint(1, 4);
        final Crop crop             = new CropByPercent(0.2, 0.2, 0.5, 0.5);

        assertEquals(new Region(60, 40, 150, 100),
                crop.getRegion(reducedSize, rf, sc));
    }

    @Test
    void getRegion3DoesNotExceedFullSizeBounds() {
        final ReductionFactor rf    = new ReductionFactor(2);
        final Size reducedSize = new Size(1000, 500);
        final ScaleConstraint sc    = new ScaleConstraint(1, 4);
        final Crop crop             = new CropByPercent(0.6, 0.6, 0.8, 0.8);
        assertEquals(new Region(600, 300, 400, 200),
                crop.getRegion(reducedSize, rf, sc));
    }

    @Test
    void getResultingSize() {
        final Crop crop          = new CropByPercent(0, 0, 0.5, 0.5);
        final Size fullSize = new Size(1000, 1000);

        // scale constraint 1:1
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        assertEquals(new Size(500, 500),
                crop.getResultingSize(fullSize, sc));

        // scale constraint 1:2
        sc = new ScaleConstraint(1, 2);
        assertEquals(new Size(500, 500),
                crop.getResultingSize(fullSize, sc));
    }

    @Test
    void hasEffect() {
        // new instance
        CropByPercent crop = new CropByPercent();
        assertFalse(crop.hasEffect());

        // 0% origin, 100% dimensions
        crop = new CropByPercent();
        crop.setWidth(1);
        crop.setHeight(1);
        assertFalse(crop.hasEffect());

        // 0% origin, <100% dimensions
        crop = new CropByPercent();
        crop.setWidth(0.8);
        crop.setHeight(0.8);
        assertTrue(crop.hasEffect());

        // >0% origin, 100% dimensions
        crop = new CropByPercent();
        crop.setX(0.1);
        crop.setY(0.1);
        assertTrue(crop.hasEffect());

        // >0% origin, <100% dimensions
        crop = new CropByPercent();
        crop.setX(0.1);
        crop.setY(0.1);
        crop.setWidth(0.9);
        crop.setHeight(0.9);
        assertTrue(crop.hasEffect());
    }

    @Test
    void hasEffectWithArguments() {
        Size fullSize = new Size(600, 400);
        OperationList opList = new OperationList();

        // new instance
        CropByPercent crop = new CropByPercent();
        assertFalse(crop.hasEffect(fullSize, opList));

        // 0% origin, 100% dimensions
        crop = new CropByPercent();
        crop.setWidth(1);
        crop.setHeight(1);
        assertFalse(crop.hasEffect(fullSize, opList));

        // 0% origin, <100% dimensions
        crop = new CropByPercent();
        crop.setWidth(0.8);
        crop.setHeight(0.8);
        assertTrue(crop.hasEffect(fullSize, opList));

        // >0% origin, 100% dimensions
        crop = new CropByPercent();
        crop.setX(0.1);
        crop.setY(0.1);
        assertTrue(crop.hasEffect(fullSize, opList));

        // >0% origin, <100% dimensions
        crop = new CropByPercent();
        crop.setX(0.1);
        crop.setY(0.1);
        crop.setWidth(0.9);
        crop.setHeight(0.9);
        assertTrue(crop.hasEffect(fullSize, opList));
    }

    @Test
    void testHashCode() {
        assertEquals(instance.toString().hashCode(), instance.hashCode());
    }

    @Test
    void setHeight() {
        double height = 0.5;
        instance.setHeight(height);
        assertEquals(height, instance.getHeight(), DELTA);
    }

    @Test
    void setHeightWithNegativeHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setHeight(-0.5));
    }

    @Test
    void setHeightWithZeroHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setHeight(0));
    }

    @Test
    void setHeightWithGreaterThan100PercentHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setHeight(1.2));
    }

    @Test
    void setHeightThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setHeight(0.3));
    }

    @Test
    void setWidth() {
        double width = 0.5;
        instance.setWidth(width);
        assertEquals(width, this.instance.getWidth(), DELTA);
    }

    @Test
    void setWidthWithNegativeWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setWidth(-0.5));
    }

    @Test
    void setWidthWithZeroWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setWidth(0));
    }

    @Test
    void setWidthWithGreaterThan100PercentWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setWidth(1.2));
    }

    @Test
    void setWidthThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setWidth(0.5));
    }

    @Test
    void setX() {
        double x = 0.5;
        instance.setX(x);
        assertEquals(x, instance.getX(), DELTA);
    }

    @Test
    void setXWithNegativeX() {
        assertThrows(IllegalArgumentException.class, () -> instance.setX(-0.5));
    }

    @Test
    void setXWithGreaterThan100PercentX() {
        assertThrows(IllegalArgumentException.class, () -> instance.setX(1.2));
    }

    @Test
    void setXThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class, () -> instance.setX(0.5));
    }

    @Test
    void setY() {
        double y = 0.5;
        instance.setY(y);
        assertEquals(y, instance.getY(), DELTA);
    }

    @Test
    void setYWithNegativeY() {
        assertThrows(IllegalArgumentException.class, () -> instance.setY(-0.5));
    }

    @Test
    void setYWithGreaterThan100PercentY() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setY(1.2));
    }

    @Test
    void setYThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class, () -> instance.setY(0.5));
    }

    @Test
    void testToMap() {
        final Crop crop          = new CropByPercent(0.1, 0.2, 0.4, 0.5);
        final Size fullSize = new Size(100, 100);
        final ScaleConstraint sc = new ScaleConstraint(1, 1);

        Map<String,Object> map = crop.toMap(fullSize, sc);
        assertEquals(crop.getClass().getSimpleName(), map.get("class"));
        assertEquals(10L, map.get("x"));
        assertEquals(20L, map.get("y"));
        assertEquals(40L, map.get("width"));
        assertEquals(50L, map.get("height"));
    }

    @Test
    void toMapReturnsUnmodifiableMap() {
        Size fullSize     = new Size(100, 100);
        ScaleConstraint sc     = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, sc);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    @Test
    void testToString() {
        instance.setX(0.1);
        instance.setY(0.2);
        instance.setWidth(0.5);
        instance.setHeight(0.4);
        assertEquals("0.1,0.2,0.5,0.4", instance.toString());
    }

    @Test
    void validateWithValidInstance() throws Exception {
        Size fullSize = new Size(1000, 1000);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        instance.setWidth(0.1);
        instance.setHeight(0.1);
        instance.validate(fullSize, sc);
    }

}
