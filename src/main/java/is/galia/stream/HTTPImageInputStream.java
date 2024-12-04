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

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Range;
import is.galia.http.Reference;
import is.galia.http.Response;
import is.galia.util.ObjectCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;

/**
 * <p>Input stream that supports pseudo-seeking over HTTP.</p>
 *
 * <p>The stream is divided conceptually into fixed-size windows from which
 * chunks of data are fetched as needed using ranged HTTP requests. This
 * technique may improve efficiency when reading small portions of large images
 * that are selectively readable, like JPEG2000 and multiresolution+tiled TIFF,
 * over low-bandwidth connections. Conversely, it may reduce efficiency when
 * reading large portions of images.</p>
 *
 * <p>Downloaded chunks can be cached in memory. This could help when readers
 * seek around a lot beyond the window size, or request a lot more data than
 * they would theoretically need.</p>
 *
 * <p>The HTTP client is abstracted into the exceedingly simple {@link
 * HTTPImageInputStreamClient} interface, so probably any existing client
 * implementation, including many cloud storage clients, can be hooked up and
 * used easily, without this class needing to know about things like SSL/TLS,
 * request signing, etc.</p>
 *
 * <p>This class works only with HTTP servers that support {@code Range}
 * requests, as advertised by the presence of a {@code Accept-Ranges: bytes}
 * header in a {@code HEAD} response.</p>
 */
public class HTTPImageInputStream extends ImageInputStreamImpl
        implements ImageInputStream {

    record ChunkKey(Reference url, Range range) {
        @Override
        public String toString() {
            return "[URI: " + url + "] [range: " + range + "]";
        }
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HTTPImageInputStream.class);

    /**
     * Enables debug logging, which may be very expensive.
     */
    private static final boolean DEBUG = false;

    /**
     * Can be overridden by {@link #setWindowSize(int)}.
     */
    private static final int DEFAULT_WINDOW_SIZE = 1024 * 512;

    private static final ObjectCache<ChunkKey,byte[]> CHUNK_CACHE =
            new ObjectCache<>(getMaxChunkCacheSize());

    private HTTPImageInputStreamClient client;
    private long streamLength   = -1;
    private int windowPos;
    private int windowSize      = DEFAULT_WINDOW_SIZE;
    private int windowIndex     = -1;
    private byte[] windowBuffer = new byte[windowSize];

    private int numChunkDownloads, numChunkCacheHits, numChunkCacheMisses;
    private long numBytesDownloaded, numBytesRead;

    private static void debug(String message, Object... vars) {
        if (DEBUG) {
            LOGGER.trace(message, vars);
        }
    }

    public static long getMaxChunkCacheSize() {
        return Configuration.forApplication()
                .getLongBytes(Key.CHUNK_CACHE_MAX_SIZE, 0);
    }

    public static boolean isChunkCacheEnabled() {
        return Configuration.forApplication()
                .getBoolean(Key.CHUNK_CACHE_ENABLED, false);
    }

    /**
     * Variant that sends a preliminary {@code HEAD} request to retrieve
     * some needed information. Use {@link #HTTPImageInputStream(
     * HTTPImageInputStreamClient, long)} instead if you already know the
     * resource length and that the server supports ranged requests.
     *
     * @param client Client to use to handle requests.
     * @throws RangesNotSupportedException if the server does not advertise
     *                                     support for ranges.
     * @throws IOException if the response does not include a valid {@code
     *                     Content-Length} header or some other communication
     *                     error occurs.
     */
    public HTTPImageInputStream(HTTPImageInputStreamClient client)
            throws IOException {
        this.client = client;
        sendHEADRequest();
    }

    /**
     * @param client         Client to use to handle requests.
     * @param resourceLength Resource length/size.
     */
    public HTTPImageInputStream(HTTPImageInputStreamClient client,
                                long resourceLength) {
        this.client       = client;
        this.streamLength = resourceLength;
    }

    public int getWindowSize() {
        return windowSize;
    }

    /**
     * <p>Sets the window size. Must be called before any reading or seeking
     * occurs.</p>
     *
     * <p>In general, a smaller size means more requests may be needed, and a
     * larger size means more irrelevant data will have to be read and
     * discarded. The optimal size will vary depending on the source image, the
     * amount of data needed from it, and network transfer rate vs.
     * latency. The size should probably always be at least a few KB so as to
     * be able to read the image header in one request.</p>
     *
     * @param windowSize Window/chunk size.
     */
    public void setWindowSize(int windowSize) {
        this.windowSize   = windowSize;
        this.windowBuffer = new byte[windowSize];
    }

    /**
     * Checks whether the server supports the {@code Range} header and reads
     * the resource length from the {@code Content-Length} header.
     */
    private void sendHEADRequest() throws IOException {
        try (Response response = client.sendHEADRequest()) {
            if (!"bytes".equals(response.getHeaders().getFirstValue("Accept-Ranges"))) {
                throw new RangesNotSupportedException();
            }
            try {
                streamLength = Long.parseLong(
                        response.getHeaders().getFirstValue("Content-Length"));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid or missing Content-Length header");
            }
        }
    }

    @Override
    public void close() throws IOException {
        logStatistics();
        try {
            super.close();
        } finally {
            client       = null;
            windowBuffer = null;
        }
    }

    private void logStatistics() {
        if (numChunkDownloads > 0) {
            LOGGER.debug("Downloaded {} chunks ({} ({}%) of {} bytes); " +
                            "read {}% of chunk data; {} cache hits; {} misses",
                    numChunkDownloads,
                    numBytesDownloaded,
                    String.format("%.2f", numBytesDownloaded * 100 / (double) streamLength),
                    streamLength,
                    String.format("%.2f", numBytesRead * 100 / (double) numBytesDownloaded),
                    numChunkCacheHits,
                    numChunkCacheMisses);
        } else {
            LOGGER.debug("Downloaded 0 chunks; {} (100%) cache hits",
                    numChunkCacheHits);
        }
    }

    /**
     * Invalidates any cached data lying entirely before the stream position.
     */
    @Override
    public void flushBefore(long pos) throws IOException {
        super.flushBefore(pos);
        CHUNK_CACHE.asMap().keySet()
                .stream()
                .filter(chunk -> chunk.url.equals(client.getReference()) &&
                        chunk.range.end() < streamPos)
                .forEach(CHUNK_CACHE::remove);
    }

    @Override
    public long length() {
        return streamLength;
    }

    @Override
    public int read() throws IOException {
        if (streamPos >= streamLength) {
            return -1;
        }

        debug("read(): begin [pos: {}] [windowPos: {}]",
                streamPos, windowPos);

        prepareWindowBuffer();
        bitOffset = 0;
        int b = windowBuffer[windowPos] & 0xff;
        numBytesRead++;
        windowPos++;
        streamPos++;

        debug("read(): end [pos: {}] [windowPos: {}]",
                streamPos, windowPos);
        return b;
    }

    @Override
    public int read(byte[] b,
                    int offset,
                    int requestedLength) throws IOException {
        // Boilerplate checks required by the method contract.
        if (streamPos >= streamLength) {
            return -1;
        } else if (offset < 0) {
            throw new IndexOutOfBoundsException("Negative offset");
        } else if (requestedLength < 0) {
            throw new IndexOutOfBoundsException("Negative length");
        } else if (offset + requestedLength > b.length) {
            throw new IndexOutOfBoundsException("offset + length > buffer length");
        }

        // Also required by the method contract.
        bitOffset = 0;

        debug("read(byte[],int,int): begin [pos: {}] [windowPos: {}] [requested: {}]",
                streamPos, windowPos, requestedLength);

        // N.B.: although the method contract says we don't have to read all of
        // requestedLength as long as we return a count of what we did read,
        // in practice, readers won't' always check the return value, so we
        // will try to read as much of requestedLength as possible.
        int fulfilledLength = 0, incrementLength = 0;
        while (fulfilledLength < requestedLength && incrementLength != -1) {
            int remainingLength = requestedLength - fulfilledLength;
            incrementLength = readIncrement(b, offset, remainingLength);
            fulfilledLength += incrementLength;
            offset          += incrementLength;
        }

        debug("read(byte[],int,int): end [pos: {}] [windowPos: {}] [fulfilled: {}]",
                streamPos, windowPos, fulfilledLength);

        return fulfilledLength;
    }

    /**
     * Reads the smaller of {@code requestedLength} or the remaining window
     * size into a byte array.
     *
     * @return Number of bytes read, or {@code -1} when there are no more
     *         bytes to read.
     */
    private int readIncrement(byte[] b,
                              int offset,
                              int requestedLength) throws IOException {
        prepareWindowBuffer();

        final int fulfilledLength = Math.min(
                requestedLength,
                windowBuffer.length - windowPos);
        System.arraycopy(
                windowBuffer, windowPos, // from, from index
                b, offset,               // to, to index
                fulfilledLength);        // length

        numBytesRead += fulfilledLength;
        windowPos    += fulfilledLength;
        streamPos    += fulfilledLength;

        return (fulfilledLength > 0) ? fulfilledLength : -1;
    }

    @Override
    public void seek(long pos) throws IOException {
        super.seek(pos);
        windowPos = getIndexWithinWindow();

        debug("seek(): [pos: {}] [windowPos: {}]",
                streamPos, windowPos);
    }

    /**
     * Checks that {@link #windowBuffer} has some readable bytes remaining,
     * and fills it with more if not.
     */
    private void prepareWindowBuffer() throws IOException {
        final int neededWindowIndex = getStreamWindowIndex();
        if (neededWindowIndex != windowIndex) {
            Range range  = getRange(neededWindowIndex);
            ChunkKey chunkKey = new ChunkKey(client.getReference(), range);
            windowBuffer = fetchChunk(chunkKey);
            windowIndex  = neededWindowIndex;
            windowPos    = getIndexWithinWindow();
        }
    }

    /**
     * Fetches a chunk for the given range by either retrieving it from the
     * chunk cache or downloading it.
     */
    private byte[] fetchChunk(ChunkKey chunkKey) throws IOException {
        byte[] chunkBytes;
        if (isChunkCacheEnabled()) {
            chunkBytes = CHUNK_CACHE.get(chunkKey);
            if (chunkBytes != null) {
                LOGGER.trace("Chunk cache hit: {}", chunkKey);
                numChunkCacheHits++;
            } else {
                LOGGER.trace("Chunk cache miss: {}", chunkKey);
                numChunkCacheMisses++;
                chunkBytes = downloadChunk(chunkKey);
                CHUNK_CACHE.put(chunkKey, chunkBytes);
            }
        } else {
            numChunkCacheMisses++;
            chunkBytes = downloadChunk(chunkKey);
        }
        return chunkBytes;
    }

    private byte[] downloadChunk(ChunkKey chunkKey) throws IOException {
        debug("Downloading chunk: {}", chunkKey);
        try (Response response  = client.sendGETRequest(chunkKey.range)) {
            byte[] entity = response.getBody();
            numBytesDownloaded += entity.length;
            numChunkDownloads++;
            return entity;
        }
    }

    private Range getRange(int windowIndex) {
        long start = (long) windowIndex * windowSize;
        long end   = Math.min(start + windowSize, streamLength) - 1;
        return new Range(start, end, streamLength);
    }

    private int getIndexWithinWindow() {
        return (int) (streamPos % (long) windowSize);
    }

    /**
     * Calculates the current window index based on {@link #streamPos}, but
     * does not update {@link #windowIndex}.
     */
    private int getStreamWindowIndex() {
        return (int) Math.floor(streamPos / (double) windowSize);
    }

}
