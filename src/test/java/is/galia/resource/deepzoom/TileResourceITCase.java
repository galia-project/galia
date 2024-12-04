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

package is.galia.resource.deepzoom;

import is.galia.delegate.Delegate;
import is.galia.http.Status;
import is.galia.image.Identifier;
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

class TileResourceITCase extends ResourceITCase {

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
                DeepZoomResource.getURIPath() + "/" + ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testAuthorizationWhenAuthorized(uri);
    }

    @Test
    void doGETAuthorizationWhenUnauthorized() {
        Reference uri = getHTTPURI(
                DeepZoomResource.getURIPath() + "/unauthorized.jpg_files/6/0_0.jpg");
        tester.testAuthorizationWhenUnauthorized(uri);
    }

    @Test
    void doGETAuthorizationWhenForbidden() {
        Reference uri = getHTTPURI(
                DeepZoomResource.getURIPath() + "/forbidden.jpg_files/6/0_0.jpg");
        tester.testAuthorizationWhenForbidden(uri);
    }

    @Test
    void doGETAuthorizationWhenNotAuthorizedWhenAccessingCachedResource()
            throws Exception {
        Reference uri = getHTTPURI(
                DeepZoomResource.getURIPath() + "/forbidden.jpg_files/6/0_0.jpg");
        tester.testAuthorizationWhenNotAuthorizedWhenAccessingCachedResource(uri);
    }

    @Test
    void doGETAuthorizationWhenRedirecting() throws Exception {
        Reference uri = getHTTPURI(
                DeepZoomResource.getURIPath() + "/redirect.jpg_files/6/0_0.jpg");
        tester.testAuthorizationWhenRedirecting(uri);
    }

    @Test
    void doGETAuthorizationWhenScaleConstraining() throws Exception {
        Reference uri = getHTTPURI(
                DeepZoomResource.getURIPath() + "/reduce.jpg_files/6/0_0.jpg");
        tester.testAuthorizationWhenScaleConstraining(uri);
    }

    @Test
    void doGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable()
            throws Exception {
        Reference uri = getHTTPURI(
                DeepZoomResource.getURIPath() + "/" + ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable(uri);
    }

    @Test
    void doGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable()
            throws Exception {
        Reference uri = getHTTPURI(
                DeepZoomResource.getURIPath() + "/bogus_files/6/0_0.jpg");
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
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg?cache=nocache");
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
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg?cache=false");
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
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg?cache=recache");
        tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable(uri);
    }

    @Test
    void doGETCacheHeadersWhenClientCachingIsDisabled() throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheHeadersWhenClientCachingIsDisabled(uri);
    }

    @Test
    void doGETCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied1()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg?cache=nocache");
        tester.testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(uri);
    }

    @Test
    void doGETCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied2()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg?cache=false");
        tester.testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(uri);
    }

    @Test
    void doGETCachingWhenCachesAreEnabledAndRecacheQueryArgumentIsSupplied()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg?cache=recache");
        tester.testCachingWhenCachesAreEnabledAndRecacheQueryArgumentIsSupplied(uri);
    }

    @Test
    void doGETCacheWithCachesEnabledAndResolveFirstEnabled() throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheWithCachesEnabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithCachesEnabledAndResolveFirstDisabled() throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheWithCachesEnabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheDisabledAndResolveFirstEnabled()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheDisabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheEnabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstDisabled()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheWithVariantCacheEnabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheDisabledAndResolveFirstDisabled()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheDisabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstDisabled()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheEnabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstEnabled()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheWithVariantCacheEnabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstEnabled()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheWithVariantCacheEnabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithCachesDisabledAndResolveFirstDisabled() throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheWithCachesDisabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithCachesDisabledAndResolveFirstEnabled() throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheWithCachesDisabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheEnabledAndResolveFirstDisabled()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheEnabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheDisabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstEnabled()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheWithVariantCacheDisabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheEnabledAndResolveFirstEnabled()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheEnabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstEnabled()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstEnabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheDisabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstDisabled()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheWithVariantCacheDisabledAndInfoCacheDisabledAndHeapInfoCacheEnabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstDisabled()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testCacheWithVariantCacheDisabledAndInfoCacheEnabledAndHeapInfoCacheDisabledAndResolveFirstDisabled(
                uri, IMAGE_PATH);
    }

    @Test
    void doGETDelegateContext() throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() +
                "/delegate-context-image-test.jpg_files/6/0_0.jpg");
        tester.testDelegateContext(uri);
    }

    @Test
    void doGETEndpointEnabled() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.DEEPZOOM_ENDPOINT_ENABLED, true);

        assertStatus(200, getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg"));
    }

    @Test
    void doGETEndpointDisabled() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.DEEPZOOM_ENDPOINT_ENABLED, false);

        assertStatus(404, getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg"));
    }

    @Test
    void doGETWithForwardSlashInIdentifier() {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() +
                "/sample-images%2Fjpg%2Fjpg.jpg_files/6/0_0.jpg");
        tester.testForwardSlashInIdentifier(uri);
    }

    @Test
    void doGETWithBackslashInIdentifier() {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() +
                "/sample-images%5Cjpg%5Cjpg.jpg_files/6/0_0.jpg");
        tester.testBackslashInIdentifier(uri);
    }

    @Test
    void doGETWithIllegalCharactersInIdentifier() {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() +
                "/[bogus]_files/6/0_0.jpg");
        tester.testIllegalCharactersInIdentifier(uri);
    }

    @Test
    void doGETHTTP2() throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testHTTP2(uri);
    }

    @Test
    void doGETHTTPS1_1() throws Exception {
        Reference uri = getHTTPSURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testHTTPS1_1(uri);
    }

    @Test
    void doGETHTTPS2() throws Exception {
        Reference uri = getHTTPSURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testHTTPS2(uri);
    }

    @Test
    void doGETMinPixels() {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/0/0_0.jpg");
        assertStatus(200, uri);
    }

    @Test
    void doGETForbidden() {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() +
                "/forbidden.jpg_files/6/0_0.jpg");
        tester.testForbidden(uri);
    }

    @Test
    void doGETNotFound() {
        Reference uri = getHTTPURI(
                DeepZoomResource.getURIPath() + "/invalid_files/6/0_0.jpg");
        tester.testNotFound(uri);
    }

    @Test
    void doGETWithPageNumberInMetaIdentifier() {
        final String identifier = "sample-images%2Ftif%2Fmultipage.tif";
        Reference uri1 = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                identifier + "_files/6/0_0.jpg");
        Reference uri2 = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                identifier + ";2_files/6/0_0.jpg");
        assertRepresentationsNotSame(uri1, uri2);
    }

    @Test
    void doGETWithInvalidPageNumber() {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() +
                "/sample-images%2Ftif%2Fmultipage.tif;999999_files/6/0_0.jpg");
        tester.testInvalidPageNumber(uri);
    }

    @Test
    void doGETPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse()
            throws Exception {
        String imagePath     = "/" + FILENAME + "_files/6/0_0.jpg";
        OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier(FILENAME)).build();
        Reference uri        = getHTTPURI(DeepZoomResource.getURIPath() + imagePath);
        tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse(
                uri, opList);
    }

    @Test
    void doGETPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue()
            throws Exception {
        String imagePath     = "/" + FILENAME + "_files/6/0_0.jpg";
        OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier(FILENAME)).build();
        Reference uri        = getHTTPURI(DeepZoomResource.getURIPath() + imagePath);
        tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue(
                uri, opList);
    }

    @Test
    void doGETRecoveryFromVariantCacheNewVariantImageInputStreamException()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testRecoveryFromVariantCacheNewVariantImageInputStreamException(uri);
    }

    @Test
    void doGETRecoveryFromVariantCacheNewVariantImageOutputStreamException()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testRecoveryFromVariantCacheNewVariantImageOutputStreamException(uri);
    }

    @Test
    void doGETRecoveryFromIncorrectSourceFormat() throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() +
                "/sample-images%2Fjpg-incorrect-extension.png_files/6/0_0.jpg");
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

        Reference fromURI = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                metaIdentifierString + "_files/6/0_0.jpg");
        Reference toURI   = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
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

        Reference fromURI = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                metaIdentifierString + "_files/6/0_0.jpg");
        Reference toURI   = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
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
        Reference fromURI = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                metaIdentifierString + "_files/6/0_0.jpg");

        // create the "to" URI
        metaIdentifier       = builder.withScaleConstraint(1, 2).build();
        metaIdentifierString = metaIdentifier.forURI(delegate);
        Reference toURI      = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                metaIdentifierString + "_files/6/0_0.jpg");

        assertRedirect(fromURI, toURI, 301);
    }

    @Test
    void doGETScaleConstraintIsRespected() throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + ";1:2_files/6/0_0.jpg");
        tester.testDimensions(uri, 32, 28);
    }

    @Test
    void doGETSlashSubstitution() {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() +
                "/sample-imagesCATSjpgCATSjpg.jpg_files/6/0_0.jpg");
        tester.testSlashSubstitution(uri);
    }

    @Test
    void doGETUnavailableSourceFormat() {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() +
                "/text.txt_files/6/0_0.jpg");
        tester.testUnavailableSourceFormat(uri);
    }

    @Test
    void doGETInvalidOutputFormat() {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.bogus");
        tester.testInvalidOutputFormat(uri);
    }

    @Test
    void doGETUnsupportedOutputFormat() {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.pdf");
        tester.testUnsupportedOutputFormat(uri);
    }

    @Test
    void doGETOutputFormatDifferentFromConfiguration() {
        Configuration.forApplication().setProperty(Key.DEEPZOOM_FORMAT, "jpg");
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.png");
        assertStatus(415, uri);
    }

    @Test
    void doGETWithInvalidResolutionLevel() {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/10/0_0.jpg");
        assertStatus(404, uri);
    }

    @Test
    void doGETWithOutOfBoundsXOrigin() {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/0/1_0.jpg");
        assertStatus(404, uri);
    }

    @Test
    void doGETWithOutOfBoundsYOrigin() {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/0/0_1.jpg");
        assertStatus(404, uri);
    }

    /**
     * Tests the default response headers. Individual headers may be tested
     * more thoroughly elsewhere.
     */
    @Test
    void doGETResponseHeaders() throws Exception {
        client = newClient(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        try (Response response = client.send()) {
            Headers headers = response.getHeaders();
            assertEquals(7, headers.size());
            // Access-Control-Allow-Origin
            assertEquals("*", headers.getFirstValue("Access-Control-Allow-Origin"));
            // Content-Type
            assertEquals("image/jpeg", headers.getFirstValue("Content-Type"));
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
    void doGETLastModifiedResponseHeaderWhenVariantCacheIsEnabledAndNotResolvingFirst()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testLastModifiedHeaderWhenVariantCacheIsEnabledAndNotResolvingFirst(uri);
    }

    @Test
    void doGETLastModifiedResponseHeaderWhenVariantCacheIsDisabled()
            throws Exception {
        Reference uri = getHTTPURI(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        tester.testLastModifiedHeaderWhenVariantCacheIsDisabled(uri);
    }

    @Test
    void doOPTIONSWhenEnabled() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.DEEPZOOM_ENDPOINT_ENABLED, true);

        client = newClient(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/0/0_0.jpg");
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
        config.setProperty(Key.DEEPZOOM_ENDPOINT_ENABLED, false);

        client = newClient(DeepZoomResource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "_files/6/0_0.jpg");
        client.setMethod(Method.OPTIONS);
        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(Status.NOT_FOUND, e.getStatus());
        }
    }

}
