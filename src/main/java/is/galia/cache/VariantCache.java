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

import is.galia.image.StatResult;
import is.galia.operation.OperationList;
import is.galia.stream.CompletableOutputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * Caches variant images.
 */
public non-sealed interface VariantCache extends Cache {

    /**
     * <p>Deletes the cached image corresponding to the given operation
     * list.</p>
     *
     * <p>If no such image exists, nothing is done.</p>
     *
     * @param opList
     * @throws IOException upon fatal error. Implementations should do the
     *         best they can to complete the operation and swallow and log
     *         non-fatal errors.
     */
    void evict(OperationList opList) throws IOException;

    /**
     * N.B.: This method is not safe to use in production, as it is not
     * concurrency-safe, i.e. the state of the cache may change between the
     * time this method returns and the time {@link
     * #newVariantImageInputStream} is invoked. This default implementation is
     * a convenience for testing that simply returns {@code true} if it can
     * open a stream. Overriding is probably pointless.
     *
     * @param opList Operation list describing the variant image.
     * @return       Whether a valid variant described by the given operation
     *               list exists in the cache.
     */
    default boolean exists(OperationList opList) throws IOException {
        try (InputStream is = newVariantImageInputStream(opList)) {
            return is != null;
        }
    }

    /**
     * @see #newVariantImageInputStream(OperationList, StatResult)
     */
    default InputStream newVariantImageInputStream(OperationList opList)
            throws IOException {
        return newVariantImageInputStream(opList, new StatResult());
    }

    /**
     * <p>Returns an input stream corresponding to the given operation list,
     * or {@code null} if a valid image corresponding to the given operation
     * list does not exist in the cache.</p>
     *
     * <p>If an invalid image corresponding to the given operation list exists
     * in the cache, implementations should delete it (ideally asynchronously)
     * and return {@code null}.</p>
     *
     * @param opList     Operation list for which to retrieve an input stream
     *                   for reading from the cache.
     * @param statResult May be populated with metadata about the cached image
     *                   if one is available and if the implementation supports
     *                   it.
     * @return           Stream corresponding to the given operation list, or
     *                   {@code null} if a valid image does not exist in the
     *                   cache.
     */
    InputStream newVariantImageInputStream(OperationList opList,
                                           StatResult statResult) throws IOException;

    /**
     * <p>Returns an output stream for writing an image to the cache.</p>
     *
     * <p>If an image corresponding to the given identifier already
     * exists, the stream should overwrite it. Implementations may choose to
     * allow multiple streams to write data to the same target concurrently
     * (assuming this is safe), or else allow only one stream to write to a
     * particular target at a time, with other clients writing to no-op
     * streams.</p>
     *
     * <p><strong>Important notes about the {@link
     * CompletableOutputStream#close()} implementation:</strong></p>
     *
     * <ul>
     *     <li>It must check the return value of {@link
     *     CompletableOutputStream#isComplete()} before committing any data to
     *     the cache. If it returns {@code false}, any written data should be
     *     discarded.</li>
     *     <li>It must {@link CacheObserver#onImageWritten(OperationList)
     *     notify all observers} when it completes.</li>
     * </ul>
     *
     * @param opList Operation list describing the target image in the cache.
     * @return       Output stream to which an image corresponding to the given
     *               operation list can be written.
     * @throws IOException upon an I/O error. Any partially written data is
     *         automatically cleaned up.
     * @see CacheObserver#onImageWritten(OperationList)
     */
    CompletableOutputStream newVariantImageOutputStream(OperationList opList)
            throws IOException;

}
