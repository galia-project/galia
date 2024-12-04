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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@JsonSerialize(using = DataSetSerializer.class)
@JsonDeserialize(using = DataSetDeserializer.class)
public final class DataSet {

    private Tag tag;
    private byte[] dataField;
    private Charset stringEncoding = StandardCharsets.US_ASCII;

    private transient int intValue = -1;
    private transient String stringValue;

    public DataSet(Tag tag, byte[] dataField) {
        this.tag = tag;
        this.dataField = dataField;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof DataSet) {
            DataSet other = (DataSet) obj;
            return Objects.equals(getTag(), other.getTag()) &&
                    Arrays.equals(getDataField(), other.getDataField());
        }
        return super.equals(obj);
    }

    public Tag getTag() {
        return tag;
    }

    /**
     * @return Raw data field bytes.
     */
    public byte[] getDataField() {
        return dataField;
    }

    /**
     * @return Data field bytes as an {@literal int}.
     */
    public int getDataFieldAsInt() {
        if (intValue == -1) {
            intValue = 0;
            int shift = 8 * (dataField.length - 1);
            for (byte b : dataField) {
                intValue |= ((b & 0xff) << shift);
                shift -= 8;
            }
        }
        return intValue;
    }

    /**
     * @return Data field bytes as a {@link String} in the encoding supplied to
     *         {@link #setStringEncoding(Charset)}, or else ASCII.
     */
    public String getDataFieldAsString() {
        if (stringValue == null) {
            switch (tag.getDataType()) {
                case UNSIGNED_INT_16:
                    stringValue = Integer.toString(getDataFieldAsInt());
                    break;
                default:
                    stringValue = new String(dataField, stringEncoding);
                    break;
            }
        }
        return stringValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTag(), Arrays.hashCode(dataField));
    }

    /**
     * Controls the encoding of strings returned from {@link
     * #getDataFieldAsString()}.
     */
    void setStringEncoding(Charset encoding) {
        this.stringEncoding = encoding;
    }

    /**
     * <p>Returns a map with the following structure:</p>
     *
     * {@code
     * {
     *     "TagName": Object
     * }}
     *
     * @return Map representation of the instance.
     */
    public Map<String,Object> toMap() {
        return Map.of(
                getTag().getName(),
                DataType.UNSIGNED_INT_16.equals(getTag().getDataType()) ?
                        getDataFieldAsInt() : getDataFieldAsString());
    }

    @Override
    public String toString() {
        return getTag() + ": " + getDataFieldAsString();
    }

}
