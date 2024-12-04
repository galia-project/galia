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

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.delegate.Delegate;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetaIdentifierTest extends BaseTest {

    @Nested
    class BuilderTest extends BaseTest {

        @Test
        void buildWithoutIdentifierThrowsException() {
            MetaIdentifier.Builder builder = MetaIdentifier.builder()
                    .withPageNumber(2)
                    .withScaleConstraint(1, 2);
            assertThrows(IllegalArgumentException.class, builder::build);
        }

        @Test
        void buildWithInvalidPageNumberThrowsException() {
            MetaIdentifier.Builder builder = MetaIdentifier.builder()
                    .withIdentifier("cats")
                    .withPageNumber(-1);
            assertThrows(IllegalArgumentException.class, builder::build);
        }

        @Test
        void build() {
            MetaIdentifier instance = MetaIdentifier.builder()
                    .withIdentifier("cats")
                    .withPageNumber(2)
                    .withScaleConstraint(1, 2)
                    .build();
            assertEquals(new Identifier("cats"), instance.identifier());
            assertEquals(2, instance.pageNumber());
            assertEquals(new ScaleConstraint(1, 2), instance.scaleConstraint());
        }

    }

    private MetaIdentifier instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withPageNumber(2)
                .withScaleConstraint(1, 2)
                .build();
    }

    /* fromString() */

    @Test
    void fromString() {
        final Configuration config =  Configuration.forApplication();
        config.setProperty(Key.META_IDENTIFIER_TRANSFORMER,
                StandardMetaIdentifierTransformer.class.getSimpleName());

        Delegate delegate       = TestUtils.newDelegate();
        String string           = "cats dogs;2;2:3";
        MetaIdentifier actual   = MetaIdentifier.fromString(string, delegate);
        MetaIdentifier expected = MetaIdentifier.builder()
                .withIdentifier("cats dogs")
                .withPageNumber(2)
                .withScaleConstraint(2, 3)
                .build();
        assertEquals(expected, actual);
    }

    /* fromURI() */

    @Test
    void fromURI() {
        final Configuration config = Configuration.forApplication();
        config.setProperty(Key.SLASH_SUBSTITUTE, "BUG");
        config.setProperty(Key.META_IDENTIFIER_TRANSFORMER,
                StandardMetaIdentifierTransformer.class.getSimpleName());

        Delegate delegate       = TestUtils.newDelegate();
        String pathComponent    = "catsBUG%3Adogs;2;2:3";
        MetaIdentifier actual   = MetaIdentifier
                .fromURI(pathComponent, delegate);
        MetaIdentifier expected = MetaIdentifier.builder()
                .withIdentifier("cats/:dogs")
                .withPageNumber(2)
                .withScaleConstraint(2, 3)
                .build();
        assertEquals(expected, actual);
    }

    /* MetaIdentifier(String) */

    @Test
    void stringConstructor() {
        instance = new MetaIdentifier("identifier");
        assertEquals("identifier", instance.identifier().toString());
        assertNull(instance.pageNumber());
        assertNull(instance.scaleConstraint());
    }

    /* MetaIdentifier(Identifier) */

    @Test
    void identifierConstructor() {
        Identifier identifier = new Identifier("identifier");
        instance = new MetaIdentifier(identifier);
        assertEquals(identifier, instance.identifier());
        assertNull(instance.pageNumber());
        assertNull(instance.scaleConstraint());
    }

    /* forURI() */

    @Test
    void forURI() {
        final Configuration config = Configuration.forApplication();
        config.setProperty(Key.SLASH_SUBSTITUTE, "BUG");
        config.setProperty(Key.META_IDENTIFIER_TRANSFORMER,
                StandardMetaIdentifierTransformer.class.getSimpleName());

        Delegate delegate = TestUtils.newDelegate();
        instance = MetaIdentifier.builder()
                .withIdentifier("cats/:dogs")
                .withPageNumber(2)
                .withScaleConstraint(2, 3)
                .build();
        String expected = "catsBUG:dogs;2;2:3";
        String actual   = instance.forURI(delegate);
        assertEquals(expected, actual);
    }

    @Test
    void rebuilder() {
        MetaIdentifier other = instance.rebuilder().build();
        assertEquals(instance, other);
    }

    /* toString() */

    @Test
    void testToString() {
        assertEquals("cats;2;1:2", instance.toString());
    }

}
