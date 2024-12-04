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

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Headers;
import is.galia.http.Method;
import is.galia.http.HTTPException;
import is.galia.http.Response;
import is.galia.http.Status;
import is.galia.resource.ResourceITCase;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractAPIResourceITCase extends ResourceITCase {

    static final String USERNAME = "admin";
    static final String SECRET   = "secret";

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.API_ENABLED, true);
        config.setProperty(Key.API_USERNAME, USERNAME);
        config.setProperty(Key.API_SECRET, SECRET);
    }

    @Test
    void doGETWithNoCredentials() throws Exception {
        client.setUsername(null);
        client.setSecret(null);
        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(401, e.getStatus().code());
        }
    }

    @Test
    void doGETWithInvalidCredentials() throws Exception {
        client.setUsername("invalid");
        client.setSecret("invalid");
        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(401, e.getStatus().code());
        }
    }

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
            assertTrue(methods.contains("GET"));
            assertTrue(methods.contains("OPTIONS"));
        }
    }

    @Test
    void doOPTIONSWithEndpointDisabled() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.API_ENABLED, false);

        client.setMethod(Method.OPTIONS);

        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(Status.NOT_FOUND, e.getStatus());
        }
    }

}
