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

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Reference;
import is.galia.http.Response;
import is.galia.http.Status;
import is.galia.resource.ResourceITCase;
import is.galia.resource.iiif.InformationResourceTester;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdentifierResourceITCase extends ResourceITCase {

    private static final String DECODED_IDENTIFIER =
            "sample-images/jpg/rgb-64x56x8-baseline.jpg";
    private static final String ENCODED_IDENTIFIER =
            Reference.encode(DECODED_IDENTIFIER);

    private final InformationResourceTester tester =
            new InformationResourceTester();

    @Test
    void doGETRedirectToInfoJSON() {
        Reference fromURI = getHTTPURI(
                IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER);
        Reference toURI   = getHTTPURI(
                IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER + "/info.json");
        tester.testRedirectToInfoJSON(fromURI, toURI);
    }

    @Test
    void doGETRedirectToInfoJSONWithEncodedCharacters() {
        doGETRedirectToInfoJSON();
    }

    @Test
    void doGETRedirectToInfoJSONWithDifferentPublicIdentifier()
            throws Exception {
        Reference uri = getHTTPURI(
                IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER);
        tester.testRedirectToInfoJSONWithDifferentPublicIdentifier(uri);
    }

    @Test
    void doGETRespectsEndpointPathConfigKey() {
        final String uriPrefix = "/cats";
        Configuration.forApplication().setProperty(
                Key.IIIF_2_ENDPOINT_PATH, uriPrefix);
        Reference fromURI = getHTTPURI(
                uriPrefix + "/" + ENCODED_IDENTIFIER);
        Reference toURI   = getHTTPURI(
                uriPrefix + "/" + ENCODED_IDENTIFIER + "/info.json");
        tester.testRedirectToInfoJSON(fromURI, toURI);
    }

    @Test
    void doGETRespectsBaseURIConfigKey() throws Exception {
        String baseURI = "http://example.org:8080/base";
        Configuration.forApplication().setProperty(Key.BASE_URI, baseURI);

        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER);
        try (Response response = client.send()) {
            assertEquals(Status.SEE_OTHER, response.getStatus());
            assertEquals(baseURI + IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER + "/info.json",
                    response.getHeaders().getFirstValue("Location"));
        }
    }

    @Test
    void doGETRespectsXForwardedHeaders() throws Exception {
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER);
        client.getHeaders().set("X-Forwarded-Proto", "HTTP");
        client.getHeaders().set("X-Forwarded-Host", "example.org");
        client.getHeaders().set("X-Forwarded-Port", "8080");
        client.getHeaders().add("X-Forwarded-BasePath", "/base");

        try (Response response = client.send()) {
            assertEquals(Status.SEE_OTHER, response.getStatus());
            assertEquals("http://example.org:8080/base" + IIIF2Resource.getURIPath() +
                            "/" + ENCODED_IDENTIFIER + "/info.json",
                    response.getHeaders().getFirstValue("Location"));
        }
    }

}
