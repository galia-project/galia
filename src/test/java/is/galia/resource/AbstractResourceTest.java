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

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Headers;
import is.galia.http.Method;
import is.galia.http.Reference;
import is.galia.http.Status;
import is.galia.image.Format;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AbstractResourceTest extends BaseTest {

    private AbstractResource instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new AbstractResource() {
            @Override
            protected Logger getLogger() {
                return LoggerFactory.getLogger(AbstractResourceTest.class);
            }
            @Override
            public Set<Route> getRoutes() {
                return Set.of();
            }
        };
        instance.setRequest(new JettyRequestWrapper(
                new MockJettyRequest()));
        instance.setResponse(new JettyResponseWrapper(
                new MockJettyResponse()));
    }

    /* doDELETE() */

    @Test
    void doDELETE() throws Exception {
        instance.doDELETE();
        assertEquals(Status.METHOD_NOT_ALLOWED,
                instance.getResponse().getStatus());
    }

    /* doGET() */

    @Test
    void doGET() throws Exception {
        instance.doGET();
        assertEquals(Status.METHOD_NOT_ALLOWED,
                instance.getResponse().getStatus());
    }

    /* doHEAD() */

    @Test
    void doHEAD() throws Exception {
        instance.doHEAD();
        assertEquals(Status.METHOD_NOT_ALLOWED,
                instance.getResponse().getStatus());
    }

    /* doOPTIONS() */

    @Test
    void doOPTIONSWithNoDeclaredRoutes() {
        instance.doOPTIONS();
        assertEquals(Status.METHOD_NOT_ALLOWED,
                instance.getResponse().getStatus());
    }

    @Test
    void doOPTIONSWithNoGETRoutes() {
        instance.doOPTIONS();
        assertEquals(Status.METHOD_NOT_ALLOWED,
                instance.getResponse().getStatus());
    }

    @Test
    void doOPTIONSWithGETRoute() {
        instance = new AbstractResource() {
            @Override
            protected Logger getLogger() {
                return LoggerFactory.getLogger(AbstractResourceTest.class);
            }
            @Override
            public Set<Route> getRoutes() {
                return Set.of(new Route(Set.of(Method.GET), Set.of()));
            }
        };
        instance.setResponse(new JettyResponseWrapper(
                new MockJettyResponse()));
        instance.doOPTIONS();
        assertEquals(204, instance.getResponse().getStatus().code());
    }

    /* doPATCH() */

    @Test
    void doPATCH() throws Exception {
        instance.doPATCH();
        assertEquals(Status.METHOD_NOT_ALLOWED,
                instance.getResponse().getStatus());
    }

    /* doPOST() */

    @Test
    void doPOST() throws Exception {
        instance.doPOST();
        assertEquals(Status.METHOD_NOT_ALLOWED,
                instance.getResponse().getStatus());
    }

    /* doPUT() */

    @Test
    void doPUT() throws Exception {
        instance.doPUT();
        assertEquals(Status.METHOD_NOT_ALLOWED,
                instance.getResponse().getStatus());
    }

    /* getCommonTemplateVars() */

    @Test
    void getCommonTemplateVars() {
        Map<String,Object> vars = instance.getCommonTemplateVars();
        assertFalse(((String) vars.get("baseUri")).endsWith("/"));
        assertNotNull(vars.get("version"));
    }

    /* getPreferredMediaTypes() */

    @Test
    void getPreferredMediaTypesWithAcceptHeaderSet() {
        MockJettyRequest request = new MockJettyRequest();
        request.getMutableHeaders().set("Accept",
                "text/html;q=0.9, application/xhtml+xml, */*;q=0.2, text/plain;q=0.5");
        instance.setRequest(new JettyRequestWrapper(request));

        List<String> types = instance.getPreferredMediaTypes();
        assertEquals(3, types.size());
        assertEquals("application/xhtml+xml", types.get(0));
        assertEquals("text/html", types.get(1));
        assertEquals("text/plain", types.get(2));
    }

    @Test
    void getPreferredMediaTypesWithAcceptHeaderNotSet() {
        MockJettyRequest request = new MockJettyRequest();
        request.getMutableHeaders().removeAll("Accept");
        instance.setRequest(new JettyRequestWrapper(request));

        List<String> types = instance.getPreferredMediaTypes();
        assertTrue(types.isEmpty());
    }

    /* getPublicReference() */

    /**
     * Tests behavior of {@link AbstractResource#getPublicReference()} when
     * using {@link Key#BASE_URI}.
     */
    @Test
    void getPublicReferenceUsingBaseURIConfigKey() {
        final String baseURI = "http://example.org:8080/base";
        Configuration.forApplication().setProperty(Key.BASE_URI, baseURI);

        Request request = MutableRequest.builder()
                .withReference(new Reference("http://localhost:8182/llamas"))
                .build();
        instance.setRequest(request);

        Reference ref = instance.getPublicReference();
        assertEquals(baseURI + "/llamas", ref.toString());
    }

    /**
     * Tests behavior of {@link AbstractResource#getPublicReference()} when
     * using {@code X-Forwarded} headers.
     *
     * This isn't a thorough test of every possible header/URI combination.
     * See {@link Reference#applyProxyHeaders(Headers)} for those.
     */
    @Test
    void getPublicReferenceUsingXForwardedHeaders() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Proto", "HTTP");
        headers.set("X-Forwarded-Host", "example.org");
        headers.set("X-Forwarded-Port", "8080");
        headers.set("X-Forwarded-BasePath", "/base");
        Request request = MutableRequest.builder()
                .withReference(new Reference("http://localhost:8182/cats"))
                .withHeaders(headers)
                .build();
        instance.setRequest(request);

        Reference ref = instance.getPublicReference();
        assertEquals("http://example.org:8080/base/cats", ref.toString());
    }

    /**
     * Tests behavior of {@link AbstractResource#getPublicReference()} when
     * using neither {@link Key#BASE_URI} nor {@literal X-Forwarded} headers.
     */
    @Test
    void getPublicReferenceFallsBackToRequestReference() {
        String resourceURI = "http://example.net/cats/dogs";
        Request request = MutableRequest.builder()
                .withReference(new Reference(resourceURI))
                .build();
        instance.setRequest(request);

        Reference ref = instance.getPublicReference();
        assertEquals(resourceURI, ref.toString());
    }

    /* getRepresentationDisposition() */

    @Test
    void getRepresentationDispositionWithNoQueryArgument() {
        instance.getRequest().getReference().getQuery().remove(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertNull(disposition);
    }

    @Test
    void getRepresentationDispositionWithInlineQueryArgument() {
        MockJettyRequest servletRequest = new MockJettyRequest();
        servletRequest.getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "inline");
        instance.setRequest(new JettyRequestWrapper(servletRequest));

        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("inline; filename=\"cats___dogs.jpg\"", disposition);
    }

    @Test
    void getRepresentationDispositionWithAttachmentQueryArgument() {
        MockJettyRequest servletRequest = new MockJettyRequest();
        servletRequest.getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment");
        instance.setRequest(new JettyRequestWrapper(servletRequest));

        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"", disposition);
    }

    @Test
    void getRepresentationDispositionWithAttachmentQueryArgumentWithASCIIFilename() {
        MockJettyRequest servletRequest = new MockJettyRequest();
        Reference newRef = new Reference("http://example.org");
        newRef.getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"dogs.jpg\"");
        servletRequest.setReference(newRef);
        instance.setRequest(new JettyRequestWrapper(servletRequest));

        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"dogs.jpg\"", disposition);
    }

    @Test
    void getRepresentationDispositionWithAttachmentQueryArgumentWithUnsafeASCIIFilename() {
        MockJettyRequest servletRequest = new MockJettyRequest();
        servletRequest.getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"unsafe_path../\\.jpg\"");
        instance.setRequest(new JettyRequestWrapper(servletRequest));

        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"unsafe_path.jpg\"",
                disposition);

        // attachment; filename="unsafe_injection_.....//./.jpg"
        servletRequest.getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"unsafe_injection_.....//./.jpg\"");

        disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"unsafe_injection_.jpg\"",
                disposition);
    }

    @Test
    void getRepresentationDispositionWithAttachmentQueryArgumentWithUnicodeFilename() {
        MockJettyRequest servletRequest = new MockJettyRequest();
        servletRequest.getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename*= UTF-8''dogs.jpg");
        instance.setRequest(new JettyRequestWrapper(servletRequest));

        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"; filename*= UTF-8''dogs.jpg",
                disposition);
    }

    @Test
    void getRepresentationDispositionWithAttachmentQueryArgumentWithUnsafeUnicodeFilename() {
        MockJettyRequest servletRequest = new MockJettyRequest();
        servletRequest.getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename*=UTF-8''unsafe_path../\\.jpg");
        instance.setRequest(new JettyRequestWrapper(servletRequest));

        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"; filename*= UTF-8''unsafe_path.jpg",
                disposition);

        // attachment; filename*= utf-8''"unsafe_injection_.....//./.jpg"
        servletRequest.getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename*= utf-8''unsafe_injection_.....//./.jpg");

        disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"; filename*= UTF-8''unsafe_injection_.jpg",
                disposition);
    }

    @Test
    void getRepresentationDispositionWithAttachmentQueryArgumentWithASCIIAndUnicodeFilenames() {
        MockJettyRequest servletRequest = new MockJettyRequest();
        servletRequest.getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"dogs.jpg\"; filename*= UTF-8''dogs.jpg");
        instance.setRequest(new JettyRequestWrapper(servletRequest));

        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"dogs.jpg\"; filename*= UTF-8''dogs.jpg",
                disposition);
    }

    @Test
    void getRepresentationDispositionFallsBackToNone() {
        MockJettyRequest servletRequest = new MockJettyRequest();
        servletRequest.getReference().getQuery().remove(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);
        instance.setRequest(new JettyRequestWrapper(servletRequest));

        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertNull(disposition);
    }

}
