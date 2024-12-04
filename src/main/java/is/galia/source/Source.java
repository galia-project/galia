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

import is.galia.delegate.Delegate;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.image.StatResult;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * <p>Locates, provides access to, and infers the format of a source image.
 * This is an abstract interface; at least one of the sub-interfaces must be
 * implemented.</p>
 *
 * <p>Methods are called in the following order:</p>
 *
 * <ol>
 *     <li>{@link #setIdentifier(Identifier)} and
 *     {@link #setDelegate(Delegate)} (in either order)</li>
 *     <li>{@link #stat()}</li>
 *     <li>Any other methods</li>
 *     <li>{@link #shutdown()}</li>
 * </ol>
 */
public interface Source {

    /**
     * @return Identifier of the source image to read.
     */
    Identifier getIdentifier();

    /**
     * @param identifier Identifier of the source image to read.
     */
    void setIdentifier(Identifier identifier);

    /**
     * <p>Checks the accessibility of the source image and returns some limited
     * metadata.</p>
     *
     * <p>Will be called only once.</p>
     *
     * @return Instance with as many of its properties set as possible.
     * @throws NoSuchFileException if an image corresponding to the set
     *         identifier does not exist.
     * @throws AccessDeniedException if an image corresponding to the set
     *         identifier is not readable.
     * @throws IOException if there is some other issue accessing the image.
     */
    StatResult stat() throws IOException;

    /**
     * N.B.: This default implementation throws an {@link
     * UnsupportedOperationException}. It must be overridden if {@link
     * #supportsFileAccess()} returns {@code true}.
     *
     * @return File referencing the source image corresponding to the
     *         identifier set with {@link #setIdentifier}; never {@code null}.
     * @throws UnsupportedOperationException if access to files is not {@link
     *         #supportsFileAccess() supported}.
     * @throws IOException if anything goes wrong.
     */
    default Path getFile() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>Returns an iterator over the results of various techniques of
     * checking the format, in the order of least to most expensive.</p>
     *
     * <p>A typical sequence of checks would be:</p>
     *
     * <ol>
     *     <li>Filename extension (using {@link Format#inferFormat(String)}</li>
     *     <li>Identifier extension (using {@link
     *     Format#inferFormat(Identifier)})</li>
     *     <li>Magic bytes (using {@link is.galia.codec.FormatDetector})</li>
     * </ol>
     *
     * <p>Any of the calls to {@link Iterator#next()} may return either an
     * inaccurate value, or {@link Format#UNKNOWN}. Clients should proceed
     * using the first non-unknown format they encounter and, if this turns out
     * to be wrong, iterate and try again.</p>
     *
     * @return Iterator over whatever format-inference strategies the instance
     *         supports. <strong>The instance is cached and the same one is
     *         returned every time.</strong>
     */
    Iterator<Format> getFormatIterator();

    /**
     * @return Instance from which to read the source image identified by the
     *         identifier passed to {@link #setIdentifier}; never {@code null}.
     * @throws IOException if anything goes wrong.
     */
    ImageInputStream newInputStream() throws IOException;

    /**
     * @param delegate Delegate for the current request.
     */
    void setDelegate(Delegate delegate);

    /**
     * <p>Shuts down the instance and any of its shared resource handles,
     * threads, etc.</p>
     *
     * <p>Only called at the end of the application lifecycle.</p>
     *
     * <p>The default implementation does nothing.</p>
     */
    default void shutdown() {}

    /**
     * <p>N.B. 1: This method's return value affects the behavior of {@link
     * #getFile()}. See the documentation of that method for more
     * information.</p>
     *
     * <p>N.B. 2: The default implementation returns {@code false}.</p>
     *
     * @return Whether the source image can be accessed via a {@link Path}.
     */
    default boolean supportsFileAccess() {
        return false;
    }

}
