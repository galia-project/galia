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

package is.galia.codec.gif;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GIFNativeMetadataTest extends BaseTest {

    private GIFNativeMetadata instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new GIFNativeMetadata();
    }

    @Test
    void testJSONSerialization() throws Exception {
        String actual = new ObjectMapper()
                .registerModule(new Jdk8Module())
                .writer()
                .writeValueAsString(instance);
        assertEquals("{\"delayTime\":0,\"loopCount\":0}", actual);
    }

    /* equals() */

    @Test
    void equalsWithEqualInstances() {
        instance.setLoopCount(2);
        instance.setDelayTime(3);
        GIFNativeMetadata instance2 = new GIFNativeMetadata();
        instance2.setLoopCount(2);
        instance2.setDelayTime(3);

        assertEquals(instance, instance2);
    }

    @Test
    void equalsWithUnequalInstances() {
        instance.setLoopCount(2);
        instance.setDelayTime(3);
        GIFNativeMetadata instance2 = new GIFNativeMetadata();
        instance2.setLoopCount(7);
        instance2.setDelayTime(6);

        assertNotEquals(instance, instance2);
    }

    /* hashCode() */

    @Test
    void hashCodeWithEqualInstances() {
        instance.setLoopCount(2);
        instance.setDelayTime(3);
        GIFNativeMetadata instance2 = new GIFNativeMetadata();
        instance2.setLoopCount(2);
        instance2.setDelayTime(3);

        assertEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    void hashCodeWithUnequalInstances() {
        instance.setLoopCount(2);
        instance.setDelayTime(3);
        GIFNativeMetadata instance2 = new GIFNativeMetadata();
        instance2.setLoopCount(5);
        instance2.setDelayTime(2);

        assertNotEquals(instance.hashCode(), instance2.hashCode());
    }

    /* toMap() */

    @Test
    void toMap() {
        instance.setDelayTime(3);
        instance.setLoopCount(5);
        Map<String,Integer> map = instance.toMap();
        assertEquals(3, map.get("delayTime"));
        assertEquals(5, map.get("loopCount"));
    }

    @Test
    void toMapReturnsUnmodifiableInstance() {
        Map<String,Integer> map = instance.toMap();
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("key", 4));
    }

    /* toString() */

    @Test
    void testToString() {
        instance.setDelayTime(3);
        instance.setLoopCount(5);
        assertEquals("delayTime:3;loopCount:5", instance.toString());
    }

}
