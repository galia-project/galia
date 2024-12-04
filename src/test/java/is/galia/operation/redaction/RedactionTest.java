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

package is.galia.operation.redaction;

import is.galia.image.Size;
import is.galia.image.Region;
import is.galia.image.ScaleConstraint;
import is.galia.operation.Color;
import is.galia.operation.Crop;
import is.galia.operation.CropByPixels;
import is.galia.operation.OperationList;
import is.galia.operation.ScaleByPercent;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RedactionTest extends BaseTest {

    private Redaction instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Redaction(new Region(50, 60, 200, 100), Color.BLACK);
    }

    /* Redaction(Region, Color) */

    @Test
    void constructor2WithNullRegionArgument() {
        assertThrows(NullPointerException.class,
                () -> new Redaction(null, Color.BLACK));
    }

    @Test
    void constructor2WithNullColorArgument() {
        assertThrows(NullPointerException.class,
                () -> new Redaction(new Region(50, 60, 200, 100), null));
    }

    /* equals() */

    @Test
    void equalsWithEqualInstances() {
        Redaction other = new Redaction(
                new Region(50, 60, 200, 100),
                Color.BLACK);
        assertEquals(instance, other);
    }

    @Test
    void equalsWithUnequalX() {
        Redaction other = new Redaction(
                new Region(51, 60, 200, 100), instance.getColor());
        assertNotEquals(instance, other);
    }

    @Test
    void equalsWithUnequalY() {
        Redaction other = new Redaction(
                new Region(50, 61, 200, 100), instance.getColor());
        assertNotEquals(instance, other);
    }

    @Test
    void equalsWithUnequalWidth() {
        Redaction other = new Redaction(
                new Region(50, 60, 201, 100), instance.getColor());
        assertNotEquals(instance, other);
    }

    @Test
    void equalsWithUnequalHeight() {
        Redaction other = new Redaction(
                new Region(50, 60, 200, 101), instance.getColor());
        assertNotEquals(instance, other);
    }

    @Test
    void equalsWithUnequalColor() {
        Redaction other = new Redaction(instance.getRegion(), Color.RED);
        assertNotEquals(instance, other);
    }

    /* getResultingRegion() */

    @Test
    void getResultingRegion() {
        Size sourceSize = new Size(500, 500);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        // redaction within source image bounds
        Crop crop = new CropByPixels(0, 0, 300, 300);
        Region resultingRegion = instance.getResultingRegion(
                sourceSize, scaleConstraint, crop);
        assertEquals(new Region(50, 60, 200, 100), resultingRegion);

        // redaction partially within source image bounds
        crop = new CropByPixels(0, 0, 100, 100);
        resultingRegion = instance.getResultingRegion(
                sourceSize, scaleConstraint, crop);
        assertEquals(new Region(50, 60, 200, 100), resultingRegion);

        // redaction outside source image bounds
        crop = new CropByPixels(300, 300, 100, 100);
        resultingRegion = instance.getResultingRegion(
                sourceSize, scaleConstraint, crop);
        assertEquals(new Region(0, 0, 0, 0), resultingRegion);
    }

    @Test
    void getResultingRegionWithScaleConstraint() {
        Size sourceSize = new Size(1000, 1000);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 2);

        // redaction within source image bounds
        Crop crop = new CropByPixels(0, 0, 300, 300);
        Region resultingRegion = instance.getResultingRegion(
                sourceSize, scaleConstraint, crop);
        assertEquals(new Region(50, 60, 200, 100), resultingRegion);

        // redaction partially within source image bounds
        crop = new CropByPixels(0, 0, 200, 200);
        resultingRegion = instance.getResultingRegion(
                sourceSize, scaleConstraint, crop);
        assertEquals(new Region(50, 60, 200, 100), resultingRegion);

        // redaction outside source image bounds
        crop = new CropByPixels(300, 300, 100, 100);
        resultingRegion = instance.getResultingRegion(
                sourceSize, scaleConstraint, crop);
        assertEquals(new Region(0, 0, 0, 0), resultingRegion);
    }

    @Test
    void getResultingSize() {
        Size fullSize = new Size(500, 500);

        // scale constraint 1:1
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        assertEquals(fullSize,
                instance.getResultingSize(fullSize, scaleConstraint));

        // scale constraint 1:2
        scaleConstraint = new ScaleConstraint(1, 2);
        assertEquals(new Size(500, 500),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    /* hasEffect() */

    @Test
    void hasEffect() {
        assertTrue(instance.hasEffect());
    }

    @Test
    void hasEffectWithZeroWidth() {
        instance = new Redaction(new Region(50, 60, 0, 100), Color.BLACK);
        assertFalse(instance.hasEffect());
    }

    @Test
    void hasEffectWithZeroHeight() {
        instance = new Redaction(new Region(50, 60, 50, 0), Color.BLACK);
        assertFalse(instance.hasEffect());
    }

    @Test
    void hasEffectWithZeroOpacity() {
        instance = new Redaction(new Region(50, 60, 50, 50),
                new Color(25, 25, 25, 0));
        assertFalse(instance.hasEffect());
    }

    @Test
    void hasEffectWithArguments() {
        final Size fullSize = new Size(600, 400);

        // N.B.: hasEffect() shouldn't be looking at the Scales. They are
        // added to ensure that it doesn't.

        // in bounds
        OperationList opList = OperationList.builder()
                .withOperations(
                        new CropByPixels(0, 0, 400, 300),
                        new ScaleByPercent(0.25))
                .build();
        assertTrue(instance.hasEffect(fullSize, opList));

        // partially in bounds
        opList = OperationList.builder()
                .withOperations(
                        new CropByPixels(100, 100, 100, 100),
                        new ScaleByPercent(0.25))
                .build();
        assertTrue(instance.hasEffect(fullSize, opList));

        // out of bounds
        opList = OperationList.builder()
                .withOperations(
                        new CropByPixels(0, 0, 400, 300),
                        new ScaleByPercent(0.25))
                .build();
        instance = new Redaction(new Region(420, 305, 20, 20), Color.BLACK);
        assertFalse(instance.hasEffect(fullSize, opList));
    }

    /* hashCode() */

    @Test
    void hashCodeWithEqualInstances() {
        Redaction other = new Redaction(
                new Region(50, 60, 200, 100),
                Color.BLACK);
        assertEquals(instance.hashCode(), other.hashCode());
    }

    @Test
    void hashCodeWithUnequalX() {
        Redaction other = new Redaction(
                new Region(51, 60, 200, 100), instance.getColor());
        assertNotEquals(instance.hashCode(), other.hashCode());
    }

    @Test
    void hashCodeWithUnequalY() {
        Redaction other = new Redaction(
                new Region(50, 61, 200, 100), instance.getColor());
        assertNotEquals(instance.hashCode(), other.hashCode());
    }

    @Test
    void hashCodeWithUnequalWidth() {
        Redaction other = new Redaction(
                new Region(50, 60, 201, 100), instance.getColor());
        assertNotEquals(instance.hashCode(), other.hashCode());
    }

    @Test
    void hashCodeWithUnequalHeight() {
        Redaction other = new Redaction(
                new Region(50, 60, 200, 101), instance.getColor());
        assertNotEquals(instance.hashCode(), other.hashCode());
    }

    @Test
    void hashCodeWithUnequalColor() {
        Redaction other = new Redaction(instance.getRegion(), Color.RED);
        assertNotEquals(instance.hashCode(), other.hashCode());
    }

    /* setColor() */

    @Test
    void setColorWithNullArgument() {
        assertThrows(NullPointerException.class,
                () -> instance.setColor(null));
    }

    @Test
    void setColorWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setColor(Color.BLUE));
    }

    /* setRegion() */

    @Test
    void setRegionWithNullArgument() {
        assertThrows(NullPointerException.class,
                () -> instance.setRegion(null));
    }

    @Test
    void setRegionWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setRegion(new Region(0, 0, 10, 10)));
    }

    /* toMap() */

    @Test
    void toMap() {
        Size fullSize = new Size(500, 500);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(50L, map.get("x"));
        assertEquals(60L, map.get("y"));
        assertEquals(200L, map.get("width"));
        assertEquals(100L, map.get("height"));
        assertEquals("#000000FF", map.get("color"));
    }

    @Test
    void toMapReturnsUnmodifiableMap() {
        Size fullSize = new Size(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    /* toString() */

    @Test
    void testToString() {
        assertEquals("50,60/200x100/#000000FF", instance.toString());
    }

}
