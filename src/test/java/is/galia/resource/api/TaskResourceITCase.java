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

package is.galia.resource.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import is.galia.Application;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Headers;
import is.galia.http.Method;
import is.galia.http.Reference;
import is.galia.http.HTTPException;
import is.galia.http.Response;
import is.galia.http.Status;
import is.galia.image.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional test of TaskResource.
 */
class TaskResourceITCase extends AbstractAPIResourceITCase {

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        client = newClient(TasksResource.URI_PATH + "/some-uuid", USERNAME, SECRET,
                AbstractAPIResource.BASIC_REALM);
    }

    @Test
    void doGETWithInvalidID() throws Exception {
        client.setMethod(Method.GET);
        try (Response response = client.send()) {
        } catch (HTTPException e) {
            assertEquals(404, e.getStatus().code());
        }
    }

    @Test
    void doGETWithValidID() throws Exception {
        try (Response response = createTask()) {
            assertEquals(Status.OK, response.getStatus());
            String responseBody = response.getBodyAsString();

            assertTrue(responseBody.contains("EvictInvalidFromCache"));
        }
    }

    @Test
    void doGETResponseHeaders() throws Exception {
        try (Response response = createTask()) {
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

        // Create a task
        APITask<?> submittedTask =
                new APITask<>(new EvictInvalidFromCacheCommand<>());
        // Get its JSON representation
        String entityStr = new ObjectMapper().writer().
                writeValueAsString(submittedTask);

        // Submit it to TasksResource
        client.setMethod(Method.POST);
        client.setURI(client.getURI().rebuilder()
                .withPath(TasksResource.URI_PATH).build());
        client.setEntity(entityStr);
        client.getHeaders().set("Content-Type", "application/json");

        try (Response response = client.send()) {
            // Retrieve it by its UUID from TaskResource
            String location = response.getHeaders().getFirstValue("Location");
            assertTrue(location.matches("http://example.org:8080/base" + TasksResource.URI_PATH + "/[a-f0-9-]{36}"));
        }
    }

    @Test
    void doGETRespectsXForwardedHeaders() throws Exception {
        client.getHeaders().set("X-Forwarded-Proto", "HTTP");
        client.getHeaders().set("X-Forwarded-Host", "example.org");
        client.getHeaders().set("X-Forwarded-Port", "8080");
        client.getHeaders().add("X-Forwarded-BasePath", "/base");

        // Create a task
        APITask<?> submittedTask =
                new APITask<>(new EvictInvalidFromCacheCommand<>());
        // Get its JSON representation
        String entityStr = new ObjectMapper().writer().
                writeValueAsString(submittedTask);

        // Submit it to TasksResource
        client.setMethod(Method.POST);
        client.setURI(client.getURI().rebuilder()
                .withPath(TasksResource.URI_PATH).build());
        client.setEntity(entityStr);
        client.getHeaders().set("Content-Type", "application/json");

        try (Response response = client.send()) {
            // Retrieve it by its UUID from TaskResource
            String location = response.getHeaders().getFirstValue("Location");
            assertTrue(location.matches("http://example.org:8080/base" + TasksResource.URI_PATH + "/[a-f0-9-]{36}"));
        }
    }

    private Response createTask() throws Exception {
        // Create a task
        APITask<?> submittedTask =
                new APITask<>(new EvictInvalidFromCacheCommand<>());
        // Get its JSON representation
        String entityStr = new ObjectMapper().writer().
                writeValueAsString(submittedTask);

        // Submit it to TasksResource
        client.setMethod(Method.POST);
        client.setURI(client.getURI().rebuilder()
                .withPath(TasksResource.URI_PATH).build());
        client.setEntity(entityStr);
        client.getHeaders().set("Content-Type", "application/json");

        try (Response response = client.send()) {
            // Retrieve it by its UUID from TaskResource
            String location = response.getHeaders().getFirstValue("Location");
            client.setURI(new Reference(location));
            client.setMethod(Method.GET);

            return client.send();
        }
    }

}
