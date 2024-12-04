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

package is.galia.source;

import is.galia.image.Identifier;
import is.galia.resource.RequestContext;
import is.galia.delegate.Delegate;

public abstract class AbstractSource {

    protected Identifier identifier;
    protected Delegate delegate;

    /**
     * @return Delegate. May be {@code null}.
     */
    protected Delegate getDelegate() {
        return delegate;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    /**
     * <p>Sets the identifier used by the instance.</p>
     *
     * <p>N.B.: The identifier property of the {@link #getDelegate()
     * delegate}'s {@link RequestContext} must also be set.</p>
     */
    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }

}
