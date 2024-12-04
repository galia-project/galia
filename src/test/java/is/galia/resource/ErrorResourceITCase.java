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

import is.galia.http.HTTPException;
import is.galia.http.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional test of error responses.
 */
class ErrorResourceITCase extends ResourceITCase {

    @Test
    void doGETContentTypeWithHTMLPreference() throws Exception {
        client = newClient("/bogus");
        client.getHeaders().add("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        try (Response response = client.send()) {
        } catch (HTTPException e) {
            assertTrue(e.getResponse().getBodyAsString().contains("html>"));
            assertTrue("text/html;charset=UTF-8".equalsIgnoreCase(
                    e.getResponse().getHeaders().getFirstValue("Content-Type")));
        }
    }

    @Test
    void doGETContentTypeWithXHTMLPreference() throws Exception {
        client = newClient("/bogus");
        client.getHeaders().add("Accept", "application/xhtml+xml;q=0.9");
        try (Response response = client.send()) {
        } catch (HTTPException e) {
            assertTrue(e.getResponse().getBodyAsString().contains("html>"));
            assertTrue("text/html;charset=UTF-8".equalsIgnoreCase(
                    e.getResponse().getHeaders().getFirstValue("Content-Type")));
        }
    }

    @Test
    void doGETContentTypeWithXMLPreference() throws Exception {
        client = newClient("/bogus");
        client.getHeaders().add("Accept", "application/xml;q=0.9");
        try (Response response = client.send()) {
        } catch (HTTPException e) {
            assertFalse(e.getResponse().getBodyAsString().contains("html>"));
            assertTrue("text/plain;charset=UTF-8".equalsIgnoreCase(
                    e.getResponse().getHeaders().getFirstValue("Content-Type")));
        }
    }

    @Test
    void doGETContentTypeWithJSONPreference() throws Exception {
        client = newClient("/bogus");
        client.getHeaders().add("Accept",
                "application/json,text/plain,application/xml;q=0.9,*/*;q=0.8");
        try (Response response = client.send()) {
        } catch (HTTPException e) {
            assertTrue(e.getResponse().getBodyAsString().startsWith("{\""));
            assertTrue("application/json;charset=UTF-8".equalsIgnoreCase(
                    e.getResponse().getHeaders().getFirstValue("Content-Type")));
        }
    }

    @Test
    void doGETContentTypeWithTextPreference() throws Exception {
        client = newClient("/bogus");
        client.getHeaders().add("Accept", "text/plain");
        try (Response response = client.send()) {
        } catch (HTTPException e) {
            assertFalse(e.getResponse().getBodyAsString().contains("html>"));
            assertTrue("text/plain;charset=UTF-8".equalsIgnoreCase(
                    e.getResponse().getHeaders().getFirstValue("Content-Type")));
        }
    }

    @Test
    void doGETContentTypeWithNoPreference() throws Exception {
        client = newClient("/bogus");
        client.getHeaders().add("Accept", "*/*");
        try (Response response = client.send()) {
        } catch (HTTPException e) {
            assertFalse(e.getResponse().getBodyAsString().contains("html>"));
            assertTrue("text/plain;charset=UTF-8".equalsIgnoreCase(
                    e.getResponse().getHeaders().getFirstValue("Content-Type")));
        }
    }

}
