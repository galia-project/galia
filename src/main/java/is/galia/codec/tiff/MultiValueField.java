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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation for {@link DataType}s that can have multiple values.
 */
public final class MultiValueField extends AbstractField implements Field {

    private final List<Object> values = new ArrayList<>();

    public MultiValueField(Tag tag, DataType dataType) {
        Objects.requireNonNull(tag);
        Objects.requireNonNull(dataType);
        if (DataType.BYTE.equals(dataType) ||
                DataType.SBYTE.equals(dataType) ||
                DataType.UNDEFINED.equals(dataType) ||
                DataType.ASCII.equals(dataType) ||
                DataType.UTF8.equals(dataType)) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() +
                            " cannot be used with data type " + dataType);
        }
        this.tag      = tag;
        this.dataType = dataType;
    }

    public MultiValueField(Tag tag, DataType dataType, List<Object> values) {
        this(tag, dataType);
        this.values.addAll(values);
    }

    void addValue(Object value) {
        values.add(value);
    }

    @Override
    public int compareTo(Field other) {
        return Integer.compare(getTag().id(), other.getTag().id());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof MultiValueField other) {
            if (!Objects.equals(getTag(), other.getTag())) {
                return false;
            } else if (values.size() != other.values.size()) {
                return false;
            }
            final int numValues = values.size();
            if (numValues > 0) {
                for (int i = 0; i < numValues; i++) {
                    Object value      = values.get(i);
                    Object otherValue = other.values.get(i);
                    if (!value.equals(otherValue)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getFirstValue() {
        return values.getFirst();
    }

    @Override
    public List<Object> getValues() {
        return Collections.unmodifiableList(values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTag(), values);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getTag());
        builder.append(": ");
        builder.append(values.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ")));
        return builder.toString();
    }

}
