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

package is.galia.image;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import is.galia.codec.Decoder;
import is.galia.codec.tiff.DataType;
import is.galia.codec.tiff.Directory;
import is.galia.codec.tiff.DirectoryField;
import is.galia.codec.tiff.EXIFBaselineTIFFTagSet;
import is.galia.codec.tiff.EXIFTagSet;
import is.galia.codec.tiff.MultiValueField;
import is.galia.codec.tiff.StringField;
import is.galia.codec.iptc.DataSet;
import is.galia.codec.DecoderFactory;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static is.galia.codec.tiff.EXIFBaselineTIFFTagSet.EXIF_IFD_POINTER;
import static is.galia.codec.tiff.EXIFBaselineTIFFTagSet.IMAGE_LENGTH;
import static is.galia.codec.tiff.EXIFBaselineTIFFTagSet.IMAGE_WIDTH;
import static is.galia.codec.tiff.EXIFTagSet.EXPOSURE_TIME;
import static is.galia.codec.tiff.EXIFTagSet.LENS_MAKE;
import static is.galia.codec.tiff.EXIFTagSet.LENS_MODEL;
import static org.junit.jupiter.api.Assertions.*;

class MutableMetadataTest extends BaseTest {

    private MutableMetadata instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new MutableMetadata();
    }

    @Test
    void testJSONSerialization() throws Exception {
        Directory exif               = new Directory(new EXIFTagSet());
        List<DataSet> iptc           = List.of(new DataSet(
                is.galia.codec.iptc.Tag.CITY,
                "Urbana".getBytes()));
        String xmp                    = "<rdf:RDF>cats</rdf:RDF>";
        NativeMetadata nativeMetadata = new MockNativeMetadata();

        instance.setEXIF(exif);
        instance.setIPTC(iptc);
        instance.setXMP(xmp);
        instance.setNativeMetadata(nativeMetadata);

        String actual = new ObjectMapper()
                .registerModule(new Jdk8Module())
                .writer()
                .withDefaultPrettyPrinter()
                .writeValueAsString(instance);
        assertNotNull(actual);
    }

    /* equals() */

    @Test
    void equalsWithEqualInstances() {
        Directory exif               = new Directory(new EXIFTagSet());
        List<DataSet> iptc           = List.of(new DataSet(
                is.galia.codec.iptc.Tag.CITY,
                "Urbana".getBytes()));
        String xmp                    = "<rdf:RDF>cats</rdf:RDF>";
        NativeMetadata nativeMetadata = new MockNativeMetadata();

        MutableMetadata m1 = new MutableMetadata();
        m1.setEXIF(exif);
        m1.setIPTC(iptc);
        m1.setXMP(xmp);
        m1.setNativeMetadata(nativeMetadata);

        MutableMetadata m2 = new MutableMetadata();
        m2.setEXIF(exif);
        m2.setIPTC(iptc);
        m2.setXMP(xmp);
        m2.setNativeMetadata(nativeMetadata);

        assertEquals(m1, m2);
    }

    @Test
    void equalsWithDifferentEXIF() {
        Directory exif1 = new Directory(new EXIFTagSet());
        exif1.add(new StringField(LENS_MODEL, DataType.ASCII, "cats"));
        MutableMetadata m1 = new MutableMetadata();
        m1.setEXIF(exif1);

        Directory exif2 = new Directory(new EXIFTagSet());
        exif2.add(new StringField(LENS_MAKE, DataType.ASCII, "cats"));
        MutableMetadata m2 = new MutableMetadata();
        m2.setEXIF(exif2);

        assertNotEquals(m1, m2);
    }

    @Test
    void equalsWithDifferentIPTC() {
        List<DataSet> iptc1 = List.of(new DataSet(
                is.galia.codec.iptc.Tag.CITY,
                "Urbana".getBytes()));
        MutableMetadata m1 = new MutableMetadata();
        m1.setIPTC(iptc1);

        List<DataSet> iptc2 = List.of(new DataSet(
                is.galia.codec.iptc.Tag.CITY,
                "Champaign".getBytes()));
        MutableMetadata m2 = new MutableMetadata();
        m2.setIPTC(iptc2);

        assertNotEquals(m1, m2);
    }

    @Test
    void equalsWithDifferentNativeMetadata() {
        MutableMetadata m1 = new MutableMetadata();
        m1.setNativeMetadata(() -> Map.of("cats", "yes"));

        MutableMetadata m2 = new MutableMetadata();
        m2.setNativeMetadata(() -> Map.of("dogs", "yes"));

        assertNotEquals(m1, m2);
    }

    @Test
    void equalsWithDifferentXMP() {
        MutableMetadata m1 = new MutableMetadata();
        m1.setXMP("<rdf:RDF>cats</rdf:RDF>");

        MutableMetadata m2 = new MutableMetadata();
        m2.setXMP("<rdf:RDF>dogs</rdf:RDF>");

        assertNotEquals(m1, m2);
    }

    /* getEXIF() */

    @Test
    void getEXIFWithPresentEXIFData() throws Exception {
        Path fixture = TestUtils.getSampleImage("jpg/exif.jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(Format.get("jpg"), arena)) {
            decoder.setSource(fixture);
            Metadata metadata = decoder.readMetadata(0);
            assertFalse(metadata.getEXIF().isEmpty());
        }
    }

    @Test
    void getEXIFWithNoEXIFData() throws Exception {
        Path fixture = TestUtils.getSampleImage("jpg/jpg.jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(Format.get("jpg"), arena)) {
            decoder.setSource(fixture);
            Metadata metadata = decoder.readMetadata(0);
            assertTrue(metadata.getEXIF().isEmpty());
        }
    }

    /* getIPTC() */

    @Test
    void getIPTCWithPresentIPTCData() throws Exception {
        Path fixture = TestUtils.getSampleImage("jpg/iptc.jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(Format.get("jpg"), arena)) {
            decoder.setSource(fixture);
            Metadata metadata = decoder.readMetadata(0);
            assertFalse(metadata.getIPTC().isEmpty());
        }
    }

    @Test
    void getIPTCWithNoIPTCData() throws Exception {
        Path fixture = TestUtils.getSampleImage("jpg/jpg.jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(Format.get("jpg"), arena)) {
            decoder.setSource(fixture);
            Metadata metadata = decoder.readMetadata(0);
            assertTrue(metadata.getIPTC().isEmpty());
        }
    }

    /* getNativeMetadata() */

    @Test
    void getNativeMetadataWithPresentData() throws Exception {
        Path fixture = TestUtils.getSampleImage("png/nativemetadata.png");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(Format.get("png"), arena)) {
            decoder.setSource(fixture);
            Metadata metadata = decoder.readMetadata(0);
            assertTrue(metadata.getNativeMetadata().isPresent());
        }
    }

    @Test
    void getNativeMetadataWithNoData() throws Exception {
        Path fixture = TestUtils.getSampleImage("png/rgb-1x1x8.png");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(Format.get("png"), arena)) {
            decoder.setSource(fixture);
            Metadata metadata = decoder.readMetadata(0);
            assertFalse(metadata.getNativeMetadata().isPresent());
        }
    }

    /* getOrientation() */

    @Test
    void getOrientationWithNoEXIFOrXMP() {
        assertEquals(Orientation.ROTATE_0, instance.getOrientation());
    }

    @Test
    void getOrientationWithOnlyEXIFButNoOrientationTag() {
        Directory exif = new Directory(new EXIFTagSet());
        instance.setEXIF(exif);
        assertEquals(Orientation.ROTATE_0, instance.getOrientation());
    }

    @Test
    void getOrientationWithOnlyEXIFButIllegalOrientationValue() throws Exception {
        Path fixture = TestUtils.getSampleImage("jpg/exif-orientation-illegal.jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(Format.get("jpg"), arena)) {
            decoder.setSource(fixture);
            Metadata metadata = decoder.readMetadata(0);
            assertEquals(Orientation.ROTATE_0, metadata.getOrientation());
        }
    }

    @Test
    void getOrientationWithOnlyEXIFOrientation() throws Exception {
        Path fixture = TestUtils.getSampleImage("jpg/exif-orientation-270.jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(Format.get("jpg"), arena)) {
            decoder.setSource(fixture);
            Metadata metadata = decoder.readMetadata(0);
            assertEquals(Orientation.ROTATE_270, metadata.getOrientation());
        }
    }

    @Test
    void getOrientationWithMalformedXMP() {
        instance.setXMP("����\u0000\u0010JFIF\u0000\u0001\u0001\u0001\u0000H\u0000H\u0000\u0000��\u0000C\u0000\b\u0006\u0006\u0007\u0006\u0005\b\u0007\u0007\u0007");
        assertEquals(Orientation.ROTATE_0, instance.getOrientation());
    }

    @Test
    void getOrientationWithOnlyXMPButNoOrientationProperty() throws Exception {
        Path fixture = TestUtils.getSampleImage("jpg/xmp.jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(Format.get("jpg"), arena)) {
            decoder.setSource(fixture);
            Metadata metadata = decoder.readMetadata(0);
            assertEquals(Orientation.ROTATE_0, metadata.getOrientation());
        }
    }

    @Test
    void getOrientationWithOnlyXMPButIllegalOrientationValue() throws Exception {
        Path fixture = TestUtils.getSampleImage("jpg/xmp-orientation-illegal.jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(Format.get("jpg"), arena)) {
            decoder.setSource(fixture);
            Metadata metadata = decoder.readMetadata(0);
            assertEquals(Orientation.ROTATE_0, metadata.getOrientation());
        }
    }

    @Test
    void getOrientationWithXMPOrientation() throws Exception {
        Path fixture = TestUtils.getSampleImage("jpg/xmp-orientation-90.jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(Format.get("jpg"), arena)) {
            decoder.setSource(fixture);
            Metadata metadata = decoder.readMetadata(0);
            assertEquals(Orientation.ROTATE_90, metadata.getOrientation());
        }
    }

    /* getXMPElements() */

    @Test
    void getXMPElementsWithPresentXMPData() throws Exception {
        Path fixture = TestUtils.getSampleImage("jpg/xmp.jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(Format.get("jpg"), arena)) {
            decoder.setSource(fixture);
            Metadata metadata        = decoder.readMetadata(0);
            Map<String,Object> model = metadata.getXMPElements();
            assertEquals(6, model.size());
        }
    }

    @Test
    void getXMPElementsWithNoXMPData() throws Exception {
        Path fixture = TestUtils.getSampleImage("jpg/jpg.jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(Format.get("jpg"), arena)) {
            decoder.setSource(fixture);
            Metadata metadata = decoder.readMetadata(0);
            assertTrue(metadata.getXMPElements().isEmpty());
        }
    }

    /* getXMPModel() */

    @Test
    void getXMPModelWithPresentXMPData() throws Exception {
        Path fixture = TestUtils.getSampleImage("jpg/xmp.jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(Format.get("jpg"), arena)) {
            decoder.setSource(fixture);
            Metadata metadata = decoder.readMetadata(0);
            Model model = metadata.getXMPModel().orElseThrow();
            assertEquals(12, model.size());
        }
    }

    @Test
    void getXMPModelWithNoXMPData() throws Exception {
        Path fixture = TestUtils.getSampleImage("jpg/jpg.jpg");
        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(Format.get("jpg"), arena)) {
            decoder.setSource(fixture);
            Metadata metadata = decoder.readMetadata(0);
            assertFalse(metadata.getXMPModel().isPresent());
        }
    }

    /* hashCode() */

    @Test
    void hashCodeWithEqualInstances() {
        Directory exif                = new Directory(new EXIFTagSet());
        String xmp                    = "<rdf:RDF>cats</rdf:RDF>";
        NativeMetadata nativeMetadata = new MockNativeMetadata();

        MutableMetadata m1 = new MutableMetadata();
        m1.setEXIF(exif);
        m1.setXMP(xmp);
        m1.setNativeMetadata(nativeMetadata);

        MutableMetadata m2 = new MutableMetadata();
        m2.setEXIF(exif);
        m2.setXMP(xmp);
        m2.setNativeMetadata(nativeMetadata);

        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void hashCodeWithDifferentEXIF() {
        Directory exif1 = new Directory(new EXIFTagSet());
        exif1.add(new StringField(LENS_MODEL, DataType.ASCII, "cats"));
        MutableMetadata m1 = new MutableMetadata();
        m1.setEXIF(exif1);

        Directory exif2 = new Directory(new EXIFTagSet());
        exif1.add(new StringField(LENS_MAKE, DataType.ASCII, "cats"));
        MutableMetadata m2 = new MutableMetadata();
        m2.setEXIF(exif2);

        assertNotEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void hashCodeWithDifferentIPTC() {
        List<DataSet> iptc1 = List.of(new DataSet(
                is.galia.codec.iptc.Tag.CITY,
                "Urbana".getBytes()));
        MutableMetadata m1 = new MutableMetadata();
        m1.setIPTC(iptc1);

        List<DataSet> iptc2 = List.of(new DataSet(
                is.galia.codec.iptc.Tag.CITY,
                "Champaign".getBytes()));
        MutableMetadata m2 = new MutableMetadata();
        m2.setIPTC(iptc2);

        assertNotEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void hashCodeWithDifferentNativeMetadata() {
        MutableMetadata m1 = new MutableMetadata();
        m1.setNativeMetadata(() -> Map.of("cats", "yes"));

        MutableMetadata m2 = new MutableMetadata();
        m2.setNativeMetadata(() -> Map.of("dogs", "yes"));

        assertNotEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void hashCodeWithDifferentXMP() {
        MutableMetadata m1 = new MutableMetadata();
        m1.setXMP("<rdf:RDF>cats</rdf:RDF>");

        MutableMetadata m2 = new MutableMetadata();
        m2.setXMP("<rdf:RDF>dogs</rdf:RDF>");

        assertNotEquals(m1.hashCode(), m2.hashCode());
    }

    /* setXMP() */

    @Test
    void setXMPWithNullByteArrayArgument() {
        instance.setXMP((byte[]) null);
        assertFalse(instance.getXMP().isPresent());
    }

    @Test
    void setXMPWithNullStringArgument() {
        instance.setXMP((String) null);
        assertFalse(instance.getXMP().isPresent());
    }

    @Test
    void setXMPTrimsData() {
        instance.setXMP("<??><rdf:RDF></rdf:RDF> <??>");
        String xmp = instance.getXMP().orElseThrow();
        assertTrue(xmp.startsWith("<rdf:RDF"));
        assertTrue(xmp.endsWith("</rdf:RDF>"));
    }

    /* toMap() */

    @Test
    void toMap() {
        // assemble the expected map structure
        final Map<String,Object> expectedMap = new HashMap<>(2);

        final Map<String,Object> baselineMap = new LinkedHashMap<>(2);
        baselineMap.put("tagSet", new EXIFBaselineTIFFTagSet().getName());
        Map<String,Object> baselineFields = new LinkedHashMap<>();
        baselineMap.put("fields", baselineFields);
        baselineFields.put(IMAGE_WIDTH.name(), List.of(64));
        baselineFields.put(IMAGE_LENGTH.name(), List.of(56));

        final Map<String,Object> exifMap = new LinkedHashMap<>(2);
        exifMap.put("tagSet", new EXIFTagSet().getName());
        Map<String,Object> exifFields = new LinkedHashMap<>();
        exifMap.put("fields", exifFields);
        exifFields.put(EXPOSURE_TIME.name(), List.of(List.of(1, 160)));
        baselineFields.put(EXIF_IFD_POINTER.name(), exifMap);

        expectedMap.put("exif", List.of(baselineMap));
        expectedMap.put("iptc", List.of(new DataSet(
                is.galia.codec.iptc.Tag.CITY,
                "Urbana".getBytes()).toMap()));
        expectedMap.put("xmp_string", "<rdf:RDF></rdf:RDF>");
        expectedMap.put("xmp_elements", Collections.emptyMap());
        expectedMap.put("native", Map.of("key1", "value1", "key2", "value2"));

        // assemble the Metadata
        // EXIF
        final Directory exifIFD = new Directory(new EXIFTagSet());
        exifIFD.add(new MultiValueField(
                EXPOSURE_TIME, DataType.RATIONAL, List.of(List.of(1, 160))));

        final Directory rootIFD = new Directory(new EXIFBaselineTIFFTagSet());
        rootIFD.add(new MultiValueField(
                IMAGE_WIDTH, DataType.SHORT, List.of(64)));
        rootIFD.add(new MultiValueField(
                IMAGE_LENGTH, DataType.SHORT, List.of(56)));
        rootIFD.add(new DirectoryField(EXIF_IFD_POINTER, exifIFD));
        instance.setEXIF(rootIFD);
        // IPTC
        List<DataSet> iptc = List.of(new DataSet(
                is.galia.codec.iptc.Tag.CITY,
                "Urbana".getBytes()));
        instance.setIPTC(iptc);
        // XMP
        instance.setXMP("<rdf:RDF></rdf:RDF>");
        // native
        instance.setNativeMetadata(
                () -> Map.of("key1", "value1", "key2", "value2"));

        final Map<String,Object> actualMap = new HashMap<>(instance.toMap());
        // remove the model key for comparison
        assertTrue(actualMap.containsKey("xmp_model"));
        actualMap.remove("xmp_model");

        // compare
        assertEquals(expectedMap, actualMap);
    }

}
