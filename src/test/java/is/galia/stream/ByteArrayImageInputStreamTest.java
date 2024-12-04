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

import is.galia.test.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ByteArrayImageInputStreamTest extends BaseTest {

    private final byte[] bytes = new byte[] {
            0x32, (byte) 0xfa, 0x45, 0x03, 0x23, (byte) 0xd4, 0x68, (byte) 0xee };
    private ByteArrayImageInputStream instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new ByteArrayImageInputStream(bytes);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        instance.close();
    }

    /* close() */

    @Test
    void closeAllowsMultipleInvocations() throws Exception {
        instance.close();
        instance.close();
    }

    /* isCached() */

    @Test
    void isCached() {
        assertTrue(instance.isCached());
    }

    /* isCachedMemory() */

    @Test
    void isCachedFile() {
        assertFalse(instance.isCachedFile());
    }

    /* isCachedMemory() */

    @Test
    void isCachedMemory() {
        assertTrue(instance.isCachedMemory());
    }

    /* length() */

    @Test
    void length() {
        assertEquals(bytes.length, instance.length());
    }

    /* read() */

    @Test
    void read1() throws Exception {
        assertEquals(0x32, instance.read());
        assertEquals(0xfa, instance.read());
        assertEquals(2, instance.getStreamPosition());
    }

    @Test
    void read1UpdatesStreamPosition() throws Exception {
        assertEquals(0, instance.getStreamPosition());
        instance.read();
        assertEquals(1, instance.getStreamPosition());
    }

    @Test
    void read1AtEndOfStream() throws Exception {
        instance.seek(bytes.length);
        assertEquals(-1, instance.read());
    }

    /* read(byte[], int, int) */

    @Test
    void read2() throws Exception {
        int offset = 3;
        int length = 5;
        byte[] buffer = new byte[32];

        assertEquals(length, instance.read(buffer, offset, length));
        byte[] expected      = Arrays.copyOfRange(bytes, 0, length);
        byte[] trimmedBuffer = Arrays.copyOfRange(buffer, 3, 3 + length);
        assertArrayEquals(expected, trimmedBuffer);
    }

    @Test
    void read2WithFewerBytesRemainingThanRequested() throws Exception {
        byte[] buffer = new byte[32];
        assertEquals(8, instance.read(buffer, 0, 10));
    }

    @Test
    void read2AtEndOfStream() throws Exception {
        byte[] buffer = new byte[32];
        instance.seek(bytes.length);
        assertEquals(-1, instance.read(buffer, 0, 10));
    }

    /* seek() */

    @Test
    void seek() throws Exception {
        instance.seek(2);
        assertEquals(bytes[2], instance.read());
    }

    @Test
    void seekOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> instance.seek(bytes.length + 3));
    }

}
