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

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Implementation for {@link DataType#BYTE}, {@link DataType#SBYTE}, and {@link
 * DataType#UNDEFINED}.
 */
public class ByteArrayField extends AbstractField implements Field {

    private final ByteArrayOutputStream byteStream =
            new ByteArrayOutputStream();

    public ByteArrayField(Tag tag, DataType dataType) {
        Objects.requireNonNull(tag);
        Objects.requireNonNull(dataType);
        if (!DataType.BYTE.equals(dataType) &&
                !DataType.SBYTE.equals(dataType) &&
                !DataType.UNDEFINED.equals(dataType)) {
            throw new IllegalArgumentException(getClass().getSimpleName() +
                    " can only be used with the " + DataType.BYTE + ", " +
                    DataType.SBYTE + ", or " + DataType.UNDEFINED + " data type.");
        }
        this.tag      = tag;
        this.dataType = dataType;
    }

    public ByteArrayField(Tag tag, DataType dataType, byte[] values) {
        this(tag, dataType);
        Objects.requireNonNull(values);
        byteStream.writeBytes(values);
    }

    @Override
    public int compareTo(Field other) {
        return Integer.compare(getTag().id(), other.getTag().id());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof ByteArrayField other) {
            if (!Objects.equals(getTag(), other.getTag())) {
                return false;
            }
            return Arrays.equals(byteStream.toByteArray(),
                    other.byteStream.toByteArray());
        }
        return false;
    }

    /**
     * @return For instances of type {@link DataType#BYTE} or {@link
     *         DataType#UNDEFINED}, a byte array. For all other types, the
     *         first value.
     */
    @Override
    public Object getFirstValue() {
        return byteStream.toByteArray();
    }

    /**
     * @return For instances of type {@link DataType#BYTE} or {@link
     *         DataType#UNDEFINED}, a one-element instance containing a byte
     *         array. For all other types, an instance containing one element
     *         per value.
     */
    @Override
    public List<Object> getValues() {
        return List.of(getFirstValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTag(),
                Arrays.hashCode(byteStream.toByteArray()));
    }

    /**
     * For use only with instances of data types {@link DataType#BYTE} and
     * {@link DataType#UNDEFINED}.
     */
    void setValue(byte[] value) {
        if (!DataType.BYTE.equals(dataType) &&
                !DataType.UNDEFINED.equals(dataType)) {
            throw new UnsupportedOperationException(
                    "Cannot set a byte[] value to an instance of data type " + dataType);
        }
        byteStream.reset();
        byteStream.writeBytes(value);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getTag().toString());
        builder.append(": ");
        if (byteStream.size() > 0) {
            builder.append("<");
            builder.append(byteStream.size());
            builder.append(" bytes>");
        }
        return builder.toString();
    }

}
