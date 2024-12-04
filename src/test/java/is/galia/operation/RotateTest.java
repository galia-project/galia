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
import is.galia.image.ScaleConstraint;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RotateTest extends BaseTest {

    private static final double DELTA = 0.00000001;

    private Rotate instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        this.instance = new Rotate();
        assertEquals(0.0, this.instance.getDegrees(), DELTA);
    }

    @Test
    void equals() {
        assertEquals(instance, new Rotate());
        assertNotEquals(instance, new Rotate(1));
        assertNotEquals(instance, new Object());
    }

    @Test
    void getResultingSizeWithNoRotation() {
        Size fullSize = new Size(300, 200);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        assertEquals(fullSize,
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSizeWithRotation() {
        Size fullSize = new Size(300, 200);
        final int degrees = 30;
        instance.setDegrees(degrees);

        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Size actualSize = instance.getResultingSize(fullSize, scaleConstraint);

        assertEquals(360, actualSize.intWidth());
        assertEquals(323, actualSize.intHeight());
    }

    @Test
    void getResultingSizeWithScaleConstraint() {
        Size fullSize = new Size(300, 200);
        final int degrees = 30;
        instance.setDegrees(degrees);

        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 2);
        Size actual = instance.getResultingSize(fullSize, scaleConstraint);

        assertEquals(360, actual.intWidth());
        assertEquals(323, actual.intHeight());
    }

    @Test
    void hasEffect() {
        assertFalse(instance.hasEffect());
        instance.setDegrees(30);
        assertTrue(instance.hasEffect());
        instance.setDegrees(0.001);
        assertTrue(instance.hasEffect());
        instance.setDegrees(0.00001);
        assertFalse(instance.hasEffect());
    }

    @Test
    void hasEffectWithArguments() {
        Size fullSize = new Size(600, 400);
        OperationList opList = new OperationList();
        opList.add(new CropByPixels(0, 0, 300, 200));

        assertFalse(instance.hasEffect(fullSize, opList));
        instance.setDegrees(30);
        assertTrue(instance.hasEffect(fullSize, opList));
        instance.setDegrees(0.001);
        assertTrue(instance.hasEffect(fullSize, opList));
        instance.setDegrees(0.00001);
        assertFalse(instance.hasEffect(fullSize, opList));
    }

    @Test
    void setDegrees() {
        double degrees = 50;
        instance.setDegrees(degrees);
        assertEquals(degrees, instance.getDegrees(), 0.000001);
    }

    @Test
    void setDegreesWith360Degrees() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setDegrees(360),
                "Degrees must be between 0 and 360");
    }

    @Test
    void setDegreesWithLargeDegrees() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setDegrees(530),
                "Degrees must be between 0 and 360");
    }

    @Test
    void setDegreesWithNegativeDegrees() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setDegrees(-50),
                "Degrees must be between 0 and 360");
    }

    @Test
    void setDegreesWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setDegrees(15));
    }

    @Test
    void toMap() {
        instance.setDegrees(15);
        Size size = new Size(0, 0);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(size, scaleConstraint);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(15.0, map.get("degrees"));
    }

    @Test
    void toMapReturnsUnmodifiableMap() {
        Size fullSize = new Size(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    @Test
    void testToString() {
        Rotate r = new Rotate(50);
        assertEquals("50", r.toString());
        r = new Rotate(50.5);
        assertEquals("50.5", r.toString());
    }

}
