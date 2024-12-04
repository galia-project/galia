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

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Implementation backed by a byte array.
 */
public class ByteArrayImageInputStream extends ImageInputStreamImpl
        implements ImageInputStream {

    private final ByteBuffer buffer;
    private boolean isClosed;

    public ByteArrayImageInputStream(byte[] bytes) {
        buffer = ByteBuffer.wrap(bytes);
    }

    /**
     * Override that allows multiple invocations.
     */
    @Override
    public void close() throws IOException {
        if (!isClosed) {
            super.close();
            isClosed = true;
        }
    }

    @Override
    public long getStreamPosition() throws IOException {
        checkClosed();
        return buffer.position();
    }

    @Override
    public boolean isCached() {
        return true;
    }

    @Override
    public boolean isCachedMemory() {
        return true;
    }

    @Override
    public long length() {
        return buffer.array().length;
    }

    @Override
    public int read() throws IOException {
        try {
            setBitOffset(0);
            int result = buffer.get() & 0xff;
            streamPos += 1;
            return result;
        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            setBitOffset(0);
            final long pos    = getStreamPosition();
            final long length = length();
            if (pos >= length) {
                return -1;
            } else if (pos + len > length) {
                len = (int) (length - pos);
            }
            buffer.get(b, off, len);
            streamPos += len;
            return len;
        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    @Override
    public void seek(long pos) throws IOException {
        super.seek(pos);
        if (pos > length()) {
            throw new IndexOutOfBoundsException("position > length");
        }
        buffer.position((int) pos);
    }

    @Override
    public void setByteOrder(ByteOrder byteOrder) {
        super.setByteOrder(byteOrder);
        buffer.order(byteOrder);
    }

}
