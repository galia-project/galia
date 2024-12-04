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

class StringFieldTest extends BaseTest {

    private StringField instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new StringField(
                DATE_TIME_ORIGINAL, DataType.ASCII, "2020-01-01");
    }

    /* StringField(Tag, DataType) */

    @Test
    void constructor1WithNullTagArgument() {
        assertThrows(NullPointerException.class,
                () -> new StringField(null, DataType.ASCII));
    }

    @Test
    void constructor1WithNullDataTypeArgument() {
        assertThrows(NullPointerException.class,
                () -> new StringField(MAKE, null));
    }

    @Test
    void constructor1WithIllegalDataTypeArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new StringField(MAKE, DataType.LONG));
    }

    /* StringField(Tag, DataType, String) */

    @Test
    void constructor2WithNullTagArgument() {
        assertThrows(NullPointerException.class,
                () -> new StringField(null, DataType.ASCII, "cats"));
    }

    @Test
    void constructor2WithNullDataTypeArgument() {
        assertThrows(NullPointerException.class,
                () -> new StringField(MAKE, null, "cats"));
    }

    @Test
    void constructor2WithIllegalDataTypeArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new StringField(MAKE, DataType.RATIONAL, "cats"));
    }

    @Test
    void constructor2WithNullValueArgument() {
        assertThrows(NullPointerException.class,
                () -> new StringField(MAKE, DataType.ASCII, null));
    }

    /* compareTo() */

    @Test
    void compareToWithLesserTagID() {
        Field other = new StringField(DATE_TIME_DIGITIZED, DataType.ASCII);
        assertEquals(-1, instance.compareTo(other));
    }

    @Test
    void compareToWithEqualTagID() {
        Field other = new StringField(DATE_TIME_ORIGINAL, DataType.ASCII);
        assertEquals(0, instance.compareTo(other));
    }

    @Test
    void compareToWithGreaterTagID() {
        Field other = new StringField(DATE_TIME_DIGITIZED, DataType.ASCII);
        assertEquals(1, other.compareTo(instance));
    }

    /* equals() */

    @Test
    void equalsWithUnequalValues() {
        Field other = new StringField(
                DATE_TIME, DataType.ASCII, "2020-01-02");
        assertNotEquals(instance, other);
    }

    @Test
    void equalsWithEqualInstances() {
        Field other = new StringField(
                DATE_TIME_ORIGINAL, DataType.ASCII, "2020-01-01");
        assertEquals(instance, other);
    }

    /* getFirstValue() */

    @Test
    void getFirstValue() {
        assertEquals("2020-01-01", instance.getFirstValue());
    }

    /* getValues() */

    @Test
    void getValues() {
        assertEquals(List.of("2020-01-01"), instance.getValues());
    }

    @Test
    void getValuesReturnsUnmodifiableInstance() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.getValues().clear());
    }

    /* hashCode() */

    @Test
    void hashCodeWithEqualInstances() {
        Field other = new StringField(
                DATE_TIME_ORIGINAL, DataType.ASCII, "2020-01-01");
        assertEquals(instance.hashCode(), other.hashCode());
    }

    @Test
    void hashCodeWithUnequalInstances() {
        Field other = new StringField(
                DATE_TIME, DataType.ASCII, "2020-01-02");
        assertNotEquals(instance.hashCode(), other.hashCode());
    }

    /* setValue() */

    @Test
    void setValue() {
        String value = "cats";
        instance.setValue(value);
        assertEquals(1, instance.getValues().size());
        assertEquals(value, instance.getFirstValue());
    }

    /* toString() */

    @Test
    void testToString() {
        assertEquals(instance.getTag() + ": " + instance.getFirstValue(),
                instance.toString());
    }

}
