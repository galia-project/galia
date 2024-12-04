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
import is.galia.http.HTTPException;
import is.galia.http.Response;
import is.galia.http.Status;
import is.galia.image.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional test of ConfigurationResource.
 */
class ConfigurationResourceITCase extends AbstractAPIResourceITCase {

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.forApplication();
        config.setProperty(Key.API_ENABLED, true);

        client = newClient(ConfigurationResource.URI_PATH, USERNAME, SECRET,
                AbstractAPIResource.BASIC_REALM);
    }

    /* doGET() */

    @Test
    void doGETWithEndpointEnabled() throws Exception {
        client.setMethod(Method.GET);

        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());
        }
    }

    @Test
    void doGETWithEndpointDisabled() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.API_ENABLED, false);

        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(Status.NOT_FOUND, e.getStatus());
        }
    }

    @Test
    void doGETResponseBody() throws Exception {
        client.setMethod(Method.GET);
        try (Response response = client.send()) {
            assertTrue(response.getBodyAsString().startsWith("{"));
        }
    }

    @Test
    void doGETResponseHeaders() throws Exception {
        client.setMethod(Method.GET);

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

    /* doOPTIONS */

    @Override
    @Test
    void doOPTIONSWithEndpointEnabled() throws Exception {
        client.setMethod(Method.OPTIONS);

        try (Response response = client.send()) {
            assertEquals(Status.NO_CONTENT, response.getStatus());

            Headers headers = response.getHeaders();
            List<String> methods =
                    List.of(StringUtils.split(headers.getFirstValue("Allow"), ", "));
            assertEquals(3, methods.size());
            assertTrue(methods.contains("GET"));
            assertTrue(methods.contains("PUT"));
            assertTrue(methods.contains("OPTIONS"));
        }
    }

    /* doPUT() */

    @Test
    void doPUTWithEndpointEnabled() throws Exception {
        Map<String,Object> entityMap = new HashMap<>();
        entityMap.put("test", "cats");
        String entity = new ObjectMapper().writer().writeValueAsString(entityMap);

        client.setMethod(Method.PUT);
        client.getHeaders().set("Content-Type", "application/json");
        client.setEntity(entity);

        try (Response response = client.send()) {
            assertEquals("cats", Configuration.forApplication().getString("test"));
        }
    }

    @Test
    void doPUTWithEndpointDisabled() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.API_ENABLED, false);

        Map<String,Object> entityMap = new HashMap<>();
        entityMap.put("test", "cats");
        String entity = new ObjectMapper().writer().writeValueAsString(entityMap);

        client.setMethod(Method.PUT);
        client.getHeaders().set("Content-Type", "application/json");
        client.setEntity(entity);

        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(Status.NOT_FOUND, e.getStatus());
        }
    }

}
