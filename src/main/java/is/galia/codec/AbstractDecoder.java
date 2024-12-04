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

import javax.imageio.stream.ImageInputStream;
import java.lang.foreign.Arena;
import java.nio.file.Path;

/**
 * {@link Decoder} implementations may subclass this to get some free
 * functionality.
 */
public abstract class AbstractDecoder {

    protected Arena arena;
    protected Path imageFile;
    protected ImageInputStream inputStream;

    public void close() {}

    public void setArena(Arena arena) {
        this.arena = arena;
    }

    /**
     * Sets the source to the given file, and nullifies (but does not close)
     * the input stream, if set.
     *
     * @see Decoder#setSource(Path)
     */
    public void setSource(Path imageFile) {
        this.imageFile   = imageFile;
        this.inputStream = null;
    }

    /**
     * Sets the source to the given stream, and nullifies the file source, if
     * set.
     *
     * @see Decoder#setSource(ImageInputStream)
     */
    public void setSource(ImageInputStream inputStream) {
        this.inputStream = inputStream;
        this.imageFile   = null;
    }

}
