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

import is.galia.util.NumberUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * TIFF field data type.
 */
public enum DataType {

    /**
     * 8-bit unsigned integer.
     */
    BYTE(1, 1),

    /**
     * 8-bit NULL-terminated string.
     */
    ASCII(2, 1),

    /**
     * 16-bit unsigned integer.
     */
    SHORT(3, 2),

    /**
     * 32-bit unsigned integer.
     */
    LONG(4, 4),

    /**
     * 64-bit unsigned integer used only in BigTIFF.
     */
    LONG8(16, 8),

    /**
     * Two 32-bit unsigned integers.
     */
    RATIONAL(5, 8),

    /**
     * 8-bit signed integer.
     */
    SBYTE(6, 1),

    /**
     * 8-bit byte.
     */
    UNDEFINED(7, 1),

    /**
     * 16-bit signed integer.
     */
    SSHORT(8, 2),

    /**
     * 32-bit signed integer.
     */
    SLONG(9, 4),

    /**
     * 64-bit signed integer, used only in BigTIFF.
     */
    SLONG8(17, 8),

    /**
     * Two 32-bit signed integers.
     */
    SRATIONAL(10, 8),

    /**
     * 4-byte single-precision IEEE floating-point value.
     */
    FLOAT(11, 4),

    /**
     * 8-byte double-precision IEEE floating-point value.
     */
    DOUBLE(12, 8),

    /**
     * 64-bit IFD offset, used only in BigTIFF.
     */
    IFD8(18, 8),

    /**
     * <p>8-bit byte representing a UTF-8 string. The final byte is terminated
     * with NULL. BOM (Byte Order Mark) shall not be used. The UTF-8 count
     * shall include NULL.</p>
     *
     * <p>Introduced in EXIF 3.0.</p>
     */
    UTF8(129, 1);

    private final short value, numBytesPerComponent;

    /**
     * @param value TIFF data type {@link #getValue() value}.
     */
    static DataType forValue(int value) {
        return Arrays.stream(DataType.values())
                .filter(t -> t.value == value)
                .findFirst()
                .orElse(null);
    }

    /**
     * @param value                Data type value.
     * @param numBytesPerComponent Number of bytes per value component.
     */
    DataType(int value, int numBytesPerComponent) {
        this.value                = (short) value;
        this.numBytesPerComponent = (short) numBytesPerComponent;
    }

    /**
     * <p>Decodes the given byte array into one of the following types:</p>
     *
     * <dl>
     *     <dt>{@link #BYTE}</dt>
     *     <dd>{@link java.lang.Byte}</dd>
     *     <dt>{@link #ASCII}</dt>
     *     <dd>Trimmed, ASCII-encoded {@link java.lang.String}</dd>
     *     <dt>{@link #SHORT}</dt>
     *     <dd>{@link java.lang.Integer}</dd>
     *     <dt>{@link #LONG}</dt>
     *     <dd>{@link java.lang.Long}</dd>
     *     <dt>{@link #LONG8}</dt>
     *     <dd>{@link java.lang.Long}</dd>
     *     <dt>{@link #RATIONAL}</dt>
     *     <dd>{@link java.util.List} of two longs</dd>
     *     <dt>{@link #SBYTE}</dt>
     *     <dd>{@link java.lang.Byte}</dd>
     *     <dt>{@link #UNDEFINED}</dt>
     *     <dd>Input byte array</dd>
     *     <dt>{@link #SSHORT}</dt>
     *     <dd>{@link java.lang.Integer}</dd>
     *     <dt>{@link #SLONG}</dt>
     *     <dd>{@link java.lang.Integer}</dd>
     *     <dt>{@link #SLONG8}</dt>
     *     <dd>{@link java.lang.Long}</dd>
     *     <dt>{@link #SRATIONAL}</dt>
     *     <dd>{@link java.util.List} of twp ints</dd>
     *     <dt>{@link #FLOAT}</dt>
     *     <dd>{@link java.lang.Float}</dd>
     *     <dt>{@link #DOUBLE}</dt>
     *     <dd>{@link java.lang.Double}</dd>
     *     <dt>{@link #IFD8}</dt>
     *     <dd>{@link java.lang.Long}</dd>
     *     <dt>{@link #UTF8}</dt>
     *     <dd>Trimmed, UTF-8-encoded {@link java.lang.String}</dd>
     * </dl>
     *
     * @param bytes     Field value.
     * @param byteOrder Byte order.
     * @return          Java equivalent value.
     */
    Object decode(byte[] bytes, ByteOrder byteOrder) {
        switch (this) {
            case ASCII: // 8-bit bytes containing 7-bit ASCII; last byte NUL
                return new String(bytes, StandardCharsets.US_ASCII).trim();
            case BYTE: // 8-bit unsigned int
            case SBYTE: // 8-bit signed int
                return bytes[0];
            case SHORT: // 16-bit unsigned int
                return NumberUtils.readUnsignedShort(bytes, byteOrder);
            case SSHORT: // 16-bit signed int
                return NumberUtils.readSignedShort(bytes, byteOrder);
            case LONG: // 32-bit unsigned int
                return NumberUtils.readUnsignedInt(bytes, byteOrder);
            case SLONG: // 32-bit signed int
                return NumberUtils.readSignedInt(bytes, byteOrder);
            case LONG8:
            case SLONG8:
            case IFD8:
                return NumberUtils.readSignedLong(bytes, byteOrder);
            case RATIONAL: // two LONGs
                byte[] numBytes      = Arrays.copyOfRange(bytes, 0, 4);
                byte[] denBytes      = Arrays.copyOfRange(bytes, 4, 8);
                long longNumerator   = NumberUtils.readUnsignedInt(numBytes, byteOrder);
                long longDenominator = NumberUtils.readUnsignedInt(denBytes, byteOrder);
                return List.of(longNumerator, longDenominator);
            case SRATIONAL: // two SLONGs
                numBytes           = Arrays.copyOfRange(bytes, 0, 4);
                denBytes           = Arrays.copyOfRange(bytes, 4, 8);
                int intNumerator   = NumberUtils.readSignedInt(numBytes, byteOrder);
                int intDenominator = NumberUtils.readSignedInt(denBytes, byteOrder);
                return List.of(intNumerator, intDenominator);
            case FLOAT: // 32-bit single-precision float
                return ByteBuffer.wrap(bytes).order(byteOrder).getFloat();
            case DOUBLE: // 64-bit double-precision float
                return ByteBuffer.wrap(bytes).order(byteOrder).getDouble();
            case UTF8:
                return new String(bytes, StandardCharsets.UTF_8).trim();
            default:
                return bytes;
        }
    }

    short getNumBytesPerComponent() {
        return numBytesPerComponent;
    }

    short getValue() {
        return value;
    }

}
