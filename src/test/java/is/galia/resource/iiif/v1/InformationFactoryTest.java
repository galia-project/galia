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

package is.galia.resource.iiif.v1;

import is.galia.codec.Decoder;
import is.galia.codec.DecoderFactory;
import is.galia.image.Format;
import is.galia.image.Info;
import is.galia.image.InfoReader;
import is.galia.image.ScaleConstraint;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InformationFactoryTest extends BaseTest {

    private String imageURI;
    private Map<String,Object> imageInfo;
    private Arena arena;
    private Decoder decoder;
    private InfoReader infoReader;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Format format = Format.get("jpg");
        Path fixture = TestUtils.getSampleImage("jpg/rgb-594x522x8-baseline.jpg");
        imageURI     = "http://example.org/bla";
        arena        = Arena.ofConfined();
        decoder      = DecoderFactory.newDecoder(format, arena);
        decoder.setSource(fixture);

        infoReader = new InfoReader();
        infoReader.setDecoder(decoder);
        infoReader.setFormat(format);
        Info info = infoReader.read();
        imageInfo = new InformationFactory().newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 1));
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        try {
            super.tearDown();
        } finally {
            decoder.close();
            arena.close();
        }
    }

    private void setUpForRotatedImage() throws Exception {
        decoder.close();

        Format format = Format.get("jpg");
        Path fixture = TestUtils.getSampleImage("jpg/xmp-orientation-90.jpg");
        decoder = DecoderFactory.newDecoder(format, arena);
        decoder.setSource(fixture);
        infoReader.setDecoder(decoder);
        infoReader.setFormat(format);

        Info info = infoReader.read();
        imageInfo = new InformationFactory().newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 1));
    }

    private void setUpForScaleConstrainedImage() throws Exception {
        decoder.close();

        Format format = Format.get("jpg");
        Path fixture = TestUtils.getSampleImage("jpg/rgb-594x522x8-baseline.jpg");
        decoder = DecoderFactory.newDecoder(format, arena);
        decoder.setSource(fixture);
        infoReader.setDecoder(decoder);
        infoReader.setFormat(format);

        Info info = infoReader.read();
        imageInfo = new InformationFactory().newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 2));
    }

    @Test
    void newImageInfoContext() {
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/context.json",
                imageInfo.get("@context"));
    }

    @Test
    void newImageInfoId() {
        assertEquals("http://example.org/bla", imageInfo.get("@id"));
    }

    @Test
    void newImageInfoWidth() {
        assertEquals(594L, imageInfo.get("width"));
    }

    @Test
    void newImageInfoWidthWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(64L, imageInfo.get("width"));
    }

    @Test
    void newImageInfoWidthWithScaleConstraint() throws Exception {
        setUpForScaleConstrainedImage();
        assertEquals(297L, imageInfo.get("width"));
    }

    @Test
    void newImageInfoHeight() {
        assertEquals(522L, imageInfo.get("height"));
    }

    @Test
    void newImageInfoHeightWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(56L, imageInfo.get("height"));
    }

    @Test
    void newImageInfoHeightWithScaleConstraint() throws Exception {
        setUpForScaleConstrainedImage();
        assertEquals(261L, imageInfo.get("height"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void newImageInfoScaleFactors() {
        List<Integer> scaleFactors =
                (List<Integer>) imageInfo.get("scale_factors");
        assertEquals(4, scaleFactors.size());
        assertEquals(1, scaleFactors.get(0));
        assertEquals(2, scaleFactors.get(1));
        assertEquals(4, scaleFactors.get(2));
        assertEquals(8, scaleFactors.get(3));
    }

    @SuppressWarnings("unchecked")
    @Test
    void newImageInfoScaleFactorsWithScaleConstrainedImage() throws Exception {
        setUpForScaleConstrainedImage();
        List<Integer> scaleFactors =
                (List<Integer>) imageInfo.get("scale_factors");
        assertEquals(3, scaleFactors.size());
        assertEquals(1, scaleFactors.get(0));
        assertEquals(2, scaleFactors.get(1));
        assertEquals(4, scaleFactors.get(2));
    }

    @Test
    void newImageInfoTileWidthWithUntiledImage() {
        assertEquals(594L, imageInfo.get("tile_width"));
    }

    @Test
    void newImageInfoTileWidthWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(64L, imageInfo.get("tile_width"));
    }

    @Test
    void newImageInfoTileWidthWithUntiledImageWithScaleConstraint()
            throws Exception {
        setUpForScaleConstrainedImage();
        assertEquals(297L, imageInfo.get("tile_width"));
    }

    @Test
    void newImageInfoTileWidthWithTiledImage() throws Exception {
        decoder.close();

        Format format = Format.get("tif");
        Path fixture = TestUtils.getSampleImage("tif/tiled-rgb-8bit-uncompressed.tif");
        decoder = DecoderFactory.newDecoder(format, arena);
        decoder.setSource(fixture);
        infoReader.setDecoder(decoder);
        infoReader.setFormat(format);

        Info info = infoReader.read();
        imageInfo = new InformationFactory().newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 1));

        assertEquals(64L, imageInfo.get("tile_width"));
    }

    @Test
    void newImageInfoTileHeightWithUntiledImage() {
        assertEquals(522L, imageInfo.get("tile_height"));
    }

    @Test
    void newImageInfoTileHeightWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(56L, imageInfo.get("tile_height"));
    }

    @Test
    void newImageInfoTileHeightWithUntiledImageWithScaleConstraint()
            throws Exception {
        setUpForScaleConstrainedImage();
        assertEquals(261L, imageInfo.get("tile_height"));
    }

    @Test
    void newImageInfoTileHeightWithTiledImage() throws Exception {
        decoder.close();

        Format format = Format.get("tif");
        Path fixture = TestUtils.getSampleImage("tif/tiled-rgb-8bit-uncompressed.tif");
        decoder = DecoderFactory.newDecoder(format, arena);
        decoder.setSource(fixture);
        infoReader.setDecoder(decoder);
        infoReader.setFormat(format);

        Info info = infoReader.read();
        imageInfo = new InformationFactory().newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 1));

        assertEquals(64L, imageInfo.get("tile_width"));
        assertEquals(56L, imageInfo.get("tile_height"));
    }

    @Test
    void newImageInfoPageCount() {
        assertEquals(1, imageInfo.get("page_count"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void newImageInfoFormats() {
        List<String> formats = (List<String>) imageInfo.get("formats");
        assertTrue(formats.contains("jpg"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void newImageInfoQualities() {
        List<String> qualities = (List<String>) imageInfo.get("qualities");
        assertTrue(qualities.contains("color"));
    }

    @Test
    void newImageInfoProfile() {
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2",
                imageInfo.get("profile"));
    }

}
