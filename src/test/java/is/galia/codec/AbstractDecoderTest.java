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

import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.image.Region;
import is.galia.image.ReductionFactor;
import is.galia.stream.ByteArrayImageInputStream;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractDecoderTest extends BaseTest {

    private static final double DELTA = 0.00000001;
    private static final Size FIXTURE_SIZE = new Size(64, 56);

    protected Decoder instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        if (instance != null) {
            instance.close();
        }
    }

    abstract protected Path getSupportedFixture();

    abstract protected Path getUnsupportedFixture();

    abstract protected Decoder newInstance() throws IOException;

    /* detectFormat() */

    @Test
    public void detectFormatWithVeryFewBytes() throws Exception {
        byte[] bytes = { 0x34, 0x23 };
        try (ImageInputStream is = new ByteArrayImageInputStream(bytes)) {
            instance.setSource(is);
            assertEquals(Format.UNKNOWN, instance.detectFormat());
        }
    }

    @Test
    public void detectFormatWithIncompatibleImage() throws Exception {
        instance.setSource(getUnsupportedFixture());
        assertEquals(Format.UNKNOWN, instance.detectFormat());
    }

    /* getNumImages() */

    @Test
    public void getNumImages() throws Exception {
        assertEquals(1, instance.getNumImages());
    }

    @Test
    public void getNumImagesWithIncompatibleImage() throws Exception {
        instance.setSource(getUnsupportedFixture());
        assertThrows(SourceFormatException.class,
                () -> instance.getNumImages());
    }

    @Test
    public void getNumResolutions() throws Exception {
        assertEquals(1, instance.getNumResolutions());
    }

    @Test
    public void getNumResolutionsWithIncompatibleImage() {
        instance.setSource(getUnsupportedFixture());
        assertThrows(SourceFormatException.class,
                () -> instance.getNumResolutions());
    }

    /* getNumThumbnails() */

    @Test
    public void getNumThumbnailsWithNonexistentImage() {
        instance.setSource(TestUtils.getFixture("bogus"));
        assertThrows(NoSuchFileException.class,
                () -> instance.getNumThumbnails(0));
    }

    @Test
    public void getNumThumbnailsWithEmptyImage() {
        instance.setSource(TestUtils.getFixture("empty"));
        assertThrows(SourceFormatException.class, () ->
                instance.getNumThumbnails(0));
    }

    @Test
    public void getNumThumbnailsWithInvalidImage() {
        instance.setSource(getUnsupportedFixture());
        assertThrows(SourceFormatException.class,
                () -> instance.getNumThumbnails(0));
    }

    @Test
    public void getNumThumbnailsWithIllegalImageIndex() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> instance.getNumThumbnails(99999));
    }

    /* getSize() */

    @Test
    public void getSize() throws Exception {
        assertEquals(FIXTURE_SIZE, instance.getSize(0));
    }

    @Test
    public void getSizeWithIncompatibleImage() {
        instance.setSource(getUnsupportedFixture());
        assertThrows(SourceFormatException.class, () -> instance.getSize(0));
    }

    @Test
    public void getSizeWithIllegalIndex() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> instance.getSize(9999));
    }

    /* getThumbnailSize() */

    @Test
    public void getThumbnailSizeWithNonexistentImage() {
        instance.setSource(Path.of("bogus"));
        assertThrows(NoSuchFileException.class,
                () -> instance.getThumbnailSize(0, 0));
    }

    @Test
    public void getThumbnailSizeWithEmptyImage() {
        instance.setSource(TestUtils.getFixture("empty"));
        assertThrows(SourceFormatException.class, () ->
                instance.getThumbnailSize(0, 0));
    }

    @Test
    public void getThumbnailSizeWithInvalidImage() {
        instance.setSource(getUnsupportedFixture());
        assertThrows(SourceFormatException.class,
                () -> instance.getThumbnailSize(0, 0));
    }

    @Test
    public void getThumbnailSizeWithIllegalImageIndex() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> instance.getThumbnailSize(99999, 0));
    }

    /* getTileSize() */

    @Test
    public void getTileSize() throws Exception {
        assertEquals(FIXTURE_SIZE, instance.getTileSize(0));
    }

    @Test
    public void getTileSizeWithIncompatibleImage() {
        instance.setSource(getUnsupportedFixture());
        assertThrows(SourceFormatException.class,
                () -> instance.getTileSize(0));
    }

    @Test
    public void getTileSizeWithIllegalIndex() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> instance.getTileSize(9999));
    }

    @Test
    public void decode1() throws Exception {
        BufferedImage result = instance.decode(0);
        assertEquals(FIXTURE_SIZE.width(), result.getWidth(), DELTA);
        assertEquals(FIXTURE_SIZE.height(), result.getHeight(), DELTA);
    }

    @Test
    public void decode1WithIncompatibleImage() {
        instance.setSource(getUnsupportedFixture());
        assertThrows(SourceFormatException.class, () -> instance.decode(0));
    }

    @Test
    public void decode1WithIllegalIndex() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> instance.decode(9999));
    }

    @Test
    public void decode2() throws Exception {
        Region region       = new Region(10, 10, 40, 40);
        double[] scales        = { 0.875, 0.875 };
        double[] diffScales    = new double[2];
        ReductionFactor rf     = new ReductionFactor();
        Set<DecoderHint> hints = new HashSet<>();

        BufferedImage image = instance.decode(
                0, region, scales, rf, diffScales, hints);

        assertEquals(40, image.getWidth());
        assertEquals(40, image.getHeight());
        assertEquals(0, rf.factor);
    }

    @Test
    public void decode2AddsDecoderHints() throws Exception {
        Region region       = new Region(10, 10, 40, 40);
        double[] scales        = { 0.875, 0.875 };
        double[] diffScales    = new double[2];
        ReductionFactor rf     = new ReductionFactor();
        Set<DecoderHint> hints = new HashSet<>();

        instance.decode(0, region, scales, rf, diffScales, hints);

        assertTrue(hints.contains(DecoderHint.IGNORED_SCALE));
    }

    @Test
    void decode2WithIncompatibleImage() {
        Region region       = new Region(0, 0, 50, 50);
        double[] scales        = { 1, 1 };
        double[] diffScales    = new double[2];
        ReductionFactor rf     = new ReductionFactor();
        Set<DecoderHint> hints = new HashSet<>();

        instance.setSource(getUnsupportedFixture());
        assertThrows(SourceFormatException.class,
                () -> instance.decode(0, region, scales, rf, diffScales, hints));
    }

    @Test
    public void decode2WithIllegalIndex() {
        Region region       = new Region(10, 10, 40, 40);
        double[] scales        = { 0.875, 0.875 };
        double[] diffScales    = new double[2];
        ReductionFactor rf     = new ReductionFactor();
        Set<DecoderHint> hints = new HashSet<>();

        assertThrows(IndexOutOfBoundsException.class,
                () -> instance.decode(9999, region, scales, rf, diffScales, hints));
    }

    /* readMetadata() */

    @Test
    public void decodeMetadataWithIncompatibleImage() {
        instance.setSource(getUnsupportedFixture());
        assertThrows(SourceFormatException.class,
                () -> instance.readMetadata(0));
    }

    @Test
    public void decodeMetadataWithIllegalIndex() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> instance.readMetadata(9999));
    }

    /* readSequence() */

    @Test
    public void decodeSequenceWithStaticImage() throws Exception {
        BufferedImageSequence result = instance.decodeSequence();
        assertEquals(1, result.length());
    }

    @Test
    public void decodeSequenceWithIncompatibleImage() {
        instance.setSource(getUnsupportedFixture());
        assertThrows(SourceFormatException.class,
                () -> instance.decodeSequence());
    }

    /* readThumbnail() */

    @Test
    public void decodeThumbnailWithNonexistentImage() {
        instance.setSource(TestUtils.getFixture("bogus"));
        assertThrows(NoSuchFileException.class,
                () -> instance.readThumbnail(0, 0));
    }

    @Test
    public void decodeThumbnailWithEmptyImage() {
        instance.setSource(TestUtils.getFixture("empty"));
        assertThrows(UnsupportedOperationException.class, () ->
                instance.readThumbnail(0, 0));
    }

    @Test
    public void decodeThumbnailWithInvalidImage() {
        instance.setSource(getUnsupportedFixture());
        assertThrows(UnsupportedOperationException.class,
                () -> instance.readThumbnail(0, 0));
    }

    @Test
    public void decodeThumbnailWithIllegalImageIndex() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.readThumbnail(99999, 0));
    }

}
