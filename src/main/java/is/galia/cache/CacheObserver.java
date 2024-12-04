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

package is.galia.cache;

import is.galia.operation.OperationList;

/**
 * <p>Observer of cache operations that may complete asynchronously.</p>
 *
 * <p>At the time of its addition, this was used only to make various tests
 * less dependent on timing.</p>
 */
public interface CacheObserver {

    /**
     * <p>Invoked when a stream returned from {@link
     * VariantCache#newVariantImageOutputStream(OperationList)} was fully
     * written, closed, and any asynchronous operations completed.</p>
     *
     * <p>May be invoked from another thread.</p>
     *
     * <p>The default implementation is a no-op.</p>
     *
     * @param opList Instance describing the image that was written.
     */
    default void onImageWritten(OperationList opList) {}

}
