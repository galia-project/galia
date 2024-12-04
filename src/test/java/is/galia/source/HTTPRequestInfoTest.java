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

package is.galia.source;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HTTPRequestInfoTest extends BaseTest {

    private HTTPRequestInfo instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new HTTPRequestInfo();
        instance.setURI("http://example.org/cats");
        instance.setUsername("user");
        instance.setSecret("secret");
        instance.setHeaders(Map.of("X-Animal", "cats"));
        instance.setSendingHeadRequest(true);
    }

    @Test
    void getBasicAuthTokenWithoutUserAndSecret() {
        instance = new HTTPRequestInfo();
        instance.setURI("http://example.org/cats");
        assertNull(instance.getBasicAuthToken());
    }

    @Test
    void getBasicAuthTokenWithUserAndSecret() {
        assertEquals("dXNlcjpzZWNyZXQ=", instance.getBasicAuthToken());
    }

    @Test
    void setHeaders() {
        assertEquals(1, instance.getHeaders().size());

        instance.setHeaders(Map.of("X-Cats", "yes"));
        assertEquals(2, instance.getHeaders().size());
        assertEquals("yes", instance.getHeaders().getFirstValue("X-Cats"));
    }

    @Test
    void setHeadersWithNullArgument() {
        instance.setHeaders(null);
        assertEquals(0, instance.getHeaders().size());
    }

}
