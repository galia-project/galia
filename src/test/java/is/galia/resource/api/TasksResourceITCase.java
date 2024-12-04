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

import is.galia.Application;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Headers;
import is.galia.http.Method;
import is.galia.http.HTTPException;
import is.galia.http.Reference;
import is.galia.http.Response;
import is.galia.http.Status;
import is.galia.image.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional test of TasksResource.
 */
class TasksResourceITCase extends AbstractAPIResourceITCase {

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        client = newClient(TasksResource.URI_PATH, USERNAME, SECRET,
                AbstractAPIResource.BASIC_REALM);
        client.setMethod(Method.POST);
    }

    /* doOPTIONS() */

    @Override
    @Test
    void doOPTIONSWithEndpointEnabled() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.API_ENABLED, true);

        client.setMethod(Method.OPTIONS);

        try (Response response = client.send()) {
            assertEquals(Status.NO_CONTENT, response.getStatus());

            Headers headers = response.getHeaders();
            List<String> methods =
                    List.of(StringUtils.split(headers.getFirstValue("Allow"), ", "));
            assertEquals(2, methods.size());
            assertTrue(methods.contains("POST"));
            assertTrue(methods.contains("OPTIONS"));
        }
    }

    /* doPOST() */

    @Test
    void doPOSTWithIncorrectContentType() throws Exception {
        client.setEntity("{ \"verb\": \"PurgeCache\" }");
        client.getHeaders().set("Content-Type", "text/plain");

        try (Response response = client.send()) {
        } catch (HTTPException e) {
            assertEquals(Status.UNSUPPORTED_MEDIA_TYPE, e.getStatus());
        }
    }

    @Test
    void doPOSTWithEmptyRequestBody() throws Exception {
        client.getHeaders().set("Content-Type", "application/json");

        try (Response response = client.send()) {
        } catch (HTTPException e) {
            assertEquals(Status.BAD_REQUEST, e.getStatus());
        }
    }

    @Test
    void doPOSTWithMalformedRequestBody() throws Exception {
        client.setEntity("{ this is: invalid\" }");
        client.getHeaders().set("Content-Type", "application/json");

        try (Response response = client.send()) {
        } catch (HTTPException e) {
            assertEquals(Status.BAD_REQUEST, e.getStatus());
        }
    }

    @Test
    void doPOSTWithMissingVerb() throws Exception {
        client.setEntity("{ \"cats\": \"yes\" }");
        client.getHeaders().set("Content-Type", "application/json");
        try (Response response = client.send()) {
        } catch (HTTPException e) {
            assertEquals(Status.BAD_REQUEST, e.getStatus());
        }
    }

    @Test
    void doPOSTWithUnsupportedVerb() throws Exception {
        client.setEntity("{ \"verb\": \"dogs\" }");
        client.getHeaders().set("Content-Type", "application/json");

        try (Response response = client.send()) {
        } catch (HTTPException e) {
            assertEquals(Status.BAD_REQUEST, e.getStatus());
        }
    }

    @Test
    void doPOSTWithPurgeInvalidFromCacheVerb() throws Exception {
        client.setEntity("{ \"verb\": \"EvictInvalidFromCache\" }");
        client.getHeaders().set("Content-Type", "application/json");

        try (Response response = client.send()) {
            assertEquals(Status.ACCEPTED, response.getStatus());
            assertNotNull(response.getHeaders().getFirstValue("Location"));
        }
    }

    @Test
    void doPOSTWithEvictItemFromCacheVerb() throws Exception {
        client.setEntity("{ \"verb\": \"EvictItemFromCache\", \"identifier\": \"cats\" }");
        client.getHeaders().set("Content-Type", "application/json");

        try (Response response = client.send()) {
            assertEquals(Status.ACCEPTED, response.getStatus());
            assertNotNull(response.getHeaders().getFirstValue("Location"));
        }
    }

    @Test
    void doPOSTResponseHeaders() throws Exception {
        client.setEntity("{ \"verb\": \"EvictInfosFromCache\" }");
        client.getHeaders().set("Content-Type", "application/json");

        try (Response response = client.send()) {
            Headers headers = response.getHeaders();
            assertEquals(5, headers.size());
            // Cache-Control
            assertEquals("no-cache", headers.getFirstValue("Cache-Control"));
            // Content-Length
            assertNotNull(headers.getFirstValue("Content-Length"));
            // Date
            assertNotNull(headers.getFirstValue("Date"));
            // Location
            assertNotNull(headers.getFirstValue("Location"));
            // X-Powered-By
            assertEquals(Application.getName() + "/" + Application.getVersion(),
                    headers.getFirstValue("X-Powered-By"));
        }
    }

    @Test
    void doPOSTRespectsBaseURIConfigKey() throws Exception {
        String baseURI = "http://example.org:8080/base";
        Configuration.forApplication().setProperty(Key.BASE_URI, baseURI);
        client.setMethod(Method.POST);

        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(Status.BAD_REQUEST, e.getStatus());
        }
    }

    @Test
    void doPOSTRespectsXForwardedHeaders() throws Exception {
        client.setMethod(Method.POST);
        client.getHeaders().set("X-Forwarded-Proto", "HTTP");
        client.getHeaders().set("X-Forwarded-Host", "example.org");
        client.getHeaders().set("X-Forwarded-Port", "8080");
        client.getHeaders().add("X-Forwarded-BasePath", "/base");

        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(Status.BAD_REQUEST, e.getStatus());
        }
    }

}
