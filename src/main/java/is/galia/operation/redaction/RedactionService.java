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

package is.galia.operation.redaction;

import is.galia.delegate.DelegateException;
import is.galia.image.Region;
import is.galia.delegate.Delegate;
import is.galia.operation.Color;

import java.util.List;

/**
 * Provides information about redactions.
 */
public final class RedactionService {

    /**
     * Factory method that returns a list of {@link Redaction redactions}
     * based on the given parameters.
     *
     * @param delegate Delegate for the current request.
     * @return         Redactions from the given proxy, or an empty list if
     *                 there are none.
     */
    public List<Redaction> redactionsFor(Delegate delegate)
            throws DelegateException {
        return delegate.getRedactions().stream()
                .map(def -> new Redaction(
                        new Region(
                                ((Number) def.get("x")).intValue(),
                                ((Number) def.get("y")).intValue(),
                                ((Number) def.get("width")).intValue(),
                                ((Number) def.get("height")).intValue()),
                        Color.fromString((String) def.get("color"))))
                .toList();
    }

}
