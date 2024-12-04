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

package is.galia.util;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilsTest extends BaseTest {

    /* readSignedInt() */

    @Test
    void readSignedIntWithBigEndian() {
        final int value = 48553;
        byte[] bytes = new byte[] {
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value };
        long actual = NumberUtils.readSignedInt(bytes, ByteOrder.BIG_ENDIAN);
        assertEquals(value, actual);
    }

    @Test
    void readSignedIntWithLittleEndian() {
        final long value = 1835845218;
        byte[] bytes = new byte[] {
                (byte) value,
                (byte) (value >>> 8),
                (byte) (value >>> 16),
                (byte) (value >>> 24)};
        long actual = NumberUtils.readSignedInt(bytes, ByteOrder.LITTLE_ENDIAN);
        assertEquals(value, actual);
    }

    /* readSignedLong() */

    @Test
    void readSignedLongWithBigEndian() {
        final long value = 4145776530L;
        byte[] bytes = new byte[] {
                (byte) (value >>> 56),
                (byte) (value >>> 48),
                (byte) (value >>> 40),
                (byte) (value >>> 32),
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value };
        long actual = NumberUtils.readSignedLong(bytes, ByteOrder.BIG_ENDIAN);
        assertEquals(value, actual);
    }

    @Test
    void readSignedLongWithLittleEndian() {
        final long value = 4145776530L;
        byte[] bytes = new byte[] {
                (byte) value,
                (byte) (value >>> 8),
                (byte) (value >>> 16),
                (byte) (value >>> 24),
                (byte) (value >>> 32),
                (byte) (value >>> 40),
                (byte) (value >>> 48),
                (byte) (value >>> 56)};
        long actual = NumberUtils.readSignedLong(bytes, ByteOrder.LITTLE_ENDIAN);
        assertEquals(value, actual);
    }

    /* readSignedShort() */

    @Test
    void readSignedShortWithBigEndian() {
        final int ushort = 48553;
        byte[] bytes = new byte[] {
                (byte) (ushort >>> 8),
                (byte) ushort };
        int actual = NumberUtils.readSignedShort(bytes, ByteOrder.BIG_ENDIAN);
        assertEquals(ushort, actual);
    }

    @Test
    void readSignedShortWithLittleEndian() {
        final int ushort = 48553;
        byte[] bytes = new byte[] {
                (byte) ushort,
                (byte) (ushort >>> 8)};
        int actual = NumberUtils.readSignedShort(bytes, ByteOrder.LITTLE_ENDIAN);
        assertEquals(ushort, actual);
    }

    /* readUnsignedInt() */

    @Test
    void readUnsignedIntWithBigEndian() {
        final int uint = 48553;
        byte[] bytes   = NumberUtils.toByteArray(uint, ByteOrder.BIG_ENDIAN);
        long actual    = NumberUtils.readUnsignedInt(bytes, ByteOrder.BIG_ENDIAN);
        assertEquals(uint, actual);
    }

    @Test
    void readUnsignedIntWithLittleEndian() {
        final long uint = 1835845218;
        byte[] bytes    = NumberUtils.toByteArray(uint, ByteOrder.LITTLE_ENDIAN);
        long actual     = NumberUtils.readUnsignedInt(bytes, ByteOrder.LITTLE_ENDIAN);
        assertEquals(uint, actual);
    }

    /* readUnsignedShort() */

    @Test
    void readUnsignedShortWithBigEndian() {
        final int ushort = 48553;
        byte[] bytes = new byte[] {
                (byte) (ushort >>> 8),
                (byte) ushort };
        int actual = NumberUtils.readUnsignedShort(bytes, ByteOrder.BIG_ENDIAN);
        assertEquals(ushort, actual);
    }

    @Test
    void readUnsignedShortWithLittleEndian() {
        final int ushort = 48553;
        byte[] bytes = new byte[] {
                (byte) ushort,
                (byte) (ushort >>> 8)};
        int actual = NumberUtils.readUnsignedShort(bytes, ByteOrder.LITTLE_ENDIAN);
        assertEquals(ushort, actual);
    }

    /* toByteArray(int) */

    @Test
    void toByteArrayWithIntWithBigEndian() {
        int number = 32429384;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(number);
        byte[] expected = buffer.array();
        byte[] actual   = NumberUtils.toByteArray(number, ByteOrder.BIG_ENDIAN);
        assertArrayEquals(expected, actual);
    }

    @Test
    void toByteArrayWithIntWithLittleEndian() {
        int number = 32429384;
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(number);
        byte[] expected = buffer.array();
        byte[] actual   = NumberUtils.toByteArray(number, ByteOrder.LITTLE_ENDIAN);
        assertArrayEquals(expected, actual);
    }

    /* toByteArray(long) */

    @Test
    void toByteArrayWithLongWithBigEndian() {
        long number = 324326429384L;
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(number);
        byte[] expected = buffer.array();
        byte[] actual   = NumberUtils.toByteArray(number, ByteOrder.BIG_ENDIAN);
        assertArrayEquals(expected, actual);
    }

    @Test
    void toByteArrayWithLongWithLittleEndian() {
        long number = 324326429384L;
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(number);
        byte[] expected = buffer.array();
        byte[] actual   = NumberUtils.toByteArray(number, ByteOrder.LITTLE_ENDIAN);
        assertArrayEquals(expected, actual);
    }

}