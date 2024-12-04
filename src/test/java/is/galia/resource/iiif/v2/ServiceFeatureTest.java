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

class ServiceFeatureTest extends BaseTest {

    @Test
    void getName() {
        assertEquals("baseUriRedirect", ServiceFeature.BASE_URI_REDIRECT.getName());
        assertEquals("canonicalLinkHeader", ServiceFeature.CANONICAL_LINK_HEADER.getName());
        assertEquals("cors", ServiceFeature.CORS.getName());
        assertEquals("jsonldMediaType", ServiceFeature.JSON_LD_MEDIA_TYPE.getName());
        assertEquals("profileLinkHeader", ServiceFeature.PROFILE_LINK_HEADER.getName());
        assertEquals("sizeByWhListed", ServiceFeature.SIZE_BY_WHITELISTED.getName());
    }

    @Test
    void testToString() {
        assertEquals("baseUriRedirect", ServiceFeature.BASE_URI_REDIRECT.toString());
        assertEquals("canonicalLinkHeader", ServiceFeature.CANONICAL_LINK_HEADER.toString());
        assertEquals("cors", ServiceFeature.CORS.toString());
        assertEquals("jsonldMediaType", ServiceFeature.JSON_LD_MEDIA_TYPE.toString());
        assertEquals("profileLinkHeader", ServiceFeature.PROFILE_LINK_HEADER.toString());
        assertEquals("sizeByWhListed", ServiceFeature.SIZE_BY_WHITELISTED.toString());
    }

}
