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

package is.galia.status;

import is.galia.source.Source;

import java.util.Objects;

/**
 * Encapsulates a successful use of a {@link Source}.
 */
public final class SourceUsage {

    private final Source source;

    SourceUsage(Source source) {
        this.source = source;
    }

    /**
     * @return True of the given instance's {@link #getSource() source} is of
     *         the same class.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof SourceUsage) {
            SourceUsage other = (SourceUsage) obj;
            return source.getClass().equals(other.source.getClass());
        }
        return super.equals(obj);
    }

    Source getSource() {
        return source;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(source.getClass());
    }

    @Override
    public String toString() {
        return String.format("%s -> %s",
                source.getIdentifier(),
                source.getClass().getName());
    }

}
