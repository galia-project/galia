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
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

public class IdentifierTest extends BaseTest {

    private Identifier instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Identifier("cats");
    }

    @Test
    void testJSONSerialization() throws Exception {
        Identifier identifier = new Identifier("cats");
        try (StringWriter writer = new StringWriter()) {
            new ObjectMapper().writeValue(writer, identifier);
            assertEquals("\"cats\"", writer.toString());
        }
    }

    @Test
    void testJSONDeserialization() throws Exception {
        Identifier identifier = new ObjectMapper().readValue("\"cats\"",
                Identifier.class);
        assertEquals("cats", identifier.toString());
    }

    @Test
    void fromURI() {
        Configuration.forApplication().setProperty(Key.SLASH_SUBSTITUTE, "BUG");

        String pathComponent = "catsBUG%3Adogs";
        Identifier actual = Identifier.fromURI(pathComponent);
        Identifier expected = new Identifier("cats/:dogs");
        assertEquals(expected, actual);
    }

    @Test
    void constructor() {
        assertEquals("cats", instance.toString());
    }

    @Test
    void constructorWithNullArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new Identifier(null));
    }

    @Test
    void compareTo() {
        Identifier id1 = new Identifier("cats");
        Identifier id2 = new Identifier("dogs");
        Identifier id3 = new Identifier("cats");
        assertTrue(id1.compareTo(id2) < 0);
        assertEquals(0, id1.compareTo(id3));
    }

    @Test
    void equalsWithEqualInstances() {
        assertEquals(new Identifier("cats"), new Identifier("cats"));
    }

    @Test
    void equalsWithUnequalInstances() {
        assertNotEquals(new Identifier("cats"), new Identifier("dogs"));
    }

    @Test
    void testHashCode() {
        Identifier id1 = new Identifier("cats");
        Identifier id2 = new Identifier("cats");
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("cats", instance.toString());
    }

}
