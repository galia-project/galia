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

package is.galia.codec.xmp;

import is.galia.test.TestUtils;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapReaderTest {

    /* MapReader(String) */

    @Test
    void constructorWithStringWithNullArgument() {
        assertThrows(NullPointerException.class,
                () -> new MapReader((String) null));
    }

    /* MapReader(Model) */

    @Test
    void constructorWithModelWithNullArgument() {
        assertThrows(NullPointerException.class,
                () -> new MapReader((Model) null));
    }

    /* readElements() */

    @Test
    void readElements() throws Exception {
        String xmp                  = Files.readString(TestUtils.getSampleImage("xmp/xmp.xmp"));
        xmp                         = XMPUtils.trimXMP(xmp);
        MapReader reader            = new MapReader(xmp);
        Map<String,Object> elements = reader.readElements();

        //print(elements);

        assertEquals(61, elements.size());
    }

    @Test
    void readElementsWithIPTC() throws Exception {
        String xmp                  = Files.readString(TestUtils.getSampleImage("xmp/iptc.xmp"));
        xmp                         = XMPUtils.trimXMP(xmp);
        MapReader reader            = new MapReader(xmp);
        Map<String,Object> elements = reader.readElements();

        //print(elements);

        assertEquals(18, elements.size());
    }

    private static void print(Map<String,Object> elements) {
        System.out.println("------ ELEMENTS -------");
        for (Map.Entry<String,Object> entry : elements.entrySet()) {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
            System.out.println("-------------");
        }
    }
}