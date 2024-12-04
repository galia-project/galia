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

class TagSetTest extends BaseTest {

    private TagSet instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new TagSet(0, "Baseline TIFF");
    }

    /* forIFDPointerTag() */

    @Test
    void forIFDPointerTag() {
        assertEquals(new EXIFBaselineTIFFTagSet(),
                TagSet.forIFDPointerTag(IFD_POINTER));
        assertEquals(new EXIFTagSet(),
                TagSet.forIFDPointerTag(EXIFTagSet.IFD_POINTER));
        assertEquals(new EXIFGPSTagSet(),
                TagSet.forIFDPointerTag(EXIFGPSTagSet.IFD_POINTER));
        assertEquals(new EXIFInteroperabilityTagSet(),
                TagSet.forIFDPointerTag(EXIFInteroperabilityTagSet.IFD_POINTER));
    }

    /* newBaselineSuperset() */

    @Test
    void newBaselineSuperset() {
        TagSet tagSet = TagSet.newBaselineSuperset();
        new BaselineTIFFTagSet().getTags().forEach(tag -> assertTrue(tagSet.containsTag(tag.id())));
        new EXIFBaselineTIFFTagSet().getTags().forEach(tag -> assertTrue(tagSet.containsTag(tag.id())));
        new GeoTIFFTagSet().getTags().forEach(tag -> assertTrue(tagSet.containsTag(tag.id())));
        new TIFFFXTagSet().getTags().forEach(tag -> assertTrue(tagSet.containsTag(tag.id())));
    }

    /* addTag(Tag) */

    @Test
    void addTag() {
        Tag tag = new Tag(99999, "Tag", false);
        instance.addTag(tag);
        assertTrue(instance.containsTag(tag.id()));
    }

    /* containsTag(int) */

    @Test
    void containsTagWithMissingTag() {
        instance.removeAllTags();
        assertFalse(instance.containsTag(MAKE.id()));
    }

    @Test
    void containsTagWithPresentTag() {
        Tag tag = new Tag(1, "Cats", false);
        instance.addTag(tag);
        assertTrue(instance.containsTag(tag.id()));
    }

    /* equals() */

    @Test
    void equalsWithUnequalNames() {
        TagSet other = new TagSet(instance.getIFDPointerTag(), "Other");
        assertNotEquals(instance, other);
    }

    @Test
    void equalsWithUnequalIFDPointers() {
        TagSet other = new TagSet(9999, instance.getName());
        assertNotEquals(instance, other);
    }

    @Test
    void equalsWithUnequalTags() {
        TagSet other = new EXIFBaselineTIFFTagSet();
        other.removeAllTags();
        assertEquals(instance, other);
    }

    @Test
    void equalsWithAllEqualProperties() {
        TagSet other = new EXIFBaselineTIFFTagSet();
        assertEquals(instance, other);
    }

    /* getIFDPointerTag() */

    @Test
    void getIFDPointerTag() {
        assertEquals(0, instance.getIFDPointerTag());
    }

    /* getName() */

    @Test
    void getName() {
        assertEquals("Baseline TIFF", instance.getName());
    }

    /* getTag(int) */

    @Test
    void getTagWithMissingTag() {
        assertNull(instance.getTag(9999999));
    }

    @Test
    void getTagPresentTag() {
        Tag tag = new Tag(1, "Cats", false);
        instance.addTag(tag);
        assertSame(tag, instance.getTag(1));
    }

    /* getTags() */

    @Test
    void getTags() {
        instance.addTag(new Tag(1, "Cats", false));
        assertEquals(1, instance.getTags().size());
    }

    @Test
    void getTagsOrderedByTagID() {
        int lastID = 0;
        for (Tag tag : instance.getTags()) {
            assertTrue(tag.id() > lastID);
            lastID = tag.id();
        }
    }

    /* hashCode() */

    @Test
    void hashCodeWithUnequalNames() {
        TagSet other = new TagSet(instance.getIFDPointerTag(), "Other");
        assertNotEquals(instance.hashCode(), other.hashCode());
    }

    @Test
    void hashCodeWithUnequalIFDPointers() {
        TagSet other = new TagSet(9999, instance.getName());
        assertNotEquals(instance.hashCode(), other.hashCode());
    }

    @Test
    void hashCodeWithUnequalTags() {
        TagSet other = new EXIFBaselineTIFFTagSet();
        other.removeAllTags();
        assertEquals(instance.hashCode(), other.hashCode());
    }

    @Test
    void hashCodeWithAllEqualProperties() {
        TagSet other = new EXIFBaselineTIFFTagSet();
        assertEquals(instance.hashCode(), other.hashCode());
    }

    /* merge() */

    @Test
    void merge() {
        TagSet other = new TIFFFXTagSet();
        instance.merge(other);
        assertEquals(other.getTags().size(), instance.getTags().size());
    }

    /* removeAllTags() */

    @Test
    void removeAllTags() {
        instance.addTag(new Tag(1, "Cats", false));
        assertFalse(instance.getTags().isEmpty());
        instance.removeAllTags();
        assertTrue(instance.getTags().isEmpty());
    }

    /* toString() */

    @Test
    void testToString() {
        assertEquals(
                instance.getName() + " (" + instance.getIFDPointerTag() + ")",
                instance.toString());
    }

}