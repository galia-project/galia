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

package is.galia.resource.health;

import is.galia.Application;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Client;
import is.galia.http.ClientFactory;
import is.galia.http.Headers;
import is.galia.http.Reference;
import is.galia.http.HTTPException;
import is.galia.http.Response;
import is.galia.http.Status;
import is.galia.resource.ResourceITCase;
import is.galia.resource.iiif.v2.IIIF2Resource;
import is.galia.status.Health;
import is.galia.status.HealthChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HealthResourceITCase extends ResourceITCase {

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        HealthChecker.getSourceUsages().clear();
        HealthChecker.overrideHealth(null);
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEALTH_ENDPOINT_ENABLED, true);
        client = newClient(HealthResource.URI_PATH);
    }

    @Test
    void doGETWithEndpointDisabled() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.HEALTH_ENDPOINT_ENABLED, false);

        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(Status.NOT_FOUND, e.getStatus());
        }
    }

    /**
     * The processing pipeline isn't exercised until an image has been
     * successfully returned from an image endpoint.
     */
    @Test
    void doGETWithNoPriorImageRequest() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.API_ENABLED, true);

        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());
        }
    }

    @Test
    void doGETWithPriorImageRequest() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.API_ENABLED, true);

        // Request an image
        String identifier = "sample-images%2Fjpg%2Frgb-64x56x8-baseline.jpg";
        Reference uri     = new Reference("http://localhost:" +
                appServer.getHTTPPort() + IIIF2Resource.getURIPath() + "/" +
                identifier + "/full/max/5/default.jpg");
        Client imageClient = ClientFactory.newClient();
        imageClient.setURI(uri);

        try (Response response = imageClient.send()) {
        } finally {
            if (imageClient != null) {
                imageClient.stop();
            }
        }
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());
            assertTrue(response.getBodyAsString().contains("\"color\":\"GREEN\""));
        }
    }

    @Test
    void doGETWithYellowStatus() throws Exception {
        var config = Configuration.forApplication();
        config.setProperty(Key.HEALTH_DEPENDENCY_CHECK, true);
        config.setProperty(Key.API_ENABLED, true);

        Health health = new Health();
        health.setMinColor(Health.Color.YELLOW);
        HealthChecker.overrideHealth(health);

        try (Response response = client.send()) {
            fail("Expected HTTP 500");
        } catch (HTTPException e) {
            assertEquals(500, e.getStatus().code());
        }
    }

    @Test
    void doGETWithRedStatus() throws Exception {
        var config = Configuration.forApplication();
        config.setProperty(Key.HEALTH_DEPENDENCY_CHECK, true);
        config.setProperty(Key.API_ENABLED, true);

        Health health = new Health();
        health.setMinColor(Health.Color.RED);
        HealthChecker.overrideHealth(health);

        try (Response response = client.send()) {
            fail("Expected HTTP 500");
        } catch (HTTPException e) {
            assertEquals(500, e.getStatus().code());
        }
    }

    @Test
    void doGETResponseBody() throws Exception {
        try (Response response = client.send()) {
            assertTrue(response.getBodyAsString().contains("\"color\":"));
        }
    }

    @Test
    void doGETResponseHeaders() throws Exception {
        try (Response response = client.send()) {
            Headers headers = response.getHeaders();
            assertEquals(5, headers.size());
            // Cache-Control
            assertEquals("no-cache", headers.getFirstValue("Cache-Control"));
            // Content-Type
            assertTrue("application/json;charset=UTF-8".equalsIgnoreCase(
                    headers.getFirstValue("Content-Type")));
            // Date
            assertNotNull(headers.getFirstValue("Date"));
            // X-Powered-By
            assertEquals(Application.getName() + "/" + Application.getVersion(),
                    headers.getFirstValue("X-Powered-By"));
        }
    }

    @Test
    void doGETRespectsBaseURIConfigKey() throws Exception {
        String baseURI = "http://example.org:8080/base";
        Configuration.forApplication().setProperty(Key.BASE_URI, baseURI);

        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());
        }
    }

    @Test
    void doGETRespectsXForwardedHeaders() throws Exception {
        client.getHeaders().set("X-Forwarded-Proto", "HTTP");
        client.getHeaders().set("X-Forwarded-Host", "example.org");
        client.getHeaders().set("X-Forwarded-Port", "8080");
        client.getHeaders().add("X-Forwarded-BasePath", "/base");

        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());
        }
    }

}
