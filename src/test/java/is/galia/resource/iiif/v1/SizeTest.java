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

import is.galia.resource.IllegalClientArgumentException;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SizeTest extends BaseTest {

    private static final float DELTA = 0.0000001f;

    private Size instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Size();
    }

    /* fromURI(String) */

    @Test
    void fromURIFull() {
        Size s = Size.fromURI("full");
        assertEquals(Size.ScaleMode.FULL, s.getScaleMode());
    }

    @Test
    void fromURIWidthScaled() {
        Size s = Size.fromURI("50,");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Size.ScaleMode.ASPECT_FIT_WIDTH, s.getScaleMode());
    }

    @Test
    void fromURIHeightScaled() {
        Size s = Size.fromURI(",50");
        assertEquals(Integer.valueOf(50), s.getHeight());
        assertEquals(Size.ScaleMode.ASPECT_FIT_HEIGHT, s.getScaleMode());
    }

    /**
     * Tests fromURI(String) with percentage scaling.
     */
    @Test
    void fromURIPercentageScaled() {
        Size s = Size.fromURI("pct:50");
        assertEquals(Float.valueOf(50), s.getPercent());
    }

    /**
     * Tests {@link Size#fromURI(String)} with absolute width and height.
     */
    @Test
    void fromURIAbsoluteScaled() {
        Size s = Size.fromURI("50,40");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Integer.valueOf(40), s.getHeight());
        assertEquals(Size.ScaleMode.NON_ASPECT_FILL, s.getScaleMode());
    }

    /**
     * Tests {@link Size#fromURI(String)} with scale-to-fit width and height.
     */
    @Test
    void fromURIScaleToFit() {
        Size s = Size.fromURI("!50,40");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Integer.valueOf(40), s.getHeight());
        assertEquals(Size.ScaleMode.ASPECT_FIT_INSIDE, s.getScaleMode());
    }

    @Test
    void fromURIWithInvalidArgument1() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("cats"));
    }

    @Test
    void fromURIWithInvalidArgument2() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("pct:cats"));
    }

    @Test
    void fromURIWithInvalidArgument3() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("pct:50,30"));
    }

    @Test
    void fromURIWithInvalidArgument4() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("120,cats"));
    }

    @Test
    void fromURIWithInvalidArgument5() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("cats,120"));
    }

    @Test
    void fromURIWithInvalidArgument6() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("!cats,120"));
    }

    @Test
    void fromURIWithInvalidArgument7() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("!120,"));
    }

    /* equals() */

    @Test
    void equals() {
        instance.setScaleMode(Size.ScaleMode.ASPECT_FIT_INSIDE);
        instance.setWidth(300);
        instance.setHeight(200);
        Size size2 = new Size();
        size2.setScaleMode(Size.ScaleMode.ASPECT_FIT_INSIDE);
        size2.setWidth(300);
        size2.setHeight(200);
        assertEquals(instance, size2);

        size2.setScaleMode(Size.ScaleMode.ASPECT_FIT_WIDTH);
        assertNotEquals(instance, size2);

        size2.setScaleMode(Size.ScaleMode.ASPECT_FIT_INSIDE);
        size2.setWidth(299);
        assertNotEquals(instance, size2);

        size2.setWidth(300);
        size2.setHeight(199);
        assertNotEquals(instance, size2);

        size2.setHeight(200);
        size2.setScaleMode(null);
        assertNotEquals(instance, size2);

        size2.setScaleMode(Size.ScaleMode.ASPECT_FIT_INSIDE);
        size2.setWidth(null);
        assertNotEquals(instance, size2);

        size2.setWidth(300);
        size2.setHeight(null);
        assertNotEquals(instance, size2);
    }

    /* setHeight() */

    @Test
    void setHeight() {
        Integer height = 50;
        instance.setHeight(height);
        assertEquals(height, instance.getHeight());
    }

    @Test
    void setNegativeHeight() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setHeight(-1),
                "Height must be a positive integer");
    }

    @Test
    void setZeroHeight() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setHeight(0),
                "Height must be a positive integer");
    }

    /* setPercent() */

    @Test
    void setPercent() {
        float percent = 50f;
        instance.setPercent(percent);
        assertEquals(percent, instance.getPercent(), DELTA);
    }

    @Test
    void setNegativePercent() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setPercent(-1.0f),
                "Percent must be positive");
    }

    @Test
    void setZeroPercent() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setPercent(0f),
                "Percent must be positive");
    }

    /* setWidth() */

    @Test
    void setWidth() {
        Integer width = 50;
        instance.setWidth(width);
        assertEquals(width, instance.getWidth());
    }

    @Test
    void setNegativeWidth() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setWidth(-1),
                "Width must be a positive integer");
    }

    @Test
    void setZeroWidth() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setWidth(0),
                "Width must be a positive integer");
    }

    /* toString() */

    @Test
    void testToString() {
        Size s = Size.fromURI("full");
        assertEquals("full", s.toString());

        s = Size.fromURI("50,");
        assertEquals("50,", s.toString());

        s = Size.fromURI(",50");
        assertEquals(",50", s.toString());

        s = Size.fromURI("pct:50");
        assertEquals("pct:50", s.toString());

        s = Size.fromURI("50,40");
        assertEquals("50,40", s.toString());

        s = Size.fromURI("!50,40");
        assertEquals("!50,40", s.toString());
    }

}
