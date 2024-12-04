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

package is.galia.codec.iptc;

import com.fasterxml.jackson.databind.ObjectMapper;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataSetTest extends BaseTest {

    private DataSet instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new DataSet(Tag.FILE_VERSION, new byte[] { 0x23, 0x42 });
    }

    @Test
    void JSONSerialization() throws Exception {
        String expected = String.format(
                "{\"record\":%d,\"tag\":%d,\"dataField\":\"I0I=\"}",
                Tag.FILE_VERSION.getRecord().getRecordNum(),
                Tag.FILE_VERSION.getDataSetNum());
        String actual = new ObjectMapper().writer()
                .writeValueAsString(instance);
        assertEquals(expected, actual);
    }

    @Test
    void JSONDeserialization() throws Exception {
        String json = String.format(
                "{\"record\":%d,\"tag\":%d,\"dataField\":\"I0I=\"}",
                Tag.FILE_VERSION.getRecord().getRecordNum(),
                Tag.FILE_VERSION.getDataSetNum());
        DataSet actual = new ObjectMapper().readValue(json, DataSet.class);
        assertEquals(instance, actual);
    }

    /* equals() */

    @Test
    void equalsWithEqualInstances() {
        DataSet instance2 = new DataSet(Tag.FILE_VERSION, new byte[] { 0x23, 0x42 });
        assertEquals(instance, instance2);
    }

    @Test
    void equalsWithUnequalTags() {
        DataSet instance2 = new DataSet(Tag.CREDIT, new byte[] { 0x23, 0x42 });
        assertNotEquals(instance, instance2);
    }

    @Test
    void equalsWithUnequalDataFields() {
        DataSet instance2 = new DataSet(Tag.FILE_VERSION, new byte[] { 0x23, 0x12 });
        assertNotEquals(instance, instance2);
    }

    /* getDataFieldAsInt() */

    @Test
    void getDataFieldAsIntWithIntegerType() {
        DataSet instance = new DataSet(Tag.FILE_VERSION,
                ByteBuffer.allocate(4).putInt(54352643).array());
        assertEquals(54352643, instance.getDataFieldAsInt());
    }

    @Test
    void getDataFieldAsIntWithDigitsType() {
        DataSet instance = new DataSet(Tag.URGENCY, new byte[] { 3 });
        assertEquals(3, instance.getDataFieldAsInt());
    }

    /* getDataFieldAsString() */

    @Test
    void getDataFieldAsStringWithIntegerType() {
        DataSet instance = new DataSet(Tag.FILE_VERSION,
                ByteBuffer.allocate(4).putInt(54352643).array());
        assertEquals("54352643", instance.getDataFieldAsString());
    }

    @Test
    void getDataFieldAsStringWithStringType() {
        DataSet instance = new DataSet(
                Tag.CITY, "Urbana".getBytes(StandardCharsets.US_ASCII));
        assertEquals("Urbana", instance.getDataFieldAsString());
    }

    @Test
    void getDataFieldAsStringWithDigitsType() {
        DataSet instance = new DataSet(Tag.URGENCY, new byte[] {
                0x32, 0x30, 0x30, 0x30, 0x30, 0x31, 0x30, 0x31 });
        assertEquals("20000101", instance.getDataFieldAsString());
    }

    /* hashCode() */

    @Test
    void hashCodeWithEqualInstances() {
        DataSet instance2 = new DataSet(Tag.FILE_VERSION, new byte[] { 0x23, 0x42 });
        assertEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    void hashCodeWithUnequalTags() {
        DataSet instance2 = new DataSet(Tag.CREDIT, new byte[] { 0x23, 0x42 });
        assertNotEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    void hashCodeWithUnequalDataFields() {
        DataSet instance2 = new DataSet(Tag.FILE_VERSION, new byte[] { 0x23, 0x12 });
        assertNotEquals(instance.hashCode(), instance2.hashCode());
    }

    /* toMap() */

    @Test
    void toMap() {
        Map<String,Object> expected = Map.of(
                Tag.FILE_VERSION.getName(),
                instance.getDataFieldAsInt());
        assertEquals(expected, instance.toMap());
    }

}
