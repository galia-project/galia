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

import is.galia.http.Cookie;
import is.galia.http.Headers;
import is.galia.http.Status;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;

class JettyResponseWrapperTest extends BaseTest {

    private JettyResponseWrapper instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        instance = new JettyResponseWrapper(new MockJettyResponse());
    }

    @Test
    void addCookie() {
        instance.addCookie(new Cookie("name", "value"));
        assertEquals("name=value",
                instance.getHeaders().getFirstValue("Set-Cookie"));
    }

    @Test
    void addHeader() {
        instance.addHeader("X-Animal", "Cat");
        assertEquals("Cat", instance.getHeaders().getFirstValue("X-Animal"));
    }

    @Test
    void getHeaders() {
        instance.addHeader("X-Animal", "Cat");
        Headers expected = new Headers();
        expected.add("X-Animal", "Cat");
        assertEquals(expected, instance.getHeaders());
    }

    @Test
    void getStatus() {
        assertEquals(new Status(0), instance.getStatus());
    }

    /* openBodyStream() */

    @Test
    void openBodyStream() throws Exception {
        try (OutputStream is = instance.openBodyStream()) {
            assertNotNull(is);
        }
    }

    @Test
    void openBodyStreamDoesNotOpenMultipleStreams() throws Exception {
        try (OutputStream is = instance.openBodyStream()) {
            assertThrows(IllegalStateException.class,
                    () -> instance.openBodyStream());
        }
    }

    /* setHeader() */

    @Test
    void setHeader() {
        instance.setHeader("X-Animal", "Cat");
        assertEquals("Cat", instance.getHeaders().getFirstValue("X-Animal"));
    }

    /* setStatus(Status) */

    @Test
    void setStatusWithStatus() {
        Status status = Status.NO_CONTENT;
        instance.setStatus(status);
        assertEquals(status, instance.getStatus());
    }

    /* setStatus(int) */

    @Test
    void setStatusWithInt() {
        int status = 204;
        instance.setStatus(status);
        assertEquals(status, instance.getStatus().code());
    }

}
