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

import is.galia.stream.PathImageInputStream;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.ImageInputStream;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TIFFMetadataTest extends BaseTest {

    private Directory newIFD(String fixtureName) throws IOException {
        final Path srcFile = TestUtils.getSampleImage(fixtureName);
        try (ImageInputStream is = new PathImageInputStream(srcFile)) {
            DirectoryReader reader = new DirectoryReader();
            reader.setSource(is);
            TagSet tagSet = new EXIFBaselineTIFFTagSet();
            tagSet.addTag(TIFFMetadata.IPTC_POINTER_TAG);
            tagSet.addTag(TIFFMetadata.XMP_POINTER_TAG);
            reader.addTagSet(tagSet);
            return reader.readFirst();
        }
    }

    @Test
    void getEXIF() throws IOException {
        Directory dir = newIFD("tif/exif.tif");
        TIFFMetadata metadata = new TIFFMetadata(dir);
        assertFalse(metadata.getEXIF().isEmpty());
    }

    @Test
    void getIPTC() throws IOException {
        Directory dir = newIFD("tif/iptc.tif");
        TIFFMetadata metadata = new TIFFMetadata(dir);
        assertFalse(metadata.getIPTC().isEmpty());
    }

    @Test
    void getNativeMetadata() throws IOException {
        Directory dir = newIFD("tif/xmp.tif");
        TIFFMetadata metadata = new TIFFMetadata(dir);
        assertFalse(metadata.getNativeMetadata().isPresent());
    }

    @Test
    void getXMP() throws IOException {
        Directory dir = newIFD("tif/xmp.tif");
        TIFFMetadata metadata = new TIFFMetadata(dir);
        String rdf = metadata.getXMP().orElseThrow();
        assertTrue(rdf.startsWith("<rdf:RDF"));
    }

    @Test
    void toMap() throws Exception {
        Directory dir = newIFD("tif/xmp.tif");
        TIFFMetadata metadata = new TIFFMetadata(dir);
        Map<String, Object> map = metadata.toMap();
        assertNotNull(map.get("exif"));
        assertNotNull(map.get("iptc"));
        assertNotNull(map.get("xmp_model"));
        assertNotNull(map.get("xmp_string"));
        assertInstanceOf(Map.class, map.get("xmp_elements"));
        assertNull(map.get("native"));
    }

}
