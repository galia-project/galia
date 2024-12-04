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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static is.galia.codec.tiff.EXIFBaselineTIFFTagSet.*;
import static is.galia.codec.tiff.EXIFGPSTagSet.*;
import static is.galia.codec.tiff.EXIFTagSet.*;
import static org.junit.jupiter.api.Assertions.*;

class DirectoryTest extends BaseTest {

    private static final String NEWLINE = System.lineSeparator();

    private Directory instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Directory(new EXIFBaselineTIFFTagSet());
        instance.add(WHITE_POINT, DataType.LONG, List.of(10));
    }

    @Test
    void JSONSerialization() throws Exception {
        final Directory exifIFD = new Directory(new EXIFTagSet());
        exifIFD.add(EXPOSURE_TIME, DataType.RATIONAL, List.of(List.of(1L, 160L)));
        exifIFD.add(PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, List.of(50));
        exifIFD.add(EXIF_VERSION, DataType.UNDEFINED, "0221".getBytes(StandardCharsets.US_ASCII));
        exifIFD.add(DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        exifIFD.add(SHUTTER_SPEED, DataType.SRATIONAL, List.of(List.of(117, 16)));
        exifIFD.add(MAKER_NOTE, DataType.UNDEFINED, new byte[] {
                0x41, 0x70, 0x70, 0x6C, 0x65, 0x20, 0x69, 0x4F, 0x53, 0x00 });
        exifIFD.add(PIXEL_X_DIMENSION, DataType.LONG, List.of(64L));
        exifIFD.add(SCENE_TYPE, DataType.UNDEFINED, new byte[] { 0x01 });
        exifIFD.add(LENS_SPECIFICATION, DataType.RATIONAL, List.of(
                List.of(83L, 20L), List.of(83L, 20L),
                List.of(11L, 5L), List.of(11L, 5L)));

        final Directory rootIFD = new Directory(new EXIFBaselineTIFFTagSet());
        rootIFD.add(IMAGE_WIDTH, DataType.SHORT, List.of(64));
        rootIFD.add(IMAGE_LENGTH, DataType.SHORT, List.of(56));
        rootIFD.add(MAKE, DataType.ASCII, "Canon");
        rootIFD.add(ORIENTATION, DataType.SHORT, List.of(1));
        rootIFD.add(X_RESOLUTION, DataType.RATIONAL, List.of(List.of(72L, 1L)));
        rootIFD.add(PLANAR_CONFIGURATION, DataType.SHORT, List.of(1));
        rootIFD.add(DATE_TIME, DataType.ASCII, "2002:07:12 16:54:59");
        rootIFD.add(EXIF_IFD_POINTER, exifIFD);

        final String expected = "{" + NEWLINE +
                "  \"fields\" : [ {" + NEWLINE +
                "    \"id\" : 256," + NEWLINE +
                "    \"dataType\" : 3," + NEWLINE +
                "    \"values\" : [ 64 ]" + NEWLINE +
                "  }, {" + NEWLINE +
                "    \"id\" : 257," + NEWLINE +
                "    \"dataType\" : 3," + NEWLINE +
                "    \"values\" : [ 56 ]" + NEWLINE +
                "  }, {" + NEWLINE +
                "    \"id\" : 271," + NEWLINE +
                "    \"dataType\" : 2," + NEWLINE +
                "    \"values\" : [ \"Canon\" ]" + NEWLINE +
                "  }, {" + NEWLINE +
                "    \"id\" : 274," + NEWLINE +
                "    \"dataType\" : 3," + NEWLINE +
                "    \"values\" : [ 1 ]" + NEWLINE +
                "  }, {" + NEWLINE +
                "    \"id\" : 282," + NEWLINE +
                "    \"dataType\" : 5," + NEWLINE +
                "    \"values\" : [ [ 72, 1 ] ]" + NEWLINE +
                "  }, {" + NEWLINE +
                "    \"id\" : 284," + NEWLINE +
                "    \"dataType\" : 3," + NEWLINE +
                "    \"values\" : [ 1 ]" + NEWLINE +
                "  }, {" + NEWLINE +
                "    \"id\" : 306," + NEWLINE +
                "    \"dataType\" : 2," + NEWLINE +
                "    \"values\" : [ \"2002:07:12 16:54:59\" ]" + NEWLINE +
                "  }, {" + NEWLINE +
                "    \"id\" : 34665," + NEWLINE +
                "    \"dataType\" : 4," + NEWLINE +
                "    \"values\" : [ {" + NEWLINE +
                "      \"parentTag\" : 34665," + NEWLINE +
                "      \"fields\" : [ {" + NEWLINE +
                "        \"id\" : 33434," + NEWLINE +
                "        \"dataType\" : 5," + NEWLINE +
                "        \"values\" : [ [ 1, 160 ] ]" + NEWLINE +
                "      }, {" + NEWLINE +
                "        \"id\" : 34855," + NEWLINE +
                "        \"dataType\" : 3," + NEWLINE +
                "        \"values\" : [ 50 ]" + NEWLINE +
                "      }, {" + NEWLINE +
                "        \"id\" : 36864," + NEWLINE +
                "        \"dataType\" : 7," + NEWLINE +
                "        \"values\" : [ \"0221\" ]" + NEWLINE +
                "      }, {" + NEWLINE +
                "        \"id\" : 36867," + NEWLINE +
                "        \"dataType\" : 2," + NEWLINE +
                "        \"values\" : [ \"2002:07:12 16:54:59\" ]" + NEWLINE +
                "      }, {" + NEWLINE +
                "        \"id\" : 37377," + NEWLINE +
                "        \"dataType\" : 10," + NEWLINE +
                "        \"values\" : [ [ 117, 16 ] ]" + NEWLINE +
                "      }, {" + NEWLINE +
                "        \"id\" : 37500," + NEWLINE +
                "        \"dataType\" : 7," + NEWLINE +
                "        \"values\" : [ \"QXBwbGUgaU9TAA==\" ]" + NEWLINE +
                "      }, {" + NEWLINE +
                "        \"id\" : 40962," + NEWLINE +
                "        \"dataType\" : 4," + NEWLINE +
                "        \"values\" : [ 64 ]" + NEWLINE +
                "      }, {" + NEWLINE +
                "        \"id\" : 41729," + NEWLINE +
                "        \"dataType\" : 7," + NEWLINE +
                "        \"values\" : [ 1 ]" + NEWLINE +
                "      }, {" + NEWLINE +
                "        \"id\" : 42034," + NEWLINE +
                "        \"dataType\" : 5," + NEWLINE +
                "        \"values\" : [ [ 83, 20 ], [ 83, 20 ], [ 11, 5 ], [ 11, 5 ] ]" + NEWLINE +
                "      } ]" + NEWLINE +
                "    } ]" + NEWLINE +
                "  } ]" + NEWLINE +
                "}";
        String actual = new ObjectMapper().writer()
                .withDefaultPrettyPrinter()
                .writeValueAsString(rootIFD);
        assertEquals(expected, actual);
    }

    @Test
    void JSONDeserialization() throws Exception {
        final String json = "{\n" +
                "  \"fields\" : [ {\n" +
                "    \"id\" : 256,\n" +
                "    \"dataType\" : 3,\n" +
                "    \"values\" : [64]\n" +
                "  }, {\n" +
                "    \"id\" : 257,\n" +
                "    \"dataType\" : 3,\n" +
                "    \"values\" : [56]\n" +
                "  }, {\n" +
                "    \"id\" : 271,\n" +
                "    \"dataType\" : 2,\n" +
                "    \"values\" : [\"Canon\"]\n" +
                "  }, {\n" +
                "    \"id\" : 274,\n" +
                "    \"dataType\" : 3,\n" +
                "    \"values\" : [1]\n" +
                "  }, {\n" +
                "    \"id\" : 282,\n" +
                "    \"dataType\" : 5,\n" +
                "    \"values\" : [[72, 1]]\n" +
                "  }, {\n" +
                "    \"id\" : 284,\n" +
                "    \"dataType\" : 3,\n" +
                "    \"values\" : [1]\n" +
                "  }, {\n" +
                "    \"id\" : 306,\n" +
                "    \"dataType\" : 2,\n" +
                "    \"values\" : [\"2002:07:12 16:54:59\"]\n" +
                "  }, {\n" +
                "    \"id\" : 34665,\n" +
                "    \"dataType\" : 4,\n" +
                "    \"values\" : [{\n" +
                "      \"parentTag\" : 34665,\n" +
                "      \"fields\" : [ {\n" +
                "        \"id\" : 33434,\n" +
                "        \"dataType\" : 5,\n" +
                "        \"values\" : [[1, 160]]\n" +
                "      }, {\n" +
                "        \"id\" : 34855,\n" +
                "        \"dataType\" : 3,\n" +
                "        \"values\" : [50]\n" +
                "      }, {\n" +
                "        \"id\" : 36864,\n" +
                "        \"dataType\" : 7,\n" +
                "        \"values\" : [\"0221\"]\n" +
                "      }, {\n" +
                "        \"id\" : 36867,\n" +
                "        \"dataType\" : 2,\n" +
                "        \"values\" : [\"2002:07:12 16:54:59\"]\n" +
                "      }, {\n" +
                "        \"id\" : 37377,\n" +
                "        \"dataType\" : 10,\n" +
                "        \"values\" : [[117, 16]]\n" +
                "      }, {\n" +
                "        \"id\" : 37500,\n" +
                "        \"dataType\" : 7,\n" +
                "        \"values\" : [\"QXBwbGUgaU9TAA==\"]\n" +
                "      }, {\n" +
                "        \"id\" : 40962,\n" +
                "        \"dataType\" : 4,\n" +
                "        \"values\" : [64]\n" +
                "      }, {\n" +
                "        \"id\" : 41729,\n" +
                "        \"dataType\" : 7,\n" +
                "        \"values\" : [1]\n" +
                "      }, {\n" +
                "        \"id\" : 42034,\n" +
                "        \"dataType\" : 5,\n" +
                "        \"values\" : [[83, 20], [83, 20], [11, 5], [11, 5]]\n" +
                "      } ]\n" +
                "    } ]\n" +
                "  } ]\n" +
                "}";

        final Directory exifIFD = new Directory(new EXIFTagSet());
        exifIFD.add(EXPOSURE_TIME, DataType.RATIONAL, List.of(List.of(1L, 160L)));
        exifIFD.add(PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, List.of(50));
        exifIFD.add(EXIF_VERSION, DataType.UNDEFINED, "0221".getBytes(StandardCharsets.US_ASCII));
        exifIFD.add(DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        exifIFD.add(SHUTTER_SPEED, DataType.SRATIONAL, List.of(List.of(117, 16)));
        exifIFD.add(MAKER_NOTE, DataType.UNDEFINED, new byte[] {
                0x41, 0x70, 0x70, 0x6C, 0x65, 0x20, 0x69, 0x4F, 0x53, 0x00 });
        exifIFD.add(PIXEL_X_DIMENSION, DataType.LONG, List.of(64L));
        exifIFD.add(SCENE_TYPE, DataType.UNDEFINED, new byte[] { 0x01 });
        exifIFD.add(LENS_SPECIFICATION, DataType.RATIONAL, List.of(
                List.of(83L, 20L), List.of(83L, 20L),
                List.of(11L, 5L), List.of(11L, 5L)));

        final Directory expectedIFD = new Directory(new EXIFBaselineTIFFTagSet());
        expectedIFD.add(IMAGE_WIDTH, DataType.SHORT, List.of(64));
        expectedIFD.add(IMAGE_LENGTH, DataType.SHORT, List.of(56));
        expectedIFD.add(MAKE, DataType.ASCII, "Canon");
        expectedIFD.add(ORIENTATION, DataType.SHORT, List.of(1));
        expectedIFD.add(X_RESOLUTION, DataType.RATIONAL, List.of(List.of(72L, 1L)));
        expectedIFD.add(PLANAR_CONFIGURATION, DataType.SHORT, List.of(1));
        expectedIFD.add(DATE_TIME, DataType.ASCII, "2002:07:12 16:54:59");
        expectedIFD.add(EXIF_IFD_POINTER, exifIFD);

        Directory actualIFD = new ObjectMapper().readValue(json, Directory.class);
        assertEquals(expectedIFD, actualIFD);
    }

    @Test
    void JSONDeserializationWithUnsupportedIFDTag() {
        final String json = "{\n" +
                "  \"fields\" : [ {\n" +
                "    \"id\" : 256,\n" +
                "    \"dataType\" : 3,\n" +
                "    \"values\" : [64]\n" +
                "  }, {\n" +
                "    \"id\" : 99999,\n" + // unsupported
                "    \"dataType\" : 4,\n" +
                "    \"values\" : [{\n" +
                "      \"parentTag\" : 99999,\n" +
                "      \"fields\" : [ {\n" +
                "        \"id\" : 33434,\n" +
                "        \"dataType\" : 5,\n" +
                "        \"values\" : [[1, 160]]\n" +
                "      } ]\n" +
                "    } ]\n" +
                "  } ]\n" +
                "}";
        JsonParseException e = assertThrows(JsonParseException.class,
                () -> new ObjectMapper().readValue(json, Directory.class));
        assertTrue(e.getMessage().contains("Unsupported tag: 99999"));
    }

    /* add(Field) */

    @Test
    void add1WithIllegalFieldTag() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.add(new StringField(
                        F_NUMBER, DataType.ASCII, "cats")));
    }

    @Test
    void add1() {
        Tag tag = IMAGE_WIDTH;
        Field field = new MultiValueField(tag, DataType.SHORT, List.of(64));
        instance.add(field);
        assertEquals(field, instance.getField(tag));
    }

    /* add(Tag, DataType, List<Object>) */

    @Test
    void add2WithIllegalTag() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.add(F_NUMBER, DataType.ASCII,
                        List.of("cats")));
    }

    @Test
    void add2() {
        Tag tag = IMAGE_WIDTH;
        instance.add(tag, DataType.SHORT, List.of(64));
        assertNotNull(instance.getField(tag));
    }

    /* add(Tag, DataType, byte[]) */

    @Test
    void add3WithIllegalTag() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.add(F_NUMBER, DataType.BYTE,
                        new byte[] { 0x00 }));
    }

    @Test
    void add3() {
        Tag tag = IMAGE_WIDTH;
        instance.add(tag, DataType.BYTE, new byte[] { 0x00 });
        assertNotNull(instance.getField(tag));
    }

    /* add(Tag, DataType, String) */

    @Test
    void add4WithIllegalTag() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.add(F_NUMBER, DataType.ASCII, "cats"));
    }

    @Test
    void add4() {
        Tag tag = IMAGE_WIDTH;
        instance.add(tag, DataType.ASCII, "cats");
        assertNotNull(instance.getField(tag));
    }

    /* add(Tag, Directory) */

    @Test
    void add5WithIllegalTag() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.add(F_NUMBER, DataType.ASCII, List.of("cats")));
    }

    @Test
    void add5() {
        Directory dir = new Directory(new EXIFGPSTagSet());
        Tag tag = GPS_IFD_POINTER;
        instance.add(tag, dir);
        assertSame(dir, instance.getField(tag).getFirstValue());
    }

    /* equals() */

    @Test
    void equalsWithEqualInstances() {
        final Directory subIFD1 = new Directory(new EXIFTagSet());
        subIFD1.add(EXPOSURE_TIME, DataType.RATIONAL, List.of(List.of(1L, 160L)));
        subIFD1.add(PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, List.of(50));
        subIFD1.add(PIXEL_X_DIMENSION, DataType.LONG, List.of(64L));
        subIFD1.add(EXIF_VERSION, DataType.UNDEFINED, "0221".getBytes(StandardCharsets.US_ASCII));
        subIFD1.add(DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        final Directory subIFD2 = new Directory(new EXIFGPSTagSet());
        subIFD2.add(GPS_LATITUDE_REF, DataType.ASCII, "N");
        subIFD2.add(GPS_LATITUDE, DataType.RATIONAL, List.of(List.of(44L, 1L)));
        final Directory rootIFD1 = new Directory(new EXIFBaselineTIFFTagSet());
        rootIFD1.add(IMAGE_LENGTH, DataType.SHORT, List.of(56));
        rootIFD1.add(MAKE, DataType.ASCII, "Canon");
        rootIFD1.add(EXIF_IFD_POINTER, subIFD1);
        rootIFD1.add(GPS_IFD_POINTER, subIFD2);

        final Directory subIFD3 = new Directory(new EXIFTagSet());
        subIFD3.add(EXPOSURE_TIME, DataType.RATIONAL, List.of(List.of(1L, 160L)));
        subIFD3.add(PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, List.of(50));
        subIFD3.add(PIXEL_X_DIMENSION, DataType.LONG, List.of(64L));
        subIFD3.add(EXIF_VERSION, DataType.UNDEFINED, "0221".getBytes(StandardCharsets.US_ASCII));
        subIFD3.add(DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        final Directory subIFD4 = new Directory(new EXIFGPSTagSet());
        subIFD4.add(GPS_LATITUDE_REF, DataType.ASCII, "N");
        subIFD4.add(GPS_LATITUDE, DataType.RATIONAL, List.of(List.of(44L, 1L)));
        final Directory rootIFD2 = new Directory(new EXIFBaselineTIFFTagSet());
        rootIFD2.add(IMAGE_LENGTH, DataType.SHORT, List.of(56));
        rootIFD2.add(MAKE, DataType.ASCII, "Canon");
        rootIFD2.add(EXIF_IFD_POINTER, subIFD3);
        rootIFD2.add(GPS_IFD_POINTER, subIFD4);

        assertEquals(rootIFD1, rootIFD2);
    }

    @Test
    void equalsWithUnequalInstances() {
        final Directory subIFD1 = new Directory(new EXIFTagSet());
        subIFD1.add(EXPOSURE_TIME, DataType.RATIONAL, List.of(List.of(1L, 160L)));
        subIFD1.add(PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, List.of(50));
        subIFD1.add(EXIF_VERSION, DataType.UNDEFINED, "0221".getBytes(StandardCharsets.US_ASCII));
        subIFD1.add(DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        final Directory rootIFD1 = new Directory(new EXIFBaselineTIFFTagSet());
        rootIFD1.add(IMAGE_LENGTH, DataType.SHORT, List.of(56));
        rootIFD1.add(MAKE, DataType.ASCII, "Canon");
        rootIFD1.add(EXIF_IFD_POINTER, subIFD1);

        final Directory subIFD2 = new Directory(new EXIFTagSet());
        subIFD2.add(EXPOSURE_TIME, DataType.RATIONAL, List.of(List.of(1L, 160L)));
        // DIFFERENT!!
        subIFD2.add(PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, List.of(49));
        subIFD2.add(EXIF_VERSION, DataType.UNDEFINED, "0221".getBytes(StandardCharsets.US_ASCII));
        subIFD2.add(DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        final Directory rootIFD2 = new Directory(new EXIFBaselineTIFFTagSet());
        rootIFD2.add(IMAGE_LENGTH, DataType.SHORT, List.of(56));
        rootIFD2.add(MAKE, DataType.ASCII, "Canon");
        rootIFD2.add(EXIF_IFD_POINTER, subIFD2);

        assertNotEquals(rootIFD1, rootIFD2);
    }

    /* getField() */

    @Test
    void getFieldWithNonExistingTag() {
        instance = new Directory(new EXIFBaselineTIFFTagSet());
        assertNull(instance.getField(ARTIST));
    }

    @Test
    void getFieldWithExistingTag() {
        assertNotNull(instance.getField(WHITE_POINT));
    }

    /* getFields() */

    @Test
    void getFields() {
        assertEquals(1, instance.getFields().size());
    }

    @Test
    void getFieldsReturnsUnmodifiableSet() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.getFields().clear());
    }

    /* getSubdirectory() */

    @Test
    void getSubdirectoryWithNonExistingSubdirectory() {
        assertNull(instance.getSubdirectory(MAKE));
    }

    @Test
    void getSubdirectoryWithExistingSubdirectory() {
        final Directory exifIFD = new Directory(new EXIFTagSet());
        instance.add(EXIF_IFD_POINTER, exifIFD);

        assertNotNull(instance.getSubdirectory(EXIF_IFD_POINTER));
    }

    /* getTagSet() */

    @Test
    void getTagSet() {
        assertNotNull(instance.getTagSet());
    }

    /* hashCode() */

    @Test
    void hashCodeWithEqualInstances() {
        final Directory subIFD1 = new Directory(new EXIFTagSet());
        subIFD1.add(EXPOSURE_TIME, DataType.RATIONAL, List.of(List.of(1L, 160L)));
        subIFD1.add(PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, List.of(50));
        subIFD1.add(PIXEL_X_DIMENSION, DataType.LONG, List.of(64L));
        subIFD1.add(EXIF_VERSION, DataType.UNDEFINED, "0221".getBytes(StandardCharsets.US_ASCII));
        subIFD1.add(DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        final Directory subIFD2 = new Directory(new EXIFGPSTagSet());
        subIFD2.add(GPS_LATITUDE_REF, DataType.ASCII, "N");
        subIFD2.add(GPS_LATITUDE, DataType.RATIONAL, List.of(List.of(44L, 1L)));
        final Directory rootIFD1 = new Directory(new EXIFBaselineTIFFTagSet());
        rootIFD1.add(IMAGE_LENGTH, DataType.SHORT, List.of(56));
        rootIFD1.add(MAKE, DataType.ASCII, "Canon");
        rootIFD1.add(EXIF_IFD_POINTER, subIFD1);
        rootIFD1.add(GPS_IFD_POINTER, subIFD2);

        final Directory subIFD3 = new Directory(new EXIFTagSet());
        subIFD3.add(EXPOSURE_TIME, DataType.RATIONAL, List.of(List.of(1L, 160L)));
        subIFD3.add(PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, List.of(50));
        subIFD3.add(PIXEL_X_DIMENSION, DataType.LONG, List.of(64L));
        subIFD3.add(EXIF_VERSION, DataType.UNDEFINED, "0221".getBytes(StandardCharsets.US_ASCII));
        subIFD3.add(DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        final Directory subIFD4 = new Directory(new EXIFGPSTagSet());
        subIFD4.add(GPS_LATITUDE_REF, DataType.ASCII, "N");
        subIFD4.add(GPS_LATITUDE, DataType.RATIONAL, List.of(List.of(44L, 1L)));
        final Directory rootIFD2 = new Directory(new EXIFBaselineTIFFTagSet());
        rootIFD2.add(IMAGE_LENGTH, DataType.SHORT, List.of(56));
        rootIFD2.add(MAKE, DataType.ASCII, "Canon");
        rootIFD2.add(EXIF_IFD_POINTER, subIFD3);
        rootIFD2.add(GPS_IFD_POINTER, subIFD4);

        assertEquals(rootIFD1.hashCode(), rootIFD2.hashCode());
    }

    @Test
    void hashCodeWithUnequalInstances() {
        final Directory subIFD1 = new Directory(new EXIFTagSet());
        subIFD1.add(EXPOSURE_TIME, DataType.RATIONAL, List.of(List.of(1L, 160L)));
        subIFD1.add(PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, List.of(50));
        subIFD1.add(EXIF_VERSION, DataType.UNDEFINED, "0221".getBytes(StandardCharsets.US_ASCII));
        subIFD1.add(DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        final Directory rootIFD1 = new Directory(new EXIFBaselineTIFFTagSet());
        rootIFD1.add(IMAGE_LENGTH, DataType.SHORT, List.of(56));
        rootIFD1.add(MAKE, DataType.ASCII, "Canon");
        rootIFD1.add(EXIF_IFD_POINTER, subIFD1);

        final Directory subIFD2 = new Directory(new EXIFTagSet());
        subIFD2.add(EXPOSURE_TIME, DataType.RATIONAL, List.of(List.of(1L, 160L)));
        // DIFFERENT!!
        subIFD2.add(PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, List.of(49));
        subIFD2.add(EXIF_VERSION, DataType.UNDEFINED, "0221".getBytes(StandardCharsets.US_ASCII));
        subIFD2.add(DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        final Directory rootIFD2 = new Directory(new EXIFBaselineTIFFTagSet());
        rootIFD2.add(IMAGE_LENGTH, DataType.SHORT, List.of(56));
        rootIFD2.add(MAKE, DataType.ASCII, "Canon");
        rootIFD2.add(EXIF_IFD_POINTER, subIFD2);

        assertNotEquals(rootIFD1.hashCode(), rootIFD2.hashCode());
    }

    /* size() */

    @Test
    void size() {
        final Directory exifIFD = new Directory(new EXIFTagSet());
        exifIFD.add(EXPOSURE_TIME, DataType.RATIONAL, List.of(List.of(1L, 160L)));

        final Directory gpsIFD = new Directory(new EXIFGPSTagSet());
        gpsIFD.add(GPS_LATITUDE_REF, DataType.ASCII, "N");
        gpsIFD.add(GPS_LATITUDE, DataType.RATIONAL, List.of(List.of(44L, 1L)));

        final Directory rootIFD = new Directory(new EXIFBaselineTIFFTagSet());
        rootIFD.add(IMAGE_WIDTH, DataType.SHORT, List.of(64));
        rootIFD.add(IMAGE_LENGTH, DataType.SHORT, List.of(56));
        rootIFD.add(EXIF_IFD_POINTER, exifIFD);
        rootIFD.add(GPS_IFD_POINTER, gpsIFD);

        assertEquals(4, rootIFD.size());
    }

    /* toMap() */

    @Test
    void toMap() {
        final Map<String,Object> expectedMap = new LinkedHashMap<>(2);
        {
            expectedMap.put("tagSet", new EXIFBaselineTIFFTagSet().getName());
            Map<String, Object> baselineFields = new LinkedHashMap<>();
            expectedMap.put("fields", baselineFields);
            baselineFields.put(IMAGE_WIDTH.name(), List.of(64));
            baselineFields.put(IMAGE_LENGTH.name(), List.of(56));

            final Map<String, Object> exifMap = new LinkedHashMap<>(2);
            exifMap.put("tagSet", new EXIFTagSet().getName());
            Map<String, Object> exifFields = new LinkedHashMap<>();
            exifMap.put("fields", exifFields);
            exifFields.put(EXPOSURE_TIME.name(), List.of(List.of(1L, 160L)));
            baselineFields.put("EXIFIFD", exifMap);
        }

        // assemble a reference Directory structure
        final Directory dir = new Directory(new EXIFBaselineTIFFTagSet());
        {
            final Directory exifIFD = new Directory(new EXIFTagSet());
            exifIFD.add(EXPOSURE_TIME, DataType.RATIONAL, List.of(List.of(1L, 160L)));
            dir.add(IMAGE_WIDTH, DataType.SHORT, List.of(64));
            dir.add(IMAGE_LENGTH, DataType.SHORT, List.of(56));
            dir.add(EXIF_IFD_POINTER, exifIFD);
        }

        assertEquals(expectedMap, dir.toMap());
    }

}
