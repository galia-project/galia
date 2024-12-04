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

class CropByPixelsTest extends BaseTest {

    static final double DELTA = 0.0000001;

    private CropByPixels instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new CropByPixels(0, 0, 1000, 1000);;
    }

    @Test
    void constructor() {
        instance = new CropByPixels(5, 10, 50, 80);
        assertEquals(5, instance.getX(), DELTA);
        assertEquals(10, instance.getY(), DELTA);
        assertEquals(50, instance.getWidth(), DELTA);
        assertEquals(80, instance.getHeight(), DELTA);
    }

    @Test
    void equalsWithEqualInstances() {
        Crop crop1 = new CropByPixels(50, 50, 50, 50);
        Crop crop2 = new CropByPixels(50, 50, 50, 50);
        assertEquals(crop1, crop2);
    }

    @Test
    void equalsWithUnequalX() {
        Crop crop1 = new CropByPixels(50, 50, 50, 50);
        Crop crop2 = new CropByPixels(51, 50, 50, 50);
        assertNotEquals(crop1, crop2);
    }

    @Test
    void equalsWithUnequalY() {
        Crop crop1 = new CropByPixels(50, 50, 50, 50);
        Crop crop2 = new CropByPixels(50, 51, 50, 50);
        assertNotEquals(crop1, crop2);
    }

    @Test
    void equalsWithUnequalWidth() {
        Crop crop1 = new CropByPixels(50, 50, 50, 50);
        Crop crop2 = new CropByPixels(50, 50, 51, 50);
        assertNotEquals(crop1, crop2);
    }

    @Test
    void equalsWithUnequalHeight() {
        Crop crop1 = new CropByPixels(50, 50, 50, 50);
        Crop crop2 = new CropByPixels(50, 50, 50, 51);
        assertNotEquals(crop1, crop2);
    }

    @Test
    void getRegion1() {
        final Size fullSize = new Size(300, 200);
        Crop crop                = new CropByPixels(20, 20, 50, 50);
        assertEquals(new Region(20, 20, 50, 50),
                crop.getRegion(fullSize));
    }

    @Test
    void getRegion1DoesNotExceedFullSizeBounds() {
        final Size fullSize = new Size(300, 200);
        Crop crop                = new CropByPixels(200, 150, 100, 100);
        assertEquals(new Region(200, 150, 100, 50),
                crop.getRegion(fullSize));
    }

    @Test
    void getRegion2() {
        final Size fullSize = new Size(300, 200);
        Crop crop = new CropByPixels(20, 20, 50, 50);

        // scale constraint 1:1
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        assertEquals(new Region(20, 20, 50, 50),
                crop.getRegion(fullSize, sc));

        // scale constraint 1:2
        sc = new ScaleConstraint(1, 2);
        assertEquals(new Region(40, 40, 100, 100),
                crop.getRegion(fullSize, sc));
    }

    @Test
    void getRegion2DoesNotExceedFullSizeBounds() {
        final Size fullSize = new Size(1000, 800);

        // scale constraint 1:1
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        Crop crop          = new CropByPixels(400, 400, 700, 500);
        assertEquals(new Region(400, 400, 600, 400),
                crop.getRegion(fullSize, sc));

        // scale constraint 1:2
        sc   = new ScaleConstraint(1, 2);
        crop = new CropByPixels(200, 200, 350, 250);
        assertEquals(new Region(400, 400, 600, 400),
                crop.getRegion(fullSize, sc));
    }

    @Test
    void getRegion3WithLargerReductionThanConstraint() {
        final Size reducedSize = new Size(500, 500);
        final ReductionFactor rf    = new ReductionFactor(2);    // full: 2000x2000
        final ScaleConstraint sc    = new ScaleConstraint(1, 2); // 1000x1000
        final Crop crop             = new CropByPixels(100, 100, 200, 200);
        assertEquals(new Region(50, 50, 100, 100),
                crop.getRegion(reducedSize, rf, sc));
    }

    @Test
    void getRegion3WithSmallerReductionThanConstraint() {
        final Size reducedSize = new Size(500, 500);
        final ReductionFactor rf    = new ReductionFactor(1);    // full: 1000x1000
        final ScaleConstraint sc    = new ScaleConstraint(1, 4); // 250x250
        final Crop crop             = new CropByPixels(100, 100, 200, 200);
        assertEquals(new Region(200, 200, 300, 300),
                crop.getRegion(reducedSize, rf, sc));
    }

    @Test
    void getRegion3DoesNotExceedFullSizeBounds() {
        final Size reducedSize = new Size(300, 200);
        final ReductionFactor rf    = new ReductionFactor(2);    // full: 1200x800
        final ScaleConstraint sc    = new ScaleConstraint(1, 4); // 300x200
        final Crop crop             = new CropByPixels(200, 150, 100, 100);
        assertEquals(new Region(200, 150, 100, 50),
                crop.getRegion(reducedSize, rf, sc));
    }

    @Test
    void getResultingSize() {
        final Size fullSize = new Size(200, 200);
        final Crop crop = new CropByPixels(20, 20, 50, 50);

        // scale constraint 1:1
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        assertEquals(new Size(50, 50),
                crop.getResultingSize(fullSize, sc));

        // scale constraint 1:2
        sc = new ScaleConstraint(1, 2);
        assertEquals(new Size(100, 100),
                crop.getResultingSize(fullSize, sc));
    }

    @Test
    void hasEffect() {
        // new instance
        CropByPixels crop = new CropByPixels(0, 0, 1000, 1000);
        assertTrue(crop.hasEffect());

        crop.setWidth(50);
        crop.setHeight(50);
        assertTrue(crop.hasEffect());
    }

    @Test
    void hasEffectWithArgumentsWithFullArea() {
        Size fullSize   = new Size(600, 400);
        OperationList opList = new OperationList();

        instance.setWidth(600);
        instance.setHeight(400);
        assertFalse(instance.hasEffect(fullSize, opList));
    }

    @Test
    void hasEffectWithArgumentsWithGreaterThanFullArea() {
        Size fullSize   = new Size(600, 400);
        OperationList opList = new OperationList();

        instance.setWidth(800);
        instance.setHeight(600);
        assertFalse(instance.hasEffect(fullSize, opList));
    }

    @Test
    void hasEffectWithArgumentsWithNonzeroOrigin() {
        Size fullSize   = new Size(600, 400);
        OperationList opList = new OperationList();

        instance.setX(5);
        instance.setY(5);
        instance.setWidth(595);
        instance.setHeight(395);
        assertTrue(instance.hasEffect(fullSize, opList));

        instance.setWidth(600);
        instance.setHeight(400);
        assertTrue(instance.hasEffect(fullSize, opList));
    }

    @Test
    void testHashCode() {
        assertEquals(instance.toString().hashCode(), instance.hashCode());
    }

    @Test
    void setHeight() {
        int height = 50;
        instance.setHeight(height);
        assertEquals(height, instance.getHeight());
    }

    @Test
    void setHeightWithZeroHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setHeight(0));
    }

    @Test
    void setHeightWithNegativeHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setHeight(-50));
    }

    @Test
    void setHeightThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setHeight(30));
    }

    @Test
    void setWidth() {
        int width = 50;
        instance.setWidth(width);
        assertEquals(width, instance.getWidth());
    }

    @Test
    void setWidthWithZeroWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setWidth(0));
    }

    @Test
    void setWidthWithNegativeWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setWidth(-13));
    }

    @Test
    void setWidthThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class, () -> instance.setWidth(0));
    }

    @Test
    void setX() {
        int x = 50;
        instance.setX(x);
        assertEquals(x, instance.getX());
    }

    @Test
    void setXWithNegativeX() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setX(-50));
    }

    @Test
    void setXThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class, () -> instance.setX(30));
    }

    @Test
    void setY() {
        int y = 50;
        instance.setY(y);
        assertEquals(y, instance.getY());
    }

    @Test
    void setYWithNegativeY() {
        assertThrows(IllegalArgumentException.class, () -> instance.setY(-10));
    }

    @Test
    void setYThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class, () -> instance.setY(30));
    }

    @Test
    void testToMap() {
        final Crop crop          = new CropByPixels(25, 25, 50, 50);
        final Size fullSize = new Size(100, 100);
        final ScaleConstraint sc = new ScaleConstraint(1, 1);

        Map<String,Object> map = crop.toMap(fullSize, sc);
        assertEquals(crop.getClass().getSimpleName(), map.get("class"));
        assertEquals(25L, map.get("x"));
        assertEquals(25L, map.get("y"));
        assertEquals(50L, map.get("width"));
        assertEquals(50L, map.get("height"));
    }

    @Test
    void toMapReturnsUnmodifiableMap() {
        Size fullSize = new Size(100, 100);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, sc);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    @Test
    void testToString() {
        instance.setX(10);
        instance.setY(10);
        instance.setWidth(50);
        instance.setHeight(40);
        assertEquals("10,10,50,40", instance.toString());
    }

    @Test
    void validateWithValidInstance() throws Exception {
        Size fullSize = new Size(1000, 1000);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        instance.setWidth(100);
        instance.setHeight(100);
        instance.validate(fullSize, sc);
    }

    @Test
    void validateWithOutOfBoundsX() {
        Size fullSize = new Size(1000, 1000);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        Crop crop          = new CropByPixels(1001, 0, 5, 5);
        assertThrows(OperationException.class,
                () -> crop.validate(fullSize, sc));
    }

    @Test
    void validateWithOutOfBoundsY() {
        Size fullSize = new Size(1000, 1000);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        Crop crop          = new CropByPixels(0, 1001, 5, 5);
        assertThrows(OperationException.class,
                () -> crop.validate(fullSize, sc));
    }

    @Test
    void validateWithZeroDimensionX() {
        Size fullSize = new Size(1000, 1000);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        Crop crop          = new CropByPixels(1000, 0, 100, 100);
        assertThrows(OperationException.class,
                () -> crop.validate(fullSize, sc));
    }

    @Test
    void validateWithZeroDimensions() {
        Size fullSize = new Size(1000, 1000);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        Crop crop          = new CropByPixels(0, 1000, 100, 100);
        assertThrows(OperationException.class,
                () -> crop.validate(fullSize, sc));
    }

}
