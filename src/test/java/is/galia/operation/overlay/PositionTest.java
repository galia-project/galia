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

package is.galia.operation.overlay;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PositionTest extends BaseTest {

    @Test
    void testFromString() {
        assertEquals(Position.TOP_LEFT, Position.fromString("top left"));
        assertEquals(Position.TOP_CENTER, Position.fromString("top center"));
        assertEquals(Position.TOP_RIGHT, Position.fromString("top right"));
        assertEquals(Position.LEFT_CENTER, Position.fromString("left center"));
        assertEquals(Position.CENTER, Position.fromString("center"));
        assertEquals(Position.RIGHT_CENTER, Position.fromString("right CENTER"));
        assertEquals(Position.BOTTOM_LEFT, Position.fromString("BOTTOM left"));
        assertEquals(Position.BOTTOM_CENTER, Position.fromString("bottom center"));
        assertEquals(Position.BOTTOM_RIGHT, Position.fromString("bottom right"));
        assertEquals(Position.TOP_LEFT, Position.fromString("left top"));
        assertEquals(Position.TOP_CENTER, Position.fromString("center top"));
        assertEquals(Position.TOP_RIGHT, Position.fromString("right top"));
        assertEquals(Position.REPEAT, Position.fromString("repeat"));
    }

    @Test
    void testToString() {
        assertEquals("N", Position.TOP_CENTER.toString());
        assertEquals("NE", Position.TOP_RIGHT.toString());
        assertEquals("E", Position.RIGHT_CENTER.toString());
        assertEquals("SE", Position.BOTTOM_RIGHT.toString());
        assertEquals("S", Position.BOTTOM_CENTER.toString());
        assertEquals("SW", Position.BOTTOM_LEFT.toString());
        assertEquals("W", Position.LEFT_CENTER.toString());
        assertEquals("NW", Position.TOP_LEFT.toString());
        assertEquals("C", Position.CENTER.toString());
        assertEquals("REPEAT", Position.REPEAT.toString());
    }

}
