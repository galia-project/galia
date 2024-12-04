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

package is.galia.resource.iiif.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import is.galia.Application;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.delegate.Delegate;
import is.galia.http.Headers;
import is.galia.http.HTTPException;
import is.galia.http.Method;
import is.galia.http.Reference;
import is.galia.http.Response;
import is.galia.http.Status;
import is.galia.image.Identifier;
import is.galia.image.MetaIdentifier;
import is.galia.image.StandardMetaIdentifierTransformer;
import is.galia.resource.AbstractResource;
import is.galia.resource.ResourceITCase;
import is.galia.resource.iiif.InformationResourceTester;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static is.galia.test.Assert.HTTPAssert.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional test of the features of InformationResource.
 */
class InformationResourceITCase extends ResourceITCase {

    private static final String DECODED_IDENTIFIER =
            "sample-images/jpg/rgb-64x56x8-baseline.jpg";
    private static final String ENCODED_IDENTIFIER =
            Reference.encode(DECODED_IDENTIFIER);
    private static final Path IMAGE_PATH =
            TestUtils.getSampleImage("jpg/rgb-64x56x8-baseline.jpg");

    private final InformationResourceTester tester =
            new InformationResourceTester();

    @Test
    void doGETAuthorizationWhenAuthorized() {
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        tester.testAuthorizationWhenAuthorized(uri);
    }

    @Test
    void doGETAuthorizationWhenUnauthorized() {
        Reference uri = getHTTPURI(InformationResource.getURIPath() +
                "/unauthorized.jpg/info.json");
        tester.testAuthorizationWhenUnauthorized(uri);
    }

    @Test
    void doGETAuthorizationWhenForbidden() {
        Reference uri = getHTTPURI(
                InformationResource.getURIPath() + "/forbidden.jpg/info.json");
        tester.testAuthorizationWhenForbidden(uri);
    }

    @Test
    void doGETAuthorizationWhenNotAuthorizedWhenAccessingCachedResource()
            throws Exception {
        Reference uri = getHTTPURI(
                InformationResource.getURIPath() + "/forbidden.jpg/info.json");
        tester.testAuthorizationWhenNotAuthorizedWhenAccessingCachedResource(uri);
    }

    @Test
    void doGETAuthorizationWhenScaleConstraining() throws Exception {
        Reference uri = getHTTPURI(
                InformationResource.getURIPath() + "/reduce.jpg/info.json");
        tester.testAuthorizationWhenScaleConstraining(uri);
    }

    @Test
    void doGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable()
            throws Exception {
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable(uri);
    }

    @Test
    void doGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable()
            throws Exception {
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/bogus/info.json");
        tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable(uri);
    }

    /**
     * Tests that there is no {@code Cache-Control} header returned when
     * {@code cache.client.enabled = true} but a {@code cache=nocache} argument
     * is present in the URL query.
     */
    @Test
    void doGETCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL1()
            throws Exception {
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json?cache=nocache");
        tester.testCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL(uri);
    }

    /**
     * Tests that there is no {@code Cache-Control} header returned when
     * {@code cache.client.enabled = true} but a {@code cache=false} argument
     * is present in the URL query.
     */
    @Test
    void doGETCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL2()
            throws Exception {
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json?cache=false");
        tester.testCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL(uri);
    }

    /**
     * Tests that there is a {@code Cache-Control} header returned when
     * {@code cache.client.enabled = true} and a {@code cache=recache} argument
     * is present in the URL query.
     */
    @Test
    void doGETCacheHeadersWhenClientCachingIsEnabledAndRecachingIsEnabledInURL()
            throws Exception {
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json?cache=recache");
        tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable(uri);
    }

    @Test
    void doGETCacheHeadersWhenClientCachingIsDisabled() throws Exception {
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        tester.testCacheHeadersWhenClientCachingIsDisabled(uri);
    }

    @Test
    void doGETCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied1()
            throws Exception {
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json?cache=nocache");
        tester.testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(uri);
    }

    @Test
    void doGETCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied2()
            throws Exception {
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json?cache=false");
        tester.testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(uri);
    }

    @Test
    void doGETCachingWhenCachesAreEnabledAndRecacheQueryArgumentIsSupplied()
            throws Exception {
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json?cache=recache");
        tester.testCachingWhenCachesAreEnabledAndRecacheQueryArgumentIsSupplied(uri);
    }

    @Test
    void doGETCacheWithCachesEnabledAndResolveFirstEnabled() throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        tester.testCacheWithCachesEnabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithCachesEnabledAndResolveFirstDisabled() throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        tester.testCacheWithCachesEnabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        tester.testCacheWithInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        tester.testCacheWithInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        tester.testCacheWithInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        tester.testCacheWithInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithCachesDisabledAndResolveFirstEnabled() throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        tester.testCacheWithCachesDisabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithCachesDisabledAndResolveFirstDisabled() throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        tester.testCacheWithCachesDisabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETDelegateContext() throws Exception {
        Reference uri = getHTTPURI(InformationResource.getURIPath() +
                "/delegate-context-info-test.jpg/info.json");
        tester.testDelegateContext(uri);
    }

    @Test
    void doGETEndpointEnabled() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, true);

        assertStatus(200, getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json"));
    }

    @Test
    void doGETEndpointDisabled() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, false);

        assertStatus(404, getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json"));
    }

    @Test
    void doGETWithForwardSlashInIdentifier() {
        Reference uri = getHTTPURI(InformationResource.getURIPath() +
                "/sample-images%2Fjpg%2Fjpg.jpg/info.json");
        tester.testForwardSlashInIdentifier(uri);
    }

    @Test
    void doGETWithBackslashInIdentifier() {
        Reference uri = getHTTPURI(InformationResource.getURIPath() +
                "/sample-images%5Cjpg%5Cjpg.jpg/info.json");
        tester.testBackslashInIdentifier(uri);
    }

    @Test
    void doGETWithCustomizedInformationJSON() throws Exception {
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        tester.testCustomizedInformationJSON(uri);
    }

    @Test
    void doGETWithIllegalCharactersInIdentifier() {
        Reference uri = getHTTPURI(InformationResource.getURIPath() +
                "/[bogus]/info.json");
        tester.testIllegalCharactersInIdentifier(uri);
    }

    @Test
    void doGETHTTP2() throws Exception {
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        tester.testHTTP2(uri);
    }

    @Test
    void doGETHTTPS1_1() throws Exception {
        Reference uri = getHTTPSURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        tester.testHTTPS1_1(uri);
    }

    @Test
    void doGETHTTPS2() throws Exception {
        Reference uri = getHTTPSURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        tester.testHTTPS2(uri);
    }

    @Test
    void doGETForbidden() {
        Reference uri = getHTTPURI(InformationResource.getURIPath() +
                "/forbidden.jpg/info.json");
        tester.testForbidden(uri);
    }

    @Test
    void doGETNotFound() {
        Reference uri = getHTTPURI(InformationResource.getURIPath() +
                "/invalid/info.json");
        tester.testNotFound(uri);
    }

    @Test
    void doGETWithPageNumberInMetaIdentifier() {
        String identifier = "sample-images%2Ftif%2Fmultipage.tif";
        Reference uri1 = getHTTPURI(InformationResource.getURIPath() + "/" +
                identifier + "/info.json");
        Reference uri2 = getHTTPURI(InformationResource.getURIPath() + "/" +
                identifier + ";2/info.json");
        assertRepresentationsNotSame(uri1, uri2);
    }

    @Test
    void doGETPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse()
            throws Exception {
        Reference uri = getHTTPURI(InformationResource.getURIPath() +
                "/rgb-64x56x8-baseline.jpg/info.json");
        tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse(
                uri, new Identifier("rgb-64x56x8-baseline.jpg"));
    }

    @Test
    void doGETPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue()
            throws Exception {
        Reference uri = getHTTPURI(InformationResource.getURIPath() +
                "/rgb-64x56x8-baseline.jpg/info.json");
        tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue(
                uri, new Identifier("rgb-64x56x8-baseline.jpg"));
    }

    @Test
    void doGETRecoveryFromIncorrectSourceFormat() throws Exception {
        Reference uri = getHTTPURI(InformationResource.getURIPath() +
                "/sample-images%2Fjpg-incorrect-extension.png/info.json");
        tester.testRecoveryFromIncorrectSourceFormat(uri);
    }

    /**
     * Tests that a scale constraint of {@literal 1:1} is redirected to no
     * scale constraint.
     */
    @Test
    void doGETRedirectToNormalizedScaleConstraint1() {
        MetaIdentifier metaIdentifier = MetaIdentifier.builder()
                .withIdentifier(ENCODED_IDENTIFIER)
                .withScaleConstraint(1, 1)
                .build();
        String metaIdentifierString = new StandardMetaIdentifierTransformer()
                .serialize(metaIdentifier, false);

        Reference fromURI = getHTTPURI(InformationResource.getURIPath() + "/" +
                metaIdentifierString + "/info.json");
        Reference toURI   = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        assertRedirect(fromURI, toURI, 301);
    }

    /**
     * Tests that a scale constraint of {@literal 2:2} is redirected to no
     * scale constraint.
     */
    @Test
    void doGETRedirectToNormalizedScaleConstraint2() {
        MetaIdentifier metaIdentifier = MetaIdentifier.builder()
                .withIdentifier(ENCODED_IDENTIFIER)
                .withScaleConstraint(2, 2)
                .build();
        String metaIdentifierString = new StandardMetaIdentifierTransformer()
                .serialize(metaIdentifier, false);

        Reference fromURI = getHTTPURI(InformationResource.getURIPath() + "/" +
                metaIdentifierString + "/info.json");
        Reference toURI   = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        assertRedirect(fromURI, toURI, 301);
    }

    /**
     * Tests that a scale constraint of {@literal 2:4} is redirected to
     * {@literal 1:2}.
     */
    @Test
    void doGETRedirectToNormalizedScaleConstraint3() {
        final Delegate delegate       = TestUtils.newDelegate();
        // build the "from" URI
        MetaIdentifier metaIdentifier = MetaIdentifier.builder()
                .withIdentifier(DECODED_IDENTIFIER)
                .withScaleConstraint(2, 4)
                .build();
        String metaIdentifierString = metaIdentifier.forURI(delegate);
        Reference fromURI = getHTTPURI(InformationResource.getURIPath() + "/" +
                metaIdentifierString + "/info.json");

        // build the "to" URI
        metaIdentifier = MetaIdentifier.builder()
                .withIdentifier(DECODED_IDENTIFIER)
                .withScaleConstraint(1, 2)
                .build();
        metaIdentifierString = metaIdentifier.forURI(delegate);
        Reference toURI = getHTTPURI(InformationResource.getURIPath() + "/" +
                metaIdentifierString + "/info.json");
        assertRedirect(fromURI, toURI, 301);
    }

    @Test
    void doGETScaleConstraintIsRespected() throws Exception {
        client = newClient(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + ";1:2/info.json");
        try (Response response = client.send()) {
            String json = response.getBodyAsString();
            ObjectMapper mapper = new ObjectMapper();
            Map<?, ?> info = mapper.readValue(json, Map.class);
            assertEquals(32, info.get("width"));
            assertEquals(28, info.get("height"));
        }
    }

    @Test
    void doGETSlashSubstitution() {
        Reference uri = getHTTPURI(InformationResource.getURIPath() +
                "/sample-imagesCATSjpgCATSjpg.jpg/info.json");
        tester.testSlashSubstitution(uri);
    }

    @Test
    void doGETUnavailableSourceFormat() {
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/text.txt/info.json");
        tester.testUnavailableSourceFormat(uri);
    }

    @Test
    void doGETURIsInJSON() throws Exception {
        client = newClient(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        try (Response response = client.send()) {
            String json = response.getBodyAsString();
            ObjectMapper mapper = new ObjectMapper();
            Map<?, ?> info = mapper.readValue(json, Map.class);
            assertEquals("http://localhost:" + getHTTPPort() +
                    InformationResource.getURIPath() + "/" + ENCODED_IDENTIFIER,
                    info.get("@id"));
        }
    }

    @Test
    void doGETURIsInJSONWithSlashSubstitution() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.SLASH_SUBSTITUTE, "CATS");

        final String path = InformationResource.getURIPath() +
                "/sample-imagesCATSjpgCATSjpg.jpg";
        client = newClient(path + "/info.json");

        try (Response response = client.send()) {
            String json = response.getBodyAsString();
            ObjectMapper mapper = new ObjectMapper();
            Map<?, ?> info = mapper.readValue(json, Map.class);
            assertEquals("http://localhost:" + getHTTPPort() + path,
                    info.get("@id"));
        }
    }

    @Test
    void doGETURIsInJSONWithEncodedCharacters() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.SLASH_SUBSTITUTE, "`");

        final String path = InformationResource.getURIPath() +
                "/sample-images%60jpg%60jpg.jpg";
        client = newClient(path + "/info.json");

        try (Response response = client.send()) {
            String json = response.getBodyAsString();
            ObjectMapper mapper = new ObjectMapper();
            Map<?, ?> info = mapper.readValue(json, Map.class);
            assertEquals("http://localhost:" + getHTTPPort() + path,
                    info.get("@id"));
        }
    }

    @Test
    void doGETURIsInJSONRespectEndpointPathConfigKey() throws Exception {
        final String endpointPath = "/stuff";
        Configuration.forApplication()
                .setProperty(Key.IIIF_2_ENDPOINT_PATH, endpointPath);

        String path = IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/info.json";
        assertTrue(path.startsWith(endpointPath));

        client = newClient(path);
        try (Response response = client.send()) {
            String json            = response.getBodyAsString();
            ObjectMapper mapper    = new ObjectMapper();
            Map<?, ?> info = mapper.readValue(json, Map.class);
            String uri             = client.getURI().rebuilder()
                    .withPath(IIIF2Resource.getURIPath())
                    .appendPath(ENCODED_IDENTIFIER)
                    .build().toString();
            assertEquals(uri, info.get("@id"));
        }
    }

    @Test
    void doGETURIsInJSONRespectBaseURIConfigKey() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.BASE_URI, "http://example.org/");

        client = newClient(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        try (Response response = client.send()) {
            String json = response.getBodyAsString();
            ObjectMapper mapper = new ObjectMapper();
            Map<?, ?> info = mapper.readValue(json, Map.class);
            assertEquals("http://example.org" +
                    InformationResource.getURIPath() + "/" + ENCODED_IDENTIFIER,
                    info.get("@id"));
        }
    }

    @Test
    void doGETURIsInJSONRespectXForwardedHeaders() throws Exception {
        client = newClient(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        client.getHeaders().set("X-Forwarded-Proto", "HTTP");
        client.getHeaders().set("X-Forwarded-Host", "example.org");
        client.getHeaders().set("X-Forwarded-Port", "8080");
        client.getHeaders().set("X-Forwarded-BasePath", "/cats");
        client.getHeaders().set(
                AbstractResource.PUBLIC_IDENTIFIER_HEADER, "originalID");

        try (Response response = client.send()) {
            String json = response.getBodyAsString();
            ObjectMapper mapper = new ObjectMapper();
            Map<?, ?> info = mapper.readValue(json, Map.class);
            assertEquals("http://example.org:8080/cats" +
                    InformationResource.getURIPath() + "/originalID", info.get("@id"));
        }
    }

    @Test
    void doGETBaseURIOverridesXForwardedHeaders() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.BASE_URI, "https://example.net/");

        client = newClient(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        client.getHeaders().set("X-Forwarded-Proto", "HTTP");
        client.getHeaders().set("X-Forwarded-Host", "example.org");
        client.getHeaders().set("X-Forwarded-Port", "8080");
        client.getHeaders().set("X-Forwarded-BasePath", "/cats");

        try (Response response = client.send()) {
            String json = response.getBodyAsString();
            ObjectMapper mapper = new ObjectMapper();
            Map<?, ?> info = mapper.readValue(json, Map.class);
            assertEquals("https://example.net" +
                    InformationResource.getURIPath() + "/" + ENCODED_IDENTIFIER, info.get("@id"));
        }
    }

    /**
     * Tests the default response headers. Individual headers may be tested
     * more thoroughly elsewhere.
     */
    @Test
    void doGETResponseHeaders() throws Exception {
        client = newClient(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        try (Response response = client.send()) {
            Headers headers = response.getHeaders();
            assertEquals(8, headers.size());
            // Access-Control-Allow-Headers
            assertEquals("Authorization",
                    headers.getFirstValue("Access-Control-Allow-Headers"));
            // Access-Control-Allow-Origin
            assertEquals("*", headers.getFirstValue("Access-Control-Allow-Origin"));
            // Content-Type
            assertTrue("application/json;charset=UTF-8".equalsIgnoreCase(
                    headers.getFirstValue("Content-Type")));
            // Date
            assertNotNull(headers.getFirstValue("Date"));
            // Last-Modified
            assertNotNull(headers.getFirstValue("Last-Modified"));
            // Vary
            List<String> parts =
                    List.of(headers.getFirstValue("Vary").split(", "));
            assertEquals(5, parts.size());
            assertTrue(parts.contains("Accept"));
            assertTrue(parts.contains("Accept-Charset"));
            assertTrue(parts.contains("Accept-Encoding"));
            assertTrue(parts.contains("Accept-Language"));
            assertTrue(parts.contains("Origin"));
            // X-Powered-By
            assertEquals(Application.getName() + "/" + Application.getVersion(),
                    headers.getFirstValue("X-Powered-By"));
        }
    }

    @Test
    void doGETLastModifiedResponseHeaderWhenInfoCacheIsEnabledAndNotResolvingFirst()
            throws Exception {
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        tester.testLastModifiedHeaderWhenInfoCacheIsEnabledAndNotResolvingFirst(uri);
    }

    @Test
    void doGETLastModifiedResponseHeaderWhenInfoCacheIsDisabled()
            throws Exception {
        Reference uri = getHTTPURI(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        tester.testLastModifiedHeaderWhenInfoCacheIsDisabled(uri, DECODED_IDENTIFIER);
    }

    @Test
    void doOPTIONSWhenEnabled() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, true);

        client = newClient(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        client.setMethod(Method.OPTIONS);

        try (Response response = client.send()) {
            assertEquals(Status.NO_CONTENT, response.getStatus());

            Headers headers = response.getHeaders();
            List<String> methods =
                    List.of(headers.getFirstValue("Allow").split(","));
            assertEquals(2, methods.size());
            assertTrue(methods.contains("GET"));
            assertTrue(methods.contains("OPTIONS"));
        }
    }

    @Test
    void doOPTIONSWhenDisabled() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, false);

        client = newClient(InformationResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json");
        client.setMethod(Method.OPTIONS);

        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(Status.NOT_FOUND, e.getStatus());
        }
    }

}
