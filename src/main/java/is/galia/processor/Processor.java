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

package is.galia.processor;

import is.galia.codec.Decoder;
import is.galia.codec.Encoder;
import is.galia.config.Configuration;
import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.operation.Operation;
import is.galia.operation.OperationList;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.foreign.Arena;

/**
 * <p>Encapsulates an image processing engine.</p>
 *
 * <p>Implementations can depend on their {@link #setDecoder(Decoder) decoder}
 * and {@link #setEncoder(Encoder) encoder} being set before any other methods
 * are called.</p>
 */
public interface Processor {

    /**
     * Called before {@link #process}.
     *
     * @param arena Instance in which to perform any native operations.
     */
    void setArena(Arena arena);

    /**
     * Will be called before {@link #process}.
     *
     * @param decoder Instance from which to read the image.
     */
    void setDecoder(Decoder decoder);

    /**
     * Will be called before {@link #process}.
     *
     * @param encoder Instance to which to write the image.
     */
    void setEncoder(Encoder encoder);

    /**
     * <p>Reads the image from the {@link #setDecoder(Decoder) decoder},
     * performs the supplied operations on it, and writes the result to the
     * supplied stream using the {@link #setEncoder(Encoder) encoder}.</p>
     *
     * <p>Implementation notes:</p>
     *
     * <ul>
     *     <li>The {@link OperationList} will be {@link OperationList#freeze()
     *     frozen}. Implementations should not perform any operations other
     *     than the ones in the list, as this could cause problems with
     *     caching.</li>
     *     <li>{@link Operation}s should be applied in the order they are
     *     iterated.</li>
     *     <li>In addition to operations, the {@link OperationList} may contain
     *     {@link OperationList#getOptions() options}, which implementations
     *     should respect, where applicable. Option values may originate from
     *     the {@link Configuration#forApplication() application configuration},
     *     so implementations should not try to read the application
     *     configuration themselves, as this could also cause problems with
     *     caching.</li>
     *     <li>The arguments should be assumed to all be valid. The provided
     *     {@link OutputStream} is likely going to write to the response, so it
     *     is already too late to e.g. render an error page.</li>
     * </ul>
     *
     * @param opList       Operation list to process, which has already been
     *                     {@link OperationList#validate(Size, Format)
     *                     validated}.
     * @param outputStream Stream to write the image to, which should not be
     *                     closed.
     * @throws IOException if anything else goes wrong.
     */
    void process(OperationList opList,
                 OutputStream outputStream) throws IOException;

}
