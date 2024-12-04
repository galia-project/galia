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
 * Implementation for IFD pointers.
 */
public class DirectoryField extends AbstractField implements Field {

    private Directory directory;

    public DirectoryField(Tag tag) {
        Objects.requireNonNull(tag);
        this.tag      = tag;
        this.dataType = DataType.LONG;
    }

    public DirectoryField(Tag tag, Directory directory) {
        this(tag);
        Objects.requireNonNull(directory);
        this.directory = directory;
    }

    @Override
    public int compareTo(Field other) {
        return Integer.compare(getTag().id(), other.getTag().id());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof DirectoryField other) {
            if (!Objects.equals(getTag(), other.getTag())) {
                return false;
            }
            return Objects.equals(directory, other.directory);
        }
        return false;
    }

    @Override
    public Directory getFirstValue() {
        return directory;
    }

    @Override
    public List<Object> getValues() {
        return List.of(directory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTag(), directory);
    }

    /**
     * For use only with instances of data types {@link DataType#BYTE} and
     * {@link DataType#UNDEFINED}.
     */
    void setDirectory(Directory directory) {
        this.directory = directory;
    }

    @Override
    public String toString() {
        return getTag().toString() + " <IFD>";
    }

}
