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

package is.galia.resource.iiif.v2;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessorFeatureTest extends BaseTest {

    @Test
    void values() {
        assertNotNull(ProcessorFeature.valueOf("MIRRORING"));
        assertNotNull(ProcessorFeature.valueOf("REGION_BY_PERCENT"));
        assertNotNull(ProcessorFeature.valueOf("REGION_BY_PIXELS"));
        assertNotNull(ProcessorFeature.valueOf("REGION_SQUARE"));
        assertNotNull(ProcessorFeature.valueOf("ROTATION_ARBITRARY"));
        assertNotNull(ProcessorFeature.valueOf("ROTATION_BY_90S"));
        assertNotNull(ProcessorFeature.valueOf("SIZE_ABOVE_FULL"));
        assertNotNull(ProcessorFeature.valueOf("SIZE_BY_DISTORTED_WIDTH_HEIGHT"));
        assertNotNull(ProcessorFeature.valueOf("SIZE_BY_FORCED_WIDTH_HEIGHT"));
        assertNotNull(ProcessorFeature.valueOf("SIZE_BY_HEIGHT"));
        assertNotNull(ProcessorFeature.valueOf("SIZE_BY_PERCENT"));
        assertNotNull(ProcessorFeature.valueOf("SIZE_BY_WIDTH"));
        assertNotNull(ProcessorFeature.valueOf("SIZE_BY_WIDTH_HEIGHT"));
    }

    @Test
    void getName() {
        assertEquals("mirroring", ProcessorFeature.MIRRORING.getName());
        assertEquals("regionByPct", ProcessorFeature.REGION_BY_PERCENT.getName());
        assertEquals("regionByPx", ProcessorFeature.REGION_BY_PIXELS.getName());
        assertEquals("regionSquare", ProcessorFeature.REGION_SQUARE.getName());
        assertEquals("rotationArbitrary", ProcessorFeature.ROTATION_ARBITRARY.getName());
        assertEquals("rotationBy90s", ProcessorFeature.ROTATION_BY_90S.getName());
        assertEquals("sizeAboveFull", ProcessorFeature.SIZE_ABOVE_FULL.getName());
        assertEquals("sizeByDistortedWh", ProcessorFeature.SIZE_BY_DISTORTED_WIDTH_HEIGHT.getName());
        assertEquals("sizeByForcedWh", ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT.getName());
        assertEquals("sizeByH", ProcessorFeature.SIZE_BY_HEIGHT.getName());
        assertEquals("sizeByPct", ProcessorFeature.SIZE_BY_PERCENT.getName());
        assertEquals("sizeByW", ProcessorFeature.SIZE_BY_WIDTH.getName());
        assertEquals("sizeByWh", ProcessorFeature.SIZE_BY_WIDTH_HEIGHT.getName());
    }

    @Test
    void testToString() {
        assertEquals("mirroring", ProcessorFeature.MIRRORING.toString());
        assertEquals("regionByPct", ProcessorFeature.REGION_BY_PERCENT.toString());
        assertEquals("regionByPx", ProcessorFeature.REGION_BY_PIXELS.toString());
        assertEquals("regionSquare", ProcessorFeature.REGION_SQUARE.toString());
        assertEquals("rotationArbitrary", ProcessorFeature.ROTATION_ARBITRARY.toString());
        assertEquals("rotationBy90s", ProcessorFeature.ROTATION_BY_90S.toString());
        assertEquals("sizeAboveFull", ProcessorFeature.SIZE_ABOVE_FULL.toString());
        assertEquals("sizeByDistortedWh", ProcessorFeature.SIZE_BY_DISTORTED_WIDTH_HEIGHT.toString());
        assertEquals("sizeByForcedWh", ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT.toString());
        assertEquals("sizeByH", ProcessorFeature.SIZE_BY_HEIGHT.toString());
        assertEquals("sizeByPct", ProcessorFeature.SIZE_BY_PERCENT.toString());
        assertEquals("sizeByW", ProcessorFeature.SIZE_BY_WIDTH.toString());
        assertEquals("sizeByWh", ProcessorFeature.SIZE_BY_WIDTH_HEIGHT.toString());
    }

}
