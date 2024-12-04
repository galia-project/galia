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

package is.galia.operation;

import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.image.MockNativeMetadata;
import is.galia.image.NativeMetadata;
import is.galia.image.ScaleConstraint;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static is.galia.operation.Encode.OPTION_PREFIX;
import static org.junit.jupiter.api.Assertions.*;

class EncodeTest extends BaseTest {

    private Encode instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Encode(Format.get("jpg"));
        assertEquals(8, instance.getMaxComponentSize());
    }

    /* getNativeMetadata() */

    @Test
    void getNativeMetadataWhenNotPresent() {
        assertFalse(instance.getNativeMetadata().isPresent());
    }

    @Test
    void getNativeMetadataWhenPresent() {
        instance.setNativeMetadata(new MockNativeMetadata());
        assertTrue(instance.getNativeMetadata().isPresent());
    }

    /* getOptions() */

    @Test
    void getOptions() {
        instance.setOption(OPTION_PREFIX + "string", "cats");
        assertEquals("cats", instance.getOptions().getString(OPTION_PREFIX + "string"));
    }

    @Test
    void getOptionsReturnsUnmodifiableConfiguration() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.getOptions().setProperty("string", "cats"));
    }

    /* getResultingSize() */

    @Test
    void getResultingSize() {
        Size size = new Size(500, 500);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        assertEquals(size, instance.getResultingSize(size, scaleConstraint));
    }

    /* getNativeMetadata() */

    @Test
    void getXMPWhenNotPresent() {
        assertFalse(instance.getXMP().isPresent());
    }

    @Test
    void getXMPWhenPresent() {
        instance.setXMP("<rdf:RDF></rdf:RDF>");
        assertTrue(instance.getXMP().isPresent());
    }

    /* hasEffect() */

    @Test
    void hasEffect() {
        assertTrue(instance.hasEffect());
    }

    @Test
    void hasEffectWithArguments() {
        Size size = new Size(500, 500);
        OperationList opList = new OperationList();
        assertTrue(instance.hasEffect(size, opList));
    }

    /* removeOption() */

    @Test
    void removeOptionWithValidKey() {
        String key = OPTION_PREFIX + "key";
        instance.setOption(key, "value");
        instance.removeOption(key);
        assertNull(instance.getOptions().getProperty(key));
    }

    @Test
    void removeOptionWithInvalidKey() {
        String key = OPTION_PREFIX + "key";
        instance.removeOption(key);
        assertNull(instance.getOptions().getProperty(key));
    }

    @Test
    void removeOptionWhenFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.removeOption("whatever"));
    }

    /* setBackgroundColor() */

    @Test
    void setBackgroundColorWhenFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setBackgroundColor(Color.RED));
    }

    /* setFormat() */

    @Test
    void setFormatWhenFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setFormat(Format.get("png")));
    }

    /* setMaxComponentSize() */

    @Test
    void setMaxComponentSizeWithZeroArgument() {
        instance.setMaxComponentSize(0);
        assertEquals(Integer.MAX_VALUE, instance.getMaxComponentSize());
    }

    @Test
    void setMaxComponentSizeWhenFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setMaxComponentSize(8));
    }

    /* setNativeMetadata() */

    @Test
    void setNativeMetadataWhenFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setNativeMetadata(new MockNativeMetadata()));
    }

    /* setOption() */

    @Test
    void setOption() {
        instance.setOption(OPTION_PREFIX + "string", "cats");
        instance.setOption(OPTION_PREFIX + "int", 3);
        assertEquals("cats", instance.getOptions().getString(OPTION_PREFIX + "string"));
        assertEquals(3, instance.getOptions().getInt(OPTION_PREFIX + "int"));
    }

    @Test
    void setOptionWithIllegalKey() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setOption("illegal", "cats"));
    }

    @Test
    void setOptionWhenFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setOption("string", "cats"));
    }

    /* setXMP() */

    @Test
    void setXMPWhenFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setXMP("<rdf:RDF></rdf:RDF>"));
    }

    @Test
    void setXMPWithValidArgument() {
        String xmp = "<rdf:RDF></rdf:RDF>";
        instance.setXMP(xmp);
        assertEquals(xmp, instance.getXMP().orElseThrow());
    }

    @Test
    void setXMPWithInvalidArgument() {
        String xmp = "invalid XMP";
        assertThrows(IllegalArgumentException.class,
                () -> instance.setXMP(xmp));
    }

    /* toMap() */

    @Test
    void toMap() {
        instance.setBackgroundColor(Color.BLUE);
        instance.setMaxComponentSize(10);
        NativeMetadata metadata = new MockNativeMetadata();
        instance.setXMP("<rdf:RDF></rdf:RDF>");
        instance.setNativeMetadata(metadata);
        instance.setOption(OPTION_PREFIX + "key1", "value");
        instance.setOption(OPTION_PREFIX + "key2", "value");

        Size size = new Size(500, 500);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        final Map<String,Object> map = instance.toMap(size, scaleConstraint);
        assertEquals("Encode", map.get("class"));
        assertEquals("#0000FF", map.get("background_color"));
        assertEquals(Format.get("jpg").getPreferredMediaType(), map.get("format"));
        assertEquals(10, map.get("max_sample_size"));
        assertEquals("<rdf:RDF></rdf:RDF>", map.get("xmp"));
        assertEquals(Map.of("number", 0), map.get("native_metadata"));
        assertEquals(Map.of(OPTION_PREFIX + "key1", "value",
                        OPTION_PREFIX + "key2", "value"),
                map.get("options"));
    }

    @Test
    void toMapReturnsUnmodifiableMap() {
        Size fullSize = new Size(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String, Object> map = instance.toMap(fullSize, scaleConstraint);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    /* toString() */

    @Test
    void testToString() {
        instance.setBackgroundColor(Color.BLUE);
        instance.setMaxComponentSize(10);
        instance.setOption(OPTION_PREFIX + "key1", "value");
        instance.setOption(OPTION_PREFIX + "key2", "value");
        NativeMetadata metadata = new MockNativeMetadata();
        instance.setXMP("<rdf:RDF></rdf:RDF>");
        instance.setNativeMetadata(metadata);

        assertTrue(instance.toString().matches("^jpg_#0000FF_10_.*"));
        assertTrue(instance.toString().contains(OPTION_PREFIX + "key1:value;" +
                OPTION_PREFIX + "key2:value"));
    }

}
