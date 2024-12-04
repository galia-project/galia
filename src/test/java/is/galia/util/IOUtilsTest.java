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

import is.galia.stream.ByteArrayImageInputStream;
import is.galia.stream.PathImageInputStream;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IOUtilsTest extends BaseTest {

    /* closeQuietly() */

    @Test
    void closeQuietlyWithNullArgument() {
        IOUtils.closeQuietly(null);
    }

    @Test
    void closeQuietlyWithArgument() {
        class IntrospectiveInputStream extends InputStream {
            boolean isClosed = false;
            @Override
            public void close() throws IOException {
                super.close();
                isClosed = true;
            }
            @Override
            public int read() {
                return 0;
            }
        }
        final IntrospectiveInputStream stream = new IntrospectiveInputStream();
        IOUtils.closeQuietly(stream);
        assertTrue(stream.isClosed);
    }

    /* consume() */

    @Test
    void consume() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/icc-chunked.jpg");
        try (ImageInputStream is = new PathImageInputStream(file)) {
            IOUtils.consume(is);
            assertEquals(is.getStreamPosition(), Files.size(file));
        }
    }

    /* transfer(ImageInputStream, OutputStream) */

    @Test
    void transfer() throws Exception {
        Path file = TestUtils.getSampleImage("jpg/icc-chunked.jpg");
        byte[] bytes = Files.readAllBytes(file);
        try (ImageInputStream is = new ByteArrayImageInputStream(bytes);
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            IOUtils.transfer(is, os);
            assertArrayEquals(bytes, os.toByteArray());
        }
    }

}