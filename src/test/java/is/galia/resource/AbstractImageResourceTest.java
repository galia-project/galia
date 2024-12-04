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
import is.galia.http.Status;
import is.galia.image.Identifier;
import is.galia.image.MetaIdentifier;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AbstractImageResourceTest extends BaseTest {

    private AbstractImageResource instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new AbstractImageResource() {
            @Override
            protected Identifier getIdentifier() {
                return null;
            }
            @Override
            protected MetaIdentifier getMetaIdentifier() {
                MetaIdentifier metaIdentifier = null;
                if (!getRequest().getPathArguments().isEmpty()) {
                    String pathComponent = getRequest().getPathArguments().getFirst();
                    if (pathComponent != null) {
                        metaIdentifier = MetaIdentifier.fromURI(
                                pathComponent, getDelegate());
                    }
                }
                return metaIdentifier;
            }
            @Override
            protected Logger getLogger() {
                return LoggerFactory.getLogger(AbstractImageResourceTest.class);
            }
            @Override
            protected Reference getPublicReference(MetaIdentifier newMetaIdentifier) {
                final Reference publicRef            = new Reference(getPublicReference());
                final List<String> pathComponents    = publicRef.getPathSegments();
                final String identifierComponent     = getRequest().getPathArguments().getFirst();
                final int identifierIndex            = pathComponents.indexOf(identifierComponent);
                final String newMetaIdentifierString =
                        newMetaIdentifier.forURI(getDelegate());
                return publicRef.rebuilder()
                        .withPathSegment(identifierIndex, newMetaIdentifierString)
                        .build();
            }
            @Override
            public Set<Route> getRoutes() {
                return Set.of();
            }
        };
        instance.setRequest(new JettyRequestWrapper(new MockJettyRequest()));
        instance.setResponse(new JettyResponseWrapper(new MockJettyResponse()));
    }

    /* getMetaIdentifier() */

    @Test
    void getMetaIdentifierWithNoPathArguments() {
        assertNull(instance.getMetaIdentifier());
    }

    @Test
    void getMetaIdentifier() {
        instance.getRequest().setPathArguments(List.of("cats"));
        assertEquals(new MetaIdentifier("cats"), instance.getMetaIdentifier());
    }

    /* getPageIndex() */

    @Test
    void getPageIndex() {
        instance.getRequest().setPathArguments(List.of("cats;3"));
        assertEquals(2, instance.getPageIndex());
    }

    /* isBypassingCache() */

    @Test
    void isBypassingCacheWhenBypassingCache() {
        MockJettyRequest request = new MockJettyRequest();
        request.getReference().getQuery().set("cache", "false");
        instance.setRequest(new JettyRequestWrapper(request));

        assertTrue(instance.isBypassingCache());
    }

    @Test
    void isBypassingCacheWhenNotBypassingCache() {
        assertFalse(instance.isBypassingCache());
    }

    /* isBypassingCacheRead() */

    @Test
    void isBypassingCacheReadWhenBypassingCacheRead() {
        MockJettyRequest request = new MockJettyRequest();
        request.getReference().getQuery().set("cache", "recache");
        instance.setRequest(new JettyRequestWrapper(request));

        assertTrue(instance.isBypassingCacheRead());
    }

    @Test
    void isBypassingCacheWhenNotBypassingCacheRead() {
        assertFalse(instance.isBypassingCacheRead());
    }

    /* redirectToNormalizedScaleConstraint() */

    @Test
    void redirectToNormalizedScaleConstraintWithNoScaleConstraintPresent()
            throws Exception {
        assertFalse(instance.redirectToNormalizedScaleConstraint());
    }

    @Test
    void redirectToNormalizedScaleConstraintWithNoOpScaleConstraintPresent()
            throws Exception {
        final String metaIdentifier = "identifier;1;5:5";
        MockJettyRequest request = new MockJettyRequest();
        request.setReference(request.getReference().rebuilder()
                .appendPath(metaIdentifier).build());
        instance.setRequest(new JettyRequestWrapper(request));
        instance.getRequest().setPathArguments(List.of(metaIdentifier));

        assertTrue(instance.redirectToNormalizedScaleConstraint());
        assertEquals(Status.MOVED_PERMANENTLY, instance.getResponse().getStatus());
        assertEquals("http://example.org/identifier",
                instance.getResponse().getHeaders().getFirstValue("Location"));
    }

    @Test
    void redirectToNormalizedScaleConstraintWithNonLowestTermsScaleConstraintPresent()
            throws Exception {
        final String metaIdentifier = "identifier;1;5:10";
        MockJettyRequest request = new MockJettyRequest();
        request.setReference(request.getReference().rebuilder()
                .appendPath(metaIdentifier).build());
        instance.setRequest(new JettyRequestWrapper(request));
        instance.getRequest().setPathArguments(List.of(metaIdentifier));

        assertTrue(instance.redirectToNormalizedScaleConstraint());
        assertEquals(Status.MOVED_PERMANENTLY, instance.getResponse().getStatus());
        assertEquals("http://example.org/identifier;1:2",
                instance.getResponse().getHeaders().getFirstValue("Location"));
    }

    @Test
    void redirectToNormalizedScaleConstraintWithPagePresent() throws Exception {
        final String metaIdentifier = "identifier;3;5:10";
        MockJettyRequest request = new MockJettyRequest();
        request.setReference(request.getReference().rebuilder()
                .appendPath(metaIdentifier).build());
        instance.setRequest(new JettyRequestWrapper(request));
        instance.getRequest().setPathArguments(List.of(metaIdentifier));

        assertTrue(instance.redirectToNormalizedScaleConstraint());
        assertEquals(Status.MOVED_PERMANENTLY, instance.getResponse().getStatus());
        assertEquals("http://example.org/identifier;3;1:2",
                instance.getResponse().getHeaders().getFirstValue("Location"));
    }

    @Test
    void redirectToNormalizedScaleConstraintWithLowestTermsScaleConstraintPresent()
            throws Exception {
        final String metaIdentifier = "identifier;7:9";
        MockJettyRequest request = new MockJettyRequest();
        request.setReference(request.getReference().rebuilder()
                .appendPath(metaIdentifier).build());
        instance.setRequest(new JettyRequestWrapper(request));
        instance.getRequest().setPathArguments(List.of(metaIdentifier));

        assertFalse(instance.redirectToNormalizedScaleConstraint());
        assertEquals(new Status(0), instance.getResponse().getStatus());
    }

}
