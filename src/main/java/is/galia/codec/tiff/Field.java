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

/**
 * TIFF field, a.k.a. IFD entry.
 */
public interface Field extends Comparable<Field> {

    static Field forTag(Tag tag, DataType dataType) {
        return switch (dataType) {
            case ASCII, UTF8            -> new StringField(tag, dataType);
            case UNDEFINED, BYTE, SBYTE -> new ByteArrayField(tag, dataType);
            default                     -> new MultiValueField(tag, dataType);
        };
    }

    DataType getDataType();

    Tag getTag();

    /**
     * @return For instances of type {@link DataType#BYTE} or {@link
     *         DataType#UNDEFINED}, a byte array. For all other types, the
     *         first value.
     */
    Object getFirstValue();

    /**
     * @return For instances of type {@link DataType#BYTE} or {@link
     *         DataType#UNDEFINED}, a one-element instance containing a byte
     *         array. For all other types, an instance containing one element
     *         per value.
     */
    List<Object> getValues();

}
