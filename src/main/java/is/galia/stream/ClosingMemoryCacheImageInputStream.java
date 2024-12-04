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

package is.galia.stream;

import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Override whose {@link #close()} method also closes the wrapped
 * {@link InputStream}.
 */
public class ClosingMemoryCacheImageInputStream
        extends MemoryCacheImageInputStream {

    /**
     * We have to maintain our own reference to this, because the one in super
     * is private.
     */
    private final InputStream wrappedStream;

    public ClosingMemoryCacheImageInputStream(InputStream stream) {
        super(stream);
        this.wrappedStream = stream;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } catch (IOException e) {
            if (!"closed".equals(e.getMessage())) {
                throw e;
            }
        } finally {
            wrappedStream.close();
        }
    }

}
