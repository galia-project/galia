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

package is.galia.resource.iiif.v3;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeatureTest extends BaseTest {

    @Test
    void getName() {
        assertEquals("baseUriRedirect", Feature.BASE_URI_REDIRECT.getName());
        assertEquals("canonicalLinkHeader", Feature.CANONICAL_LINK_HEADER.getName());
        assertEquals("cors", Feature.CORS.getName());
        assertEquals("jsonldMediaType", Feature.JSON_LD_MEDIA_TYPE.getName());
        assertEquals("mirroring", Feature.MIRRORING.getName());
        assertEquals("profileLinkHeader", Feature.PROFILE_LINK_HEADER.getName());
        assertEquals("regionByPct", Feature.REGION_BY_PERCENT.getName());
        assertEquals("regionByPx", Feature.REGION_BY_PIXELS.getName());
        assertEquals("regionSquare", Feature.REGION_SQUARE.getName());
        assertEquals("rotationArbitrary", Feature.ROTATION_ARBITRARY.getName());
        assertEquals("rotationBy90s", Feature.ROTATION_BY_90S.getName());
        assertEquals("sizeByConfinedWh", Feature.SIZE_BY_CONFINED_WIDTH_HEIGHT.getName());
        assertEquals("sizeByH", Feature.SIZE_BY_HEIGHT.getName());
        assertEquals("sizeByPct", Feature.SIZE_BY_PERCENT.getName());
        assertEquals("sizeByW", Feature.SIZE_BY_WIDTH.getName());
        assertEquals("sizeByWh", Feature.SIZE_BY_WIDTH_HEIGHT.getName());
        assertEquals("sizeUpscaling", Feature.SIZE_UPSCALING.getName());
    }

    @Test
    void testToString() {
        assertEquals("baseUriRedirect", Feature.BASE_URI_REDIRECT.toString());
        assertEquals("canonicalLinkHeader", Feature.CANONICAL_LINK_HEADER.toString());
        assertEquals("cors", Feature.CORS.toString());
        assertEquals("jsonldMediaType", Feature.JSON_LD_MEDIA_TYPE.toString());
        assertEquals("mirroring", Feature.MIRRORING.toString());
        assertEquals("profileLinkHeader", Feature.PROFILE_LINK_HEADER.toString());
        assertEquals("regionByPct", Feature.REGION_BY_PERCENT.toString());
        assertEquals("regionByPx", Feature.REGION_BY_PIXELS.toString());
        assertEquals("regionSquare", Feature.REGION_SQUARE.toString());
        assertEquals("rotationArbitrary", Feature.ROTATION_ARBITRARY.toString());
        assertEquals("rotationBy90s", Feature.ROTATION_BY_90S.toString());
        assertEquals("sizeByConfinedWh", Feature.SIZE_BY_CONFINED_WIDTH_HEIGHT.toString());
        assertEquals("sizeByH", Feature.SIZE_BY_HEIGHT.toString());
        assertEquals("sizeByPct", Feature.SIZE_BY_PERCENT.toString());
        assertEquals("sizeByW", Feature.SIZE_BY_WIDTH.toString());
        assertEquals("sizeByWh", Feature.SIZE_BY_WIDTH_HEIGHT.toString());
        assertEquals("sizeUpscaling", Feature.SIZE_UPSCALING.toString());
    }

}
