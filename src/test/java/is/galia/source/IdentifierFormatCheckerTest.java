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

package is.galia.source;

import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdentifierFormatCheckerTest extends BaseTest {

    @Test
    void testCheckWithKnownFormat() {
        var instance = new IdentifierFormatChecker(new Identifier("cats.jpg"));
        assertEquals(Format.get("jpg"), instance.check());
    }

    @Test
    void testCheckWithUnknownFormat() {
        var instance = new IdentifierFormatChecker(new Identifier("cats"));
        assertEquals(Format.UNKNOWN, instance.check());
    }

}
