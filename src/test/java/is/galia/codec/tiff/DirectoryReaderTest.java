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

import is.galia.codec.SourceFormatException;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.ImageInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;

import static is.galia.codec.tiff.EXIFBaselineTIFFTagSet.*;
import static is.galia.codec.tiff.EXIFTagSet.*;
import static org.junit.jupiter.api.Assertions.*;

class DirectoryReaderTest extends BaseTest {

    private DirectoryReader instance;

    private static Directory getExpectedIntelIFD0() {
        final Directory exifIFD = new Directory(new EXIFTagSet());
        exifIFD.add(EXPOSURE_TIME, DataType.RATIONAL, List.of(List.of(1L, 40L)));
        exifIFD.add(F_NUMBER, DataType.RATIONAL, List.of(List.of(11L, 5L)));
        exifIFD.add(EXPOSURE_PROGRAM, DataType.SHORT, List.of(2));
        exifIFD.add(PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, List.of(40));
        exifIFD.add(EXIF_VERSION, DataType.UNDEFINED, "0221".getBytes(StandardCharsets.US_ASCII));
        exifIFD.add(DATE_TIME_ORIGINAL, DataType.ASCII, "2015:12:31 12:42:48");
        exifIFD.add(DATE_TIME_DIGITIZED, DataType.ASCII, "2015:12:31 12:42:48");
        exifIFD.add(COMPONENTS_CONFIGURATION, DataType.UNDEFINED, new byte[] { 1, 2, 3, 0});
        exifIFD.add(SHUTTER_SPEED, DataType.SRATIONAL, List.of(List.of(21309, 4004)));
        exifIFD.add(APERTURE, DataType.RATIONAL, List.of(List.of(8074L, 3549L)));
        exifIFD.add(BRIGHTNESS, DataType.SRATIONAL, List.of(List.of(4664, 1205)));
        exifIFD.add(EXPOSURE_BIAS, DataType.SRATIONAL, List.of(List.of(0, 1)));
        exifIFD.add(METERING_MODE, DataType.SHORT, List.of(5));
        exifIFD.add(FLASH, DataType.SHORT, List.of(16));
        exifIFD.add(FOCAL_LENGTH, DataType.RATIONAL, List.of(List.of(21L, 5L)));
        exifIFD.add(SUBJECT_AREA, DataType.SHORT, List.of(1631, 1223, 1795, 1077));
        exifIFD.add(MAKER_NOTE, DataType.UNDEFINED, new byte[] {
                0x41, 0x70, 0x70, 0x6C, 0x65, 0x20, 0x69, 0x4F, 0x53, 0x00,
                0x00, 0x01, 0x4D, 0x4D, 0x00, 0x0A, 0x00, 0x01, 0x00, 0x09,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x04, 0x00, 0x03,
                0x00, 0x07, 0x00, 0x00, 0x00, 0x68, 0x00, 0x00, 0x00, (byte) 0x8C,
                0x00, 0x04, 0x00, 0x09, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
                0x00, 0x01, 0x00, 0x05, 0x00, 0x09, 0x00, 0x00, 0x00, 0x01,
                0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x06, 0x00, 0x09, 0x00,
                0x00, 0x00, 0x01, 0x00, 0x00, 0x00, (byte) 0x87, 0x00, 0x07,
                0x00, 0x09, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x00, 0x08, 0x00, 0x0A, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00,
                0x00, (byte) 0xF4, 0x00, 0x0A, 0x00, 0x09, 0x00, 0x00, 0x00,
                0x01, 0x00, 0x00, 0x00, 0x02, 0x00, 0x0E, 0x00, 0x09, 0x00,
                0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x14, 0x00,
                0x09, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x03, 0x00,
                0x00, 0x00, 0x00, 0x62, 0x70, 0x6C, 0x69, 0x73, 0x74, 0x30,
                0x30, (byte) 0xD4, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                0x08, 0x55, 0x66, 0x6C, 0x61, 0x67, 0x73, 0x55, 0x76, 0x61,
                0x6C, 0x75, 0x65, 0x55, 0x65, 0x70, 0x6F, 0x63, 0x68, 0x59,
                0x74, 0x69, 0x6D, 0x65, 0x73, 0x63, 0x61, 0x6C, 0x65, 0x10,
                0x01, 0x13, 0x00, 0x03, 0x2A, 0x07, 0x20, 0x58, 0x39,
                (byte) 0xC4, 0x10, 0x00, 0x12, 0x3B, (byte) 0x9A, (byte) 0xCA,
                0x00, 0x08, 0x11, 0x17, 0x1D, 0x23, 0x2D, 0x2F, 0x38, 0x3A,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x3F, (byte) 0xFF, (byte) 0xFF, (byte) 0xEC, 0x2C, 0x00,
                0x00, 0x14, (byte) 0x87, (byte) 0xFF, (byte) 0xFF, (byte) 0xF9,
                (byte) 0xBC, 0x00, 0x00, (byte) 0xBF, 0x6D, 0x00, 0x00, 0x07,
                0x0E, 0x00, 0x00, 0x1A, (byte) 0xFB });
        exifIFD.add(SUB_SEC_TIME_ORIGINAL, DataType.ASCII, "865");
        exifIFD.add(SUB_SEC_TIME_DIGITIZED, DataType.ASCII, "865");
        exifIFD.add(FLASHPIX_VERSION, DataType.UNDEFINED, "0100".getBytes(StandardCharsets.US_ASCII));
        exifIFD.add(COLOR_SPACE, DataType.SHORT, List.of(1));
        exifIFD.add(PIXEL_X_DIMENSION, DataType.SHORT, List.of(64));
        exifIFD.add(PIXEL_Y_DIMENSION, DataType.SHORT, List.of(56));
        exifIFD.add(SENSING_METHOD, DataType.SHORT, List.of(2));
        exifIFD.add(SCENE_TYPE, DataType.UNDEFINED, new byte[] { 1, 0, 0, 0 });
        exifIFD.add(CUSTOM_RENDERED, DataType.SHORT, List.of(2));
        exifIFD.add(EXPOSURE_MODE, DataType.SHORT, List.of(0));
        exifIFD.add(WHITE_BALANCE, DataType.SHORT, List.of(0));
        exifIFD.add(FOCAL_LENGTH_IN_35MM_FILM, DataType.SHORT, List.of(29));
        exifIFD.add(SCENE_CAPTURE_TYPE, DataType.SHORT, List.of(0));
        exifIFD.add(LENS_SPECIFICATION, DataType.RATIONAL, List.of(
                List.of(83L, 20L), List.of(83L, 20L),
                List.of(11L, 5L), List.of(11L, 5L)));
        exifIFD.add(LENS_MAKE, DataType.ASCII, "Apple");
        exifIFD.add(LENS_MODEL, DataType.ASCII, "iPhone 5s back camera 4.15mm f/2.2");

        final Directory ifd0 = new Directory(new EXIFBaselineTIFFTagSet());
        ifd0.add(MAKE, DataType.ASCII, "Apple");
        ifd0.add(MODEL, DataType.ASCII, "iPhone 5s");
        ifd0.add(ORIENTATION, DataType.SHORT, List.of(1));
        ifd0.add(X_RESOLUTION, DataType.RATIONAL, List.of(List.of(72L, 1L)));
        ifd0.add(Y_RESOLUTION, DataType.RATIONAL, List.of(List.of(72L, 1L)));
        ifd0.add(RESOLUTION_UNIT, DataType.SHORT, List.of(2));
        ifd0.add(SOFTWARE, DataType.ASCII, "Photos 1.5");
        ifd0.add(DATE_TIME, DataType.ASCII, "2015:12:31 12:42:48");
        ifd0.add(Y_CB_CR_POSITIONING, DataType.SHORT, List.of(1));
        ifd0.add(EXIF_IFD_POINTER, exifIFD);

        return ifd0;
    }

    private static Directory getExpectedMotorolaIFD0() {
        final Directory exifIFD = new Directory(new EXIFTagSet());
        exifIFD.add(EXPOSURE_TIME, DataType.RATIONAL, List.of(List.of(1L, 40L)));
        exifIFD.add(F_NUMBER, DataType.RATIONAL, List.of(List.of(11L, 5L)));
        exifIFD.add(EXPOSURE_PROGRAM, DataType.SHORT, List.of(2));
        exifIFD.add(PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, List.of(40));
        exifIFD.add(EXIF_VERSION, DataType.UNDEFINED, "0221".getBytes(StandardCharsets.US_ASCII));
        exifIFD.add(DATE_TIME_ORIGINAL, DataType.ASCII, "2015:12:31 12:42:48");
        exifIFD.add(DATE_TIME_DIGITIZED, DataType.ASCII, "2015:12:31 12:42:48");
        exifIFD.add(COMPONENTS_CONFIGURATION, DataType.UNDEFINED, new byte[] { 1, 2, 3, 0 });
        exifIFD.add(SHUTTER_SPEED, DataType.SRATIONAL, List.of(List.of(2294, 431)));
        exifIFD.add(APERTURE, DataType.RATIONAL, List.of(List.of(7801L, 3429L)));
        exifIFD.add(BRIGHTNESS, DataType.SRATIONAL, List.of(List.of(4664, 1205)));
        exifIFD.add(EXPOSURE_BIAS, DataType.SRATIONAL, List.of(List.of(0, 1)));
        exifIFD.add(METERING_MODE, DataType.SHORT, List.of(5));
        exifIFD.add(FLASH, DataType.SHORT, List.of(16));
        exifIFD.add(FOCAL_LENGTH, DataType.RATIONAL, List.of(List.of(83L, 20L)));
        exifIFD.add(SUBJECT_AREA, DataType.SHORT, List.of(1631, 1223, 1795, 1077));
        exifIFD.add(MAKER_NOTE, DataType.UNDEFINED, new byte[] {
                0x41, 0x70, 0x70, 0x6C, 0x65, 0x20, 0x69, 0x4F, 0x53, 0x00,
                0x00, 0x01, 0x4D, 0x4D, 0x00, 0x0A, 0x00, 0x01, 0x00, 0x09,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x04, 0x00, 0x03,
                0x00, 0x07, 0x00, 0x00, 0x00, 0x68, 0x00, 0x00, 0x00, (byte) 0x8C,
                0x00, 0x04, 0x00, 0x09, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
                0x00, 0x01, 0x00, 0x05, 0x00, 0x09, 0x00, 0x00, 0x00, 0x01,
                0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x06, 0x00, 0x09, 0x00,
                0x00, 0x00, 0x01, 0x00, 0x00, 0x00, (byte) 0x87, 0x00, 0x07,
                0x00, 0x09, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x00, 0x08, 0x00, 0x0A, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00,
                0x00, (byte) 0xF4, 0x00, 0x0A, 0x00, 0x09, 0x00, 0x00, 0x00,
                0x01, 0x00, 0x00, 0x00, 0x02, 0x00, 0x0E, 0x00, 0x09, 0x00,
                0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x14, 0x00,
                0x09, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x03, 0x00,
                0x00, 0x00, 0x00, 0x62, 0x70, 0x6C, 0x69, 0x73, 0x74, 0x30,
                0x30, (byte) 0xD4, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                0x08, 0x55, 0x66, 0x6C, 0x61, 0x67, 0x73, 0x55, 0x76, 0x61,
                0x6C, 0x75, 0x65, 0x55, 0x65, 0x70, 0x6F, 0x63, 0x68, 0x59,
                0x74, 0x69, 0x6D, 0x65, 0x73, 0x63, 0x61, 0x6C, 0x65, 0x10,
                0x01, 0x13, 0x00, 0x03, 0x2A, 0x07, 0x20, 0x58, 0x39,
                (byte) 0xC4, 0x10, 0x00, 0x12, 0x3B, (byte) 0x9A, (byte) 0xCA,
                0x00, 0x08, 0x11, 0x17, 0x1D, 0x23, 0x2D, 0x2F, 0x38, 0x3A,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x3F, (byte) 0xFF, (byte) 0xFF, (byte) 0xEC, 0x2C, 0x00,
                0x00, 0x14, (byte) 0x87, (byte) 0xFF, (byte) 0xFF, (byte) 0xF9,
                (byte) 0xBC, 0x00, 0x00, (byte) 0xBF, 0x6D, 0x00, 0x00, 0x07,
                0x0E, 0x00, 0x00, 0x1A, (byte) 0xFB });
        exifIFD.add(SUB_SEC_TIME_ORIGINAL, DataType.ASCII, "865");
        exifIFD.add(SUB_SEC_TIME_DIGITIZED, DataType.ASCII, "865");
        exifIFD.add(FLASHPIX_VERSION, DataType.UNDEFINED, "0100".getBytes(StandardCharsets.US_ASCII));
        exifIFD.add(COLOR_SPACE, DataType.SHORT, List.of(1));
        exifIFD.add(PIXEL_X_DIMENSION, DataType.LONG, List.of(64L));
        exifIFD.add(PIXEL_Y_DIMENSION, DataType.LONG, List.of(56L));
        exifIFD.add(SENSING_METHOD, DataType.SHORT, List.of(2));
        exifIFD.add(SCENE_TYPE, DataType.UNDEFINED, new byte[] { 1, 0, 0, 0 });
        exifIFD.add(CUSTOM_RENDERED, DataType.SHORT, List.of(2));
        exifIFD.add(EXPOSURE_MODE, DataType.SHORT, List.of(0));
        exifIFD.add(WHITE_BALANCE, DataType.SHORT, List.of(0));
        exifIFD.add(FOCAL_LENGTH_IN_35MM_FILM, DataType.SHORT, List.of(29));
        exifIFD.add(SCENE_CAPTURE_TYPE, DataType.SHORT, List.of(0));
        exifIFD.add(LENS_SPECIFICATION, DataType.RATIONAL, List.of(
                List.of(83L, 20L), List.of(83L, 20L),
                List.of(11L, 5L), List.of(11L, 5L)));
        exifIFD.add(LENS_MAKE, DataType.ASCII, "Apple");
        exifIFD.add(LENS_MODEL, DataType.ASCII, "iPhone 5s back camera 4.15mm f/2.2");

        final Directory ifd0 = new Directory(new EXIFBaselineTIFFTagSet());
        ifd0.add(MAKE, DataType.ASCII, "Apple");
        ifd0.add(MODEL, DataType.ASCII, "iPhone 5s");
        ifd0.add(ORIENTATION, DataType.SHORT, List.of(1));
        ifd0.add(X_RESOLUTION, DataType.RATIONAL, List.of(List.of(72L, 1L)));
        ifd0.add(Y_RESOLUTION, DataType.RATIONAL, List.of(List.of(72L, 1L)));
        ifd0.add(RESOLUTION_UNIT, DataType.SHORT, List.of(2));
        ifd0.add(SOFTWARE, DataType.ASCII, "Photos 1.5");
        ifd0.add(DATE_TIME, DataType.ASCII, "2015:12:31 12:42:48");
        ifd0.add(EXIF_IFD_POINTER, exifIFD);

        return ifd0;
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new DirectoryReader();
        instance.addTagSet(new EXIFBaselineTIFFTagSet());
        instance.addTagSet(new EXIFTagSet());
        instance.addTagSet(new EXIFGPSTagSet());
        instance.addTagSet(new EXIFInteroperabilityTagSet());
    }

    /* addTagSet() */

    @Test
    void addTagSet() {
        instance.removeTagSets();
        instance.addTagSet(new EXIFGPSTagSet());
        assertEquals(1, instance.getTagSets().size());
    }

    /* getTagSet(int) */

    @Test
    void getTagSetWithPresentIFDPointer() {
        assertNotNull(instance.getTagSet(EXIFBaselineTIFFTagSet.IFD_POINTER));
    }

    @Test
    void getTagSetWithNonexistentIFDPointer() {
        assertNull(instance.getTagSet(9999));
    }

    /* getTagSets() */

    @Test
    void getTagSets() {
        assertEquals(4, instance.getTagSets().size());
    }

    @Test
    void getTagSetsReturnsUnmodifiableInstance() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.getTagSets().clear());
    }

    /* isBigTIFF() */

    @Test
    void isBigTIFFBeforeReading() {
        assertThrows(IllegalStateException.class,
                () -> instance.isBigTIFF());
    }

    @Test
    void isBigTIFFWithStandardTIFF() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("tif/tif.tif")) {
            instance.setSource(is);
            instance.iterator();
            assertFalse(instance.isBigTIFF());
        }
    }

    @Test
    void isBigTIFFWithBigTIFF() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("tif/bigtiff.tf8")) {
            instance.setSource(is);
            instance.iterator();
            assertTrue(instance.isBigTIFF());
        }
    }

    /* iterate() */

    @Test
    void iteratorWithNoTagSets() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/intel.bin")) {
            instance.setSource(is);
            instance.removeTagSets();
            assertThrows(IllegalStateException.class, () -> instance.iterator());
        }
    }

    @Test
    void iteratorWithNoBaselineSets() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/intel.bin")) {
            instance.setSource(is);
            instance.removeTagSets();
            instance.addTagSet(new EXIFGPSTagSet());
            assertThrows(IllegalStateException.class, () -> instance.iterator());
        }
    }

    @Test
    void iteratorWithSourceNotSet() {
        assertThrows(IllegalStateException.class, () -> instance.iterator());
    }

    @Test
    void iteratorWithUnsupportedFileType() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/illegal-file-type.bin")) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.iterator());
        }
    }

    @Test
    void iteratorWithLittleEndian() throws Exception {
        // N.B.: convenient way of swapping byte order:
        // exiftool -all= -tagsfromfile bigendian.jpg -all:all -unsafe -exifbyteorder=little-endian bigendian.jpg
        Directory expected = getExpectedIntelIFD0();
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/intel.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory actual     = it.next();
            assertEquals(expected, actual);
        }
    }

    @Test
    void iteratorWithBigEndian() throws Exception {
        // N.B.: convenient way of swapping byte order:
        // exiftool -all= -tagsfromfile littleendian.jpg -all:all -unsafe -exifbyteorder=big-endian littleendian.jpg
        Directory expected = getExpectedMotorolaIFD0();
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/motorola.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory actual     = it.next();
            assertEquals(expected, actual);
        }
    }

    @Test
    void iteratorWithBigTIFF() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("tif/bigtiff.tf8")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            assertFalse(dir.getFields().isEmpty());
        }
    }

    /**
     * EXIF data within a JPEG APP0 segment will have a header of "ExifNULNUL".
     * This test tests that the reader ignores that.
     */
    @Test
    void iteratorWithJPEGAPP0Segment() throws Exception {
        Directory expected = getExpectedMotorolaIFD0();
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/jpeg-app0.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            assertEquals(expected, dir);
        }
    }

    @Test
    void iteratorWithMultiPageTIFFFile() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("tif/multipage.tif")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            for (int i = 0; i < 9; i++) {
                assertTrue(it.hasNext());
                Directory dir = it.next();
                assertFalse(dir.getFields().isEmpty());
            }
            assertFalse(it.hasNext());
            assertThrows(NoSuchElementException.class, () -> it.next());
        }
    }

    @Test
    void iteratorWithTIFFFile() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("tif/exif.tif")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            assertFalse(dir.getFields().isEmpty());
        }
    }

    @Test
    void iteratorWithCustomTag() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("tif/xmp.tif")) {
            Tag tag = new Tag(700, "XMP", false);
            TagSet baselineTagSet = instance.getTagSet(EXIFBaselineTIFFTagSet.IFD_POINTER);
            baselineTagSet.addTag(tag);
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(tag);
            byte[] value         = (byte[]) field.getFirstValue();
            String xmp           = new String(value, StandardCharsets.UTF_8);
            assertTrue(xmp.startsWith("<x:xmpmeta"));
        }
    }

    /* readAll() */

    @Test
    void readAllWithNoTagSets() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/intel.bin")) {
            instance.setSource(is);
            instance.removeTagSets();
            assertThrows(IllegalStateException.class, () -> instance.readAll());
        }
    }

    @Test
    void readAllWithNoBaselineSets() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/intel.bin")) {
            instance.setSource(is);
            instance.removeTagSets();
            instance.addTagSet(new EXIFGPSTagSet());
            assertThrows(IllegalStateException.class, () -> instance.readAll());
        }
    }

    @Test
    void readAllWithSourceNotSet() {
        assertThrows(IllegalStateException.class, () -> instance.readAll());
    }

    @Test
    void readAllWithUnsupportedFileType() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/illegal-file-type.bin")) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.readAll());
        }
    }

    @Test
    void readAllWithLittleEndian() throws Exception {
        Directory expected = getExpectedIntelIFD0();
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/intel.bin")) {
            instance.setSource(is);
            List<Directory> dirs = instance.readAll();
            Directory actual     = dirs.getFirst();
            assertEquals(expected, actual);
        }
    }

    @Test
    void readAllWithBigEndian() throws Exception {
        Directory expected = getExpectedMotorolaIFD0();
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/motorola.bin")) {
            instance.setSource(is);
            List<Directory> dirs = instance.readAll();
            Directory actual     = dirs.getFirst();
            assertEquals(expected, actual);
        }
    }

    /**
     * EXIF data within a JPEG APP0 segment will have a header of "ExifNULNUL".
     * This test tests that the reader ignores that.
     */
    @Test
    void readAllWithJPEGAPP0Segment() throws Exception {
        Directory expected = getExpectedMotorolaIFD0();
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/jpeg-app0.bin")) {
            instance.setSource(is);
            List<Directory> dirs = instance.readAll();
            assertEquals(expected, dirs.getFirst());
        }
    }

    @Test
    void readAllWithSinglePage() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("tif/exif.tif")) {
            instance.setSource(is);
            List<Directory> dirs = instance.readAll();
            assertEquals(1, dirs.size());
        }
    }

    @Test
    void readAllWithMultiplePages() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("tif/multipage.tif")) {
            instance.setSource(is);
            List<Directory> dirs = instance.readAll();
            assertEquals(9, dirs.size());
        }
    }

    /* readFirst() */

    @Test
    void readFirst() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("tif/multipage.tif")) {
            instance.setSource(is);
            Directory dir = instance.readFirst();
            assertNotNull(dir);
        }
    }

    /* removeTagSets() */

    @Test
    void removeTagSets() {
        assertFalse(instance.getTagSets().isEmpty());
        instance.removeTagSets();
        assertEquals(0, instance.getTagSets().size());
    }

    //endregion
    //region Device-specific tests

    @Test
    void iteratorWithApple_iPhone_12_mini() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Apple iPhone 12 mini.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("Apple", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithCanon_PowerShot_S10() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Canon PowerShot S10.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("Canon", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithCasio_EX_Z750() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Casio EX-Z750.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("CASIO COMPUTER CO.,LTD", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithDxO_ONE() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/DxO ONE.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("DxO", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithEpson_PhotoPC_3000Z() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Epson PhotoPC 3000Z.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("SEIKO EPSON CORP.", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithFujifilm_MX_2700() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Fujifilm MX-2700.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("FUJIFILM", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithFujifilm_X_T50() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Fujifilm X-T50.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("FUJIFILM", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithGoPro_Hero12_Black() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/GoPro Hero12 Black.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("GoPro", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithKodak_C875() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Kodak C875.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("EASTMAN KODAK COMPANY", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithKodak_DC265() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Kodak DC265.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("Eastman Kodak Company", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithKonica_Minolta_7D() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Konica Minolta 7D.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("KONICA MINOLTA", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithKyocera_Finecam_s3() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Kyocera Finecam S3.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("KYOCERA", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithHasselblad_X1D() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Hasselblad X1D.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("Hasselblad", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithHP_850() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/HP 850.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("Hewlett-Packard", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithLeica_D_Lux_8() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Leica D-Lux 8.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("LEICA CAMERA AG", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithLeica_Digilux_2() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Leica Digilux 2.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("LEICA", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithMinolta_DiMAGE_7() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Minolta DiMAGE 7.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("Minolta Co., Ltd.", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithNikon_Coolpix_950() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Nikon Coolpix 950.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("NIKON", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithNikon_Zf() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Nikon Zf.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("NIKON CORPORATION", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithOlympus_C_2000Z() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Olympus C-2000z.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("OLYMPUS OPTICAL CO.,LTD", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithOM_System_OM_1_Mark_II() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/OM System OM-1 Mark II.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("OM Digital Solutions", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithPanasonic_FZ3() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Panasonic FZ3.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("Panasonic", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithPanasonic_Lumix_DC_G9_II() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Panasonic Lumix DC-G9 II.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("Panasonic", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithPentax_K_3_Mark_III() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Pentax K-3 Mark III.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("RICOH IMAGING COMPANY, LTD.",
                    field.getFirstValue());
        }
    }

    @Test
    void iteratorWithPentax_Optio_330() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Pentax Optio 330.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("Asahi Optical Co.,Ltd", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithRicoh_GR_Digital() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Ricoh GR Digital.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("RICOH", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithRicoh_GR_IIIx() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Ricoh GR IIIx.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("RICOH IMAGING COMPANY, LTD.", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithSamsung_Galaxy_Camera_2() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Samsung Galaxy Camera 2.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("SAMSUNG", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithSamsung_NX1() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Samsung NX1.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("SAMSUNG", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithSeaLife_DC2000() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/SeaLife DC2000.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("SEALIFE", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithSigma_fp_L() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Sigma fp L.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("SIGMA", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithSigma_SD9() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Sigma SD9.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("SIGMA", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithSony_a9_II() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Sony a9 II.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("SONY", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithSony_DSC_F505() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Sony DSC-F505.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("SONY", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithYI_M1() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/YI M1.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("XIAOYI", field.getFirstValue());
        }
    }

    @Test
    void iteratorWithZeiss_ZX1() throws Exception {
        try (ImageInputStream is = TestUtils.newSampleImageInputStream("exif/Zeiss ZX1.bin")) {
            instance.setSource(is);
            DirectoryIterator it = instance.iterator();
            Directory dir        = it.next();
            Field field          = dir.getField(MAKE);
            assertEquals("ZEISS", field.getFirstValue());
        }
    }

}
