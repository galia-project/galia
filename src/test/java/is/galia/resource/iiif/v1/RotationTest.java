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

import is.galia.operation.Rotate;
import is.galia.resource.IllegalClientArgumentException;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RotationTest extends BaseTest {

    private static final float DELTA = 0.0000001f;

    private Rotation instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new Rotation();
        assertEquals(0f, instance.getDegrees(), DELTA);
    }

    /* fromURI(String) */

    /**
     * Tests {@link Rotation#fromURI(String)} with a legal value.
     */
    @Test
    void fromURI() {
        Rotation r = Rotation.fromURI("35");
        assertEquals(35f, r.getDegrees(), DELTA);

        r = Rotation.fromURI("35.5");
        assertEquals(35.5f, r.getDegrees(), DELTA);
    }

    /**
     * Tests {@link Rotation#fromURI(String)} with a large value.
     */
    @Test
    void fromURIWithLargeValue() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Rotation.fromURI("720"),
                "Degrees must be between 0 and 360");
    }

    /**
     * Tests {@link Rotation#fromURI(String)} with a negative value.
     */
    @Test
    void fromURIWithNegativeValue() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Rotation.fromURI("-35"),
                "Degrees must be between 0 and 360");
    }

    @Test
    void testEquals() {
        Rotation r2 = new Rotation();
        assertEquals(r2, instance);
        r2.setDegrees(15);
        assertNotEquals(r2, instance);
    }

    /* setDegrees() */

    @Test
    void setDegrees() {
        float degrees = 50.0f;
        instance.setDegrees(degrees);
        assertEquals(degrees, instance.getDegrees(), DELTA);
    }

    @Test
    void setLargeDegrees() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setDegrees(530),
                "Degrees must be between 0 and 360");
    }

    @Test
    void setNegativeDegrees() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setDegrees(-50),
                "Degrees must be between 0 and 360");
    }

    @Test
    void toRotate() {
        Rotate rotate = instance.toRotate();
        assertEquals(instance.getDegrees(), rotate.getDegrees(), DELTA);
    }

    @Test
    void testToString() {
        Rotation r = Rotation.fromURI("50");
        assertEquals("50", r.toString());

        r = Rotation.fromURI("50.50");
        assertEquals("50.5", r.toString());
    }

}