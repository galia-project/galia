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

/**
 * Encapsulates an IIIF "feature" that is application-dependent.
 *
 * @see ProcessorFeature
 */
enum ServiceFeature implements Feature {

    /**
     * The base URI of the service will redirect to the image information
     * document.
     */
    BASE_URI_REDIRECT("baseUriRedirect"),

    /**
     * The canonical image URI HTTP link header is provided on image responses.
     */
    CANONICAL_LINK_HEADER("canonicalLinkHeader"),

    /**
     * The CORS HTTP header is provided on all responses.
     */
    CORS("cors"),

    /**
     * The JSON-LD media type is provided when JSON-LD is requested.
     */
    JSON_LD_MEDIA_TYPE("jsonldMediaType"),

    /**
     * The profile HTTP link header is provided on image responses.
     */
    PROFILE_LINK_HEADER("profileLinkHeader"),

    /**
     * Deprecated in Image API 2.1.
     */
    SIZE_BY_WHITELISTED("sizeByWhListed");

    private String name;

    ServiceFeature(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    /**
     * @return The name.
     */
    public String toString() {
        return this.getName();
    }

}
