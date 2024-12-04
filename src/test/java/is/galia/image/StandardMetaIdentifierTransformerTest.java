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
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StandardMetaIdentifierTransformerTest extends BaseTest {

    private StandardMetaIdentifierTransformer instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new StandardMetaIdentifierTransformer();
    }

    /* deserialize() */

    @Test
    void deserializeWithIdentifier() {
        MetaIdentifier metaID = instance.deserialize("cats cats.jp2;1;2:3;cats");
        assertEquals(new Identifier("cats cats.jp2;1;2:3;cats"),
                metaID.identifier());
        assertNull(metaID.pageNumber());
        assertNull(metaID.scaleConstraint());
    }

    @Test
    void deserializeWithIdentifierAndPageNumber() {
        MetaIdentifier metaID = instance.deserialize("cats;cats.jp2;3");
        assertEquals(new Identifier("cats;cats.jp2"), metaID.identifier());
        assertEquals(3, metaID.pageNumber());
        assertNull(metaID.scaleConstraint());
    }

    @Test
    void deserializeWithIdentifierAndScaleConstraint() {
        MetaIdentifier metaID = instance.deserialize("cats;cats.jp2;1:2");
        assertEquals(new Identifier("cats;cats.jp2"), metaID.identifier());
        assertNull(metaID.pageNumber());
        assertEquals(new ScaleConstraint(1, 2), metaID.scaleConstraint());
    }

    @Test
    void deserializeWithIdentifierAndPageNumberAndScaleConstraint() {
        MetaIdentifier metaID = instance.deserialize("cats;cats.jp2;3;1:2");
        assertEquals(new Identifier("cats;cats.jp2"), metaID.identifier());
        assertEquals(3, metaID.pageNumber());
        assertEquals(new ScaleConstraint(1, 2), metaID.scaleConstraint());
    }

    @Test
    void deserializeRespectsMetaIdentifierDelimiter() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.STANDARD_META_IDENTIFIER_TRANSFORMER_DELIMITER, "CATS");
        MetaIdentifier metaID = instance.deserialize("catsCATS2CATS2:3");
        assertEquals(new Identifier("cats"), metaID.identifier());
        assertEquals(2, metaID.pageNumber());
        assertEquals(new ScaleConstraint(2, 3), metaID.scaleConstraint());
    }

    /* serialize(MetaIdentifier) */

    @Test
    void serialize1WithIdentifier() {
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .build();
        assertEquals("cats;cats", instance.serialize(meta));
    }

    @Test
    void serialize1WithIdentifierAndPageNumber() {
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .withPageNumber(3)
                .build();
        assertEquals("cats;cats;3", instance.serialize(meta));
    }

    @Test
    void serialize1OmitsPage1() {
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .withPageNumber(1)
                .build();
        assertEquals("cats;cats", instance.serialize(meta));
    }

    @Test
    void serialize1WithIdentifierAndScaleConstraint() {
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .withScaleConstraint(2, 3)
                .build();
        assertEquals("cats;cats;2:3", instance.serialize(meta));
    }

    @Test
    void serialize1OmitsNoOpScaleConstraint() {
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .withScaleConstraint(2, 2)
                .build();
        assertEquals("cats;cats", instance.serialize(meta));
    }

    @Test
    void serialize1WithIdentifierAndPageNumberAndScaleConstraint() {
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .withPageNumber(3)
                .withScaleConstraint(2, 3)
                .build();
        assertEquals("cats;cats;3;2:3", instance.serialize(meta));
    }

    @Test
    void serialize1RespectsMetaIdentifierDelimiter() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.STANDARD_META_IDENTIFIER_TRANSFORMER_DELIMITER, "DOGS");
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .withPageNumber(3)
                .withScaleConstraint(2, 3)
                .build();
        assertEquals("cats;catsDOGS3DOGS2:3", instance.serialize(meta));
    }

    /* serialize(MetaIdentifier, boolean) */

    @Test
    void serialize2IncludesPage1WhenNotNormalizing() {
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .withPageNumber(1)
                .build();
        assertEquals("cats;cats;1", instance.serialize(meta, false));
    }

    @Test
    void serialize2IncludesNoOpScaleConstraintWhenNotNormalizing() {
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .withScaleConstraint(2, 2)
                .build();
        assertEquals("cats;cats;2:2", instance.serialize(meta, false));
    }

}