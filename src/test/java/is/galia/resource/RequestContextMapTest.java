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

import is.galia.http.Reference;
import is.galia.image.MutableMetadata;
import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.image.ScaleConstraint;
import is.galia.operation.Encode;
import is.galia.operation.OperationList;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static is.galia.resource.RequestContextMap.*;
import static org.junit.jupiter.api.Assertions.*;

class RequestContextMapTest extends BaseTest {

    private RequestContextMap<String,Object> instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        final RequestContext context = new RequestContext();
        // client IP
        context.setClientIP("1.2.3.4");
        // cookies
        Map<String,String> cookies = Map.of("cookie", "yes");
        context.setCookies(cookies);
        // full size
        Size fullSize = new Size(200, 200);
        context.setFullSize(fullSize);
        // identifier
        Identifier identifier = new Identifier("cats");
        context.setIdentifier(identifier);
        // metadata
        context.setMetadata(new MutableMetadata());
        // operation list
        Format outputFormat   = Format.get("gif");
        int pageNumber        = 3;
        OperationList opList  = OperationList.builder()
                .withIdentifier(identifier)
                .withOperations(new Encode(outputFormat))
                .build();
        context.setOperationList(opList);
        // output format
        context.setOutputFormat(outputFormat);
        // page count
        context.setPageCount(3);
        // page number
        context.setPageNumber(pageNumber);
        // request headers
        context.setRequestHeaders(Map.of("X-Cats", "Yes"));
        // resource class
        context.setResourceClass("org.example.CatsResource");
        // resulting size
        context.setResultingSize(opList.getResultingSize(fullSize));
        // client-requested URI
        context.setRequestURI(new Reference("http://example.org/cats"));
        // local URI
        context.setLocalURI(new Reference("http://example.org/cats"));
        // scale constraint
        context.setScaleConstraint(new ScaleConstraint(1, 2));

        instance = new RequestContextMap<>(context);
    }

    @Test
    void clear() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.clear());
    }

    @Test
    void containsKey() {
        assertTrue(instance.containsKey(IDENTIFIER_KEY));
        assertFalse(instance.containsKey("bogus"));
    }

    @Test
    void containsValue() {
        assertTrue(instance.containsValue("1.2.3.4"));
        assertFalse(instance.containsValue("bogus"));
    }

    @Test
    void entrySet() {
        assertEquals(15, instance.entrySet().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getWithNonNullValues() {
        // client IP
        assertInstanceOf(String.class, instance.get(CLIENT_IP_KEY));
        // cookies
        assertInstanceOf(Map.class, instance.get(COOKIES_KEY));
        assertThrows(UnsupportedOperationException.class,
                () -> ((Map<String,String>) instance.get(COOKIES_KEY)).clear());
        // full size
        assertInstanceOf(Map.class, instance.get(FULL_SIZE_KEY));
        // identifier
        assertInstanceOf(String.class, instance.get(IDENTIFIER_KEY));
        // local URI
        assertInstanceOf(String.class, instance.get(LOCAL_URI_KEY));
        // metadata
        assertInstanceOf(Map.class, instance.get(METADATA_KEY));
        // operations
        assertInstanceOf(List.class, instance.get(OPERATIONS_KEY));
        // output format
        assertInstanceOf(String.class, instance.get(OUTPUT_FORMAT_KEY));
        // page count
        assertInstanceOf(Integer.class, instance.get(PAGE_COUNT_KEY));
        // page number
        assertInstanceOf(Integer.class, instance.get(PAGE_NUMBER_KEY));
        // request headers
        assertInstanceOf(Map.class, instance.get(REQUEST_HEADERS_KEY));
        // test that request headers map is immutable
        Map<String,String> headers = (Map<String,String>) instance.get(REQUEST_HEADERS_KEY);
        assertThrows(UnsupportedOperationException.class, headers::clear);
        // test that request headers support case-insensitive access
        headers.forEach((h,v) -> {
            assertEquals(v, headers.get(h));
            assertEquals(v, headers.get(h.toLowerCase()));
        });
        // request URI
        assertInstanceOf(String.class, instance.get(REQUEST_URI_KEY));
        // resource class
        assertInstanceOf(String.class, instance.get(RESOURCE_CLASS_KEY));
        // resulting size
        assertInstanceOf(Map.class, instance.get(RESULTING_SIZE_KEY));
        // scale constraint
        assertInstanceOf(List.class, instance.get(SCALE_CONSTRAINT_KEY));
    }

    @Test
    void getWithNullValues() {
        instance = new RequestContextMap<>(new RequestContext());
        // client IP
        assertNull(instance.get(CLIENT_IP_KEY));
        // cookies
        assertNull(instance.get(COOKIES_KEY));
        // full size
        assertNull(instance.get(FULL_SIZE_KEY));
        // identifier
        assertNull(instance.get(IDENTIFIER_KEY));
        // local URI
        assertNull(instance.get(LOCAL_URI_KEY));
        // metadata
        assertNull(instance.get(METADATA_KEY));
        // operations
        assertNull(instance.get(OPERATIONS_KEY));
        // output format
        assertNull(instance.get(OUTPUT_FORMAT_KEY));
        // page count
        assertNull(instance.get(PAGE_COUNT_KEY));
        // page number
        assertNull(instance.get(PAGE_NUMBER_KEY));
        // request headers
        assertNull(instance.get(REQUEST_HEADERS_KEY));
        // request URI
        assertNull(instance.get(REQUEST_URI_KEY));
        // resource class
        assertNull(instance.get(RESOURCE_CLASS_KEY));
        // resulting size
        assertNull(instance.get(RESULTING_SIZE_KEY));
        // scale constraint
        assertNull(instance.get(SCALE_CONSTRAINT_KEY));
    }

    @Test
    void isEmpty() {
        assertFalse(instance.isEmpty());
    }

    @Test
    void keySet() {
        assertEquals(15, instance.keySet().size());
    }

    @Test
    void put() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.put("key", "value"));
    }

    @Test
    void putAll() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.putAll(Collections.emptyMap()));
    }

    @Test
    void remove() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.remove("key"));
    }

    @Test
    void size() {
        assertEquals(15, instance.size());
    }

    @Test
    void testToString() {
        assertNotNull(instance.toString());
    }

    @Test
    void values() {
        assertEquals(15, instance.values().size());
    }

}