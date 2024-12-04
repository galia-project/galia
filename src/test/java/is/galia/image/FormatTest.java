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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FormatTest extends BaseTest {

    @Test
    void all() {
        final Set<Format> knownSupported = Set.of(
                Format.get("bmp"), Format.get("gif"), Format.get("jpg"),
                Format.get("png"), Format.get("tif"));
        final Set<Format> all = Format.all();
        knownSupported.forEach(f -> assertTrue(all.contains(f)));
    }

    @Test
    void getWithValidKey() {
        assertEquals(FormatRegistry.formatWithKey("jpg"), Format.get("jpg"));
    }

    @Test
    void getWithInvalidKey() {
        assertNull(Format.get("bogus"));
    }

    @Test
    void inferFormatWithIdentifier() {
        // BMP
        assertEquals(Format.get("bmp"),
                Format.inferFormat(new Identifier("bla.bmp")));
        // GIF
        assertEquals(Format.get("gif"),
                Format.inferFormat(new Identifier("bla.gif")));
        // JPG
        assertEquals(Format.get("jpg"),
                Format.inferFormat(new Identifier("bla.jpg")));
        // PNG
        assertEquals(Format.get("png"),
                Format.inferFormat(new Identifier("bla.png")));
        // TIF
        assertEquals(Format.get("tif"),
                Format.inferFormat(new Identifier("bla.tif")));
    }

    @Test
    void inferFormatWithString() {
        // BMP
        assertEquals(Format.get("bmp"), Format.inferFormat("bla.bmp"));
        // GIF
        assertEquals(Format.get("gif"), Format.inferFormat("bla.gif"));
        // JPG
        assertEquals(Format.get("jpg"), Format.inferFormat("bla.jpg"));
        // PNG
        assertEquals(Format.get("png"), Format.inferFormat("bla.png"));
        // TIF
        assertEquals(Format.get("tif"), Format.inferFormat("bla.tif"));
        // UNKNOWN
        assertEquals(Format.UNKNOWN, Format.inferFormat("bla.bogus"));
    }

    @Test
    void withExtensionAndAMatch() {
        assertEquals(Format.get("jpg"), Format.withExtension("jpg"));
        assertEquals(Format.get("jpg"), Format.withExtension(".jpg"));
        assertEquals(Format.get("jpg"), Format.withExtension("JPG"));
        assertEquals(Format.get("jpg"), Format.withExtension(".JPG"));
    }

    @Test
    void withExtensionAndNoMatch() {
        assertNull(Format.withExtension("bogus"));
    }

    @Test
    void compareTo() {
        assertTrue(Format.get("jpg").compareTo(Format.get("tif")) < 0);
        assertEquals(0, Format.get("gif").compareTo(Format.get("gif")));
        assertTrue(Format.get("tif").compareTo(Format.get("gif")) > 0);
    }

    @Test
    void equalsWithEqualInstances() {
        assertEquals(Format.get("jpg"), Format.get("jpg"));
    }

    @Test
    void equalsWithUnequalInstances() {
        assertNotEquals(Format.get("jpg"), Format.get("tif"));
    }

    @Test
    void getPreferredExtension() {
        assertEquals("bmp", Format.get("bmp").getPreferredExtension());
        assertEquals("gif", Format.get("gif").getPreferredExtension());
        assertEquals("jpg", Format.get("jpg").getPreferredExtension());
        assertEquals("png", Format.get("png").getPreferredExtension());
        assertEquals("tif", Format.get("tif").getPreferredExtension());
        assertEquals("unknown", Format.UNKNOWN.getPreferredExtension());
    }

    @Test
    void getPreferredMediaType() {
        assertEquals("image/bmp",
                Format.get("bmp").getPreferredMediaType().toString());
        assertEquals("image/gif",
                Format.get("gif").getPreferredMediaType().toString());
        assertEquals("image/jpeg",
                Format.get("jpg").getPreferredMediaType().toString());
        assertEquals("image/png",
                Format.get("png").getPreferredMediaType().toString());
        assertEquals("image/tiff",
                Format.get("tif").getPreferredMediaType().toString());
        assertEquals("unknown/unknown",
                Format.UNKNOWN.getPreferredMediaType().toString());
    }

    @Test
    void hashCodeWithEqualInstances() {
        assertEquals(Format.get("jpg").hashCode(), Format.get("jpg").hashCode());
    }

    @Test
    void hashCodeWithUnequalInstances() {
        assertNotEquals(Format.get("jpg").hashCode(), Format.get("tif").hashCode());
    }

    @Test
    void testToString() {
        for (Format format : Format.all()) {
            assertEquals(format.getPreferredExtension(),
                    format.toString());
        }
    }

}
