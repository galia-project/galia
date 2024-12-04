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

package is.galia.resource.iiif.v3;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.operation.ScaleByPercent;
import is.galia.operation.ScaleByPixels;
import is.galia.resource.IllegalClientArgumentException;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SizeTest extends BaseTest {

    private static final double DELTA = 0.0000001;

    private Size instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.instance = new Size();
    }

    /* fromURI(String) */

    @Test
    void fromURIWithMax() {
        Size s = Size.fromURI("max");
        assertEquals(Size.Type.MAX, s.getType());
        assertFalse(s.isUpscalingAllowed());
        assertFalse(s.isExact());
    }

    @Test
    void fromURIWithMaxAllowingUpscaling() {
        Size s = Size.fromURI("^max");
        assertEquals(Size.Type.MAX, s.getType());
        assertTrue(s.isUpscalingAllowed());
        assertFalse(s.isExact());
    }

    @Test
    void fromURIWithScaleToWidth() {
        Size s = Size.fromURI("50,");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Size.Type.ASPECT_FIT_WIDTH, s.getType());
        assertFalse(s.isUpscalingAllowed());
        assertTrue(s.isExact());
    }

    @Test
    void fromURIWithScaleToWidthAllowingUpscaling() {
        Size s = Size.fromURI("^50,");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Size.Type.ASPECT_FIT_WIDTH, s.getType());
        assertTrue(s.isUpscalingAllowed());
        assertTrue(s.isExact());
    }

    @Test
    void fromURIWithScaleToHeight() {
        Size s = Size.fromURI(",50");
        assertEquals(Integer.valueOf(50), s.getHeight());
        assertEquals(Size.Type.ASPECT_FIT_HEIGHT, s.getType());
        assertFalse(s.isUpscalingAllowed());
        assertTrue(s.isExact());
    }

    @Test
    void fromURIWithScaleToHeightAllowingUpscaling() {
        Size s = Size.fromURI("^,50");
        assertEquals(Integer.valueOf(50), s.getHeight());
        assertEquals(Size.Type.ASPECT_FIT_HEIGHT, s.getType());
        assertTrue(s.isUpscalingAllowed());
        assertTrue(s.isExact());
    }

    @Test
    void fromURIWithScaleToPercentage() {
        Size s = Size.fromURI("pct:50.5");
        assertEquals(50.5f, s.getPercent());
        assertFalse(s.isUpscalingAllowed());
        assertTrue(s.isExact());
    }

    @Test
    void fromURIWithScaleToPercentageAllowingUpscaling() {
        Size s = Size.fromURI("^pct:50.5");
        assertEquals(50.5f, s.getPercent());
        assertTrue(s.isUpscalingAllowed());
        assertTrue(s.isExact());
    }

    @Test
    void fromURIWithAbsoluteScale() {
        Size s = Size.fromURI("50,40");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Integer.valueOf(40), s.getHeight());
        assertEquals(Size.Type.NON_ASPECT_FILL, s.getType());
        assertFalse(s.isUpscalingAllowed());
        assertTrue(s.isExact());
    }

    @Test
    void fromURIWithAbsoluteScaleAllowingUpscaling() {
        Size s = Size.fromURI("^50,40");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Integer.valueOf(40), s.getHeight());
        assertEquals(Size.Type.NON_ASPECT_FILL, s.getType());
        assertTrue(s.isUpscalingAllowed());
        assertTrue(s.isExact());
    }

    @Test
    void fromURIWithScaleToFit() {
        Size s = Size.fromURI("!50,40");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Integer.valueOf(40), s.getHeight());
        assertEquals(Size.Type.ASPECT_FIT_INSIDE, s.getType());
        assertFalse(s.isUpscalingAllowed());
        assertFalse(s.isExact());
    }

    @Test
    void fromURIWithScaleToFitAllowingUpscaling() {
        Size s = Size.fromURI("^!50,40");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Integer.valueOf(40), s.getHeight());
        assertEquals(Size.Type.ASPECT_FIT_INSIDE, s.getType());
        assertTrue(s.isUpscalingAllowed());
        assertFalse(s.isExact());
    }

    @Test
    void fromURIWithPercentEncodedArgument() {
        Size s = Size.fromURI("%5Emax");
        assertEquals(Size.Type.MAX, s.getType());
        assertTrue(s.isUpscalingAllowed());
        assertFalse(s.isExact());
    }

    @Test
    void fromURIWithIllegalArgument1() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("cats"));
    }

    @Test
    void fromURIWithIllegalArgument2() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("pct:cats"));
    }

    @Test
    void fromURIWithIllegalArgument3() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("pct:50,30"));
    }

    @Test
    void fromURIWithIllegalArgument4() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("120,cats"));
    }

    @Test
    void fromURIWithIllegalArgument5() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("cats,120"));
    }

    @Test
    void fromURIWithIllegalArgument6() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("!cats,120"));
    }

    @Test
    void fromURIWithIllegalArgument7() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("!120,"));
    }

    /* equals() */

    @Test
    void equals() {
        instance.setType(Size.Type.ASPECT_FIT_INSIDE);
        instance.setWidth(300);
        instance.setHeight(200);
        Size size2 = new Size();
        size2.setType(Size.Type.ASPECT_FIT_INSIDE);
        size2.setWidth(300);
        size2.setHeight(200);
        assertEquals(instance, size2);

        size2.setType(Size.Type.ASPECT_FIT_WIDTH);
        assertNotEquals(instance, size2);

        size2.setType(Size.Type.ASPECT_FIT_INSIDE);
        size2.setWidth(299);
        assertNotEquals(instance, size2);

        size2.setWidth(300);
        size2.setHeight(199);
        assertNotEquals(instance, size2);

        size2.setHeight(200);
        size2.setType(null);
        assertNotEquals(instance, size2);

        size2.setType(Size.Type.ASPECT_FIT_INSIDE);
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
        assertEquals(new ScaleByPercent(0.5), instance.toScale(1));
    }

    @Test
    void toScaleWithMaxTypeAndUpscalingAllowedAndPositiveMaxArgument() {
        instance.setType(Size.Type.MAX);
        instance.setUpscalingAllowed(true);
        // 1
        ScaleByPercent actual = (ScaleByPercent) instance.toScale(1);
        assertEquals(1, actual.getPercent(), DELTA);
        // 2
        actual = (ScaleByPercent) instance.toScale(2);
        assertEquals(2, actual.getPercent(), DELTA);
    }

    @Test
    void toScaleWithMaxTypeAndUpscalingAllowedAndZeroMaxArgumentAndPositiveMaxPixels() {
        final long maxPixels = 1000000;
        Configuration.forApplication().setProperty(Key.MAX_PIXELS, maxPixels);
        instance.setType(Size.Type.MAX);
        instance.setUpscalingAllowed(true);

        ScaleByPixels actual = (ScaleByPixels) instance.toScale(0);
        assertEquals((int) Math.sqrt(maxPixels), actual.getWidth());
        assertEquals((int) Math.sqrt(maxPixels), actual.getHeight());
    }

    @Test
    void toScaleWithMaxTypeAndUpscalingAllowedAndZeroMaxArgumentAndZeroMaxPixels() {
        Configuration.forApplication().setProperty(Key.MAX_PIXELS, 0);
        instance.setType(Size.Type.MAX);
        instance.setUpscalingAllowed(true);

        ScaleByPercent actual = (ScaleByPercent) instance.toScale(0);
        assertEquals(1, actual.getPercent(), DELTA);
    }

    @Test
    void toScaleWithMaxTypeAndUpscalingNotAllowed() {
        instance.setType(Size.Type.MAX);
        instance.setUpscalingAllowed(false);

        ScaleByPercent actual = (ScaleByPercent) instance.toScale(1);
        assertEquals(1, actual.getPercent(), DELTA);

        actual = (ScaleByPercent) instance.toScale(2);
        assertEquals(1, actual.getPercent(), DELTA);
    }

    @Test
    void toScaleWithAspectFitWidthType() {
        instance.setType(Size.Type.ASPECT_FIT_WIDTH);
        instance.setWidth(300);
        assertEquals(
                new ScaleByPixels(300, null, ScaleByPixels.Mode.ASPECT_FIT_WIDTH),
                instance.toScale(1));
    }

    @Test
    void toScaleWithAspectFitHeightType() {
        instance.setType(Size.Type.ASPECT_FIT_HEIGHT);
        instance.setHeight(300);
        assertEquals(
                new ScaleByPixels(null, 300, ScaleByPixels.Mode.ASPECT_FIT_HEIGHT),
                instance.toScale(1));
    }

    @Test
    void toScaleWithAspectFitInsideType() {
        instance.setType(Size.Type.ASPECT_FIT_INSIDE);
        instance.setWidth(300);
        instance.setHeight(200);
        assertEquals(
                new ScaleByPixels(300, 200, ScaleByPixels.Mode.ASPECT_FIT_INSIDE),
                instance.toScale(1));
    }

    @Test
    void toScaleWithNonAspectFillType() {
        instance.setType(Size.Type.NON_ASPECT_FILL);
        instance.setWidth(300);
        instance.setHeight(200);
        assertEquals(
                new ScaleByPixels(300, 200, ScaleByPixels.Mode.NON_ASPECT_FILL),
                instance.toScale(1));
    }

    /* toString */

    @Test
    void testToString() {
        Size s = Size.fromURI("max");
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

        Size s = Size.fromURI("max");
        assertEquals("max", s.toCanonicalString(fullSize));

        s = Size.fromURI("^max");
        assertEquals("^max", s.toCanonicalString(fullSize));

        s = Size.fromURI("50,");
        assertEquals("50,40", s.toCanonicalString(fullSize));

        s = Size.fromURI(",50");
        assertEquals("63,50", s.toCanonicalString(fullSize));

        s = Size.fromURI("pct:50");
        assertEquals("500,400", s.toCanonicalString(fullSize));

        s = Size.fromURI("50,40");
        assertEquals("50,40", s.toCanonicalString(fullSize));

        s = Size.fromURI("^50,40");
        assertEquals("50,40", s.toCanonicalString(fullSize));

        s = Size.fromURI("!50,40");
        assertEquals("50,40", s.toCanonicalString(fullSize));

        s = Size.fromURI("^!50,40");
        assertEquals("50,40", s.toCanonicalString(fullSize));
    }

}
