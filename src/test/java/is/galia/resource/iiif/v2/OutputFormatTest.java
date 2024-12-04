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

import is.galia.image.Format;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutputFormatTest extends BaseTest {

    private OutputFormat instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new OutputFormat("jpg");
    }

    /* constructor */

    @Test
    void constructorWithNullName() {
        assertThrows(NullPointerException.class, () -> new OutputFormat(null));
    }

    @Test
    void constructorWithEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> new OutputFormat(""));
    }

    /* equals() */

    @Test
    void equalsWithUnequalInstances() {
        assertNotEquals(new OutputFormat("tif"), instance);
    }

    @Test
    void equalsWithEqualInstances() {
        assertEquals(new OutputFormat("jpg"), instance);
    }

    /* hashCode() */

    @Test
    void hashCodeWithUnequalInstances() {
        assertNotEquals(new OutputFormat("tif").hashCode(), instance.hashCode());
    }

    @Test
    void hashCodeWithEqualInstances() {
        assertEquals(new OutputFormat("jpg").hashCode(), instance.hashCode());
    }

    /* toFormat() */

    @Test
    void toFormatWithUnsupportedFormat() {
        assertNull(new OutputFormat("bogus").toFormat());
    }

    @Test
    void toFormatWithSupportedFormat() {
        assertEquals(Format.get("jpg"), new OutputFormat("jpg").toFormat());
        assertEquals(Format.get("png"), new OutputFormat("png").toFormat());
    }

    /* toString() */

    @Test
    void testToString() {
        assertEquals("jpg", instance.toString());
    }

}
