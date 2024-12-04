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

import java.util.List;
import java.util.Objects;

/**
 * Implementation for {@link DataType#ASCII} and {@link DataType#UTF8}.
 */
public final class StringField extends AbstractField implements Field {

    private String value;

    public StringField(Tag tag, DataType dataType) {
        Objects.requireNonNull(tag);
        Objects.requireNonNull(dataType);
        if (!DataType.ASCII.equals(dataType) &&
                !DataType.UTF8.equals(dataType)) {
            throw new IllegalArgumentException(getClass().getSimpleName() +
                    " can only be used with the " + DataType.ASCII + " or " +
                    DataType.UTF8 + " data type.");
        }
        this.tag      = tag;
        this.dataType = dataType;
    }

    /**
     * Convenience constructor for assembly of test fixtures.
     */
    public StringField(Tag tag, DataType dataType, String value) {
        this(tag, dataType);
        Objects.requireNonNull(value);
        this.value = value;
    }

    @Override
    public int compareTo(Field other) {
        return Integer.compare(getTag().id(), other.getTag().id());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof StringField other) {
            if (!Objects.equals(getTag(), other.getTag())) {
                return false;
            }
            return Objects.equals(value, other.value);
        }
        return false;
    }

    /**
     * @return For instances of type {@link DataType#BYTE} or {@link
     *         DataType#UNDEFINED}, a byte array. For all other types, the
     *         first value.
     */
    @Override
    public String getFirstValue() {
        return value;
    }

    /**
     * @return For instances of type {@link DataType#BYTE} or {@link
     *         DataType#UNDEFINED}, a one-element instance containing a byte
     *         array. For all other types, an instance containing one element
     *         per value.
     */
    @Override
    public List<Object> getValues() {
        return List.of(value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTag(), value);
    }

    /**
     * For use only with instances of data types {@link DataType#BYTE} and
     * {@link DataType#UNDEFINED}.
     */
    void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return getTag().toString() + ": " + value;
    }

}
