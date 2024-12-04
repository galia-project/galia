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

package is.galia.resource.iiif.v1;

import is.galia.operation.Crop;
import is.galia.operation.CropByPercent;
import is.galia.operation.CropByPixels;
import is.galia.resource.IllegalClientArgumentException;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegionTest extends BaseTest {

    private static final float DELTA = 0.0000001f;

    private Region instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new Region();
        instance.setPercent(true);
        instance.setX(20f);
        instance.setY(20f);
        instance.setWidth(20f);
        instance.setHeight(20f);
    }

    /* fromURI(String) */

    @Test
    void fromURIWithFull() {
        Region r = Region.fromURI("full");
        assertTrue(r.isFull());
        assertFalse(r.isPercent());
    }

    @Test
    void fromURIWithPixels() {
        Region r = Region.fromURI("0,0,50,40");
        assertEquals(0f, r.getX(), DELTA);
        assertEquals(0f, r.getY(), DELTA);
        assertEquals(50f, r.getWidth(), DELTA);
        assertEquals(40f, r.getHeight(), DELTA);
        assertFalse(r.isPercent());
        assertFalse(r.isFull());
    }

    @Test
    void fromURIWithPercent() {
        Region r = Region.fromURI("pct:0,0,50,40");
        assertEquals(0f, r.getX(), DELTA);
        assertEquals(0f, r.getY(), DELTA);
        assertEquals(50f, r.getWidth(), DELTA);
        assertEquals(40f, r.getHeight(), DELTA);
        assertTrue(r.isPercent());
        assertFalse(r.isFull());
    }

    @Test
    void fromURIWithIllegalX() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Region.fromURI("pct:-2,3,50,50"));
    }

    @Test
    void fromURIWithIllegalY() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Region.fromURI("pct:2,-3,50,50"));
    }

    @Test
    void fromURIWithIllegalWidth() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Region.fromURI("2,3,-50,50"));
    }

    @Test
    void fromURIWithIllegalHeight() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Region.fromURI("2,3,50,-50"));
    }

    /* equals() */

    @Test
    void equalsWithFullRegions() {
        Region region1 = new Region();
        region1.setFull(true);
        Region region2 = new Region();
        region2.setFull(true);
        assertEquals(region1, region2);
    }

    @Test
    void equalsWithEqualPixelRegions() {
        Region region1 = new Region();
        region1.setPercent(false);
        region1.setX(20f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(false);
        region2.setX(20f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);

        assertEquals(region1, region2);
    }

    @Test
    void equalsWithUnequalPixelRegionX() {
        Region region1 = new Region();
        region1.setPercent(false);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(false);
        region2.setX(51f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    void equalsWithUnequalPixelRegionY() {
        Region region1 = new Region();
        region1.setPercent(false);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(false);
        region2.setX(50f);
        region2.setY(21f);
        region2.setWidth(20f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    void equalsWithUnequalPixelRegionWidth() {
        Region region1 = new Region();
        region1.setPercent(false);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(false);
        region2.setX(50f);
        region2.setY(20f);
        region2.setWidth(21f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    void equalsWithUnequalPixelRegionHeight() {
        Region region1 = new Region();
        region1.setPercent(false);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(false);
        region2.setX(50f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(21f);

        assertNotEquals(region1, region2);
    }

    @Test
    void equalsWithEqualPercentRegions() {
        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(20f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);
        assertEquals(region2, instance);
    }

    @Test
    void equalsWithUnequalPercentRegionX() {
        Region region1 = new Region();
        region1.setPercent(true);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(51f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    void equalsWithUnequalPercentRegionY() {
        Region region1 = new Region();
        region1.setPercent(true);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(50f);
        region2.setY(21f);
        region2.setWidth(20f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    void equalsWithUnequalPercentRegionWidth() {
        Region region1 = new Region();
        region1.setPercent(true);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(50f);
        region2.setY(20f);
        region2.setWidth(21f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    void equalsWithUnequalPercentRegionHeight() {
        Region region1 = new Region();
        region1.setPercent(true);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(50f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(21f);

        assertNotEquals(region1, region2);
    }

    /* height */

    @Test
    void setHeight() {
        float height = 50f;
        instance.setHeight(height);
        assertEquals(height, instance.getHeight(), DELTA);
    }

    @Test
    void setZeroHeight() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setHeight(0));
    }

    @Test
    void setNegativeHeight() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setHeight(-1));
    }

    /* width */

    @Test
    void setWidth() {
        float width = 50f;
        instance.setWidth(width);
        assertEquals(width, instance.getWidth(), DELTA);
    }

    @Test
    void setZeroWidth() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setWidth(0));
    }

    @Test
    void setNegativeWidth() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setWidth(-1));
    }

    /* x */

    @Test
    void setX() {
        float x = 50f;
        instance.setX(x);
        assertEquals(x, instance.getX(), DELTA);
    }

    @Test
    void setNegativeX() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setX(-1));
    }

    /* y */

    @Test
    void setY() {
        float y = 50.0f;
        instance.setY(y);
        assertEquals(y, instance.getY(), DELTA);
    }

    @Test
    void setNegativeY() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setY(-1));
    }

    /* toCrop() */

    @Test
    void toCropWithFull() {
        instance = new Region();
        instance.setFull(true);
        Crop actual = instance.toCrop();
        Crop expected = new CropByPercent(0, 0, 1, 1);
        assertEquals(expected, actual);
    }

    @Test
    void toCropWithPixels() {
        Crop expected = new CropByPixels(10, 20, 200, 100);

        instance = new Region();
        instance.setX(10f);
        instance.setY(20f);
        instance.setWidth(200f);
        instance.setHeight(100f);
        Crop actual = instance.toCrop();

        assertEquals(expected, actual);
    }

    @Test
    void toCropWithPercent() {
        Crop expected = new CropByPercent(0.3, 0.4, 0.5, 0.6);

        instance = new Region();
        instance.setPercent(true);
        instance.setX(30f);
        instance.setY(40f);
        instance.setWidth(50f);
        instance.setHeight(60f);
        Crop actual = instance.toCrop();

        assertEquals(expected, actual);
    }

    /* toString() */

    @Test
    void toStringWithFull() {
        Region r = Region.fromURI("full");
        assertEquals("full", r.toString());
    }

    @Test
    void toStringWithPixels() {
        Region r = Region.fromURI("0,0,50,40");
        assertEquals("0,0,50,40", r.toString());
    }

    @Test
    void toStringWithPercent() {
        Region r = Region.fromURI("pct:0,0,50,40");
        assertEquals("pct:0,0,50,40", r.toString());
    }

}
