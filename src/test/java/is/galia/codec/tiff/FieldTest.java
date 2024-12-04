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
import org.junit.jupiter.api.Test;

import static is.galia.codec.tiff.EXIFBaselineTIFFTagSet.MODEL;
import static org.junit.jupiter.api.Assertions.*;

class FieldTest extends BaseTest {

    @Test
    void forTagWithASCIIDataType() {
        Tag tag           = MODEL;
        DataType dataType = DataType.ASCII;
        Field field       = Field.forTag(tag, dataType);
        assertInstanceOf(StringField.class, field);
        assertEquals(dataType, field.getDataType());
    }

    @Test
    void forTagWithBYTEDataType() {
        Tag tag           = MODEL;
        DataType dataType = DataType.BYTE;
        Field field       = Field.forTag(tag, dataType);
        assertInstanceOf(ByteArrayField.class, field);
        assertEquals(dataType, field.getDataType());
    }

    @Test
    void forTagWithSBYTEDataType() {
        Tag tag           = MODEL;
        DataType dataType = DataType.SBYTE;
        Field field       = Field.forTag(tag, dataType);
        assertInstanceOf(ByteArrayField.class, field);
        assertEquals(dataType, field.getDataType());
    }

    @Test
    void forTagWithFLOATDataType() {
        Tag tag           = MODEL;
        DataType dataType = DataType.FLOAT;
        Field field       = Field.forTag(tag, dataType);
        assertInstanceOf(MultiValueField.class, field);
        assertEquals(dataType, field.getDataType());
    }

    @Test
    void forTagWithDOUBLEDataType() {
        Tag tag           = MODEL;
        DataType dataType = DataType.DOUBLE;
        Field field       = Field.forTag(tag, dataType);
        assertInstanceOf(MultiValueField.class, field);
        assertEquals(dataType, field.getDataType());
    }

    @Test
    void forTagWithLONGDataType() {
        Tag tag           = MODEL;
        DataType dataType = DataType.LONG;
        Field field       = Field.forTag(tag, dataType);
        assertInstanceOf(MultiValueField.class, field);
        assertEquals(dataType, field.getDataType());
    }

    @Test
    void forTagWithSLONGDataType() {
        Tag tag           = MODEL;
        DataType dataType = DataType.SLONG;
        Field field       = Field.forTag(tag, dataType);
        assertInstanceOf(MultiValueField.class, field);
        assertEquals(dataType, field.getDataType());
    }

    @Test
    void forTagWithRATIONALDataType() {
        Tag tag           = MODEL;
        DataType dataType = DataType.RATIONAL;
        Field field       = Field.forTag(tag, dataType);
        assertInstanceOf(MultiValueField.class, field);
        assertEquals(dataType, field.getDataType());
    }

    @Test
    void forTagWithSRATIONALDataType() {
        Tag tag           = MODEL;
        DataType dataType = DataType.SRATIONAL;
        Field field       = Field.forTag(tag, dataType);
        assertInstanceOf(MultiValueField.class, field);
        assertEquals(dataType, field.getDataType());
    }

    @Test
    void forTagWithSHORTDataType() {
        Tag tag           = MODEL;
        DataType dataType = DataType.SHORT;
        Field field       = Field.forTag(tag, dataType);
        assertInstanceOf(MultiValueField.class, field);
        assertEquals(dataType, field.getDataType());
    }

    @Test
    void forTagWithSSHORTDataType() {
        Tag tag           = MODEL;
        DataType dataType = DataType.SSHORT;
        Field field       = Field.forTag(tag, dataType);
        assertInstanceOf(MultiValueField.class, field);
        assertEquals(dataType, field.getDataType());
    }

    @Test
    void forTagWithUNDEFINEDDataType() {
        Tag tag           = MODEL;
        DataType dataType = DataType.UNDEFINED;
        Field field       = Field.forTag(tag, dataType);
        assertInstanceOf(ByteArrayField.class, field);
        assertEquals(dataType, field.getDataType());
    }

    @Test
    void forTagWithUTF8DataType() {
        Tag tag           = MODEL;
        DataType dataType = DataType.UTF8;
        Field field       = Field.forTag(tag, dataType);
        assertInstanceOf(StringField.class, field);
        assertEquals(dataType, field.getDataType());
    }

}