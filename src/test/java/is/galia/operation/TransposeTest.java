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

public class TransposeTest extends BaseTest {

    private Transpose instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.instance = Transpose.HORIZONTAL;
    }

    @Test
    void getEffectiveSize() {
        Size fullSize = new Size(200, 200);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        assertEquals(fullSize,
                Transpose.VERTICAL.getResultingSize(fullSize, scaleConstraint));
        assertEquals(fullSize,
                Transpose.HORIZONTAL.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void hasEffect() {
        assertTrue(instance.hasEffect());
    }

    @Test
    void hasEffectWithArguments() {
        Size fullSize        = new Size(600, 400);
        OperationList opList = OperationList.builder()
                .withOperations(new CropByPixels(0, 0, 300, 200))
                .build();
        assertTrue(instance.hasEffect(fullSize, opList));
    }

    @Test
    void toMap() {
        Size size                       = new Size(0, 0);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map          = instance.toMap(size, scaleConstraint);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals("horizontal", map.get("axis"));
    }

    @Test
    void toMapReturnsUnmodifiableMap() {
        Size fullSize                   = new Size(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map          = instance.toMap(fullSize, scaleConstraint);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    @Test
    void testToString() {
        instance = Transpose.HORIZONTAL;
        assertEquals("h", instance.toString());
        instance = Transpose.VERTICAL;
        assertEquals("v", instance.toString());
    }

}
