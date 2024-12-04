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

package is.galia.codec.tiff;

import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.ImageInputStream;

import java.io.EOFException;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class DirectoryIteratorTest extends BaseTest {

    private ImageInputStream inputStream;
    private DirectoryIterator instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        inputStream = TestUtils.newSampleImageInputStream("tif/multipage.tif");
        DirectoryReader reader = new DirectoryReader();
        reader.addTagSet(new BaselineTIFFTagSet());
        reader.setSource(inputStream);
        instance = reader.iterator();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        inputStream.close();
    }

    /* hasNext() */

    @Test
    void hasNextWithNext() {
        assertTrue(instance.hasNext());
    }

    @Test
    void hasNextWithoutNext() throws Exception {
        for (int i = 0; i < 9; i++) {
            instance.next();
        }
        assertFalse(instance.hasNext());
    }

    /* next() */

    @Test
    void nextWithIllegalIFDOffset() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/illegal-ifd-offset.bin")) {
            DirectoryReader reader = new DirectoryReader();
            reader.addTagSet(new EXIFBaselineTIFFTagSet());
            reader.addTagSet(new EXIFTagSet());
            reader.setSource(is);
            instance = reader.iterator();
            assertThrows(EOFException.class, () -> instance.next());
        }
    }

    @Test
    void nextWithNext() throws Exception {
        assertNotNull(instance.next());
    }

    @Test
    void nextWithoutNext() throws Exception {
        for (int i = 0; i < 9; i++) {
            instance.next();
        }
        assertThrows(NoSuchElementException.class, () -> instance.next());
    }

}
