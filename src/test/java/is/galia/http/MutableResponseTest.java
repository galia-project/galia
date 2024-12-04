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

package is.galia.http;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class MutableResponseTest extends BaseTest {

    private MutableResponse instance;

    @Override
    @BeforeEach
    public void setUp() {
        instance = new MutableResponse();
    }

    @Test
    void body() {
        byte[] body = new byte[] { 1, 2, 3 };
        instance.setBody(body);
        assertArrayEquals(body, instance.getBody());
    }

    @Test
    void getBodyAsStream() throws Exception {
        byte[] body = new byte[] { 1, 2, 3 };
        instance.setBody(body);

        try (InputStream is = instance.getBodyAsStream();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            is.transferTo(os);
            assertArrayEquals(body, os.toByteArray());
        }
    }

    @Test
    void getBodyAsString() throws Exception {
        String string = "cats";
        byte[] body = string.getBytes();
        instance.setBody(body);

        String bodyString = instance.getBodyAsString();
        assertEquals(string, bodyString);
    }

    @Test
    void headers() {
        Headers headers = new Headers();
        instance.setHeaders(headers);
        assertSame(headers, instance.getHeaders());
    }

    @Test
    void status() {
        Status status = Status.FORBIDDEN;
        instance.setStatus(status);
        assertEquals(status, instance.getStatus());
    }

    @Test
    void transport() {
        instance.setTransport(Transport.HTTP1_1);
        assertEquals(Transport.HTTP1_1, instance.getTransport());
    }

}
