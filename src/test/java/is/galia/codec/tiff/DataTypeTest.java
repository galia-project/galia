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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataTypeTest extends BaseTest {

    private static final float DELTA_F  = 0.00001f;
    private static final double DELTA_L = 0.0000000001;

    /* forValue() */

    @Test
    void forValueWithUnsupportedValue() {
        assertNull(DataType.forValue(99));
    }

    @Test
    void forValue() {
        assertEquals(DataType.BYTE, DataType.forValue(1));
        assertEquals(DataType.ASCII, DataType.forValue(2));
        assertEquals(DataType.SHORT, DataType.forValue(3));
        assertEquals(DataType.LONG, DataType.forValue(4));
        assertEquals(DataType.RATIONAL, DataType.forValue(5));
        assertEquals(DataType.SBYTE, DataType.forValue(6));
        assertEquals(DataType.UNDEFINED, DataType.forValue(7));
        assertEquals(DataType.SSHORT, DataType.forValue(8));
        assertEquals(DataType.SLONG, DataType.forValue(9));
        assertEquals(DataType.SRATIONAL, DataType.forValue(10));
        assertEquals(DataType.FLOAT, DataType.forValue(11));
        assertEquals(DataType.DOUBLE, DataType.forValue(12));
        assertEquals(DataType.LONG8, DataType.forValue(16));
        assertEquals(DataType.SLONG8, DataType.forValue(17));
        assertEquals(DataType.IFD8, DataType.forValue(18));
        assertEquals(DataType.UTF8, DataType.forValue(129));
    }

    /* decode() */

    @Test
    void decodeWithBYTEBigEndian() {
        byte[] bytes = new byte[] { 0x79 };
        assertEquals((byte) 0x79, DataType.BYTE.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void decodeWithBYTELittleEndian() {
        byte[] bytes = new byte[] { 0x79 };
        assertEquals((byte) 0x79, DataType.BYTE.decode(bytes, ByteOrder.LITTLE_ENDIAN));
    }

    @Test
    void decodeWithASCIIBigEndian() {
        byte[] bytes = "cats\u0000".getBytes(StandardCharsets.US_ASCII);
        assertEquals("cats", DataType.ASCII.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void decodeWithASCIILittleEndian() {
        byte[] bytes = "cats\u0000".getBytes(StandardCharsets.US_ASCII);
        assertEquals("cats", DataType.ASCII.decode(bytes, ByteOrder.LITTLE_ENDIAN));
    }

    @Test
    void decodeWithASCIITrimsValue() {
        byte[] bytes = "cats   \0\0".getBytes(StandardCharsets.US_ASCII);
        assertEquals("cats", DataType.UTF8.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void decodeWithSHORTBigEndian() {
        byte[] bytes = new byte[] { 0x12, 0x79 };
        assertEquals(4729, DataType.SHORT.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void decodeWithSHORTLittleEndian() {
        byte[] bytes = new byte[] { 0x79, 0x12 };
        assertEquals(4729, DataType.SHORT.decode(bytes, ByteOrder.LITTLE_ENDIAN));
    }

    @Test
    void decodeWithLONGBigEndian() {
        final byte[] fourBytes = new byte[] { 0x05, 0x08, 0x12, 0x33 };
        assertEquals(84415027L, DataType.LONG.decode(fourBytes, ByteOrder.BIG_ENDIAN));

        byte[] threeBytes = Arrays.copyOfRange(fourBytes, 1, 4);
        assertEquals(528947L, DataType.LONG.decode(threeBytes, ByteOrder.BIG_ENDIAN));

        byte[] twoBytes = Arrays.copyOfRange(fourBytes, 2, 4);
        assertEquals(4659L, DataType.LONG.decode(twoBytes, ByteOrder.BIG_ENDIAN));

        byte[] oneByte = Arrays.copyOfRange(fourBytes, 3, 4);
        assertEquals(51L, DataType.LONG.decode(oneByte, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void decodeWithLONGLittleEndian() {
        final byte[] fourBytes = new byte[] { 0x33, 0x12, 0x08, 0x05 };
        assertEquals(84415027L, DataType.LONG.decode(fourBytes, ByteOrder.LITTLE_ENDIAN));

        byte[] threeBytes = Arrays.copyOfRange(fourBytes, 0, 3);
        assertEquals(528947L, DataType.LONG.decode(threeBytes, ByteOrder.LITTLE_ENDIAN));

        byte[] twoBytes = Arrays.copyOfRange(fourBytes, 0, 2);
        assertEquals(4659L, DataType.LONG.decode(twoBytes, ByteOrder.LITTLE_ENDIAN));

        byte[] oneByte = Arrays.copyOfRange(fourBytes, 0, 1);
        assertEquals(51L, DataType.LONG.decode(oneByte, ByteOrder.LITTLE_ENDIAN));
    }

    @Test
    void decodeWithLONG8BigEndian() {
        final ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
        final long value          = 234987234L;
        byte[] bytes              = new byte[8];
        ByteBuffer.wrap(bytes).order(byteOrder).putLong(value);
        assertEquals(value, (long) DataType.LONG8.decode(bytes, byteOrder));
    }

    @Test
    void decodeWithLONG8LittleEndian() {
        final ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
        final long value          = 234987234L;
        byte[] bytes              = new byte[8];
        ByteBuffer.wrap(bytes).order(byteOrder).putLong(value);
        assertEquals(value, (long) DataType.LONG8.decode(bytes, byteOrder));
    }

    @Test
    void decodeWithRATIONALBigEndian() {
        byte[] bytes = new byte[] { 0x00, 0x00, 0x03, 0x04, 0x05, 0x08, 0x12, 0x33 };
        assertEquals(List.of(772L, 84415027L),
                DataType.RATIONAL.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void decodeWithRATIONALLittleEndian() {
        byte[] bytes = new byte[] { 0x00, 0x00, 0x03, 0x04, 0x05, 0x08, 0x12, 0x33 };
        assertEquals(List.of(67305472L, 856819717L),
                DataType.RATIONAL.decode(bytes, ByteOrder.LITTLE_ENDIAN));
    }

    @Test
    void decodeWithSBYTEBigEndian() {
        byte[] bytes = new byte[] { 0x79 };
        assertEquals((byte) 0x79, DataType.SBYTE.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void decodeWithSBYTELittleEndian() {
        byte[] bytes = new byte[] { 0x79 };
        assertEquals((byte) 0x79, DataType.SBYTE.decode(bytes, ByteOrder.LITTLE_ENDIAN));
    }

    @Test
    void decodeWithSSHORTBigEndian() {
        byte[] bytes = new byte[] { 0x12, 0x79 };
        assertEquals(4729, DataType.SSHORT.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void decodeWithSSHORTLittleEndian() {
        byte[] bytes = new byte[] { 0x79, 0x12 };
        assertEquals(4729, DataType.SSHORT.decode(bytes, ByteOrder.LITTLE_ENDIAN));
    }

    @Test
    void decodeWithSLONGBigEndian() {
        final int value = 395834982;
        byte[] bytes = new byte[4];
        ByteBuffer.wrap(bytes).putInt(value);
        assertEquals(value, DataType.SLONG.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void decodeWithSLONGLittleEndian() {
        final int value = 395834982;
        byte[] bytes = new byte[4];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(value);
        assertEquals(value, DataType.SLONG.decode(bytes, ByteOrder.LITTLE_ENDIAN));
    }

    @Test
    void decodeWithSLONG8BigEndian() {
        final ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
        final long value          = 234987234L;
        byte[] bytes              = new byte[8];
        ByteBuffer.wrap(bytes).order(byteOrder).putLong(value);
        assertEquals(value, (long) DataType.SLONG8.decode(bytes, byteOrder));
    }

    @Test
    void decodeWithSLONG8LittleEndian() {
        final ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
        final long value          = 234987234L;
        byte[] bytes              = new byte[8];
        ByteBuffer.wrap(bytes).order(byteOrder).putLong(value);
        assertEquals(value, (long) DataType.SLONG8.decode(bytes, byteOrder));
    }

    @Test
    void decodeWithSRATIONALBigEndian() {
        byte[] bytes = new byte[] { 0x00, 0x00, 0x03, 0x04, 0x05, 0x08, 0x12, 0x33 };
        assertEquals(List.of(772, 84415027),
                DataType.SRATIONAL.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void decodeWithSRATIONALLittleEndian() {
        byte[] bytes = new byte[] { 0x00, 0x00, 0x03, 0x04, 0x05, 0x08, 0x12, 0x33 };
        assertEquals(List.of(67305472, 856819717),
                DataType.SRATIONAL.decode(bytes, ByteOrder.LITTLE_ENDIAN));
    }

    @Test
    void decodeWithFLOATBigEndian() {
        final float value = 342.234232f;
        byte[] bytes = new byte[4];
        ByteBuffer.wrap(bytes).putFloat(value);
        assertEquals(value, (float) DataType.FLOAT.decode(bytes, ByteOrder.BIG_ENDIAN), DELTA_F);
    }

    @Test
    void decodeWithFLOATLittleEndian() {
        final float value = 342.234232f;
        byte[] bytes = new byte[4];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putFloat(value);
        assertEquals(value, (float) DataType.FLOAT.decode(bytes, ByteOrder.LITTLE_ENDIAN), DELTA_F);
    }

    @Test
    void decodeWithDOUBLEBigEndian() {
        final double value = 342.234202;
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        assertEquals(value, (double) DataType.DOUBLE.decode(bytes, ByteOrder.BIG_ENDIAN), DELTA_L);
    }

    @Test
    void decodeWithDOUBLELittleEndian() {
        final double value = 342.234202;
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putDouble(value);
        assertEquals(value, (double) DataType.DOUBLE.decode(bytes, ByteOrder.LITTLE_ENDIAN), DELTA_L);
    }

    @Test
    void decodeWithIFD8BigEndian() {
        final ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
        final long value          = 234987234L;
        byte[] bytes              = new byte[8];
        ByteBuffer.wrap(bytes).order(byteOrder).putLong(value);
        assertEquals(value, (long) DataType.IFD8.decode(bytes, byteOrder));
    }

    @Test
    void decodeWithIFD8LittleEndian() {
        final ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
        final long value          = 234987234L;
        byte[] bytes              = new byte[8];
        ByteBuffer.wrap(bytes).order(byteOrder).putLong(value);
        assertEquals(value, (long) DataType.IFD8.decode(bytes, byteOrder));
    }

    @Test
    void decodeWithUTF8BigEndian() {
        byte[] bytes = "cats\u0000".getBytes(StandardCharsets.UTF_8);
        assertEquals("cats", DataType.UTF8.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void decodeWithUTF8LittleEndian() {
        byte[] bytes = "cats\u0000".getBytes(StandardCharsets.UTF_8);
        assertEquals("cats", DataType.UTF8.decode(bytes, ByteOrder.LITTLE_ENDIAN));
    }

    @Test
    void decodeWithUTF8TrimsValue() {
        byte[] bytes = "cats   \0\0".getBytes(StandardCharsets.UTF_8);
        assertEquals("cats", DataType.UTF8.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

}
