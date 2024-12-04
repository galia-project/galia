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
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ColorTransformTest extends BaseTest {

    @Test
    void testValues() {
        assertEquals(2, ColorTransform.values().length);
        assertNotNull(ColorTransform.BITONAL);
        assertNotNull(ColorTransform.GRAY);
    }

    @Test
    void testGetEffectiveSize() {
        Size fullSize = new Size(200, 200);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 2);
        assertSame(fullSize,
                ColorTransform.BITONAL.getResultingSize(fullSize, scaleConstraint));
        assertSame(fullSize,
                ColorTransform.GRAY.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void testHasEffect() {
        assertTrue(ColorTransform.BITONAL.hasEffect());
        assertTrue(ColorTransform.GRAY.hasEffect());
    }

    @Test
    void testHasEffectWithArguments() {
        Size fullSize = new Size(600, 400);
        OperationList opList = OperationList.builder()
                .withOperations(new CropByPixels(0, 0, 300, 200))
                .build();
        assertTrue(ColorTransform.BITONAL.hasEffect(fullSize, opList));
        assertTrue(ColorTransform.GRAY.hasEffect(fullSize, opList));
    }

    @Test
    void testToMap() {
        Size size = new Size(0, 0);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = ColorTransform.BITONAL.toMap(size, scaleConstraint);

        assertEquals(ColorTransform.class.getSimpleName(), map.get("class"));
        assertEquals("bitonal", map.get("type"));
    }

    @Test
    public void toMapReturnsUnmodifiableMap() {
        Size fullSize = new Size(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = ColorTransform.GRAY.toMap(fullSize, scaleConstraint);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    @Test
    void testToString() {
        assertEquals("bitonal", ColorTransform.BITONAL.toString());
        assertEquals("gray", ColorTransform.GRAY.toString());
    }

}
