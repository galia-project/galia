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
import static is.galia.codec.tiff.EXIFTagSet.*;
import static org.junit.jupiter.api.Assertions.*;

class ByteArrayFieldTest extends BaseTest {

    private ByteArrayField instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new ByteArrayField(
                SCENE_TYPE, DataType.UNDEFINED, new byte[] { 0x01 });
    }

    /* StringField(Tag, DataType) */

    @Test
    void constructor1WithNullTagArgument() {
        assertThrows(NullPointerException.class,
                () -> new ByteArrayField(null, DataType.BYTE));
    }

    @Test
    void constructor1WithNullDataTypeArgument() {
        assertThrows(NullPointerException.class,
                () -> new ByteArrayField(MAKE, null));
    }

    @Test
    void constructor1WithIllegalDataTypeArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new ByteArrayField(MAKE, DataType.FLOAT));
    }

    /* ByteArrayField(Tag, DataType, byte[]) */

    @Test
    void constructor2WithNullTagArgument() {
        assertThrows(NullPointerException.class,
                () -> new ByteArrayField(null, DataType.BYTE, new byte[] { 0x01 }));
    }

    @Test
    void constructor2WithNullDataTypeArgument() {
        assertThrows(NullPointerException.class,
                () -> new ByteArrayField(MAKE, null, new byte[] { 0x01 }));
    }

    @Test
    void constructor2WithIllegalDataTypeArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new ByteArrayField(
                        MAKE, DataType.FLOAT, new byte[] { 0x01 }));
    }

    @Test
    void constructor2WithNullValueArgument() {
        assertThrows(NullPointerException.class,
                () -> new ByteArrayField(MAKE, DataType.BYTE, null));
    }

    /* compareTo() */

    @Test
    void compareToWithLesserTagID() {
        Field other = new ByteArrayField(FILE_SOURCE, DataType.UNDEFINED);
        assertEquals(1, instance.compareTo(other));
    }

    @Test
    void compareToWithEqualTagID() {
        Field other = new ByteArrayField(SCENE_TYPE, DataType.UNDEFINED);
        assertEquals(0, instance.compareTo(other));
    }

    @Test
    void compareToWithGreaterTagID() {
        Field other = new ByteArrayField(CFA_PATTERN, DataType.UNDEFINED);
        assertEquals(-1, instance.compareTo(other));
    }

    /* equals() */

    @Test
    void equalsWithUnequalValues() {
        Field other = new ByteArrayField(
                SCENE_TYPE, DataType.UNDEFINED, new byte[] { 0x02 });
        assertNotEquals(instance, other);
    }

    @Test
    void equalsWithEqualInstances() {
        Field other = new ByteArrayField(
                SCENE_TYPE, DataType.UNDEFINED, new byte[] { 0x01 });
        assertEquals(instance, other);
    }

    /* getFirstValue() */

    @Test
    void getFirstValue() {
        assertArrayEquals(new byte[] { 0x01 },
                (byte[]) instance.getFirstValue());
    }

    /* getValues() */

    @Test
    void getValues() {
        assertArrayEquals(new byte[] { 0x01 },
                (byte[]) instance.getValues().getFirst());
    }

    @Test
    void getValuesReturnsUnmodifiableInstance() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.getValues().clear());
    }

    /* hashCode() */

    @Test
    void hashCodeWithUnequalInstances() {
        Field other = new ByteArrayField(
                DATE_TIME, DataType.BYTE, new byte[] { 0x02 });
        assertNotEquals(instance.hashCode(), other.hashCode());
    }

    @Test
    void hashCodeWithEqualInstances() {
        Field other = new ByteArrayField(
                SCENE_TYPE, DataType.UNDEFINED, new byte[] { 0x01 });
        assertEquals(instance.hashCode(), other.hashCode());
    }

    /* setValue() */

    @Test
    void setValue() {
        byte[] value = new byte[] { 0x05 };
        instance.setValue(value);
        assertEquals(1, instance.getValues().size());
        assertArrayEquals(value, (byte[]) instance.getFirstValue());
    }

    /* toString() */

    @Test
    void testToString() {
        assertEquals(instance.getTag() + ": <" +
                ((byte[]) instance.getFirstValue()).length + " bytes>",
                instance.toString());
    }

}
