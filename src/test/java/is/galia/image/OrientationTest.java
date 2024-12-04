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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrientationTest extends BaseTest {

    @Test
    void forTIFFOrientation() {
        assertEquals(Orientation.ROTATE_0, Orientation.forTIFFOrientation(1));
        assertEquals(Orientation.ROTATE_180, Orientation.forTIFFOrientation(3));
        assertEquals(Orientation.ROTATE_90, Orientation.forTIFFOrientation(6));
        assertEquals(Orientation.ROTATE_270, Orientation.forTIFFOrientation(8));

        for (int i : new int[] { 0, 2, 4, 5, 7, 9 }) {
            assertThrows(IllegalArgumentException.class,
                    () -> Orientation.forTIFFOrientation(i));
        }
    }

    @Test
    void adjustedSize() {
        final Size size = new Size(100, 50);
        assertSame(size, Orientation.ROTATE_0.adjustedSize(size));
        assertEquals(new Size(50, 100), Orientation.ROTATE_90.adjustedSize(size));
        assertSame(size, Orientation.ROTATE_180.adjustedSize(size));
        assertEquals(new Size(50, 100), Orientation.ROTATE_270.adjustedSize(size));
    }

    @Test
    void degrees() {
        assertEquals(0, Orientation.ROTATE_0.degrees());
        assertEquals(90, Orientation.ROTATE_90.degrees());
        assertEquals(180, Orientation.ROTATE_180.degrees());
        assertEquals(270, Orientation.ROTATE_270.degrees());
    }

}
