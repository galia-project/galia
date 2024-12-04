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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class MutableRequestTest extends BaseTest {

    @Nested
    class BuilderTest extends BaseTest {
        @Test
        void build() throws Exception {
            String remoteAddr   = "1.2.3.4";
            Method method       = Method.GET;
            Reference reference = new Reference("http://example.org/path");
            Headers headers     = new Headers();
            headers.set("X-Cats", "Yes");
            instance = MutableRequest.builder()
                    .withRemoteAddr(remoteAddr)
                    .withMethod(method)
                    .withReference(reference)
                    .withHeaders(headers)
                    .build();

            assertEquals(method, instance.getMethod());
            assertEquals(reference, instance.getReference());
            assertEquals(headers, instance.getHeaders());
            try (InputStream is = instance.openBodyStream()) {
                assertNull(is);
            }
            assertEquals(remoteAddr, instance.getRemoteAddr());
        }
    }

    private MutableRequest instance;

    /* builder() */

    @Test
    void builder() {
        assertNotNull(MutableRequest.builder());
    }

    /* getCookies() */

    @Test
    void getCookies() {
        Headers headers = new Headers();
        headers.add("Cookie", "fruit=apples; animal=cats");
        headers.add("Cookie", "shape=cube; car=ford");
        instance = MutableRequest.builder()
                .withHeaders(headers)
                .build();

        Cookies cookies = instance.getCookies();
        assertEquals(4, cookies.size());
        assertEquals("apples", cookies.getFirstValue("fruit"));
        assertEquals("cats", cookies.getFirstValue("animal"));
        assertEquals("cube", cookies.getFirstValue("shape"));
        assertEquals("ford", cookies.getFirstValue("car"));
    }

    /* getHeaders() */

    @Test
    void getHeaders() {
        Headers headers = new Headers();
        headers.add("Cookie", "cats=yes");
        headers.add("Accept", "text/plain");
        instance = MutableRequest.builder()
                .withHeaders(headers)
                .build();

        headers = instance.getHeaders();
        assertEquals(2, headers.size());
        assertEquals("cats=yes", headers.getFirstValue("Cookie"));
        assertEquals("text/plain", headers.getFirstValue("Accept"));
    }

    /* getInputStream() */

    @Test
    void openBodyStream() throws Exception {
        instance = MutableRequest.builder().build();
        try (InputStream is = instance.openBodyStream()) {
            assertNull(is);
        }
    }

    /* getMethod() */

    @Test
    void getMethod() {
        instance = MutableRequest.builder()
                .withMethod(Method.PUT)
                .build();
        assertEquals(Method.PUT, instance.getMethod());
    }

    /* getReference() */

    @Test
    void getReference() {
        Reference reference = new Reference("http://example.org/cats?query=yes");
        instance = MutableRequest.builder()
                .withReference(reference)
                .build();
        assertEquals(reference, instance.getReference());
    }

    /* getRemoteAddr() */

    @Test
    void getRemoteAddr() {
        String addr = "10.2.5.3";
        instance = MutableRequest.builder()
                .withRemoteAddr(addr)
                .build();
        assertEquals(addr, instance.getRemoteAddr());
    }

}
