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

class MultiValueFieldTest extends BaseTest {

    private MultiValueField instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new MultiValueField(
                TEMPERATURE, DataType.LONG, List.of(80));
    }

    /* MultiValueField(Tag, DataType) */

    @Test
    void constructor1WithNullTagArgument() {
        assertThrows(NullPointerException.class,
                () -> new MultiValueField(null, DataType.LONG));
    }

    @Test
    void constructor1WithNullDataTypeArgument() {
        assertThrows(NullPointerException.class,
                () -> new MultiValueField(MAKE, null));
    }

    @Test
    void constructor1WithBYTEDataType() {
        assertThrows(IllegalArgumentException.class,
                () -> new MultiValueField(MAKE, DataType.BYTE));
    }

    @Test
    void constructor1WithSBYTEDataType() {
        assertThrows(IllegalArgumentException.class,
                () -> new MultiValueField(MAKE, DataType.SBYTE));
    }

    @Test
    void constructor1WithUNDEFINEDDataType() {
        assertThrows(IllegalArgumentException.class,
                () -> new MultiValueField(MAKE, DataType.UNDEFINED));
    }

    @Test
    void constructor1WithASCIIDataType() {
        assertThrows(IllegalArgumentException.class,
                () -> new MultiValueField(MAKE, DataType.ASCII));
    }

    @Test
    void constructor1WithUTF8DataType() {
        assertThrows(IllegalArgumentException.class,
                () -> new MultiValueField(MAKE, DataType.UTF8));
    }

    /* MultiValueField(Tag, DataType, List<Object>) */

    @Test
    void constructor2WithNullTagArgument() {
        assertThrows(NullPointerException.class,
                () -> new MultiValueField(null, DataType.LONG, List.of(80)));
    }

    @Test
    void constructor2WithNullDataTypeArgument() {
        assertThrows(NullPointerException.class,
                () -> new MultiValueField(MAKE, null, List.of(80)));
    }

    @Test
    void constructor2WithBYTEDataType() {
        assertThrows(IllegalArgumentException.class,
                () -> new MultiValueField(MAKE, DataType.BYTE, List.of(80)));
    }

    @Test
    void constructor2WithSBYTEDataType() {
        assertThrows(IllegalArgumentException.class,
                () -> new MultiValueField(MAKE, DataType.SBYTE, List.of(80)));
    }

    @Test
    void constructor2WithUNDEFINEDDataType() {
        assertThrows(IllegalArgumentException.class,
                () -> new MultiValueField(MAKE, DataType.UNDEFINED, List.of(80)));
    }

    @Test
    void constructor2WithASCIIDataType() {
        assertThrows(IllegalArgumentException.class,
                () -> new MultiValueField(MAKE, DataType.ASCII, List.of(80)));
    }

    @Test
    void constructor2WithUTF8DataType() {
        assertThrows(IllegalArgumentException.class,
                () -> new MultiValueField(MAKE, DataType.UTF8, List.of(80)));
    }

    @Test
    void constructor2WithNullValueArgument() {
        assertThrows(NullPointerException.class,
                () -> new MultiValueField(MAKE, DataType.LONG, null));
    }

    /* addValue() */

    @Test
    void addValue() {
        instance.addValue(75);
        assertEquals(2, instance.getValues().size());
        assertEquals(75, instance.getValues().get(1));
    }

    /* compareTo() */

    @Test
    void compareToWithLesserTagID() {
        Field other = new MultiValueField(USER_COMMENT, DataType.LONG);
        assertEquals(1, instance.compareTo(other));
    }

    @Test
    void compareToWithEqualTagID() {
        Field other = new MultiValueField(TEMPERATURE, DataType.LONG);
        assertEquals(0, instance.compareTo(other));
    }

    @Test
    void compareToWithGreaterTagID() {
        Field other = new MultiValueField(WATER_DEPTH, DataType.LONG);
        assertEquals(-1, instance.compareTo(other));
    }

    /* equals() */

    @Test
    void equalsWithUnequalValues() {
        Field other = new MultiValueField(
                TEMPERATURE, DataType.LONG, List.of(80, 75));
        assertNotEquals(instance, other);
    }

    @Test
    void equalsWithEqualInstances() {
        Field other = new MultiValueField(
                TEMPERATURE, DataType.LONG, List.of(80));
        assertEquals(instance, other);
    }

    /* getFirstValue() */

    @Test
    void getFirstValue() {
        assertEquals(80, instance.getFirstValue());
    }

    /* getValues() */

    @Test
    void getValues() {
        assertEquals(List.of(80), instance.getValues());
    }

    @Test
    void getValuesReturnsUnmodifiableInstance() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.getValues().clear());
    }

    /* hashCode() */

    @Test
    void hashCodeWithEqualInstances() {
        Field other = new MultiValueField(
                TEMPERATURE, DataType.LONG, List.of(80));
        assertEquals(instance.hashCode(), other.hashCode());
    }

    @Test
    void hashCodeWithUnequalInstances() {
        Field other = new MultiValueField(
                TEMPERATURE, DataType.LONG, List.of(80, 75));
        assertNotEquals(instance.hashCode(), other.hashCode());
    }

    /* toString() */

    @Test
    void testToString() {
        instance.addValue(75);
        assertEquals(instance.getTag() + ": " +
                        instance.getFirstValue() + ", " +
                        instance.getValues().get(1),
                instance.toString());
    }

}
