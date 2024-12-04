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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PNGNativeMetadataTest extends BaseTest {

    private PNGNativeMetadata instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new PNGNativeMetadata();
    }

    @Test
    void testJSONSerialization() throws Exception {
        instance.put("key1", "value");
        instance.put("key2", "value");
        String actual = new ObjectMapper()
                .registerModule(new Jdk8Module())
                .writer()
                .writeValueAsString(instance);
        assertEquals("{\"key1\":\"value\",\"key2\":\"value\"}", actual);
    }

    /* equals() */

    @Test
    void equalsWithEqualInstances() {
        instance.put("key1", "value");
        instance.put("key2", "value");
        PNGNativeMetadata instance2 = new PNGNativeMetadata();
        instance2.put("key1", "value");
        instance2.put("key2", "value");
        assertEquals(instance, instance2);
    }

    @Test
    void equalsWithUnequalInstances() {
        instance.put("key1", "value");
        instance.put("key2", "value");
        PNGNativeMetadata instance2 = new PNGNativeMetadata();
        instance2.put("key3", "value");
        instance2.put("key6", "value");
        assertNotEquals(instance, instance2);
    }

    /* hashCode() */

    @Test
    void hashCodeWithEqualInstances() {
        instance.put("key1", "value");
        instance.put("key2", "value");
        PNGNativeMetadata instance2 = new PNGNativeMetadata();
        instance2.put("key1", "value");
        instance2.put("key2", "value");
        assertEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    void hashCodeWithUnequalInstances() {
        instance.put("key1", "value");
        instance.put("key2", "value");
        PNGNativeMetadata instance2 = new PNGNativeMetadata();
        instance2.put("key3", "value");
        instance2.put("key6", "value");
        assertNotEquals(instance.hashCode(), instance2.hashCode());
    }

    /* put() */

    @Test
    void put() {
        instance.put("key", "value");
        assertEquals("value", instance.get("key"));
    }

    /* size() */

    @Test
    void size() {
        assertEquals(0, instance.size());

        instance.put("key1", "value");
        instance.put("key2", "value");
        assertEquals(2, instance.size());
    }

    /* toMap() */

    @Test
    void toMap() {
        instance.put("key", "value");
        Map<String,String> map = instance.toMap();
        assertEquals("value", map.get("key"));
    }

    @Test
    void toMapReturnsUnmodifiableInstance() {
        Map<String,String> map = instance.toMap();
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("key", "value"));
    }

    /* toString() */

    @Test
    void testToStringWithEmptyInstance() {
        assertEquals("(empty)", instance.toString());
    }

    @Test
    void testToString() {
        instance.put("key2", "value2");
        assertEquals("key2:value2", instance.toString());

        instance.put("key1", "value1");
        assertEquals("key1:value1;key2:value2", instance.toString());
    }

}
