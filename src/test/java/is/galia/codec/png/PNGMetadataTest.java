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

package is.galia.codec.png;

import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PNGMetadataTest extends BaseTest {

    private PNGMetadata getInstance(String fixtureName) throws IOException {
        final Path srcFile = TestUtils.getSampleImage(fixtureName);
        final Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("PNG");
        final ImageReader reader = it.next();
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile.toFile())) {
            reader.setInput(is);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            return new PNGMetadata(metadata,
                    metadata.getNativeMetadataFormatName());
        } finally {
            reader.dispose();
        }
    }

    @Test
    void getNativeMetadata() throws IOException {
        final PNGNativeMetadata metadata =
                (PNGNativeMetadata) getInstance("png/nativemetadata.png").getNativeMetadata().orElseThrow();
        assertEquals(1, metadata.size());
        assertEquals("Cat Highway", metadata.get("Title"));
    }

    @Test
    void getXMP() throws IOException {
        final String rdf = getInstance("png/xmp.png").getXMP().orElseThrow();
        final Model model = ModelFactory.createDefaultModel();
        try (Reader reader = new StringReader(rdf)) {
            model.read(reader, "", "RDF/XML");
        }
    }

    @Test
    void toMap() throws Exception {
        final PNGMetadata metadata = getInstance("png/xmp.png");
        Map<String,Object> map = metadata.toMap();
        assertNotNull(map.get("xmp_model"));
        assertNotNull(map.get("xmp_string"));
        assertInstanceOf(Map.class, map.get("xmp_elements"));
    }

}
