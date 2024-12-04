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

import is.galia.Application;
import is.galia.http.HTTPException;
import is.galia.http.Response;
import is.galia.http.Status;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FileResourceITCase extends ResourceITCase {

    @Test
    void doGETWithPresentResourceWhenDeveloping() throws Exception {
        System.setProperty(Application.ENVIRONMENT_VM_ARGUMENT,
                Application.DEVELOPMENT_ENVIRONMENT);

        client = newClient("/static/styles/public.css");
        try (Response response = client.send()) {
            // Check the status
            assertEquals(Status.OK, response.getStatus());
            // Check the headers
            assertEquals("no-store, no-cache",
                    response.getHeaders().getFirstValue("Cache-Control"));
            assertEquals("text/css",
                    response.getHeaders().getFirstValue("Content-Type"));
            // Check the entity
            byte[] entityBytes = response.getBody();
            String entity = new String(entityBytes, StandardCharsets.UTF_8);
            assertTrue(entity.contains("body {"));
        } finally {
            System.setProperty(Application.ENVIRONMENT_VM_ARGUMENT,
                    Application.TEST_ENVIRONMENT);
        }
    }

    @Test
    void doGETWithPresentResourceWhenNotDeveloping() throws Exception {
        client = newClient("/static/styles/public.css");
        try (Response response = client.send()) {
            // Check the status
            assertEquals(Status.OK, response.getStatus());
            // Check the headers
            assertEquals("public, max-age=2592000",
                    response.getHeaders().getFirstValue("Cache-Control"));
            assertEquals("text/css",
                    response.getHeaders().getFirstValue("Content-Type"));
            // Check the entity
            byte[] entityBytes = response.getBody();
            String entity = new String(entityBytes, StandardCharsets.UTF_8);
            assertTrue(entity.contains("body {"));
        }
    }

    @Test
    void doGETWithMissingResource() throws Exception {
        client = newClient("/bogus");
        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(Status.NOT_FOUND, e.getStatus());
        }
    }

}
