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

import java.nio.ByteOrder;

public final class NumberUtils {

    /**
     * @param bytes     Array of 1 to 4 bytes. Bytes beyond these are ignored.
     * @param byteOrder Byte order of the array.
     * @return          Signed value.
     */
    public static int readSignedInt(byte[] bytes, ByteOrder byteOrder) {
        if (ByteOrder.LITTLE_ENDIAN.equals(byteOrder)) {
            bytes = ArrayUtils.reverse(bytes);
        }
        int length = Math.min(4, bytes.length);
        int value = 0;
        for (int i = 0; i < length; i++) {
            value = (value << 8) | (bytes[i] & 0xff);
        }
        return value;
    }

    /**
     * @param bytes     Array of 1 to 8 bytes. Bytes beyond these are ignored.
     * @param byteOrder Byte order of the array.
     * @return          Signed value.
     */
    public static long readSignedLong(byte[] bytes, ByteOrder byteOrder) {
        if (ByteOrder.LITTLE_ENDIAN.equals(byteOrder)) {
            bytes = ArrayUtils.reverse(bytes);
        }
        int length = Math.min(8, bytes.length);
        long value = 0;
        for (int i = 0; i < length; i++) {
            value = (value << 8) | (bytes[i] & 0xff);
        }
        return value;
    }

    /**
     * @param bytes     Array of 1 to 2 bytes. Bytes beyond these are ignored.
     * @param byteOrder Byte order of the array.
     * @return          Signed value.
     */
    public static int readSignedShort(byte[] bytes, ByteOrder byteOrder) {
        return readUnsignedShort(bytes, byteOrder);
    }

    /**
     * @param bytes     Array of 1 to 4 bytes. Bytes beyond these are ignored.
     * @param byteOrder Byte order of the array.
     * @return          Unsigned value.
     */
    public static long readUnsignedInt(byte[] bytes, ByteOrder byteOrder) {
        if (ByteOrder.LITTLE_ENDIAN.equals(byteOrder)) {
            if (bytes.length < 2) {
                return bytes[0] & 0xffL;
            } else if (bytes.length < 3) {
                return (bytes[1] & 0xffL) << 8 |
                        (bytes[0] & 0xffL);
            } else if (bytes.length < 4) {
                return (bytes[2] & 0xffL) << 16 |
                        (bytes[1] & 0xffL) << 8 |
                        (bytes[0] & 0xffL);
            }
            return (bytes[3] & 0xffL) << 24 |
                    (bytes[2] & 0xffL) << 16 |
                    (bytes[1] & 0xffL) << 8  |
                    (bytes[0] & 0xffL);
        } else {
            if (bytes.length < 2) {
                return bytes[0] & 0xffL;
            } else if (bytes.length < 3) {
                return (bytes[0] & 0xffL) << 8 |
                        bytes[1] & 0xffL;
            } else if (bytes.length < 4) {
                return (bytes[0] & 0xffL) << 16 |
                        (bytes[1] & 0xffL) << 8 |
                        bytes[2] & 0xffL;
            }
            return (bytes[0] & 0xffL) << 24 |
                    (bytes[1] & 0xffL) << 16 |
                    (bytes[2] & 0xffL) << 8 |
                    (bytes[3] & 0xffL);
        }
    }

    /**
     * @param bytes     Array of 1 to 2 bytes. Bytes beyond these are ignored.
     * @param byteOrder Byte order of the array.
     * @return          Unsigned value.
     */
    public static int readUnsignedShort(byte[] bytes, ByteOrder byteOrder) {
        if (ByteOrder.LITTLE_ENDIAN.equals(byteOrder)) {
            if (bytes.length < 2) {
                return (bytes[0] & 0xff);
            } else {
                return (bytes[1] & 0xff) << 8 |
                        (bytes[0] & 0xff);
            }
        } else {
            if (bytes.length < 2) {
                return bytes[0] & 0xff;
            } else {
                return (bytes[0] & 0xff) << 8 |
                        (bytes[1] & 0xff);
            }
        }
    }

    public static byte[] toByteArray(int value, ByteOrder byteOrder) {
        if (ByteOrder.LITTLE_ENDIAN.equals(byteOrder)) {
            return new byte[] {
                    (byte) value,
                    (byte) (value >>> 8),
                    (byte) (value >>> 16),
                    (byte) (value >>> 24) };
        } else {
            return new byte[] {
                    (byte) (value >>> 24),
                    (byte) (value >>> 16),
                    (byte) (value >>> 8),
                    (byte) value };
        }
    }

    public static byte[] toByteArray(long value, ByteOrder byteOrder) {
        if (ByteOrder.LITTLE_ENDIAN.equals(byteOrder)) {
            return new byte[] {
                    (byte) value,
                    (byte) (value >>> 8),
                    (byte) (value >>> 16),
                    (byte) (value >>> 24),
                    (byte) (value >>> 32),
                    (byte) (value >>> 40),
                    (byte) (value >>> 48),
                    (byte) (value >>> 56)
            };
        } else {
            return new byte[] {
                    (byte) (value >>> 56),
                    (byte) (value >>> 48),
                    (byte) (value >>> 40),
                    (byte) (value >>> 32),
                    (byte) (value >>> 24),
                    (byte) (value >>> 16),
                    (byte) (value >>> 8),
                    (byte) value };
        }
    }

    private NumberUtils() {}

}
