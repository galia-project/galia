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

package is.galia.codec;

import is.galia.image.Format;
import is.galia.stream.PathImageInputStream;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.ImageInputStream;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FormatDetectorTest extends BaseTest {

    /* detect(ImageInputStream) */

    @Test
    void detectWithSupportedInputStream() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("png/png.png")) {
            assertEquals(Format.get("png"), FormatDetector.detect(is));
        }
    }

    @Test
    void detectWithUnsupportedInputStream() throws Exception {
        Path fixture = TestUtils.getFixture("text.txt");
        try (ImageInputStream is = new PathImageInputStream(fixture)) {
            assertEquals(Format.UNKNOWN, FormatDetector.detect(is));
        }
    }

    /* detect(Path) */

    @Test
    void detectWithSupportedPath() throws Exception {
        Path fixture = TestUtils.getSampleImage("png/png.png");
        assertEquals(Format.get("png"), FormatDetector.detect(fixture));
    }

    @Test
    void detectWithUnsupportedPath() throws Exception {
        Path fixture = TestUtils.getFixture("text.txt");
        assertEquals(Format.UNKNOWN, FormatDetector.detect(fixture));
    }

}
