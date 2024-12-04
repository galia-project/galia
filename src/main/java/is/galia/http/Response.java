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

package is.galia.http;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public interface Response extends Closeable {

    byte[] getBody() throws IOException;

    /**
     * @return A {@link ByteArrayInputStream} around the return value of {@link
     *         #getBody()}. Implementations should override if they can be more
     *         efficient.
     */
    default InputStream getBodyAsStream() throws IOException {
        return new ByteArrayInputStream(getBody());
    }

    /**
     * @return New UTF-8 string based on the return value of {@link
     *         #getBody()}.
     */
    default String getBodyAsString() throws IOException {
        return new String(getBody(), StandardCharsets.UTF_8);
    }

    Headers getHeaders();

    Status getStatus();

    Transport getTransport();

}
