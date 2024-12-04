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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Wraps two sub-streams for pseudo-simultaneous writing.
 */
public class TeeOutputStream extends OutputStream {

    private final OutputStream os1, os2;

    public TeeOutputStream(OutputStream os1, OutputStream os2) {
        this.os1 = os1;
        this.os2 = os2;
    }

    @Override
    public void close() throws IOException {
        try {
            os1.close();
        } finally {
            os2.close();
        }
    }

    @Override
    public void flush() throws IOException {
        os1.flush();
        os2.flush();
    }

    @Override
    public void write(int b) throws IOException {
        os1.write(b);
        os2.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        os1.write(b);
        os2.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        os1.write(b, off, len);
        os2.write(b, off, len);
    }

}
