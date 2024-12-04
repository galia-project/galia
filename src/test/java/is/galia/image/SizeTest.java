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

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SizeTest extends BaseTest {

    private static final double DELTA = 0.00000001;

    private Size instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Size(1000, 800);
    }

    /* isPyramid() */

    @Test
    void isPyramidWithSingleLevel() {
        List<Size> levels = Collections.singletonList(new Size(500, 500));
        assertFalse(Size.isPyramid(levels));
    }

    @Test
    void isPyramidWithPyramidalLevels() {
        List<Size> levels = List.of(
                new Size(1000, 800),
                new Size(500, 400),
                new Size(250, 200),
                new Size(125, 100),
                new Size(63, 50),
                new Size(32, 25));
        assertTrue(Size.isPyramid(levels));
    }

    @Test
    void isPyramidWithNonPyramidalLevels() {
        List<Size> levels = List.of(
                new Size(1000, 800),
                new Size(1200, 600),
                new Size(900, 200));
        assertFalse(Size.isPyramid(levels));
    }

    /* ofScaledArea() */

    @Test
    void ofScaledArea() {
        instance = new Size(1000, 800);
        assertEquals(new Size(500, 400),
                Size.ofScaledArea(instance, 200000));
    }

    /* Size(double, double) */

    @Test
    void doubleConstructor() {
        instance = new Size(5.5, 4.4);
        assertEquals(5.5, instance.width(), DELTA);
        assertEquals(4.4, instance.height(), DELTA);
    }

    @Test
    void doubleConstructorWithNegativeArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new Size(-5.5, -4.4));
    }

    /* Size(long, long) */

    @Test
    void longConstructor() {
        instance = new Size(5, 4);
        assertEquals(5, instance.width(), DELTA);
        assertEquals(4, instance.height(), DELTA);
    }

    @Test
    void longConstructorWithNegativeArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new Size(-5, -4));
    }

    /* Size(Size) */

    @Test
    void copyConstructor() {
        Size other = new Size(instance);
        assertEquals(other, instance);
    }

    /* area() */

    @Test
    void area() {
        assertEquals(800000, instance.area());
    }

    /* width() */

    @Test
    void width() {
        assertEquals(1000, instance.width(), DELTA);
    }

    /* height() */

    @Test
    void height() {
        assertEquals(800, instance.height(), DELTA);
    }

    /* intArea() */

    @Test
    void intArea() {
        instance = new Size(45.2, 45.6);
        assertEquals(2061, instance.intArea());
    }

    /* intWidth() */

    @Test
    void intWidth() {
        instance = new Size(45.2, 45.6);
        assertEquals(45, instance.intWidth());

        instance = new Size(45.6, 45.6);
        assertEquals(46, instance.intWidth());
    }

    /* intHeight() */

    @Test
    void intHeight() {
        instance = new Size(45.2, 45.2);
        assertEquals(45, instance.intHeight());

        instance = new Size(45.2, 45.6);
        assertEquals(46, instance.intHeight());
    }

    /* inverted() */

    @Test
    void inverted() {
        Size inverted = instance.inverted();
        assertEquals(800, inverted.width(), DELTA);
        assertEquals(1000, inverted.height(), DELTA);
    }

    /* isEmpty() */

    @Test
    void isEmpty() {
        // width > 0.5, height > 0.5
        assertFalse(instance.isEmpty());

        // width < 0.5, height > 0.5
        instance = new Size(0.4, 0.6);
        assertTrue(instance.isEmpty());

        // width > 0.5, height < 0.5
        instance = new Size(0.6, 0.4);
        assertTrue(instance.isEmpty());

        // width < 0.5, height < 0.5
        instance = new Size(0.4, 0.4);
        assertTrue(instance.isEmpty());
    }

    /* longArea() */

    @Test
    void longArea() {
        instance = new Size(45.2, 45.6);
        assertEquals(2061, instance.longArea());
    }

    /* longWidth() */

    @Test
    void longWidth() {
        instance = new Size(45.4, 45.6);
        assertEquals(45, instance.longWidth());
        instance = new Size(45.6, 45.6);
        assertEquals(46, instance.longWidth());
    }

    /* longHeight() */

    @Test
    void longHeight() {
        instance = new Size(45.2, 45.4);
        assertEquals(45, instance.longHeight());
        instance = new Size(45.2, 45.6);
        assertEquals(46, instance.longHeight());
    }

    /* scaled() */

    @Test
    void scaled() {
        Size scaled = instance.scaled(1.5);
        assertEquals(1500, scaled.width(), DELTA);
        assertEquals(1200, scaled.height(), DELTA);
    }

    /* toString() */

    @Test
    void testToString() {
        instance = new Size(1000.5, 800.5);
        assertEquals("1000.5x800.5", instance.toString());
    }

}