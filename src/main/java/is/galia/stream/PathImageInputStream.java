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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * <p>Analog of {@link javax.imageio.stream.FileImageInputStream} that uses
 * {@link Path}s instead of {@link java.io.File}s.</p>
 *
 * <p>Also, unlike {@link javax.imageio.stream.FileImageInputStream} which is
 * backed by a {@link java.io.RandomAccessFile}, this implementation is backed
 * by a faster buffered implementation.</p>
 */
public class PathImageInputStream extends ImageInputStreamImpl
        implements ImageInputStream {

    private RandomAccessFile randomAccessFile;

    /**
     * <p>Constructs an instance that will read from a given {@link Path}.</p>
     *
     * <p>The file contents must not change between the time this object is
     * constructed and the time of the last call to a read method.</p>
     *
     * @param path Path to read from.
     *
     * @throws NoSuchFileException if {@code path} is a directory or cannot
     *         be opened for reading for any other reason.
     * @throws IOException if an I/O error occurs.
     */
    public PathImageInputStream(Path path) throws IOException {
        Objects.requireNonNull(path);
        try {
            this.randomAccessFile = new RandomAccessFile(path.toString(), "r");
        } catch (FileNotFoundException e) {
            throw new NoSuchFileException(e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        randomAccessFile.close();
        randomAccessFile = null;
    }

    @Override
    public long length() {
        try {
            checkClosed();
            return randomAccessFile.length();
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public int read() throws IOException {
        checkClosed();
        bitOffset = 0;
        int val = randomAccessFile.read();
        if (val != -1) {
            ++streamPos;
        }
        return val;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkClosed();
        bitOffset = 0;
        int nbytes = randomAccessFile.read(b, off, len);
        if (nbytes != -1) {
            streamPos += nbytes;
        }
        return nbytes;
    }

    @Override
    public void seek(long pos) throws IOException {
        checkClosed();
        if (pos < flushedPos) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }
        bitOffset = 0;
        randomAccessFile.seek(pos);
        streamPos = randomAccessFile.getFilePointer();
    }

}
