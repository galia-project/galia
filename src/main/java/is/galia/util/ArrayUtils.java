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

package is.galia.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ArrayUtils {

    /**
     * Splits the given data into chunks no larger than the given size. The
     * last chunk may be smaller than the others.
     *
     * @param bytes        Data to chunkify.
     * @param maxChunkSize Maximum chunk size.
     * @return             Chunked data.
     */
    public static List<byte[]> chunkify(byte[] bytes, int maxChunkSize) {
        final int listSize = (int) Math.ceil(bytes.length / (double) maxChunkSize);
        final List<byte[]> chunks = new ArrayList<>(listSize);
        if (bytes.length <= maxChunkSize) {
            chunks.add(bytes);
        } else {
            for (int startOffset = 0;
                 startOffset < bytes.length;
                 startOffset += maxChunkSize) {
                int endOffset = startOffset + maxChunkSize;
                if (endOffset > bytes.length) {
                    endOffset = bytes.length;
                }
                byte[] chunk = Arrays.copyOfRange(bytes, startOffset, endOffset);
                chunks.add(chunk);
            }
        }
        return chunks;
    }

    /**
     *
     * @param array        Bytes to search within.
     * @param search       Bytes to search for.
     * @param searchOffset Offset within {@code array} to start the search.
     * @return             Offset of the first match, or {@code -1} if not
     *                     found.
     */
    public static int indexOf(byte[] array, byte[] search, int searchOffset) {
        final int arrayLength  = array.length;
        final int searchLength = search.length;
        if (searchLength > arrayLength) {
            return -1;
        } else if (searchLength == arrayLength && Arrays.equals(array, search)) {
            return 0;
        } else {
            for (int i = searchOffset; i < arrayLength - searchLength + 1; i++) {
                boolean found = true;
                for (int j = 0; j < searchLength; j++) {
                    if (array[i + j] != search[j]) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * @param chunks Ordered list of chunks to merge.
     * @return       Merged chunks.
     */
    public static byte[] merge(List<byte[]> chunks) {
        if (chunks.isEmpty()) {
            return new byte[] {};
        } else if (chunks.size() == 1) {
            return chunks.getFirst();
        }

        int totalLength = 0;
        for (byte[] chunk : chunks) {
            totalLength += chunk.length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);

        for (byte[] chunk : chunks) {
            buffer.put(chunk);
        }
        return buffer.array();
    }

    /**
     * @param inArray Array to reverse.
     * @return New reversed array.
     */
    public static byte[] reverse(byte[] inArray) {
        final int length = inArray.length;
        byte[] reversed = new byte[inArray.length];
        for (int i = 0; i < length; i++) {
            reversed[length - 1 - i] = inArray[i];
        }
        return reversed;
    }

    /**
     * @param haystack Bytes to search against.
     * @param needle   Bytes to search for.
     * @return Whether {@code haystack} starts with {@code needle}.
     */
    public static boolean startsWith(byte[] haystack, byte[] needle) {
        return startsWith(haystack, needle, 0);
    }

    /**
     * @param haystack       Bytes to search against.
     * @param needle         Bytes to search for.
     * @param haystackOffset Offset within {@code haystack} to start searching.
     * @return Whether {@code haystack} starts with {@code needle} at the given
     *         offset.
     */
    public static boolean startsWith(byte[] haystack,
                                     byte[] needle,
                                     int haystackOffset) {
        if (needle.length + haystackOffset > haystack.length) {
            return false;
        }
        return Arrays.equals(
                haystack,
                haystackOffset,
                needle.length + haystackOffset,
                needle,
                0,
                needle.length);
    }

    private ArrayUtils() {}

}
