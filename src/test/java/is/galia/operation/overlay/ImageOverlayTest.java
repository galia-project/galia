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

package is.galia.operation.overlay;

import is.galia.image.Size;
import is.galia.image.ScaleConstraint;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import is.galia.util.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ImageOverlayTest extends BaseTest {

    private static final Path OVERLAY_FIXTURE =
            TestUtils.getSampleImage("jpg/jpg.jpg");

    private ImageOverlay instance;

    @BeforeEach
    public void setUp() throws Exception {
        URI imageURI = OVERLAY_FIXTURE.toUri();
        instance = new ImageOverlay(imageURI, Position.BOTTOM_RIGHT, 5);
    }

    @Test
    void openStream() throws Exception {
        try (ImageInputStream is = instance.openStream();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            IOUtils.transfer(is, os);
            assertEquals(Files.size(OVERLAY_FIXTURE), os.toByteArray().length);
        }
    }

    @Test
    void openStreamWithNonexistentImage() {
        instance = new ImageOverlay(new File("/dev/cats").toURI(),
                Position.BOTTOM_RIGHT, 5);
        assertThrows(IOException.class, () -> {
            try (ImageInputStream is = instance.openStream()) {
            }
        });
    }

    @Test
    void setURIThrowsExceptionWhenFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setURI(new URI("http://example.org/cats")));
    }

    @Test
    void toMap() {
        Size fullSize = new Size(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(instance.getURI().toString(), map.get("uri"));
        assertEquals(instance.getInset(), map.get("inset"));
        assertEquals(instance.getPosition().toString(), map.get("position"));
    }

    @Test
    void toMapReturnsUnmodifiableMap() {
        Size fullSize = new Size(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    @Test
    void testToString() {
        URI uri = OVERLAY_FIXTURE.toUri();
        instance.setURI(uri);
        assertEquals(uri + "_SE_5", instance.toString());
    }

}
