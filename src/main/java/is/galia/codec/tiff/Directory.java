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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Image file directory (IFD).
 */
@JsonSerialize(using = DirectorySerializer.class)
@JsonDeserialize(using = DirectoryDeserializer.class)
public final class Directory {

    private final TagSet tagSet;
    private final Set<Field> fields = new TreeSet<>();

    /**
     * @param tagSet Tag set on which this instance will be based.
     */
    public Directory(TagSet tagSet) {
        this.tagSet = tagSet;
    }

    /**
     * @param field  Field to add.
     * @throws IllegalArgumentException if the argument's {@link Field#getTag()
     *         tag} does not exist in the instance's {@link TagSet}.
     */
    public void add(Field field) {
        if (!tagSet.containsTag(field.getTag().id())) {
            throw new IllegalArgumentException(
                    field.getTag() + " does not exist in " + tagSet);
        }
        fields.add(field);
    }

    /**
     * Adds a {@link MultiValueField}.
     *
     * @param tag      Field tag.
     * @param dataType Field data type.
     * @param values   Field values.
     * @throws IllegalArgumentException if the tag does not exist among the
     *         instance's {@link #getTagSet()} tag sets .
     */
    void add(Tag tag, DataType dataType, List<Object> values) {
        add(new MultiValueField(tag, dataType, values));
    }

    /**
     * Adds a {@link ByteArrayField}.
     *
     * @param tag      Field tag.
     * @param dataType Field data type.
     * @param values   Field values.
     * @throws IllegalArgumentException if the tag does not exist among the
     *         instance's {@link #getTagSet()} tag sets .
     */
    void add(Tag tag, DataType dataType, byte[] values) {
        add(new ByteArrayField(tag, dataType, values));
    }

    /**
     * Adds a {@link StringField}.
     *
     * @param tag      Field tag.
     * @param dataType Field data type.
     * @param value   Field value.
     * @throws IllegalArgumentException if the tag does not exist among the
     *         instance's {@link #getTagSet()} tag sets .
     */
    void add(Tag tag, DataType dataType, String value) {
        add(new StringField(tag, dataType, value));
    }

    /**
     * Adds a {@link DirectoryField}.
     *
     * @param tag Tag.
     * @param dir Sub-IFD.
     */
    void add(Tag tag, Directory dir) {
        add(new DirectoryField(tag, dir));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Directory other) {
            // Check the tag set.
            if (!tagSet.equals(other.getTagSet())) {
                return false;
            } else if (fields.size() != other.fields.size()) {
                return false;
            }
            Iterator<Field> theseFields = fields.iterator();
            Iterator<Field> otherFields = other.fields.iterator();
            while (theseFields.hasNext()) {
                Field thisField  = theseFields.next();
                Field otherField = otherFields.next();
                if (!thisField.equals(otherField)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public Field getField(Tag tag) {
        return fields.stream()
                .filter(f -> f.getTag().equals(tag))
                .findFirst()
                .orElse(null);
    }

    /**
     * @return Immutable instance, sorted by tag number.
     */
    public Set<Field> getFields() {
        return Collections.unmodifiableSet(fields);
    }

    public Directory getSubdirectory(Tag ifdPointerTag) {
        Field field = getField(ifdPointerTag);
        if (field != null) {
            return (Directory) field.getFirstValue();
        }
        return null;
    }

    public TagSet getTagSet() {
        return tagSet;
    }

    @Override
    public int hashCode() {
        Object[] objects = new Object[1 + fields.size()];
        objects[0] = tagSet;
        Iterator<Field> it = fields.iterator();
        int i = 1;
        while (it.hasNext()) {
            objects[i] = it.next();
            i++;
        }
        return Objects.hash(objects);
    }

    /**
     * @return Number of fields in the instance, including sub-IFD pointer
     *         fields but excluding sub-IFD fields.
     */
    public int size() {
        return fields.size();
    }

    /**
     * <p>Returns a map with the following structure:</p>
     *
     * {@code
     * {
     *     "tagSet": String,
     *     "fields": {
     *         "Field1Name": [
     *             Object
     *         ],
     *         "Field2Name": [
     *             Object,
     *             Object
     *         ],
     *         "SubIFDPointerName": {
     *             "tagSet": String,
     *             "Fields": {
     *                 "Field1Name": [
     *                     Object,
     *                     Object
     *                 ]
     *                 "Field2Name": [
     *                     Object
     *                 ]
     *             }
     *         }
     *     }
     * }}
     *
     * @return Map representation of the instance.
     */
    public Map<String,Object> toMap() {
        final Map<String,Object> map = new LinkedHashMap<>(2);
        map.put("tagSet", getTagSet().getName());

        final Map<String,Object> fields = new LinkedHashMap<>(getFields().size());
        getFields().forEach(field -> {
            final String fieldName = field.getTag().name();
            Object firstValue = field.getValues().getFirst();
            if (firstValue instanceof Directory) {
                fields.put(fieldName, ((Directory) firstValue).toMap());
            } else {
                fields.put(fieldName, field.getValues());
            }
        });
        map.put("fields", fields);
        return Collections.unmodifiableMap(map);
    }

}
