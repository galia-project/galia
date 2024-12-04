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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceTest extends BaseTest {

    @Nested
    class BuilderTest extends BaseTest {

        private Reference.Builder builder;

        @Override
        @BeforeEach
        public void setUp() throws Exception {
            builder = Reference.builder().withHost("example.org");
        }

        /* appendPath() */

        @Test
        void appendPathWithNullArgument() {
            builder.appendPath(null);
            Reference reference = builder.build();
            assertEquals("", reference.getPath());
        }

        @Test
        void appendPathWithUnslashedPath() {
            builder.appendPath("cats");
            Reference reference = builder.build();
            assertEquals("/cats", reference.getPath());
        }

        @Test
        void appendPathWithLeadingSlash() {
            builder.appendPath("/cats");
            Reference reference = builder.build();
            assertEquals("/cats", reference.getPath());
        }

        @Test
        void appendPathWithTrailingSlash() {
            builder.appendPath("cats/");
            Reference reference = builder.build();
            assertEquals("/cats/", reference.getPath());
        }

        /* applyProxyHeaders() */

        @Test
        void applyProxyHeadersWithNoProxyHeaders() {
            final String expected = builder.toString();
            builder.applyProxyHeaders(new Headers());
            assertEquals(expected, builder.toString());
        }

        @Test
        void applyProxyHeadersWithHTTPSchemeAndXForwardedProtoHTTP() {
            Headers headers = new Headers();
            headers.set("X-Forwarded-Proto", "HTTP");
            builder.withPath("/cats");
            builder.applyProxyHeaders(headers);
            Reference reference = builder.build();
            assertEquals("http://example.org/cats", reference.toString());
        }

        @Test
        void applyProxyHeadersWithHTTPSchemeAndXForwardedProtoHTTPS() {
            Headers headers = new Headers();
            headers.set("X-Forwarded-Proto", "HTTPS");
            builder.withScheme("https");
            builder.withPath("/cats");
            builder.applyProxyHeaders(headers);
            Reference reference = builder.build();
            assertEquals("https://example.org/cats", reference.toString());
        }

        @Test
        void applyProxyHeadersWithHTTPSSchemeAndXForwardedProtoHTTP() {
            Headers headers = new Headers();
            headers.set("X-Forwarded-Proto", "HTTP");
            builder.withScheme("https");
            builder.withPath("/cats");
            builder.applyProxyHeaders(headers);
            Reference reference = builder.build();
            assertEquals("http://example.org/cats", reference.toString());
        }

        @Test
        void applyProxyHeadersWithHTTPSSchemeAndXForwardedProtoHTTPS() {
            Headers headers = new Headers();
            headers.set("X-Forwarded-Proto", "HTTPS");
            builder.withScheme("https");
            builder.withPath("/cats");
            builder.applyProxyHeaders(headers);
            Reference reference = builder.build();
            assertEquals("https://example.org/cats", reference.toString());
        }

        @Test
        void applyProxyHeadersWithXForwardedHost() {
            Headers headers = new Headers();
            headers.set("X-Forwarded-Host", "example.net");
            builder.withPath("/cats");
            builder.applyProxyHeaders(headers);
            Reference reference = builder.build();
            assertEquals("http://example.net/cats", reference.toString());
        }

        @Test
        void applyProxyHeadersWithXForwardedHostContainingDefaultHTTPPort() {
            Headers headers = new Headers();
            headers.set("X-Forwarded-Host", "example.net:80");
            builder.withPath("/cats");
            builder.applyProxyHeaders(headers);
            Reference reference = builder.build();
            assertEquals("http://example.net/cats", reference.toString());
        }

        @Test
        void applyProxyHeadersWithXForwardedHostContainingDefaultHTTPSPort() {
            Headers headers = new Headers();
            headers.set("X-Forwarded-Host", "example.net:443");
            builder.withPath("/cats");
            builder.applyProxyHeaders(headers);
            Reference reference = builder.build();
            assertEquals("http://example.net:443/cats", reference.toString());
        }

        @Test
        void applyProxyHeadersWithXForwardedHostContainingCustomPort() {
            Headers headers = new Headers();
            headers.set("X-Forwarded-Host", "example.net:8080");
            builder.withPath("/cats");
            builder.applyProxyHeaders(headers);
            Reference reference = builder.build();
            assertEquals("http://example.net:8080/cats", reference.toString());
        }

        @Test
        void applyProxyHeadersWithXForwardedHostContainingCustomPortAndXForwardedPort() {
            Headers headers = new Headers();
            headers.set("X-Forwarded-Host", "example.net:8080");
            headers.set("X-Forwarded-Port", "8283");
            builder.withPath("/cats");
            builder.applyProxyHeaders(headers);
            Reference reference = builder.build();
            assertEquals("http://example.net:8283/cats", reference.toString());
        }

        @Test
        void applyProxyHeadersWithXForwardedHostContainingCustomPortAndXForwardedProto() {
            Headers headers = new Headers();
            headers.set("X-Forwarded-Host", "example.net:8080");
            headers.set("X-Forwarded-Proto", "HTTP");
            builder.withPath("/cats");
            builder.applyProxyHeaders(headers);
            Reference reference = builder.build();
            assertEquals("http://example.net:8080/cats", reference.toString());
        }

        @Test
        void applyProxyHeadersWithXForwardedPortMatchingDefaultHTTPPort() {
            Headers headers = new Headers();
            headers.set("X-Forwarded-Port", "80");
            builder.withPath("/cats");
            builder.applyProxyHeaders(headers);
            Reference reference = builder.build();
            assertEquals("http://example.org/cats", reference.toString());
        }

        @Test
        void applyProxyHeadersWithXForwardedPortMatchingDefaultHTTPSPort() {
            Headers headers = new Headers();
            headers.set("X-Forwarded-Port", "443");
            builder.withPath("/cats");
            builder.applyProxyHeaders(headers);
            Reference reference = builder.build();
            assertEquals("http://example.org:443/cats", reference.toString());
        }

        @Test
        void applyProxyHeadersWithXForwardedPort() {
            Headers headers = new Headers();
            headers.set("X-Forwarded-Port", "569");
            builder.withPath("/cats");
            builder.applyProxyHeaders(headers);
            Reference reference = builder.build();
            assertEquals("http://example.org:569/cats", reference.toString());
        }

        @Test
        void applyProxyHeadersWithXForwardedPath1() {
            Headers headers = new Headers();
            headers.set("X-Forwarded-BasePath", "/");
            builder.withPath("/cats");
            builder.applyProxyHeaders(headers);
            Reference reference = builder.build();
            assertEquals("http://example.org/cats", reference.toString());
        }

        @Test
        void applyProxyHeadersWithXForwardedPath2() {
            Headers headers = new Headers();
            headers.set("X-Forwarded-BasePath", "/this/is/the/path");
            builder.withPath("/cats");
            builder.applyProxyHeaders(headers);
            Reference reference = builder.build();
            assertEquals("http://example.org/this/is/the/path/cats",
                    reference.toString());
        }

        /**
         * Tests behavior when using chained {@literal X-Forwarded} headers.
         */
        @Test
        void applyProxyHeadersUsingChainedXForwardedHeaders() {
            Headers headers = new Headers();
            headers.set("X-Forwarded-Proto", "http,https");
            headers.set("X-Forwarded-Host", "example.org,example.mil");
            headers.set("X-Forwarded-Port", "80,8080");
            headers.set("X-Forwarded-BasePath", "/animals/foxes,/animals/dogs");
            builder.withPath("/cats");
            builder.applyProxyHeaders(headers);
            Reference reference = builder.build();
            assertEquals("http://example.org/animals/foxes/cats",
                    reference.toString());
        }

        /* build() */

        @Test
        void buildWithNullScheme() {
            builder.withScheme(null);
            assertThrows(NullPointerException.class, () -> builder.build());
        }

        @Test
        void buildWithBlankScheme() {
            builder.withScheme("");
            assertThrows(NullPointerException.class, () -> builder.build());
        }

        @Test
        void buildWithUnsupportedScheme() {
            builder.withScheme("cats");
            assertThrows(IllegalArgumentException.class, () -> builder.build());
        }

        @Test
        void buildWithNoHostAndHTTPScheme() {
            builder.withHost(null);
            builder.withScheme("http");
            assertThrows(NullPointerException.class, () -> builder.build());
        }

        @Test
        void buildWithNoHostAndHTTPSScheme() {
            builder.withHost(null);
            builder.withScheme("https");
            assertThrows(NullPointerException.class, () -> builder.build());
        }

        @Test
        void buildWithSecretAndBlankUser() {
            builder.withSecret("user");
            assertThrows(IllegalArgumentException.class, () -> builder.build());
        }

        /* prependPath() */

        @Test
        void prependPathWithNullArgument() {
            builder.withPath("/cats");
            builder.prependPath(null);
            Reference reference = builder.build();
            assertEquals("/cats", reference.getPath());
        }

        @Test
        void prependPathWithUnslashedPath() {
            builder.withPath("/cats");
            builder.prependPath("newpath");
            Reference reference = builder.build();
            assertEquals("/newpath/cats", reference.getPath());
        }

        @Test
        void prependPathWithLeadingSlash() {
            builder.withPath("/cats");
            builder.prependPath("/newpath");
            Reference reference = builder.build();
            assertEquals("/newpath/cats", reference.getPath());
        }

        @Test
        void prependPathWithTrailingSlash() {
            builder.withPath("/cats");
            builder.prependPath("newpath/");
            Reference reference = builder.build();
            assertEquals("/newpath/cats", reference.getPath());
        }

        /* withFragment() */

        @Test
        void withFragmentWithNull() {
            builder.withFragment(null);
            Reference reference = builder.build();
            assertEquals("", reference.getFragment());
        }

        @Test
        void withFragment() {
            builder.withFragment("fragment");
            Reference reference = builder.build();
            assertEquals("fragment", reference.getFragment());
        }

        /* withHost() */

        @Test
        void withHostWithNull() {
            builder.withScheme("file");
            builder.withHost(null);
            Reference reference = builder.build();
            assertEquals("", reference.getHost());
        }

        @Test
        void withHost() {
            builder.withHost("host");
            Reference reference = builder.build();
            assertEquals("host", reference.getHost());
        }

        /* withPath() */

        @Test
        void withPathWithNull() {
            builder.withPath(null);
            Reference reference = builder.build();
            assertEquals("", reference.getPath());
        }

        @Test
        void withPathWithUnslashedPath() {
            builder.withPath("cats");
            Reference reference = builder.build();
            assertEquals("/cats", reference.getPath());
        }

        @Test
        void withPathWithLeadingSlash() {
            builder.withPath("/cats");
            Reference reference = builder.build();
            assertEquals("/cats", reference.getPath());
        }

        @Test
        void withPathWithTrailingSlash() {
            builder.withPath("/cats/");
            Reference reference = builder.build();
            assertEquals("/cats/", reference.getPath());
        }

        /* withPathSegment() */

        @Test
        void withPathSegmentWithUnslashedSegment() {
            builder.withPath("/path");
            builder.withPathSegment(0, "base");
            Reference reference = builder.build();
            assertEquals("/base", reference.getPath());
        }

        @Test
        void withPathSegmentWithLeadingSlash() {
            builder.withPath("/path");
            builder.withPathSegment(0, "/base");
            Reference reference = builder.build();
            assertEquals("/base", reference.getPath());
        }

        @Test
        void withPathSegmentWithTrailingSlash() {
            builder.withPath("/path");
            builder.withPathSegment(0, "base/");
            Reference reference = builder.build();
            assertEquals("/base", reference.getPath());
        }

        @Test
        void withPathSegmentWithNextIndex() {
            builder.withPath("/path");
            builder.withPathSegment(1, "newpath");
            Reference reference = builder.build();
            assertEquals("/path/newpath", reference.getPath());
        }

        @Test
        void withPathSegmentWithIndexOutOfBounds() {
            builder.withPath("/path");
            assertThrows(IndexOutOfBoundsException.class,
                    () -> builder.withPathSegment(2, "base"));
        }

        /* withPort() */

        @Test
        void withPort() {
            builder.withPort(8080);
            Reference reference = builder.build();
            assertEquals(8080, reference.getPort());
        }

        /* withQuery() */

        @Test
        void withQueryWithNull() {
            builder.withQuery(null);
            Reference reference = builder.build();
            assertTrue(reference.getQuery().isEmpty());
        }

        @Test
        void withQuery() {
            Query query = new Query();
            query.set("q1", "cats");
            builder.withQuery(query);
            Reference reference = builder.build();
            assertEquals(query, reference.getQuery());
        }

        /* withScheme() */

        @Test
        void withScheme() {
            builder.withScheme("https");
            Reference reference = builder.build();
            assertEquals("https", reference.getScheme());
        }

        /* withSecret() */

        @Test
        void withSecretWithNull() {
            builder.withUser("user");
            builder.withSecret(null);
            Reference reference = builder.build();
            assertEquals("", reference.getSecret());
        }

        @Test
        void withSecret() {
            builder.withUser("user");
            builder.withSecret("secret");
            Reference reference = builder.build();
            assertEquals("secret", reference.getSecret());
        }

        /* withUser() */

        @Test
        void withUserWithNull() {
            builder.withUser(null);
            Reference reference = builder.build();
            assertEquals("", reference.getUser());
        }

        @Test
        void withUser() {
            builder.withUser("user");
            Reference reference = builder.build();
            assertEquals("user", reference.getUser());
        }

        /* withoutFragment() */

        @Test
        void withoutFragment() {
            builder.withFragment("fragment").withoutFragment();
            Reference reference = builder.build();
            assertEquals("", reference.getFragment());
        }

        /* withoutPath() */

        @Test
        void withoutPath() {
            builder.withPath("/path").withoutPath();
            Reference reference = builder.build();
            assertEquals("", reference.getPath());
        }

        /* withoutPort() */

        @Test
        void withoutPort() {
            builder.withPort(8080).withoutPort();
            Reference reference = builder.build();
            assertEquals(-1, reference.getPort());
        }

        /* withoutQuery() */

        @Test
        void withoutQuery() {
            Query query = new Query();
            query.set("q1", "cats");
            builder.withQuery(query).withoutQuery();
            Reference reference = builder.build();
            assertTrue(reference.getQuery().isEmpty());
        }

        /* withoutTrailingSlashInPath() */

        @Test
        void withoutTrailingSlashInPath() {
            builder.withPath("/path/").withoutTrailingSlashInPath();
            Reference reference = builder.build();
            assertEquals("/path", reference.getPath());
        }

        /* withoutSecret() */

        @Test
        void withoutSecret() {
            builder.withSecret("secret").withoutSecret();
            Reference reference = builder.build();
            assertEquals("", reference.getSecret());
        }

        /* withoutUser() */

        @Test
        void withoutUser() {
            builder.withUser("user").withoutUser();
            Reference reference = builder.build();
            assertEquals("", reference.getUser());
        }

    }

    private Reference instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Reference("http://user:secret@example.org:81/p1/p2.jpg?q1=cats&q2=dogs#35");
    }

    /* decode() */

    @Test
    void decode() {
        String uri      = "filename%3D%22space%20space.jpg%22";
        String expected = "filename=\"space space.jpg\"";
        assertEquals(expected, Reference.decode(uri));
    }

    /* encode() */

    @Test
    void encode() {
        String str      = "dogs?cats=dogs";
        String expected = "dogs%3Fcats%3Ddogs";
        assertEquals(expected, Reference.encode(str));
    }

    @Test
    void encodeOmitsCertainCharacters() {
        assertEquals("~:;", Reference.encode("~:;"));
    }

    /* Reference(Reference) */

    @Test
    void copyConstructor() {
        Reference copy = new Reference(instance);
        assertEquals(copy, instance);
        assertNotSame(copy.getQuery(), instance.getQuery());
    }

    @Test
    void copyConstructorClonesQuery() {
        Reference copy = new Reference(instance);
        assertNotSame(instance.getQuery(), copy.getQuery());
    }

    /* Reference(Path) */

    @Test
    void pathConstructor() {
        Path path = Paths.get("/bla/bla/bla");
        Reference ref = new Reference(path);
        assertEquals("file:///bla/bla/bla", ref.toString());
    }

    /* Reference(String) */

    @Test
    void stringConstructor() {
        String uri = "http://example.org";
        Reference ref = new Reference(uri);
        assertEquals(uri, ref.toString());
        assertEquals("", ref.getPath());
    }

    @Test
    void stringConstructorWithFileScheme() {
        String uri = "file:///dev/null";
        Reference ref = new Reference(uri);
        assertEquals(uri, ref.toString());
    }

    @Test
    void stringConstructorWithS3Scheme() {
        String uri = "s3://bucket/key";
        Reference ref = new Reference(uri);
        assertEquals(uri, ref.toString());
    }

    @Test
    void stringConstructorWithUnencodedCharacters() {
        String uri = "http://example.org/cats`/`dogs?cats=dogs`";
        Reference ref = new Reference(uri);
        assertEquals("http://example.org/cats%60/%60dogs?cats=dogs%60",
                ref.toString());
    }

    @Test
    void stringConstructorWithEncodedCharacters() {
        String uri = "http://example.org/cats%2Fdogs?q=a%3Db";
        Reference ref = new Reference(uri);
        assertEquals("http://example.org/cats%2Fdogs?q=a%3Db", ref.toString());
    }

    /* Reference(URI) */

    @Test
    void uriConstructor() throws Exception {
        URI uri = new URI("http://example.org/cats/dogs?cats=dogs");
        Reference ref = new Reference(uri);
        assertEquals(uri.toString(), ref.toString());
    }

    /* equals() */

    @Test
    void equalsWithDifferentScheme() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("http://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1, ref2);
    }

    @Test
    void equalsWithDifferentUser() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://bob:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1, ref2);
    }

    @Test
    void equalsWithDifferentSecret() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://user:bla@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1, ref2);
    }

    @Test
    void equalsWithDifferentHost() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://user:secret@example.net:81/cats/dogs?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1, ref2);
    }

    @Test
    void equalsWithDifferentPort() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://user:secret@example.org:82/cats/dogs?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1, ref2);
    }

    @Test
    void equalsWithDifferentPath() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://user:secret@example.org:81/cats/wolves?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1, ref2);
    }

    @Test
    void equalsWithDifferentQuery() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=ants#frag");
        assertNotEquals(ref1, ref2);
    }

    @Test
    void equalsWithDifferentFragment() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#2");
        assertNotEquals(ref1, ref2);
    }

    @Test
    void equalsWithEqualStrings() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        assertEquals(ref1, ref2);
    }

    @Test
    void equalsWithHTTPPort() {
        Reference ref1 = new Reference("http://user:secret@example.org/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("http://user:secret@example.org:80/cats/dogs?cats=dogs&foxes=hens#frag");
        assertEquals(ref1, ref2);
    }

    @Test
    void equalsWithHTTPSPort() {
        Reference ref1 = new Reference("https://user:secret@example.org/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://user:secret@example.org:443/cats/dogs?cats=dogs&foxes=hens#frag");
        assertEquals(ref1, ref2);
    }

    /* getAuthority() */

    @Test
    void getAuthorityWithUserAndSecret() {
        assertEquals("user:secret@example.org:81", instance.getAuthority());
    }

    @Test
    void getAuthorityWithUser() {
        instance = new Reference("http://user@example.org:81");
        assertEquals("user@example.org:81", instance.getAuthority());
    }

    @Test
    void getAuthorityWithoutUserOrSecret() {
        instance = new Reference("http://example.org:81");
        assertEquals("example.org:81", instance.getAuthority());
    }

    @Test
    void getAuthorityWithHTTPSchemeAndStandardPort() {
        instance = new Reference("http://user:secret@example.org");
        assertEquals("user:secret@example.org", instance.getAuthority());
    }

    @Test
    void getAuthorityWithHTTPSchemeAndNonStandardPort() {
        instance = new Reference("http://user:secret@example.org:81");
        assertEquals("user:secret@example.org:81", instance.getAuthority());
    }

    @Test
    void getAuthorityWithHTTPSSchemeAndStandardPort() {
        instance = new Reference("https://user:secret@example.org:443");
        assertEquals("user:secret@example.org", instance.getAuthority());
    }

    @Test
    void getAuthorityWithHTTPSSchemeAndNonStandardPort() {
        instance = new Reference("https://user:secret@example.org:444");
        assertEquals("user:secret@example.org:444", instance.getAuthority());
    }

    @Test
    void getAuthorityWithFileScheme() {
        instance = new Reference("file:///dev/null");
        assertEquals("", instance.getAuthority());
    }

    /* getFragment() */

    @Test
    void getFragment() {
        assertEquals("35", instance.getFragment());
    }

    /* getHost() */

    @Test
    void getHost() {
        assertEquals("example.org", instance.getHost());
    }

    /* getPath() */

    @Test
    void getPathWithNoPath() {
        instance = new Reference("http://example.org");
        assertEquals("", instance.getPath());
    }

    @Test
    void getPath() {
        assertEquals("/p1/p2.jpg", instance.getPath());
    }

    /* getPathSegments() */

    @Test
    void getPathSegmentsWithNoPath() {
        instance = new Reference("http://example.org");
        List<String> segments = instance.getPathSegments();
        assertTrue(segments.isEmpty());
    }

    @Test
    void getPathSegments() {
        List<String> segments = instance.getPathSegments();
        assertEquals(2, segments.size());
        assertEquals("p1", segments.get(0));
        assertEquals("p2.jpg", segments.get(1));
    }

    /* getPathExtension() */

    @Test
    void getPathExtensionWithNoPeriodsInLastPathComponent() {
        instance = new Reference("http://example.org/p1/p2");
        assertEquals("", instance.getPathExtension());
    }

    @Test
    void getPathExtensionWithSinglePeriodInLastPathComponent() {
        instance = new Reference("http://example.org/p1.p1/p2.jpg");
        assertEquals("jpg", instance.getPathExtension());
    }

    @Test
    void getPathExtensionWithMultiplePeriodsInLastPathComponent() {
        instance = new Reference("http://example.org/p1/p2.cats.jpg");
        assertEquals("jpg", instance.getPathExtension());
    }

    @Test
    void getPathExtensionWithPeriodAtBeginningOfLastPathComponent() {
        instance = new Reference("http://example.org/p1/.jpg");
        assertEquals("", instance.getPathExtension());
    }

    /* getPort() */

    @Test
    void getPort() {
        assertEquals(81, instance.getPort());
    }

    /* getQuery() */

    @Test
    void getQuery() {
        Query expected = new Query();
        expected.set("q1", "cats");
        expected.set("q2", "dogs");
        Query actual = instance.getQuery();
        assertEquals(expected, actual);
    }

    /* getScheme() */

    @Test
    void getScheme() {
        assertEquals("http", instance.getScheme());
    }

    /* hashCode() */

    @Test
    void hashCodeWithDifferentScheme() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("http://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1.hashCode(), ref2.hashCode());
    }

    @Test
    void hashCodeWithDifferentUser() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://bob:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1.hashCode(), ref2.hashCode());
    }

    @Test
    void hashCodeWithDifferentSecret() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://user:bla@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1.hashCode(), ref2.hashCode());
    }

    @Test
    void hashCodeWithDifferentHost() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://user:secret@example.net:81/cats/dogs?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1.hashCode(), ref2.hashCode());
    }

    @Test
    void hashCodeWithDifferentPort() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://user:secret@example.org:82/cats/dogs?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1.hashCode(), ref2.hashCode());
    }

    @Test
    void hashCodeWithDifferentPath() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://user:secret@example.org:81/cats/wolves?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1.hashCode(), ref2.hashCode());
    }

    @Test
    void hashCodeWithDifferentQuery() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=ants#frag");
        assertNotEquals(ref1.hashCode(), ref2.hashCode());
    }

    @Test
    void hashCodeWithDifferentFragment() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#2");
        assertNotEquals(ref1.hashCode(), ref2.hashCode());
    }

    @Test
    void hashCodeWithEqualStrings() {
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        assertEquals(ref1, ref2);
    }

    @Test
    void hashCodeWithHTTPPort() {
        Reference ref1 = new Reference("http://user:secret@example.org/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("http://user:secret@example.org:80/cats/dogs?cats=dogs&foxes=hens#frag");
        assertEquals(ref1, ref2);
    }

    @Test
    void hashCodeWithHTTPSPort() {
        Reference ref1 = new Reference("https://user:secret@example.org/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://user:secret@example.org:443/cats/dogs?cats=dogs&foxes=hens#frag");
        assertEquals(ref1, ref2);
    }

    /* toString() */

    @Test
    void toString1WithFileScheme() {
        instance = new Reference("file:///dev/null");
        String expected = "file:///dev/null";
        String actual = instance.toString();
        assertEquals(expected, actual);
    }

    @Test
    void toString1WithBlankUser() {
        instance = new Reference("http://example.org");
        String expected = "http://example.org";
        String actual = instance.toString();
        assertEquals(expected, actual);
    }

    @Test
    void toString1WithBlankSecret() {
        instance = new Reference("http://user@example.org");
        String expected = "http://user@example.org";
        String actual = instance.toString();
        assertEquals(expected, actual);
    }

    @Test
    void toString1WithStandardPort() {
        instance = new Reference("http://example.org");
        String expected = "http://example.org";
        String actual = instance.toString();
        assertEquals(expected, actual);
    }

    @Test
    void toString1WithBlankPath() {
        instance = new Reference("http://user:secret@example.org:81?q1=cats&q2=dogs#35");
        String expected = "http://user:secret@example.org:81/?q1=cats&q2=dogs#35";
        String actual = instance.toString();
        assertEquals(expected, actual);
    }

    @Test
    void toString1WithNoQuery() {
        instance = new Reference("http://user:secret@example.org:81#35");
        String expected = "http://user:secret@example.org:81#35";
        String actual = instance.toString();
        assertEquals(expected, actual);
    }

    @Test
    void toString1WithNoFragment() {
        instance = new Reference("http://user:secret@example.org:81?q1=cats&q2=dogs");
        String expected = "http://user:secret@example.org:81/?q1=cats&q2=dogs";
        String actual = instance.toString();
        assertEquals(expected, actual);
    }

    @Test
    void toString1WithAllComponents() {
        String expected = "http://user:secret@example.org:81/p1/p2.jpg?q1=cats&q2=dogs#35";
        String actual = instance.toString();
        assertEquals(expected, actual);
    }

    @Test
    void toString1Encoding() {
        instance        = new Reference("http://user`:secret`@example.org:81/p`/p2.jpg?q1=cats`");
        String expected = "http://user%60:secret%60@example.org:81/p%60/p2.jpg?q1=cats%60";
        String actual   = instance.toString();
        assertEquals(expected, actual);
    }

    /* toString(boolean) */

    @Test
    void toString2WithEncodingPath() {
        instance        = new Reference("http://user`:secret`@example.org:81/p`/p2.jpg?q1=cats`");
        String expected = "http://user%60:secret%60@example.org:81%2Fp%2560%2Fp2.jpg?q1=cats%60";
        String actual   = instance.toString(true);
        assertEquals(expected, actual);
    }

    @Test
    void toString2WithNotEncodingPath() {
        instance        = new Reference("http://user`:secret`@example.org:81/pp%2Fpp2.jpg?q1=cats`");
        String expected = "http://user%60:secret%60@example.org:81/pp%2Fpp2.jpg?q1=cats%60";
        String actual   = instance.toString(false);
        assertEquals(expected, actual);
    }

    /* toURI() */

    @Test
    void toURI() {
        URI expected = URI.create("http://user:secret@example.org:81/p1/p2.jpg?q1=cats&q2=dogs#35");
        URI actual = instance.toURI();
        assertEquals(expected, actual);
    }

}
