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
import is.galia.image.MetaIdentifier;
import is.galia.image.ScaleConstraint;
import is.galia.operation.Encode;
import is.galia.operation.OperationList;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

import static is.galia.resource.RequestContextMap.*;

class RequestContextTest extends BaseTest {

    private RequestContext instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new RequestContext();
        // client IP
        instance.setClientIP("1.2.3.4");
        // cookies
        Map<String,String> cookies = Map.of("cookie", "yes");
        instance.setCookies(cookies);
        // full size
        Size fullSize = new Size(200, 200);
        instance.setFullSize(fullSize);
        // identifier
        Identifier identifier = new Identifier("cats");
        instance.setIdentifier(identifier);
        // metadata
        instance.setMetadata(new MutableMetadata());
        // operation list
        Format outputFormat   = Format.get("gif");
        OperationList opList  = OperationList.builder()
                .withIdentifier(identifier)
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withPageNumber(3)
                        .withScaleConstraint(1, 2)
                        .build())
                .withOperations(new Encode(outputFormat))
                .build();
        instance.setOperationList(opList);
        // output format
        instance.setOutputFormat(outputFormat);
        // page count
        instance.setPageCount(3);
        // request headers
        Map<String,String> headers = Map.of("X-Cats", "Yes");
        instance.setRequestHeaders(headers);
        // resource class
        instance.setResourceClass("org.example.CatsResource");
        // resulting size
        instance.setResultingSize(opList.getResultingSize(fullSize));
        // client-requested URI
        instance.setRequestURI(new Reference("http://example.org/cats"));
        // local URI
        instance.setLocalURI(new Reference("http://example.org/cats"));
    }

    @Test
    void setClientIP() {
        String ip = "3.4.5.6";
        instance.setClientIP(ip);
        assertSame(ip, instance.getClientIP());
        instance.setClientIP(null);
        assertNull(instance.getClientIP());
    }

    @Test
    void setCookies() {
        Map<String,String> cookies = Collections.emptyMap();
        instance.setCookies(cookies);
        assertSame(cookies, instance.getCookies());
        instance.setCookies(null);
        assertNull(instance.getCookies());
    }

    @Test
    void setFullSize() {
        Size size = new Size(500, 200);
        instance.setFullSize(size);
        assertSame(size, instance.getFullSize());
        instance.setFullSize(null);
        assertNull(instance.getFullSize());
    }

    @Test
    void setIdentifier() {
        Identifier identifier = new Identifier("cats");
        instance.setIdentifier(identifier);
        assertSame(identifier, instance.getIdentifier());
        instance.setIdentifier(null);
        assertNull(instance.getIdentifier());
    }

    @Test
    void setLocalURI() {
        Reference uri = new Reference("http://example.org/");
        instance.setLocalURI(uri);
        assertSame(uri, instance.getLocalURI());
        instance.setLocalURI(null);
        assertNull(instance.getLocalURI());
    }

    @Test
    void setMetadata() {
        MutableMetadata metadata = new MutableMetadata();
        instance.setMetadata(metadata);
        assertSame(metadata, instance.getMetadata());
        instance.setMetadata(null);
        assertNull(instance.getMetadata());
    }

    @Test
    void setOperationList() {
        OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        instance.setOperationList(opList);
        assertSame(opList, instance.getOperationList());

        instance.setOperationList(null);
        assertNull(instance.getOperationList());
    }

    @Test
    void setOutputFormat() {
        Format format = Format.get("jpg");
        instance.setOutputFormat(format);
        assertEquals(format, instance.getOutputFormat());
        instance.setOutputFormat(null);
        assertNull(instance.getOutputFormat());
    }

    @Test
    void setPageCount() {
        int pageCount = 5;
        instance.setPageCount(pageCount);
        assertEquals(pageCount, instance.getPageCount());
        instance.setPageCount(null);
        assertNull(instance.getPageCount());
    }

    @Test
    void setPageNumber() {
        int pageNumber = 5;
        instance.setPageNumber(pageNumber);
        assertEquals(pageNumber, instance.getPageNumber());
        instance.setPageNumber(null);
        assertNull(instance.getPageNumber());
    }

    @Test
    void setRequestHeaders() {
        Map<String,String> headers = Collections.emptyMap();
        instance.setRequestHeaders(headers);
        assertSame(headers, instance.getRequestHeaders());
        instance.setRequestHeaders(null);
        assertNull(instance.getRequestHeaders());
    }

    @Test
    void setRequestURI() {
        Reference uri = new Reference("http://example.org/");
        instance.setRequestURI(uri);
        assertSame(uri, instance.getRequestURI());
        instance.setRequestURI(null);
        assertNull(instance.getRequestURI());
    }

    @Test
    void setResourceClass() {
        String resourceClass = "org.example.DogsResource";
        instance.setResourceClass(resourceClass);
        assertSame(resourceClass, instance.getResourceClass());
        instance.setResourceClass(null);
        assertNull(instance.getResourceClass());
    }

    @Test
    void setResultingSize() {
        Size size = new Size(450, 400);
        instance.setResultingSize(size);
        assertSame(size, instance.getResultingSize());
        instance.setResultingSize(null);
        assertNull(instance.getResultingSize());
    }

    @Test
    void setScaleConstraint() {
        ScaleConstraint constraint = new ScaleConstraint(1, 3);
        instance.setScaleConstraint(constraint);
        assertSame(constraint, instance.getScaleConstraint());
        instance.setScaleConstraint(null);
        assertNull(instance.getScaleConstraint());
    }

    @Test
    void toMap() {
        Map<String,Object> actual = instance.toMap();
        // client IP
        assertEquals("1.2.3.4", actual.get(CLIENT_IP_KEY));
        // cookies
        assertEquals("yes", ((Map<?, ?>) actual.get(COOKIES_KEY)).get("cookie"));
        // full size
        assertNotNull(actual.get(FULL_SIZE_KEY));
        // identifier
        assertEquals("cats", actual.get(IDENTIFIER_KEY));
        // local URI
        assertEquals("http://example.org/cats", actual.get(LOCAL_URI_KEY));
        // metadata
        assertNotNull(actual.get(METADATA_KEY));
        // operations
        assertNotNull(actual.get(OPERATIONS_KEY));
        // output format
        assertEquals("image/gif", actual.get(OUTPUT_FORMAT_KEY));
        // request headers
        assertEquals("Yes", ((Map<?, ?>) actual.get(REQUEST_HEADERS_KEY)).get("X-Cats"));
        // request URI
        assertEquals("http://example.org/cats", actual.get(REQUEST_URI_KEY));
        // resource class
        assertEquals("org.example.CatsResource", actual.get(RESOURCE_CLASS_KEY));
        // resulting size
        assertNotNull(actual.get(RESULTING_SIZE_KEY));
        // scale constraint
        assertNotNull(actual, SCALE_CONSTRAINT_KEY);
    }

    @Test
    void toMapLiveView() {
        instance.setClientIP("2.3.4.5");
        Map<String,Object> actual = instance.toMap();
        assertEquals("2.3.4.5", actual.get(CLIENT_IP_KEY));
        instance.setClientIP("3.4.5.6");
        assertEquals("3.4.5.6", actual.get(CLIENT_IP_KEY));
    }

}
