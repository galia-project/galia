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

import static org.junit.jupiter.api.Assertions.*;

class RegionTest extends BaseTest {

    private static final double DELTA = 0.00000001;

    private Region instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Region(10, 5, 1000, 800);
    }

    /* Region(double, double, double, double) */

    @Test
    void doubleConstructor() {
        instance = new Region(10.2, 5.2, 1000.2, 800.2);
        assertEquals(10.2, instance.x(), DELTA);
        assertEquals(5.2, instance.y(), DELTA);
        assertEquals(1000.2, instance.width(), DELTA);
        assertEquals(800.2, instance.height(), DELTA);
    }

    /* Region(long, long, long, long) */

    @Test
    void longConstructor() {
        assertEquals(10, instance.x(), DELTA);
        assertEquals(5, instance.y(), DELTA);
        assertEquals(1000, instance.width(), DELTA);
        assertEquals(800, instance.height(), DELTA);
    }

    /* Region(Region) */

    @Test
    void copyConstructor() {
        Region other = new Region(instance);
        assertEquals(other, instance);
    }

    /* clippedTo() */

    @Test
    void clippedToWithEqualWidthAndHeight() {
        Region newRegion = instance.clippedTo(new Size(instance.width(), instance.height()));
        assertNotSame(instance, newRegion);
        assertEquals(1000, newRegion.width(), DELTA);
        assertEquals(800, newRegion.height(), DELTA);
    }

    @Test
    void clippedToWithLargerWidthAndHeight() {
        Region newRegion = instance.clippedTo(new Size(2000, 1000));
        assertEquals(1000, newRegion.width(), DELTA);
        assertEquals(800, newRegion.height(), DELTA);
    }

    @Test
    void clippedToWithLargerWidthAndSmallerHeight() {
        Region newRegion = instance.clippedTo(new Size(2000, 500));
        assertEquals(1000, newRegion.width(), DELTA);
        assertEquals(500, newRegion.height(), DELTA);
    }

    @Test
    void clippedToWithSmallerWidthAndLargerHeight() {
        Region newRegion = instance.clippedTo(new Size(300, 2000));
        assertEquals(300, newRegion.width(), DELTA);
        assertEquals(800, newRegion.height(), DELTA);
    }

    @Test
    void clippedToWithSmallerWidthHeight() {
        Region newRegion = instance.clippedTo(new Size(300, 200));
        assertEquals(300, newRegion.width(), DELTA);
        assertEquals(200, newRegion.height(), DELTA);
    }

    /* contains() */

    @Test
    void containsAgainstSmallerInstance() {
        assertTrue(instance.contains(new Region(11, 6, 998, 798)));
    }

    @Test

    void containsAgainstEqualInstance() {
        assertTrue(instance.contains(new Region(10, 5, 1000, 800)));
    }

    @Test
    void containsAgainstOutOfBoundsOrigins() {
        // X
        assertFalse(instance.contains(new Region(9, 5, 1000, 800)));
        // Y
        assertFalse(instance.contains(new Region(10, 4, 1000, 800)));
    }

    @Test
    void containsAgainstOutOfBoundsDimensions() {
        // X
        assertFalse(instance.contains(new Region(10, 5, 1001, 800)));
        assertFalse(instance.contains(new Region(500, 5, 511, 800)));
        // Y
        assertFalse(instance.contains(new Region(10, 5, 1000, 801)));
        assertFalse(instance.contains(new Region(10, 400, 1000, 406)));
    }

    /* equals() */

    @Test
    void equalsWithEqualInstances() {
        assertEquals(instance, new Region(10, 5, 1000, 800));
    }

    @Test
    void equalsWithUnequalInstances() {
        assertNotEquals(instance, new Region(11, 5, 1000, 800));
        assertNotEquals(instance, new Region(10, 6, 1000, 800));
        assertNotEquals(instance, new Region(10, 5, 1001, 800));
        assertNotEquals(instance, new Region(10, 5, 1000, 801));
    }

    /* intX() */

    @Test
    void intX() {
        instance = new Region(5.4, 0, 10, 10);
        assertEquals(5, instance.intX());
        instance = new Region(5.6, 0, 10, 10);
        assertEquals(6, instance.intX());
    }

    /* intY() */

    @Test
    void intY() {
        instance = new Region(0, 5.4, 10, 10);
        assertEquals(5, instance.intY());
        instance = new Region(0, 5.6, 10, 10);
        assertEquals(6, instance.intY());
    }

    /* intWidth() */

    @Test
    void intWidth() {
        instance = new Region(0, 0, 5.4, 10);
        assertEquals(5, instance.intWidth());
        instance = new Region(0, 0, 5.6, 10);
        assertEquals(6, instance.intWidth());
    }

    /* intHeight() */

    @Test
    void intHeight() {
        instance = new Region(0, 0, 10, 5.4);
        assertEquals(5, instance.intHeight());
        instance = new Region(0, 0, 10, 5.6);
        assertEquals(6, instance.intHeight());
    }

    /* intersects() */

    @Test
    void intersectsWithIntersectingInstance() {
        Region other = new Region(instance);
        assertTrue(other.intersects(instance));

        other = new Region(0, 0, instance.size());
        assertTrue(other.intersects(instance));

        other = new Region(500, 500, instance.size());
        assertTrue(other.intersects(instance));
    }

    @Test
    void intersectsWithNonIntersectingInstance() {
        // too far N
        Region other = new Region(10, 0, 1000, 5);
        assertFalse(other.intersects(instance));

        // too far E
        other = new Region(1100, 0, 1000, 800);
        assertFalse(other.intersects(instance));

        // too far S
        other = new Region(10, 900, 1000, 800);
        assertFalse(other.intersects(instance));

        // too far W
        other = new Region(0, 0, 4, 800);
        assertFalse(other.intersects(instance));
    }

    /* isEmpty() */

    @Test
    void isEmptyWithNonEmptyInstance() {
        assertFalse(instance.isEmpty());
    }

    @Test
    void isEmptyWithFullInstance() {
        instance = new Region(0, 0, 0, 0, true);
        assertFalse(instance.isEmpty());
    }

    @Test
    void isEmptyWithEmptyWidth() {
        instance = new Region(0, 0, 0.4, 10);
        assertTrue(instance.isEmpty());
    }

    @Test
    void isEmptyWithEmptyHeight() {
        instance = new Region(0, 0, 10, 0.4);
        assertTrue(instance.isEmpty());
    }

    /* longX() */

    @Test
    void longX() {
        instance = new Region(5.4, 0, 10, 10);
        assertEquals(5, instance.longX());
        instance = new Region(5.6, 0, 10, 10);
        assertEquals(6, instance.longX());
    }

    /* longY() */

    @Test
    void longY() {
        instance = new Region(0, 5.4, 10, 10);
        assertEquals(5, instance.longY());
        instance = new Region(0, 5.6, 10, 10);
        assertEquals(6, instance.longY());
    }

    /* longWidth() */

    @Test
    void longWidth() {
        instance = new Region(0, 0, 5.4, 10);
        assertEquals(5, instance.longWidth());
        instance = new Region(0, 0, 5.6, 10);
        assertEquals(6, instance.longWidth());
    }

    /* longHeight() */

    @Test
    void longHeight() {
        instance = new Region(0, 0, 1, 5.4);
        assertEquals(5, instance.longHeight());
        instance = new Region(0, 0, 1, 5.6);
        assertEquals(6, instance.longHeight());
    }

    /* moved() */

    @Test
    void moved() {
        final double initialX = instance.x();
        final double initialY = instance.y();
        final double amtX     = 3.5;
        final double amtY     = 2.5;
        Region moved       = instance.moved(amtX, amtY);

        Region expected    = new Region(
                initialX + amtX, initialY + amtY,
                instance.width(), instance.height());
        assertEquals(expected, moved);
    }

    /* oriented() */

    @Test
    void orientedWith0Orientation() {
        Size fullSize       = new Size(500, 500);
        instance            = new Region(100, 100, 200, 200, false);
        Region orientedRect = instance.oriented(fullSize, Orientation.ROTATE_0);
        assertEquals(new Region(100, 100, 200, 200), orientedRect);
    }

    @Test
    void orientedWith0OrientationAndFullSize() {
        Size fullSize       = new Size(500, 300);
        instance            = new Region(0, 0, 500, 300, true);
        Region orientedRect = instance.oriented(fullSize, Orientation.ROTATE_0);
        assertEquals(new Region(0, 0, 500, 300, true), orientedRect);
    }

    @Test
    void orientedWith90Orientation() {
        Size fullSize       = new Size(500, 500);
        instance            = new Region(100, 100, 200, 200, false);
        Region orientedRect = instance.oriented(fullSize, Orientation.ROTATE_90);
        assertEquals(new Region(100, 200, 200, 200), orientedRect);
    }

    @Test
    void orientedWith90OrientationAndFullSize() {
        Size fullSize       = new Size(500, 300);
        instance            = new Region(0, 0, 500, 300, true);
        Region orientedRect = instance.oriented(fullSize, Orientation.ROTATE_90);
        assertEquals(new Region(0, 0, 300, 500, true), orientedRect);
    }

    @Test
    void orientedWith180Orientation() {
        Size fullSize       = new Size(500, 500);
        instance            = new Region(100, 100, 200, 200, false);
        Region orientedRect = instance.oriented(fullSize, Orientation.ROTATE_180);
        assertEquals(new Region(200, 200, 200, 200), orientedRect);
    }

    @Test
    void orientedWith180OrientationAndFullSize() {
        Size fullSize       = new Size(500, 300);
        instance            = new Region(0, 0, 500, 300, true);
        Region orientedRect = instance.oriented(fullSize, Orientation.ROTATE_180);
        assertEquals(new Region(0, 0, 500, 300), orientedRect);
    }

    @Test
    void orientedWith270Orientation() {
        Size fullSize       = new Size(500, 500);
        instance            = new Region(100, 100, 200, 200, false);
        Region orientedRect = instance.oriented(fullSize, Orientation.ROTATE_270);
        assertEquals(new Region(200, 100, 200, 200), orientedRect);
    }

    @Test
    void orientedWith270OrientationAndFullSize() {
        Size fullSize       = new Size(500, 500);
        instance            = new Region(100, 100, 200, 200, true);
        Region orientedRect = instance.oriented(fullSize, Orientation.ROTATE_270);
        assertEquals(new Region(100, 100, 200, 200, true), orientedRect);
    }

    /* resized() */

    @Test
    void resizedWithSameSize() {
        Region newRegion = instance.resized(instance.width(), instance.height());
        assertNotSame(instance, newRegion);
    }

    @Test
    void resized() {
        Region newRegion = instance.resized(25, 30);
        assertNotSame(instance, newRegion);
        assertEquals(instance.x(), newRegion.x());
        assertEquals(instance.y(), newRegion.y());
        assertEquals(25, newRegion.width());
        assertEquals(30, newRegion.height());
    }

    @Test
    void resizedPreservesIsFull() {
        instance = new Region(0, 0, 50, 50, true);
        Region newRegion = instance.resized(25, 30);
        assertEquals(instance.x(), newRegion.x());
        assertEquals(instance.y(), newRegion.y());
        assertTrue(newRegion.isFull());
    }

    /* scaled(double) */

    @Test
    void scaledWithDouble() {
        final double initialW = instance.width();
        final double initialH = instance.height();
        final double amount   = 3.2;
        Region scaled         = instance.scaled(amount);

        Region expected = new Region(
                instance.x() * amount, instance.y() * amount,
                initialW * amount, initialH * amount);
        assertEquals(expected, scaled);
    }

    @Test
    void scaledWithDoublePreservesIsFull() {
        instance = new Region(0, 0, 50, 50, true);
        Region scaled = instance.scaled(3.0);
        assertTrue(scaled.isFull());
    }

    /* scaled(double, double) */

    @Test
    void scaledWithTwoDoubles() {
        final double initialW = instance.width();
        final double initialH = instance.height();
        final double amtX     = 3.2;
        final double amtY     = 2.5;
        Region scaled         = instance.scaled(amtX, amtY);

        Region expected = new Region(
                instance.x() * amtX, instance.y() * amtY,
                initialW * amtX, initialH * amtY);
        assertEquals(expected, scaled);
    }

    @Test
    void scaledWithTwoDoublesPreservesIsFull() {
        instance      = new Region(0, 0, 50, 50, true);
        Region scaled = instance.scaled(0.8, 0.8);
        assertTrue(scaled.isFull());
    }

    /* size() */

    @Test
    void size() {
        assertEquals(new Size(1000, 800), instance.size());
    }

    /* toAWTRectangle() */

    @Test
    void toAWTRectangle() {
        java.awt.Rectangle expected = new java.awt.Rectangle(10, 5, 1000, 800);
        assertEquals(expected, instance.toAWTRectangle());
    }

    /* toString() */

    @Test
    void testToString() {
        instance = new Region(10.5, 5.5, 1000.5, 800.5);
        assertEquals("10.5,5.5/1000.5x800.5", instance.toString());
    }

}
