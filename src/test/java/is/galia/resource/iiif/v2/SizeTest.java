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

package is.galia.resource.iiif.v2;

import is.galia.operation.ScaleByPercent;
import is.galia.operation.ScaleByPixels;
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
        this.instance = new Size();
    }

    /* fromUri(String) */

    /**
     * Tests fromUri(String) with a value of "max".
     */
    @Test
    void fromURIMax() {
        Size s = Size.fromURI("max");
        assertEquals(Size.ScaleMode.MAX, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with a value of "full".
     */
    @Test
    void fromURIFull() {
        Size s = Size.fromURI("full");
        assertEquals(Size.ScaleMode.FULL, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with width scaling.
     */
    @Test
    void fromURIWidthScaled() {
        Size s = Size.fromURI("50,");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Size.ScaleMode.ASPECT_FIT_WIDTH, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with height scaling.
     */
    @Test
    void fromURIHeightScaled() {
        Size s = Size.fromURI(",50");
        assertEquals(Integer.valueOf(50), s.getHeight());
        assertEquals(Size.ScaleMode.ASPECT_FIT_HEIGHT, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with percentage scaling.
     */
    @Test
    void fromURIPercentageScaled() {
        Size s = Size.fromURI("pct:50");
        assertEquals(Float.valueOf(50), s.getPercent());
    }

    /**
     * Tests fromUri(String) with absolute width and height.
     */
    @Test
    void fromURIAbsoluteScaled() {
        Size s = Size.fromURI("50,40");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Integer.valueOf(40), s.getHeight());
        assertEquals(Size.ScaleMode.NON_ASPECT_FILL, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with scale-to-fit width and height.
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
        this.instance.setHeight(height);
        assertEquals(height, this.instance.getHeight());
    }

    @Test
    void setZeroHeight() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setHeight(0),
                "Height must be a positive integer");
    }

    @Test
    void setNegativeHeight() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setHeight(-1),
                "Height must be a positive integer");
    }

    /* setPercent() */

    @Test
    void setPercent() {
        float percent = 50f;
        instance.setPercent(percent);
        assertEquals(percent, this.instance.getPercent(), DELTA);
    }

    @Test
    void setZeroPercent() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setPercent(0f),
                "Percent must be positive");
    }

    @Test
    void setNegativePercent() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setPercent(-1f),
                "Percent must be positive");
    }

    /* setWidth() */

    @Test
    void setWidth() {
        Integer width = 50;
        this.instance.setWidth(width);
        assertEquals(width, this.instance.getWidth());
    }

    @Test
    void setZeroWidth() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setWidth(0),
                "Width must be positive");
    }

    @Test
    void setNegativeWidth() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setWidth(-1),
                "Width must be positive");
    }

    /* toScale() */

    @Test
    void toScaleWithPercent() {
        instance.setPercent(50f);
        assertEquals(new ScaleByPercent(0.5), instance.toScale());
    }

    @Test
    void toScaleWithFull() {
        instance.setScaleMode(Size.ScaleMode.FULL);
        assertEquals(new ScaleByPercent(), instance.toScale());
    }

    @Test
    void toScaleWithMax() {
        instance.setScaleMode(Size.ScaleMode.MAX);
        assertEquals(new ScaleByPercent(), instance.toScale());
    }

    @Test
    void toScaleWithAspectFitWidth() {
        instance.setScaleMode(Size.ScaleMode.ASPECT_FIT_WIDTH);
        instance.setWidth(300);
        assertEquals(
                new ScaleByPixels(300, null, ScaleByPixels.Mode.ASPECT_FIT_WIDTH),
                instance.toScale());
    }

    @Test
    void toScaleWithAspectFitHeight() {
        instance.setScaleMode(Size.ScaleMode.ASPECT_FIT_HEIGHT);
        instance.setHeight(300);
        assertEquals(
                new ScaleByPixels(null, 300, ScaleByPixels.Mode.ASPECT_FIT_HEIGHT),
                instance.toScale());
    }

    @Test
    void toScaleWithAspectFitInside() {
        instance.setScaleMode(Size.ScaleMode.ASPECT_FIT_INSIDE);
        instance.setWidth(300);
        instance.setHeight(200);
        assertEquals(
                new ScaleByPixels(300, 200, ScaleByPixels.Mode.ASPECT_FIT_INSIDE),
                instance.toScale());
    }

    @Test
    void toScaleWithNonAspectFill() {
        instance.setScaleMode(Size.ScaleMode.NON_ASPECT_FILL);
        instance.setWidth(300);
        instance.setHeight(200);
        assertEquals(
                new ScaleByPixels(300, 200, ScaleByPixels.Mode.NON_ASPECT_FILL),
                instance.toScale());
    }

    /* toString */

    @Test
    void testToString() {
        Size s = Size.fromURI("full");
        assertEquals("full", s.toString());

        s = Size.fromURI("max");
        assertEquals("max", s.toString());

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

    @Test
    void toCanonicalString() {
        final is.galia.image.Size fullSize = new is.galia.image.Size(1000, 800);

        Size s = Size.fromURI("full");
        assertEquals("full", s.toCanonicalString(fullSize));

        s = Size.fromURI("max");
        assertEquals("max", s.toCanonicalString(fullSize));

        s = Size.fromURI("50,");
        assertEquals("50,", s.toCanonicalString(fullSize));

        s = Size.fromURI(",50");
        assertEquals("63,", s.toCanonicalString(fullSize));

        s = Size.fromURI("pct:50");
        assertEquals("500,", s.toCanonicalString(fullSize));

        s = Size.fromURI("50,40");
        assertEquals("50,40", s.toCanonicalString(fullSize));

        s = Size.fromURI("!50,40");
        assertEquals("50,", s.toCanonicalString(fullSize));
    }

}
