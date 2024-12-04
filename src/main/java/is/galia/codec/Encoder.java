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

package is.galia.codec;

import is.galia.config.Configuration;
import is.galia.image.Format;
import is.galia.image.Metadata;
import is.galia.operation.Encode;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.util.Set;

/**
 * <p>Image encoder.</p>
 *
 * <p>N.B.: <strong>Implementations should not obtain encoding options from the
 * {@link Configuration#forApplication application configuration}.</strong>
 * Instead they should obtain them from the {@link Encode#getOptions() encoding
 * options}, which will be pre-populated with all options that are prefixed
 * with {@link Encode#OPTION_PREFIX}. Ergo, all option keys used by encoders
 * should start with that prefix.</p>
 */
public interface Encoder extends AutoCloseable {

    /**
     * Releases all resources. Must be called when an instance is no longer
     * needed.
     */
    @Override
    void close();

    /**
     * @return All formats supported by the instance. Note that the
     *         implementation must create the instances from scratch and
     *         <strong>not</strong> obtain them from the {@link
     *         is.galia.image.FormatRegistry format registry}, which may be
     *         empty at the time this method is invoked.
     */
    Set<Format> getSupportedFormats();

    /**
     * <p>Implementations that make use of the Foreign Function &amp; Memory
     * API should do their native work in this instance. They should not
     * close it.</p>
     *
     * <p>The default implementation does nothing.</p>
     *
     * @param arena Arena in which to allocate foreign memory.
     */
    default void setArena(Arena arena) {
    }

    /**
     * <p>Implementations should respect the settings of this instance,
     * including its {@link Metadata#getXMP()} XMP metadata}, which they should
     * try to embed, and any relevant {@link Encode#getOptions() encoding
     * options}.</p>
     *
     * <p>N.B.: {@link Encode#getOptions() Option keys} must conform to the
     * syntax described in the documentation of {@link Encode#setOption(String,
     * Object)}.</p>
     *
     * @param encode Instance specifying encoding parameters.
     */
    void setEncode(Encode encode);

    /**
     * Encodes the given image to the given stream.
     *
     * @param image        Image to write.
     * @param outputStream Stream to write the image to, which will not be
     *                     closed.
     */
    void encode(RenderedImage image,
                OutputStream outputStream) throws IOException;

    /**
     * <p>Encodes the given image sequence to the given stream.</p>
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}.</p>
     *
     * @param sequence     Image sequence to write.
     * @param outputStream Stream to write the image to, which will not be
     *                     closed.
     * @throws UnsupportedOperationException if the implementation does not
     *         support sequences.
     */
    default void encode(BufferedImageSequence sequence,
                        OutputStream outputStream) throws IOException {
        throw new UnsupportedOperationException();
    }

}
