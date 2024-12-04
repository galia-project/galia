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

import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArrayUtilsTest extends BaseTest {

    /* chunkify() */

    @Test
    void chunkifyWithSingleChunk() {
        byte[] data = { 0x00, 0x01, 0x02, 0x03, 0x04 };
        List<byte[]> chunks = ArrayUtils.chunkify(data, 5);
        assertEquals(1, chunks.size());
        assertArrayEquals(data, chunks.getFirst());
    }

    @Test
    void chunkifyWithMultipleChunks() {
        byte[] data = { 0x00, 0x01, 0x02, 0x03, 0x04 };
        List<byte[]> chunks = ArrayUtils.chunkify(data, 2);
        assertEquals(3, chunks.size());
        assertArrayEquals(new byte[] { 0x00, 0x01 }, chunks.get(0));
        assertArrayEquals(new byte[] { 0x02, 0x03 }, chunks.get(1));
        assertArrayEquals(new byte[] { 0x04 }, chunks.get(2));
    }

    @Test
    void chunkifyWithEmptyArgument() {
        byte[] data = {};
        List<byte[]> chunks = ArrayUtils.chunkify(data, 5);
        assertEquals(1, chunks.size());
        assertArrayEquals(data, chunks.getFirst());
    }

    /* indexOf() */

    @Test
    void indexOfWithMatch() {
        byte[] array = {1, 2, 3, 4};
        assertEquals(0, ArrayUtils.indexOf(array, new byte[]{1, 2}, 0));
        assertEquals(0, ArrayUtils.indexOf(array, new byte[]{1, 2, 3, 4}, 0));
        assertEquals(1, ArrayUtils.indexOf(array, new byte[]{2, 3, 4}, 0));
        assertEquals(2, ArrayUtils.indexOf(array, new byte[]{3, 4}, 0));
    }

    @Test
    void indexOfWithNoMatch() {
        byte[] array = {1, 2, 3, 4};
        assertEquals(-1, ArrayUtils.indexOf(array, new byte[]{1, 2, 3, 4, 5}, 0));
        assertEquals(-1, ArrayUtils.indexOf(array, new byte[]{3, 4, 5}, 0));
        assertEquals(-1, ArrayUtils.indexOf(array, new byte[]{4, 5}, 0));
        assertEquals(-1, ArrayUtils.indexOf(array, new byte[]{4, 5, 6, 7, 8}, 0));
    }

    @Test
    void indexOfWithPositiveOffset() {
        byte[] array = {1, 2, 3, 4};
        assertEquals(-1, ArrayUtils.indexOf(array, new byte[]{1, 2}, 1));
        assertEquals(1, ArrayUtils.indexOf(array, new byte[]{2, 3, 4}, 1));
        assertEquals(2, ArrayUtils.indexOf(array, new byte[]{3, 4}, 2));
    }

    /* merge() */

    @Test
    void mergeWithEmptyList() {
        List<byte[]> arrays = new LinkedList<>();

        assertArrayEquals(new byte[] {}, ArrayUtils.merge(arrays));
    }

    @Test
    void mergeWithOneByteArray() {
        List<byte[]> arrays = new LinkedList<>();
        arrays.add(new byte[] { 0x32 });

        assertArrayEquals(new byte[] { 0x32 }, ArrayUtils.merge(arrays));
    }

    @Test
    void mergeWithMultipleByteArrays() {
        List<byte[]> arrays = new LinkedList<>();
        arrays.add(new byte[] { 0x32, 0x38 });
        arrays.add(new byte[] { 0x1f });

        assertArrayEquals(new byte[] { 0x32, 0x38, 0x1f },
                ArrayUtils.merge(arrays));
    }

    /* reverse() */

    @Test
    void reverse() {
        byte[] inArray  = { 0x25, 0x3c, 0x0a };
        byte[] expected = { 0x0a, 0x3c, 0x25 };
        byte[] actual   = ArrayUtils.reverse(inArray);
        assertArrayEquals(expected, actual);
    }

    /* startsWith(byte[], byte[]) */

    @Test
    void startsWith1WithLongerNeedle() {
        byte[] haystack = new byte[] { 0x3d, 0x45, 0x12, 0x0a, 0x54 };
        byte[] needle   = new byte[] { 0x3d, 0x45, 0x12, 0x0a, 0x54, 0x36 };
        assertFalse(ArrayUtils.startsWith(haystack, needle));
    }

    @Test
    void startsWith1WithMatch() {
        byte[] inArray = new byte[] { 0x3d, 0x45, 0x12, 0x0a, 0x54 };
        byte[] bytes   = new byte[] { 0x3d, 0x45, 0x12, 0x0a, 0x54 };
        assertTrue(ArrayUtils.startsWith(inArray, bytes));

        inArray = new byte[] { 0x3d, 0x45, 0x12, 0x0a, 0x54 };
        bytes   = new byte[] { 0x3d, 0x45, 0x12 };
        assertTrue(ArrayUtils.startsWith(inArray, bytes));
    }

    @Test
    void startsWith1WithoutMatch() {
        byte[] inArray = new byte[] { 0x3d, 0x45, 0x12, 0x0a, 0x54 };
        byte[] bytes   = new byte[] { 0x3d, 0x45, 0x12, 0x0a, 0x52 };
        assertFalse(ArrayUtils.startsWith(inArray, bytes));

        inArray = new byte[] { 0x3d, 0x45, 0x12, 0x0a, 0x54 };
        bytes   = new byte[] { 0x3d, 0x45, 0x12, 0x0a, 0x54, 0x53 };
        assertFalse(ArrayUtils.startsWith(inArray, bytes));
    }

    /* startsWith(byte[], byte[], int) */

    @Test
    void startsWith2WithLongerNeedle() {
        byte[] haystack = new byte[] { 0x3d, 0x45, 0x12, 0x0a, 0x54 };
        byte[] needle   = new byte[] { 0x3d, 0x45, 0x12, 0x0a, 0x54, 0x36 };
        assertFalse(ArrayUtils.startsWith(haystack, needle, 3));
    }

    @Test
    void startsWith2WithMatch() {
        byte[] haystack = new byte[] { 0, 0, 0x3d, 0x45, 0x12, 0x0a, 0x54 };
        byte[] needle   = new byte[] { 0x3d, 0x45, 0x12, 0x0a, 0x54 };
        assertTrue(ArrayUtils.startsWith(haystack, needle, 2));
    }

    @Test
    void startsWith2WithoutMatch() {
        byte[] haystack = new byte[] { 0, 0, 0x3d, 0x45, 0x12, 0x0a, 0x54 };
        byte[] needle   = new byte[] { 0x3d, 0x45, 0x12, 0x0a, 0x52 };
        assertFalse(ArrayUtils.startsWith(haystack, needle));
    }

}