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
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Headers;
import is.galia.http.Method;
import is.galia.http.Reference;
import is.galia.http.Response;
import is.galia.http.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static is.galia.test.Assert.HTTPAssert.*;
import static org.junit.jupiter.api.Assertions.*;

class LandingResourceITCase extends ResourceITCase {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        client = newClient("");
    }

    /* doGET() */

    @Test
    void doGET() {
        assertStatus(200, getHTTPURI(""));
    }

    @Test
    void doGETResponseBody() {
        assertRepresentationContains("<body", getHTTPURI(""));
    }

    @Test
    void doGETResponseHeaders() throws Exception {
        try (Response response = client.send()) {
            Headers headers = response.getHeaders();
            assertEquals(5, headers.size());
            // Cache-Control
            assertTrue(headers.getFirstValue("Cache-Control").contains("public"));
            assertTrue(headers.getFirstValue("Cache-Control").contains("max-age="));
            // Content-Type
            assertTrue("text/html;charset=UTF-8".equalsIgnoreCase(
                    headers.getFirstValue("Content-Type")));
            // Date
            assertNotNull(headers.getFirstValue("Date"));
            // X-Powered-By
            assertEquals(Application.getName() + "/" + Application.getVersion(),
                    headers.getFirstValue("X-Powered-By"));
        }
    }

    @Test
    void doGETResponseHeadersWithoutCustomerKeySet() throws Exception {
        Configuration.forApplication().clearProperty(Key.CUSTOMER_KEY);
        try (Response response = client.send()) {
            Headers headers = response.getHeaders();
            assertEquals(Application.getName() + "/" + Application.getVersion(),
                    headers.getFirstValue("X-Powered-By"));
        }
    }

    @Test
    void doGETRespectsBaseURIConfigKey() throws Exception {
        Reference baseURI = client.getURI().rebuilder().withPath("/base").build();
        Configuration.forApplication().setProperty(Key.BASE_URI, baseURI.toString());

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

    /* doOPTIONS() */

    @Test
    void doOPTIONS() throws Exception {
        client.setMethod(Method.OPTIONS);
        try (Response response = client.send()) {
            assertEquals(Status.NO_CONTENT, response.getStatus());

            Headers headers = response.getHeaders();
            List<String> methods = List.of(headers.getFirstValue("Allow").split(","));
            assertEquals(2, methods.size());
            assertTrue(methods.contains("GET"));
            assertTrue(methods.contains("OPTIONS"));
        }
    }

}
