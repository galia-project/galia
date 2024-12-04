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

import com.fasterxml.jackson.databind.ObjectMapper;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

class MediaTypeTest extends BaseTest {

    private MediaType instance;

    //region setup

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new MediaType("image", "jpeg");
    }

    //endregion
    //region serialization

    @Test
    void serialization() throws IOException {
        MediaType type = new MediaType("image", "jpeg");
        try (StringWriter writer = new StringWriter()) {
            new ObjectMapper().writeValue(writer, type);
            assertEquals("\"image/jpeg\"", writer.toString());
        }
    }

    @Test
    void deserialization() throws IOException {
        MediaType type = new ObjectMapper().readValue("\"image/jpeg\"",
                MediaType.class);
        assertEquals("image/jpeg", type.toString());
    }

    //endregion
    //region fromContentType()

    @Test
    void fromContentType() {
        assertEquals(new MediaType("image", "jp2"),
                MediaType.fromContentType("image/jp2"));
        assertEquals(new MediaType("image", "jp2"),
                MediaType.fromContentType("image/jp2; charset=UTF-8"));
    }

    //endregion
    //region fromString(String)

    @Test
    void fromStringWithValidString() {
        instance = MediaType.fromString("image/jpeg");
        assertEquals("image/jpeg", instance.toString());
    }

    @Test
    void fromStringWithInvalidString() {
        assertThrows(IllegalArgumentException.class,
                () -> MediaType.fromString("cats"));
    }

    //endregion
    //region toFormat()

    @Test
    void toFormat() {
        assertEquals(Format.get("bmp"), new MediaType("image", "bmp").toFormat());
        assertEquals(Format.get("gif"), new MediaType("image", "gif").toFormat());
        assertEquals(Format.get("jpg"), new MediaType("image", "jpeg").toFormat());
        assertEquals(Format.get("png"), new MediaType("image", "png").toFormat());
        assertEquals(Format.get("tif"), new MediaType("image", "tiff").toFormat());
    }

    //endregion
    //region toString()

    @Test
    void testToString() {
        assertEquals("image/jpeg", instance.toString());
    }

}
