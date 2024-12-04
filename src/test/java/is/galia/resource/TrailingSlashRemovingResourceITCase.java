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

package is.galia.resource;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Response;
import is.galia.http.Status;
import is.galia.resource.iiif.v2.IIIF2Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrailingSlashRemovingResourceITCase extends ResourceITCase {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        client = newClient(IIIF2Resource.getURIPath() + "/");
    }

    @Test
    void doGET() throws Exception {
        try (Response response = client.send()) {
            assertEquals(Status.MOVED_PERMANENTLY, response.getStatus());
            assertFalse(response.getHeaders().getFirstValue("Location").endsWith("/"));
            assertTrue(response.getBodyAsString().isEmpty());
        }
    }

    @Test
    void doGETRespectsBaseURIConfigKey() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.BASE_URI, "http://example.org/cats");

        try (Response response = client.send()) {
            assertEquals(Status.MOVED_PERMANENTLY, response.getStatus());
            assertTrue(response.getHeaders().getFirstValue("Location").
                    endsWith("/cats" + IIIF2Resource.getURIPath()));
            assertTrue(response.getBodyAsString().isEmpty());
        }
    }

    @Test
    void doGETRespectsXForwardedHeaders() throws Exception {
        client.getHeaders().set("X-Forwarded-Host", "example.org");
        client.getHeaders().set("X-Forwarded-Proto", "HTTP");
        client.getHeaders().set("X-Forwarded-Port", "80");
        client.getHeaders().set("X-Forwarded-BasePath", "/base");

        try (Response response = client.send()) {
            assertEquals(Status.MOVED_PERMANENTLY, response.getStatus());
            assertEquals("http://example.org/base" + IIIF2Resource.getURIPath(),
                    response.getHeaders().getFirstValue("Location"));
            assertTrue(response.getBodyAsString().isEmpty());
        }
    }

}
