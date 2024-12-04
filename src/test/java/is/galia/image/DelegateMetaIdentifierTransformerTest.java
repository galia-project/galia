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

import is.galia.delegate.Delegate;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DelegateMetaIdentifierTransformerTest extends BaseTest {

    private DelegateMetaIdentifierTransformer instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Delegate delegate = TestUtils.newDelegate();
        instance = new DelegateMetaIdentifierTransformer();
        instance.setDelegate(delegate);
    }

    /* deserialize() */

    @Test
    void testDeserializeWithIdentifier() {
        MetaIdentifier expected = MetaIdentifier.builder()
                .withIdentifier("cats")
                .build();
        MetaIdentifier actual = instance.deserialize("cats");
        assertEquals(expected, actual);
    }

    @Test
    void testDeserializeWithIdentifierAndPageNumber() {
        MetaIdentifier expected = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withPageNumber(3)
                .build();
        MetaIdentifier actual = instance.deserialize("cats;3");
        assertEquals(expected, actual);
    }

    @Test
    void testDeserializeWithIdentifierAndScaleConstraint() {
        MetaIdentifier expected = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withScaleConstraint(3, 4)
                .build();
        MetaIdentifier actual = instance.deserialize("cats;3:4");
        assertEquals(expected, actual);
    }

    @Test
    void testDeserializeWithAllComponents() {
        MetaIdentifier expected = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withPageNumber(3)
                .withScaleConstraint(3, 4)
                .build();
        MetaIdentifier actual = instance.deserialize("cats;3;3:4");
        assertEquals(expected, actual);
    }

    /* serialize() */

    @Test
    void testSerializeWithIdentifier() {
        MetaIdentifier metaID = MetaIdentifier.builder()
                .withIdentifier("cats")
                .build();
        assertEquals("cats", instance.serialize(metaID));
    }

    @Test
    void testSerializeWithIdentifierAndPageNumber() {
        MetaIdentifier metaID = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withPageNumber(3)
                .build();
        assertEquals("cats;3", instance.serialize(metaID));
    }

    @Test
    void testSerializeWithIdentifierAndScaleConstraint() {
        MetaIdentifier metaID = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withScaleConstraint(3, 4)
                .build();
        assertEquals("cats;3:4", instance.serialize(metaID));
    }

    @Test
    void testSerializeWithAllComponents() {
        MetaIdentifier metaID = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withPageNumber(3)
                .withScaleConstraint(3, 4)
                .build();
        assertEquals("cats;3;3:4", instance.serialize(metaID));
    }

}