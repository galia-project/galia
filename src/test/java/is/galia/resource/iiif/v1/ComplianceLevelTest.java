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

import is.galia.image.Format;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ComplianceLevelTest extends BaseTest {

    @Test
    void getLevel() {
        Set<Format> outputFormats = new HashSet<>();
        assertEquals(ComplianceLevel.LEVEL_0,
                ComplianceLevel.getLevel(outputFormats));

        // add the set of level 1 features
        outputFormats.add(Format.get("jpg"));
        assertEquals(ComplianceLevel.LEVEL_1,
                ComplianceLevel.getLevel(outputFormats));

        // add the set of level 2 features
        outputFormats.add(Format.get("png"));
        assertEquals(ComplianceLevel.LEVEL_2,
                ComplianceLevel.getLevel(outputFormats));
    }

    @Test
    void getURI() {
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level0",
                ComplianceLevel.LEVEL_0.getURI());
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level1",
                ComplianceLevel.LEVEL_1.getURI());
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2",
                ComplianceLevel.LEVEL_2.getURI());
    }

}
