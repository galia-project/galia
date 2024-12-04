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

import is.galia.http.Cookies;
import is.galia.http.Headers;
import is.galia.http.Method;
import is.galia.http.Reference;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JettyRequestWrapperTest extends BaseTest {

    private JettyRequestWrapper instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        MockJettyRequest jettyRequest = new MockJettyRequest();
        jettyRequest.setContextPath("/context");
        jettyRequest.setHost("1.2.3.4");
        jettyRequest.getMutableHeaders().set("Cookie", "name=value");
        jettyRequest.getMutableHeaders().set("X-Animal", "Cat");
        instance = new JettyRequestWrapper(jettyRequest);
    }

    @Test
    void getCookies() {
        Cookies expected = new Cookies();
        expected.add("name", "value");

        Cookies actual = instance.getCookies();
        assertEquals(expected, actual);
    }

    @Test
    void getHeaders() {
        Headers expected = new Headers();
        expected.add("Cookie", "name=value");
        expected.add("X-Animal", "Cat");

        Headers actual = instance.getHeaders();
        assertEquals(expected, actual);
    }

    @Test
    void getMethod() {
        assertEquals(Method.GET, instance.getMethod());
    }

    @Test
    void getPathArguments() {
        List<String> pathArgs = List.of("cats", "dogs");
        instance.setPathArguments(pathArgs);
        assertEquals(pathArgs, instance.getPathArguments());
    }

    @Test
    void getReference() {
        assertEquals(new Reference("http://example.org"),
                instance.getReference());
    }

    @Test
    void getRemoteAddr() {
        assertEquals("1.2.3.4", instance.getRemoteAddr());
    }

    @Test
    void openBodyStream() throws Exception {
        try (InputStream is = instance.openBodyStream()) {
            assertNotNull(is);
        }
    }

}
