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

import is.galia.Application;
import is.galia.test.BaseTest;
import is.galia.util.SoftwareVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class InfoTest extends BaseTest {

    /*********************** Builder tests **************************/

    @Nested
    class BuilderTest extends BaseTest {

        @Test
        void withFormat() {
            Info info = Info.builder().withFormat(Format.get("png")).build();
            assertEquals(Format.get("png"), info.getSourceFormat());
        }

        @Test
        void withIdentifier() {
            Identifier identifier = new Identifier("cats");
            Info info = Info.builder().withIdentifier(identifier).build();
            assertEquals(identifier, info.getIdentifier());
        }

        @Test
        void withMetadata() {
            Metadata metadata = new MutableMetadata();
            Info info = Info.builder().withMetadata(metadata).build();
            assertEquals(metadata, info.getMetadata());
        }

        @Test
        void withNumResolutions() {
            Info info = Info.builder().withNumResolutions(5).build();
            assertEquals(5, info.getNumResolutions());
        }

        @Test
        void withSize1() {
            Size size = new Size(45, 50);
            Info info = Info.builder().withSize(size).build();
            assertEquals(size, info.getSize());
        }

        @Test
        void withSize2() {
            int width = 45;
            int height = 50;
            Info info = Info.builder().withSize(width, height).build();
            assertEquals(new Size(45, 50), info.getSize());
        }

        @Test
        void withTileSize1() {
            Size size = new Size(45, 50);
            Info info = Info.builder().withTileSize(size).build();
            assertEquals(size, info.getImages().getFirst().getTileSize());
        }

        @Test
        void withTileSize2() {
            int width = 45;
            int height = 50;
            Info info = Info.builder().withTileSize(width, height).build();
            assertEquals(new Size(width, height),
                    info.getImages().getFirst().getTileSize());
        }

    }

    /********************* Info.Image tests *************************/

    @Nested
    class InfoImageTest {

        @Test
        void constructor() {
            Info.Image image = new Info.Image();
            assertEquals(new Size(0, 0), image.getSize());
            assertEquals(new Size(0, 0), image.getTileSize());
        }

        @Test
        void equalsWithEqualInstances() {
            Info.Image image1 = new Info.Image();
            image1.setSize(new Size(100, 50));
            image1.setTileSize(new Size(50, 25));

            Info.Image image2 = new Info.Image();
            image2.setSize(image1.getSize());
            image2.setTileSize(image1.getTileSize());

            assertEquals(image1, image2);
        }

        @Test
        void equalsWithUnequalSizes() {
            Info.Image image1 = new Info.Image();
            image1.setSize(new Size(100, 50));
            image1.setTileSize(new Size(50, 25));

            Info.Image image2 = new Info.Image();
            image2.setSize(new Size(100, 49));
            image2.setTileSize(image1.getTileSize());

            assertNotEquals(image1, image2);
        }

        @Test
        void equalsWithUnequalTileSizes() {
            Info.Image image1 = new Info.Image();
            image1.setSize(new Size(100, 50));
            image1.setTileSize(new Size(50, 25));

            Info.Image image2 = new Info.Image();
            image2.setSize(image1.getSize());
            image2.setTileSize(new Size(50, 24));

            assertNotEquals(image1, image2);
        }

        @Test
        void hashCodeWithEqualInstances() {
            Info.Image image1 = new Info.Image();
            image1.setSize(new Size(100, 50));
            image1.setTileSize(new Size(50, 25));

            Info.Image image2 = new Info.Image();
            image2.setSize(image1.getSize());
            image2.setTileSize(image1.getTileSize());

            assertEquals(image1.hashCode(), image2.hashCode());
        }

        @Test
        void hashCodeWithUnequalSizes() {
            Info.Image image1 = new Info.Image();
            image1.setSize(new Size(100, 50));
            image1.setTileSize(new Size(50, 25));

            Info.Image image2 = new Info.Image();
            image2.setSize(new Size(100, 49));
            image2.setTileSize(image1.getTileSize());

            assertNotEquals(image1.hashCode(), image2.hashCode());
        }

        @Test
        void hashCodeWithUnequalTileSizes() {
            Info.Image image1 = new Info.Image();
            image1.setSize(new Size(100, 50));
            image1.setTileSize(new Size(50, 25));

            Info.Image image2 = new Info.Image();
            image2.setSize(image1.getSize());
            image2.setTileSize(new Size(50, 24));

            assertNotEquals(image1.hashCode(), image2.hashCode());
        }

        /* setMetadata() */

        @Test
        void setMetadata() {
            Metadata metadata = new MutableMetadata();
            Info.Image image = new Info.Image();
            image.setMetadata(metadata);
            assertSame(metadata, image.getMetadata());
        }

    }

    private Info instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        final MutableMetadata metadata = new MutableMetadata();
        metadata.setXMP("<cats/>");

        instance = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withFormat(Format.get("jpg"))
                .withNumResolutions(3)
                .withMetadata(metadata)
                .build();
    }

    /************************ Info tests ****************************/

    /* fromJSON(Path) */

    @Test
    void fromJSONWithPath() throws Exception {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("test", "json");

            // Serialize the instance to JSON and write it to a file.
            String json = instance.toJSON();
            Files.write(tempFile, json.getBytes(StandardCharsets.UTF_8));

            Info info = Info.fromJSON(tempFile);
            assertEquals(obscureTimestamps(info.toString()),
                    obscureTimestamps(instance.toString()));
        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /* fromJSON(InputStream) */

    @Test
    void fromJSONWithInputStream() throws Exception {
        String json = instance.toJSON();
        InputStream inputStream = new ByteArrayInputStream(json.getBytes());

        Info info = Info.fromJSON(inputStream);
        assertEquals(obscureTimestamps(info.toString()),
                obscureTimestamps(instance.toString()));
    }

    /* fromJSON(String) */

    @Test
    void fromJSONWithString() throws Exception {
        String json = instance.toJSON();
        Info info = Info.fromJSON(json);
        assertEquals(obscureTimestamps(info.toString()),
                obscureTimestamps(instance.toString()));
    }

    /* fromJSON() serialization */

    @Test
    void fromJSONWithVersion1Serialization() throws Exception {
        Instant timestamp = Instant.now();
        String v1json = "{\n" +
                "  \"applicationVersion\": \"1.0\",\n" +
                "  \"serializationVersion\": 1,\n" +
                "  \"serializationTimestamp\": \"" + timestamp.toString() + "\",\n" +
                "  \"identifier\": \"cats\",\n" +
                "  \"mediaType\": \"image/jpeg\",\n" +
                "  \"numResolutions\": 3,\n" +
                "  \"images\": [\n" +
                "    {\n" +
                "      \"width\": 100,\n" +
                "      \"height\": 80,\n" +
                "      \"tileWidth\": 50,\n" +
                "      \"tileHeight\": 40,\n" +
                "      \"metadata\": {\n" +
                "        \"xmp\": \"<cats/>\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        MutableMetadata metadata = new MutableMetadata();
        metadata.setXMP("<cats/>");
        Info actual = Info.fromJSON(v1json);
        Info expected = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withFormat(Format.get("jpg"))
                .withNumResolutions(3)
                .withMetadata(metadata)
                .withSize(100, 80)
                .withTileSize(50, 40)
                .build();
        expected.setApplicationVersion(new SoftwareVersion(1));
        expected.setSerializationTimestamp(timestamp);
        assertEquals(expected, actual);
    }

    /* Info() */

    @Test
    void constructor() {
        instance = new Info();
        assertEquals(1, instance.getImages().size());
        assertNotNull(instance.getMetadata());
    }

    /* equals() */

    @Test
    void equalsWithEqualInstances() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertEquals(instance, info2);
    }

    @Test
    void equalsWithDifferentApplicationVersions() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        info2.setApplicationVersion(new SoftwareVersion(99));
        assertNotEquals(instance, info2);
    }

    @Test
    void equalsWithDifferentSerializationVersions() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        info2.setSerializationVersion(2);
        assertNotEquals(instance, info2);
    }

    @Test
    void equalsWithDifferentSerializationTimestamps() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        info2.setSerializationTimestamp(Instant.now());
        assertEquals(instance, info2);
    }

    @Test
    void equalsWithDifferentIdentifiers() {
        Info info2 = Info.builder()
                .withIdentifier(new Identifier("mules"))
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void equalsWithDifferentWidths() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(999, instance.getSize().intHeight())
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void equalsWithDifferentHeights() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize().intWidth(), 999)
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void equalsWithDifferentTileWidths() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(999, instance.getImages().getFirst().getTileSize().intHeight())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void equalsWithDifferentTileHeights() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().getFirst().getTileSize().intWidth(), 999)
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void equalsWithDifferentMetadatas() {
        MutableMetadata metadata2 = new MutableMetadata();
        metadata2.setXMP("<dogs/>");

        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(metadata2)
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void equalsWithDifferentNumResolutions() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions() + 1)
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void equalsWithDifferentFormats() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(Format.get("gif"))
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance, info2);
    }

    /* getApplicationVersion() */

    @Test
    void getApplicationVersion() {
        assertEquals(Application.getVersion(),
                instance.getApplicationVersion());
    }

    /* getImages() */

    @Test
    void getImages() {
        assertEquals(1, instance.getImages().size());
    }

    /* getMetadata() */

    @Test
    void getMetadata() {
        assertEquals("<cats/>", instance.getMetadata().getXMP().orElseThrow());
    }

    /* getNumPages() */

    @Test
    void getNumPagesWithSingleResolutionImage() {
        instance = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withFormat(Format.get("jpg"))
                .withNumResolutions(1)
                .build();
        assertEquals(1, instance.getNumPages());
    }

    @Test
    void getNumPagesWithPyramidalImage() {
        instance = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(1000, 800)
                .withFormat(Format.get("jpg"))
                .withNumResolutions(6)
                .build();
        // level 2
        Info.Image image = new Info.Image();
        image.setSize(new Size(500, 400));
        instance.getImages().add(image);
        // level 3
        image = new Info.Image();
        image.setSize(new Size(250, 200));
        instance.getImages().add(image);
        // level 4
        image = new Info.Image();
        image.setSize(new Size(125, 100));
        instance.getImages().add(image);
        // level 5
        image = new Info.Image();
        image.setSize(new Size(63, 50));
        instance.getImages().add(image);
        // level 6
        image = new Info.Image();
        image.setSize(new Size(32, 25));
        instance.getImages().add(image);

        assertEquals(1, instance.getNumPages());
    }

    @Test
    void getNumPagesWithNonPyramidalMultiImageImage() {
        instance = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(1000, 800)
                .withFormat(Format.get("jpg"))
                .withNumResolutions(1)
                .build();
        Info.Image image = new Info.Image();
        image.setSize(new Size(600, 300));
        instance.getImages().add(image);
        // level 3
        image = new Info.Image();
        image.setSize(new Size(200, 900));
        instance.getImages().add(image);

        assertEquals(3, instance.getNumPages());
    }

    /* getNumResolutions() */

    @Test
    void getNumResolutions() {
        assertEquals(3, instance.getNumResolutions());
    }

    /* getSerialization() */

    @Test
    void getSerialization() {
        assertEquals(Info.Serialization.CURRENT, instance.getSerialization());
    }

    /* adjustedSize() */

    @Test
    void getSize() {
        assertEquals(new Size(100, 80), instance.getSize());
    }

    /* adjustedSize(int) */

    @Test
    void getSizeWithIndex() {
        Info.Image image = new Info.Image();
        image.setSize(new Size(59, 48));
        instance.getImages().add(image);

        image = new Info.Image();
        image.setSize(new Size(25, 20));
        instance.getImages().add(image);

        assertEquals(new Size(25, 20), instance.getSize(2));
    }

    /* getSourceFormat() */

    @Test
    void getSourceFormat() {
        assertEquals(Format.get("jpg"), instance.getSourceFormat());

        instance.setSourceFormat(null);
        assertEquals(Format.UNKNOWN, instance.getSourceFormat());
    }

    /* hashCode() */

    @Test
    void hashCodeWithEqualInstances() {
        MutableMetadata metadata2 = new MutableMetadata();
        metadata2.setXMP("<cats/>");

        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(metadata2)
                .build();
        assertEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void hashCodeWithDifferentSerializationTimestamps() {
        MutableMetadata metadata2 = new MutableMetadata();
        metadata2.setXMP("<cats/>");

        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(metadata2)
                .build();
        info2.setSerializationTimestamp(Instant.now());
        assertEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void hashCodeWithDifferentIdentifiers() {
        Info info2 = Info.builder()
                .withIdentifier(new Identifier("cows"))
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void hashCodeWithDifferentWidths() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(999, instance.getSize().intHeight())
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void hashCodeWithDifferentHeights() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize().intWidth(), 999)
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void hashCodeWithDifferentTileWidths() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(999, instance.getImages().getFirst().getTileSize().intHeight())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void hashCodeWithDifferentTileHeights() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().getFirst().getTileSize().intWidth(), 999)
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void hashCodeWithDifferentMetadatas() {
        MutableMetadata metadata2 = new MutableMetadata();
        metadata2.setXMP("<dogs/>");

        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(metadata2)
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void hashCodeWithDifferentNumResolutions() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions() + 1)
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void hashCodeWithDifferentFormats() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().getFirst().getTileSize())
                .withFormat(Format.get("gif"))
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    /* isPyramid() */

    @Test
    void isPyramidWithSingleResolutionImage() {
        instance = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withFormat(Format.get("jpg"))
                .withNumResolutions(1)
                .build();
        assertFalse(instance.isPyramid());
    }

    @Test
    void isPyramidWithPyramidalImage() {
        instance = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(1000, 800)
                .withFormat(Format.get("jpg"))
                .withNumResolutions(6)
                .build();
        // level 2
        Info.Image image = new Info.Image();
        image.setSize(new Size(500, 400));
        instance.getImages().add(image);
        // level 3
        image = new Info.Image();
        image.setSize(new Size(250, 200));
        instance.getImages().add(image);
        // level 4
        image = new Info.Image();
        image.setSize(new Size(125, 100));
        instance.getImages().add(image);
        // level 5
        image = new Info.Image();
        image.setSize(new Size(63, 50));
        instance.getImages().add(image);
        // level 6
        image = new Info.Image();
        image.setSize(new Size(32, 25));
        instance.getImages().add(image);

        assertTrue(instance.isPyramid());
    }

    @Test
    void isPyramidWithNonPyramidalMultiImageImage() {
        instance = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(1000, 800)
                .withFormat(Format.get("jpg"))
                .withNumResolutions(1)
                .build();
        Info.Image image = new Info.Image();
        image.setSize(new Size(600, 300));
        instance.getImages().add(image);
        // level 3
        image = new Info.Image();
        image.setSize(new Size(200, 900));
        instance.getImages().add(image);

        assertFalse(instance.isPyramid());
    }

    /* setApplicationVersion() */

    @Test
    void setApplicationVersion() {
        SoftwareVersion version = new SoftwareVersion(7, 5, 12);
        instance.setApplicationVersion(version);
        assertEquals(version, instance.getApplicationVersion());
    }

    /* setIdentifier() */

    @Test
    void setIdentifier() {
        Identifier identifier = new Identifier("Some Identifier");
        instance.setIdentifier(identifier);
        assertEquals(identifier, instance.getIdentifier());
    }

    /* setMediaType() */

    @Test
    void setMediaType() {
        MediaType type = new MediaType("image", "jpg");
        instance.setMediaType(type);
        assertEquals(type, instance.getMediaType());
    }

    /* setNumResolutions() */

    @Test
    void setNumResolutions() {
        instance.setNumResolutions(7);
        assertEquals(7, instance.getNumResolutions());
    }

    /* setSerializationTimestamp() */

    @Test
    void setSerializationTimestamp() {
        Instant timestamp = Instant.now();
        instance.setSerializationTimestamp(timestamp);
        assertEquals(timestamp, instance.getSerializationTimestamp());
    }

    /* setSerializationVersion() */

    @Test
    void setSerializationVersionWithIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setSerializationVersion(99));
    }

    @Test
    void setSerializationVersion() {
        instance.setSerializationVersion(2);
        assertEquals(Info.Serialization.VERSION_2, instance.getSerialization());
    }

    /* setSourceFormat() */

    @Test
    void setSourceFormat() {
        Format format = Format.get("png");
        instance.setSourceFormat(format);
        assertEquals(format, instance.getSourceFormat());
    }

    /* toJSON() */

    @Test
    void toJSONContents() throws Exception {
        assertEquals("{" +
                        "\"applicationVersion\":\"" + Application.getVersion() + "\"," +
                        "\"serializationVersion\":" + Info.Serialization.CURRENT.getVersion() + "," +
                        "\"serializationTimestamp\":\"0000-00-00T00:00:00.000000Z\"," +
                        "\"identifier\":\"cats\"," +
                        "\"mediaType\":\"image/jpeg\"," +
                        "\"numResolutions\":3," +
                        "\"images\":[" +
                            "{" +
                                "\"width\":100," +
                                "\"height\":80," +
                                "\"tileWidth\":50," +
                                "\"tileHeight\":40," +
                                "\"metadata\":{" +
                                    "\"xmp\":\"<cats/>\"," +
                                    "\"iptc\":[]" +
                                "}" +
                            "}" +
                        "]" +
                        "}",
                obscureTimestamps(instance.toJSON()));
    }

    @Test
    void toJSONRoundTrip() throws Exception {
        String json = instance.toJSON();
        Info info2 = Info.fromJSON(json);
        assertEquals(instance, info2);
    }

    @Test
    void toJSONOmitsNullValues() throws Exception {
        String json = instance.toJSON();
        assertFalse(json.contains("null"));
    }

    /* toString() */

    @Test
    void testToString() throws Exception {
        assertEquals(obscureTimestamps(instance.toJSON()),
                obscureTimestamps(instance.toString()));
    }

    /* writeAsJSON() */

    @Test
    void writeAsJSON() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        instance.writeAsJSON(baos);

        String expected = baos.toString(StandardCharsets.UTF_8);
        String actual   = new String(instance.toJSON().getBytes(),
                StandardCharsets.UTF_8);
        assertEquals(obscureTimestamps(expected), obscureTimestamps(actual));
    }

    /**
     * Converts any ISO-8601 timestamps in the given string to
     * {@literal 0000-00-00T00:00:00.000000Z}.
     */
    private static String obscureTimestamps(String inString) {
        return inString.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d+Z",
                "0000-00-00T00:00:00.000000Z");
    }

}
