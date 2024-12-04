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

package is.galia.codec.tiff;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static is.galia.codec.tiff.EXIFBaselineTIFFTagSet.*;
import static org.junit.jupiter.api.Assertions.*;

class TagTest extends BaseTest {

    private Tag instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = SOFTWARE;
    }

    @Test
    void compareToWithLesserTagID() {
        assertEquals(1, instance.compareTo(ROWS_PER_STRIP));
    }

    @Test
    void compareToWithEqualTagID() {
        assertEquals(0, instance.compareTo(instance));
    }

    @Test
    void compareToWithGreaterTagID() {
        assertEquals(-1, instance.compareTo(WHITE_POINT));
    }

}
