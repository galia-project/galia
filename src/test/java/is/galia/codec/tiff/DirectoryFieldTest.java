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

import java.util.List;

import static is.galia.codec.tiff.EXIFBaselineTIFFTagSet.*;
import static is.galia.codec.tiff.EXIFTagSet.*;
import static org.junit.jupiter.api.Assertions.*;

class DirectoryFieldTest extends BaseTest {

    private DirectoryField instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Directory exifIFD = new Directory(new EXIFTagSet());
        exifIFD.add(SCENE_TYPE, DataType.UNDEFINED, new byte[] { 0x01 });
        instance = new DirectoryField(EXIF_IFD_POINTER, exifIFD);
    }

    /* DirectoryField(Tag, Directory) */

    @Test
    void constructor2WithNullDirectoryArgument() {
        assertThrows(NullPointerException.class,
                () -> new DirectoryField(EXIF_IFD_POINTER, null));
    }

    /* compareTo() */

    @Test
    void compareToWithLesserTagID() {
        Field other = new DirectoryField(new Tag(5, "Test", true));
        assertEquals(1, instance.compareTo(other));
    }

    @Test
    void compareToWithEqualTagID() {
        Field other = new DirectoryField(EXIF_IFD_POINTER);
        assertEquals(0, instance.compareTo(other));
    }

    @Test
    void compareToWithGreaterTagID() {
        Field other = new DirectoryField(GPS_IFD_POINTER);
        assertEquals(-1, instance.compareTo(other));
    }

    /* equals() */

    @Test
    void equalsWithUnequalValues() {
        Directory exifIFD = new Directory(new EXIFTagSet());
        exifIFD.add(PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, List.of(50));
        Field other = new DirectoryField(EXIF_IFD_POINTER, exifIFD);
        assertNotEquals(instance, other);
    }

    @Test
    void equalsWithEqualInstances() {
        Directory exifIFD = new Directory(new EXIFTagSet());
        exifIFD.add(SCENE_TYPE, DataType.UNDEFINED, new byte[] { 0x01 });
        Field other = new DirectoryField(EXIF_IFD_POINTER, exifIFD);
        assertEquals(instance, other);
    }

    /* getFirstValue() */

    @Test
    void getFirstValue() {
        assertNotNull(instance.getFirstValue());
    }

    /* getValues() */

    @Test
    void getValues() {
        assertEquals(List.of(instance.getFirstValue()), instance.getValues());
    }

    @Test
    void getValuesReturnsUnmodifiableInstance() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.getValues().clear());
    }

    /* hashCode() */

    @Test
    void hashCodeWithEqualInstances() {
        Directory exifIFD = new Directory(new EXIFTagSet());
        exifIFD.add(SCENE_TYPE, DataType.UNDEFINED, new byte[] { 0x01 });
        Field other = new DirectoryField(EXIF_IFD_POINTER, exifIFD);
        assertEquals(instance.hashCode(), other.hashCode());
    }

    @Test
    void hashCodeWithUnequalInstances() {
        Directory exifIFD = new Directory(new EXIFTagSet());
        exifIFD.add(PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, List.of(50));
        Field other = new DirectoryField(EXIF_IFD_POINTER, exifIFD);
        assertNotEquals(instance.hashCode(), other.hashCode());
    }

    /* setValue() */

    @Test
    void setValue() {
        Directory exifIFD = new Directory(new EXIFTagSet());
        instance.setDirectory(exifIFD);
        assertEquals(1, instance.getValues().size());
        assertEquals(exifIFD, instance.getFirstValue());
    }

    /* toString() */

    @Test
    void testToString() {
        assertEquals(instance.getTag() + " <IFD>", instance.toString());
    }

}
