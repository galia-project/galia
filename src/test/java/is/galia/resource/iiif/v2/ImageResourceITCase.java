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

import is.galia.delegate.Delegate;
import is.galia.http.Status;
import org.junit.jupiter.api.Test;
import is.galia.Application;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Headers;
import is.galia.http.Method;
import is.galia.http.Reference;
import is.galia.http.HTTPException;
import is.galia.http.Response;
import is.galia.image.MetaIdentifier;
import is.galia.image.StandardMetaIdentifierTransformer;
import is.galia.operation.OperationList;
import is.galia.resource.ResourceITCase;
import is.galia.resource.iiif.ImageResourceTester;
import is.galia.test.TestUtils;

import java.nio.file.Path;
import java.util.List;

import static is.galia.test.Assert.HTTPAssert.*;
import static org.junit.jupiter.api.Assertions.*;

class ImageResourceITCase extends ResourceITCase {

    private static final String FILENAME = "rgb-64x56x8-baseline.jpg";
    private static final String DECODED_IDENTIFIER =
            "sample-images/jpg/" + FILENAME;
    private static final String ENCODED_IDENTIFIER =
            Reference.encode(DECODED_IDENTIFIER);
    private static final Path IMAGE_PATH =
            TestUtils.getSampleImage("jpg/rgb-64x56x8-baseline.jpg");

    private final ImageResourceTester tester = new ImageResourceTester();

    @Test
    void doGETAuthorizationWhenAuthorized() {
        Reference uri = getHTTPURI(
                IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER + "/full/full/0/color.jpg");
        tester.testAuthorizationWhenAuthorized(uri);
    }

    @Test
    void doGETAuthorizationWhenUnauthorized() {
        Reference uri = getHTTPURI(
                IIIF2Resource.getURIPath() + "/unauthorized.jpg/full/full/0/color.jpg");
        tester.testAuthorizationWhenUnauthorized(uri);
    }

    @Test
    void doGETAuthorizationWhenForbidden() {
        Reference uri = getHTTPURI(
                IIIF2Resource.getURIPath() + "/forbidden.jpg/full/full/0/color.jpg");
        tester.testAuthorizationWhenForbidden(uri);
    }

    @Test
    void doGETAuthorizationWhenNotAuthorizedWhenAccessingCachedResource()
            throws Exception {
        Reference uri = getHTTPURI(
                IIIF2Resource.getURIPath() + "/forbidden.jpg/full/full/0/color.jpg");
        tester.testAuthorizationWhenNotAuthorizedWhenAccessingCachedResource(uri);
    }

    @Test
    void doGETAuthorizationWhenRedirecting() throws Exception {
        Reference uri = getHTTPURI(
                IIIF2Resource.getURIPath() + "/redirect.jpg/full/full/0/color.jpg");
        tester.testAuthorizationWhenRedirecting(uri);
    }

    @Test
    void doGETAuthorizationWhenScaleConstraining() throws Exception {
        Reference uri = getHTTPURI(
                IIIF2Resource.getURIPath() + "/reduce.jpg/full/full/0/color.jpg");
        tester.testAuthorizationWhenScaleConstraining(uri);
    }

    @Test
    void doGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable()
            throws Exception {
        Reference uri = getHTTPURI(
                IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER + "/full/full/0/color.jpg");
        tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable(uri);
    }

    @Test
    void doGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable()
            throws Exception {
        Reference uri = getHTTPURI(
                IIIF2Resource.getURIPath() + "/bogus/full/full/0/color.jpg");
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
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.jpg?cache=nocache");
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
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.jpg?cache=false");
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
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.jpg?cache=recache");
        tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable(uri);
    }

    @Test
    void doGETCacheHeadersWhenClientCachingIsDisabled() throws Exception {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.jpg");
        tester.testCacheHeadersWhenClientCachingIsDisabled(uri);
    }

    @Test
    void doGETCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied1()
            throws Exception {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.png?cache=nocache");
        tester.testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(uri);
    }

    @Test
    void doGETCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied2()
            throws Exception {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.png?cache=false");
        tester.testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(uri);
    }

    @Test
    void doGETCachingWhenCachesAreEnabledAndRecacheQueryArgumentIsSupplied()
            throws Exception {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.png?cache=recache");
        tester.testCachingWhenCachesAreEnabledAndRecacheQueryArgumentIsSupplied(uri);
    }

    @Test
    void doGETCacheWithCachesEnabledAndResolveFirstEnabled() throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/1/color.jpg");
        tester.testCacheWithCachesEnabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithCachesEnabledAndResolveFirstDisabled() throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/1/color.jpg");
        tester.testCacheWithCachesEnabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheDisabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/1/color.jpg");
        tester.testCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheDisabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheEnabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/1/color.jpg");
        tester.testCacheWithVariantCacheEnabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheDisabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/1/color.jpg");
        tester.testCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheDisabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/1/color.jpg");
        tester.testCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheEnabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/1/color.jpg");
        tester.testCacheWithVariantCacheEnabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/1/color.jpg");
        tester.testCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithCachesDisabledAndResolveFirstDisabled() throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/1/color.jpg");
        tester.testCacheWithCachesDisabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithCachesDisabledAndResolveFirstEnabled() throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/1/color.jpg");
        tester.testCacheWithCachesDisabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheEnabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/1/color.jpg");
        tester.testCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheEnabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheDisabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/1/color.jpg");
        tester.testCacheWithVariantCacheDisabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheEnabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/1/color.jpg");
        tester.testCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheEnabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/1/color.jpg");
        tester.testCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheDisabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/1/color.jpg");
        tester.testCacheWithVariantCacheDisabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/1/color.jpg");
        tester.testCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETContentDispositionHeaderWithNoHeader() throws Exception {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.jpg");
        tester.testContentDispositionHeaderWithNoHeader(uri);
    }

    @Test
    void doGETContentDispositionHeaderSetToInline() throws Exception {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.jpg?response-content-disposition=inline");
        tester.testContentDispositionHeaderSetToInline(uri, DECODED_IDENTIFIER);
    }

    @Test
    void doGETContentDispositionHeaderSetToAttachment() throws Exception {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER +
                "/full/full/0/color.jpg?response-content-disposition=attachment");
        tester.testContentDispositionHeaderSetToAttachment(uri, DECODED_IDENTIFIER);
    }

    @Test
    void doGETContentDispositionHeaderSetToAttachmentWithFilename()
            throws Exception {
        final String filename = "cats%20dogs.jpg";
        final String expected = "cats dogs.jpg";

        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.jpg?response-content-disposition=attachment;filename%3D%22" + filename + "%22");
        tester.testContentDispositionHeaderSetToAttachmentWithFilename(uri, expected);

        uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/color.jpg?response-content-disposition=attachment;%20filename%3D%22" + filename + "%22");
        tester.testContentDispositionHeaderSetToAttachmentWithFilename(uri, expected);

        uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/color.jpg?response-content-disposition=attachment;filename%3D" + filename);
        tester.testContentDispositionHeaderSetToAttachmentWithFilename(uri, expected);

        uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/color.jpg?response-content-disposition=attachment;%20filename%3D" + filename);
        tester.testContentDispositionHeaderSetToAttachmentWithFilename(uri, expected);
    }

    @Test
    void doGETDelegateContext() throws Exception {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() +
                "/delegate-context-image-test.jpg/full/30,/0/default.jpg");
        tester.testDelegateContext(uri);
    }

    @Test
    void doGETEndpointEnabled() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, true);

        assertStatus(200, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.jpg"));
    }

    @Test
    void doGETEndpointDisabled() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, false);

        assertStatus(404, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.jpg"));
    }

    @Test
    void doGETWithForwardSlashInIdentifier() {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() +
                "/sample-images%2Fjpg%2Fjpg.jpg/full/max/0/default.jpg");
        tester.testForwardSlashInIdentifier(uri);
    }

    @Test
    void doGETWithBackslashInIdentifier() {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() +
                "/sample-images%5Cjpg%5Cjpg.jpg/full/max/0/default.jpg");
        tester.testBackslashInIdentifier(uri);
    }

    @Test
    void doGETWithIllegalCharactersInIdentifier() {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() +
                "/[bogus]/full/full/0/default.jpg");
        tester.testIllegalCharactersInIdentifier(uri);
    }

    @Test
    void doGETHTTP2() throws Exception {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.jpg");
        tester.testHTTP2(uri);
    }

    @Test
    void doGETHTTPS1_1() throws Exception {
        Reference uri = getHTTPSURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.jpg");
        tester.testHTTPS1_1(uri);
    }

    @Test
    void doGETHTTPS2() throws Exception {
        Reference uri = getHTTPSURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.jpg");
        tester.testHTTPS2(uri);
    }

    @Test
    void doGETLinkHeader() throws Exception {
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/color.jpg");
        try (Response response = client.send()) {
            String value = response.getHeaders().getFirstValue("Link");
            assertTrue(value.startsWith("<http://localhost"));
        }
    }

    @Test
    void doGETLinkHeaderWithSlashSubstitution() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.SLASH_SUBSTITUTE, "CATS");

        client = newClient(IIIF2Resource.getURIPath() +
                "/sample-imagesCATSjpgCATSjpg.jpg/full/full/0/color.jpg");
        try (Response response = client.send()) {
            String value = response.getHeaders().getFirstValue("Link");
            assertTrue(value.contains("sample-imagesCATSjpgCATSjpg.jpg"));
        }
    }

    @Test
    void doGETLinkHeaderWithEncodedCharacters() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.SLASH_SUBSTITUTE, "`");

        client = newClient(IIIF2Resource.getURIPath() +
                "/sample-images%60jpg%60jpg.jpg/full/full/0/color.jpg");
        try (Response response = client.send()) {
            String value = response.getHeaders().getFirstValue("Link");
            assertTrue(value.contains("sample-images%60jpg%60jpg.jpg"));
        }
    }

    @Test
    void doGETLinkHeaderRespectsEndpointPathConfigKey() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.IIIF_2_ENDPOINT_PATH, "/base");

        client = newClient(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/max/0/color.jpg");
        try (Response response = client.send()) {
            String expectedURI = client.getURI().rebuilder()
                    .withPath("/base")
                    .appendPath(ENCODED_IDENTIFIER)
                    .appendPath("full")
                    .appendPath("max")
                    .appendPath("0")
                    .appendPath("color.jpg")
                    .build().toString();
            assertEquals("<" + expectedURI + ">;rel=\"canonical\"",
                    response.getHeaders().getFirstValue("Link"));
        }
    }

    @Test
    void doGETLinkHeaderRespectsBaseURIConfigKey() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.BASE_URI, "http://example.org/");

        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/color.jpg");
        try (Response response = client.send()) {
            String value = response.getHeaders().getFirstValue("Link");
            assertTrue(value.startsWith("<http://example.org/"));
        }
    }

    @Test
    void doGETLinkHeaderRespectsXForwardedHeaders() throws Exception {
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/pct:50,50,50,50/,35/0/color.jpg");
        client.getHeaders().set("X-Forwarded-Proto", "HTTP");
        client.getHeaders().set("X-Forwarded-Host", "example.org");
        client.getHeaders().set("X-Forwarded-Port", "8080");
        client.getHeaders().set("X-Forwarded-BasePath", "/cats");

        try (Response response = client.send()) {
            assertEquals("<http://example.org:8080/cats/iiif/2/" +
                            ENCODED_IDENTIFIER +
                            "/32,28,32,28/40,/0/color.jpg>;rel=\"canonical\"",
                    response.getHeaders().getFirstValue("Link"));
        }
    }

    @Test
    void doGETLinkHeaderBaseURIOverridesXForwardedHeaders() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.BASE_URI, "https://example.net/");

        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/color.jpg");
        client.getHeaders().set("X-Forwarded-Proto", "HTTP");
        client.getHeaders().set("X-Forwarded-Host", "example.org");
        client.getHeaders().set("X-Forwarded-Port", "8080");
        client.getHeaders().set("X-Forwarded-BasePath", "/cats");

        try (Response response = client.send()) {
            assertEquals("<https://example.net/iiif/2/" + ENCODED_IDENTIFIER +
                            "/full/full/0/color.jpg>;rel=\"canonical\"",
                    response.getHeaders().getFirstValue("Link"));
        }
    }

    @Test
    void doGETLessThanOrEqualToFullScale() {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.png");
        tester.testLessThanOrEqualToMaxScale(uri);
    }

    @Test
    void doGETGreaterThanFullScale() {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/pct:101/0/color.png");
        tester.testGreaterThanMaxScale(uri, 403);
    }

    @Test
    void doGETMinPixels() {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/0,0,0,0/full/0/color.png");
        tester.testMinPixels(uri);
    }

    @Test
    void doGETLessThanMaxPixels() {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.png");
        tester.testLessThanMaxPixels(uri);
    }

    @Test
    void doGETMoreThanMaxPixelsWithFullSizeArgument() {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.png");
        tester.testForbiddingMoreThanMaxPixels(uri);
    }

    @Test
    void doGETMoreThanMaxPixelsWithMaxSizeArgument() throws Exception {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/max/0/color.png");
        tester.testDownscalingToMaxPixels(uri, 64, 56, 1000);
    }

    @Test
    void doGETForbidden() {
        Reference uri = getHTTPURI(
                IIIF2Resource.getURIPath() + "/forbidden.jpg/full/full/0/color.jpg");
        tester.testForbidden(uri);
    }

    @Test
    void doGETNotFound() {
        Reference uri = getHTTPURI(
                IIIF2Resource.getURIPath() + "/invalid/full/full/0/color.jpg");
        tester.testNotFound(uri);
    }

    @Test
    void doGETWithPageNumberInMetaIdentifier() {
        final String identifier = "sample-images%2Ftif%2Fmultipage.tif";
        Reference uri1 = getHTTPURI(
                IIIF2Resource.getURIPath() + "/" + identifier + "/full/max/0/color.jpg");
        Reference uri2 = getHTTPURI(
                IIIF2Resource.getURIPath() + "/" + identifier + ";2/full/max/0/color.jpg");
        assertRepresentationsNotSame(uri1, uri2);
    }

    @Test
    void doGETWithInvalidPageNumber() {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() +
                "/sample-images%2Ftif%2Fmultipage.tif;999999/full/full/0/color.jpg");
        tester.testInvalidPageNumber(uri);
    }

    @Test
    void doGETPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse()
            throws Exception {
        Delegate delegate    = TestUtils.newDelegate();
        String imagePath     = "/" + FILENAME + "/full/full/0/color.jpg";
        OperationList opList = Parameters.fromURI(imagePath)
                .toOperationList(delegate);
        Reference uri        = getHTTPURI(IIIF2Resource.getURIPath() + imagePath);
        tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse(
                uri, opList);
    }

    @Test
    void doGETPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue()
            throws Exception {
        Delegate delegate    = TestUtils.newDelegate();
        String imagePath     = "/" + FILENAME + "/full/full/0/color.jpg";
        OperationList opList = Parameters.fromURI(imagePath)
                .toOperationList(delegate);
        Reference uri        = getHTTPURI(IIIF2Resource.getURIPath() + imagePath);
        tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue(
                uri, opList);
    }

    @Test
    void doGETRecoveryFromVariantCacheNewVariantImageInputStreamException()
            throws Exception {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.jpg");
        tester.testRecoveryFromVariantCacheNewVariantImageInputStreamException(uri);
    }

    @Test
    void doGETRecoveryFromVariantCacheNewVariantImageOutputStreamException()
            throws Exception {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.png");
        tester.testRecoveryFromVariantCacheNewVariantImageOutputStreamException(uri);
    }

    @Test
    void doGETRecoveryFromIncorrectSourceFormat() throws Exception {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() +
                "/sample-images%2Fjpg-incorrect-extension.png/full/full/0/color.jpg");
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

        Reference fromURI = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                metaIdentifierString + "/full/full/0/color.png");
        Reference toURI   = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.png");
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
                .withScaleConstraint(1, 1)
                .build();
        String metaIdentifierString = new StandardMetaIdentifierTransformer()
                .serialize(metaIdentifier, false);

        Reference fromURI = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                metaIdentifierString + "/full/full/0/color.png");
        Reference toURI   = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.png");
        assertRedirect(fromURI, toURI, 301);
    }

    /**
     * Tests that a scale constraint of {@literal 2:4} is redirected to
     * {@literal 1:2}.
     */
    @Test
    void doGETRedirectToNormalizedScaleConstraint3() {
        final MetaIdentifier.Builder builder = MetaIdentifier.builder()
                .withIdentifier(DECODED_IDENTIFIER);
        final Delegate delegate       = TestUtils.newDelegate();
        // create the "from" URI
        MetaIdentifier metaIdentifier = builder.withScaleConstraint(2, 4).build();
        String metaIdentifierString   = metaIdentifier.forURI(delegate);
        Reference fromURI = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                metaIdentifierString + "/full/full/0/color.png");

        // create the "to" URI
        metaIdentifier       = builder.withScaleConstraint(1, 2).build();
        metaIdentifierString = metaIdentifier.forURI(delegate);
        Reference toURI      = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                metaIdentifierString + "/full/full/0/color.png");

        assertRedirect(fromURI, toURI, 301);
    }

    @Test
    void doGETScaleConstraintIsRespected() throws Exception {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + ";1:2/full/full/0/color.jpg");
        tester.testDimensions(uri, 32, 28);
    }

    @Test
    void doGETNotRestrictedToSizes() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.IIIF_RESTRICT_TO_SIZES, false);

        assertStatus(200, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/53,37/0/color.jpg"));
    }

    @Test
    void doGETRestrictedToSizes() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.IIIF_RESTRICT_TO_SIZES, true);

        assertStatus(403, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/53,37/0/color.jpg"));
    }

    @Test
    void doGETSlashSubstitution() {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() +
                "/sample-imagesCATSjpgCATSjpg.jpg/full/full/0/color.jpg");
        tester.testSlashSubstitution(uri);
    }

    @Test
    void doGETUnavailableSourceFormat() {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() +
                "/text.txt/full/full/0/color.jpg");
        tester.testUnavailableSourceFormat(uri);
    }

    @Test
    void doGETInvalidOutputFormat() {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.bogus");
        tester.testInvalidOutputFormat(uri);
    }

    @Test
    void doGETUnsupportedOutputFormat() {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.pdf");
        tester.testUnsupportedOutputFormat(uri);
    }

    /**
     * Tests the default response headers. Individual headers may be tested
     * more thoroughly elsewhere.
     */
    @Test
    void doGETResponseHeaders() throws Exception {
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/color.jpg");
        try (Response response = client.send()) {
            Headers headers = response.getHeaders();
            assertEquals(9, headers.size());
            // Access-Control-Allow-Headers
            assertEquals("Authorization",
                    headers.getFirstValue("Access-Control-Allow-Headers"));
            // Access-Control-Allow-Origin
            assertEquals("*", headers.getFirstValue("Access-Control-Allow-Origin"));
            // Content-Type
            assertEquals("image/jpeg", headers.getFirstValue("Content-Type"));
            // Date
            assertNotNull(headers.getFirstValue("Date"));
            // Last-Modified
            assertNotNull(headers.getFirstValue("Last-Modified"));
            // Link
            assertTrue(headers.getFirstValue("Link").contains("://"));
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
    void doGETLastModifiedResponseHeaderWhenVariantCacheIsEnabledAndNotResolvingFirst()
            throws Exception {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/max/0/color.jpg");
        tester.testLastModifiedHeaderWhenVariantCacheIsEnabledAndNotResolvingFirst(uri);
    }

    @Test
    void doGETLastModifiedResponseHeaderWhenVariantCacheIsDisabled()
            throws Exception {
        Reference uri = getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/max/0/color.jpg");
        tester.testLastModifiedHeaderWhenVariantCacheIsDisabled(uri);
    }

    @Test
    void doOPTIONSWhenEnabled() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, true);

        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/color.jpg");
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

        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/color.jpg");
        client.setMethod(Method.OPTIONS);
        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(Status.NOT_FOUND, e.getStatus());
        }
    }

}
