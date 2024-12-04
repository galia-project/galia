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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class FormatRegistryTest extends BaseTest {

    //region setup

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        FormatRegistry.reset();
    }

    //endregion
    //region addFormat()

    @Test
    void addFormatAddsAFormat() {
        Format format = new Format("new", "New Format",
                Collections.emptyList(),
                Collections.emptyList(),
                true,
                false,
                false);
        FormatRegistry.addFormat(format);
        assertSame(format, FormatRegistry.formatWithKey("new"));
    }

    //endregion
    //region allFormats()

    @Test
    void allFormatsReturnsAllFormats() {
        assertTrue(FormatRegistry.allFormats().size() > 2);
    }

    @Test
    void allFormatsReturnedSetIsImmutable() {
        assertThrows(UnsupportedOperationException.class,
                () -> FormatRegistry.allFormats().clear());
    }

    //endregion
    //region containsMediaType()

    @Test
    void containsMediaTypeWithHit() {
        Format format = FormatRegistry.formatWithKey("jpg");
        assertTrue(FormatRegistry.containsMediaType(format.getPreferredMediaType()));
    }

    @Test
    void containsMediaTypeWithMiss() {
        assertFalse(FormatRegistry.containsMediaType(new MediaType("bogus", "bogus")));
    }

    //endregion
    //region formatWithKey()

    @Test
    void formatWithKeyWithRecognizedKey() {
        Format format = FormatRegistry.formatWithKey("jpg");
        assertEquals("JPEG", format.name());
    }

    @Test
    void formatWithKeyWithUnrecognizedKey() {
        Format format = FormatRegistry.formatWithKey("bogus");
        assertNull(format);
    }

    //endregion

}
