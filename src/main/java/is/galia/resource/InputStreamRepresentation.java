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

package is.galia.resource;

import is.galia.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Writes an {@link InputStream} directly to the response.
 */
public class InputStreamRepresentation implements Representation {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(InputStreamRepresentation.class);

    private InputStream inputStream;

    public InputStreamRepresentation(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        try {
            final Stopwatch watch = new Stopwatch();
            inputStream.transferTo(outputStream);
            LOGGER.trace("Written in {}", watch);
        } finally {
            inputStream.close();
        }
    }

}
