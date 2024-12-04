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

package is.galia.resource.iiif.v1;

import is.galia.codec.Decoder;
import is.galia.codec.DecoderFactory;
import is.galia.codec.Encoder;
import is.galia.codec.EncoderFactory;
import is.galia.codec.MockEncoderPlugin;
import is.galia.http.Reference;
import is.galia.http.HTTPException;
import is.galia.http.Response;
import is.galia.http.Status;
import is.galia.image.Format;
import is.galia.image.MediaType;
import is.galia.processor.Processor;
import is.galia.processor.ProcessorFactory;
import is.galia.resource.ResourceITCase;
import is.galia.test.TestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static is.galia.test.Assert.HTTPAssert.*;

/**
 * <p>Functional test of conformance to the IIIF Image API 1.1 spec. Methods
 * are implemented in the order of the assertions in the spec document.</p>
 *
 * @see <a href="http://iiif.io/api/image/1.1/#image-info-request">IIIF Image
 * API 1.1</a>
 */
class API1_1ConformanceITCase extends ResourceITCase {

    private static final String DECODED_IDENTIFIER =
            "sample-images/jpg/rgb-64x56x8-baseline.jpg";
    private static final String ENCODED_IDENTIFIER =
            Reference.encode(DECODED_IDENTIFIER);

    /**
     * 2.2. "It is recommended that if the image’s base URI is dereferenced,
     * then the client should either redirect to the information request using
     * a 303 status code (see Section 6.1), or return the same result."
     */
    @Test
    void baseURIReturnsImageInfoViaHttp303() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER);
        try (Response response = client.send()) {
            assertEquals(Status.SEE_OTHER, response.getStatus());
            assertEquals(
                    getHTTPURI(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER + "/info.json").toString(),
                    response.getHeaders().getFirstValue("Location"));
        }
    }

    /**
     * 3. "the identifier MUST be expressed as a string. All special characters
     * (e.g. ? or #) MUST be URI encoded to avoid unpredictable client
     * behaviors. The URL syntax relies upon slash (/) separators so any
     * slashes in the identifier MUST be URI encoded (aka. percent-encoded,
     * replace / with %2F )."
     */
    @Test
    void identifierWithEncodedCharacters() {
        assertTrue(ENCODED_IDENTIFIER.contains("%"));
        // image endpoint
        assertStatus(200, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/native.jpg"));
        // information endpoint
        assertStatus(200, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json"));
    }

    /**
     * 4.1
     */
    @Test
    void fullRegion() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/native.jpg");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());

            try (InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(64, image.getWidth());
                assertEquals(56, image.getHeight());
            }
        }
    }

    /**
     * 4.1
     */
    @Test
    void absolutePixelRegion() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/20,20,100,100/full/0/color.jpg");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());

            try (InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(44, image.getWidth());
                assertEquals(36, image.getHeight());
            }
        }
    }

    /**
     * 4.1
     */
    @Test
    void percentageRegionWithIntegers() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/pct:20,20,50,50/full/0/color.jpg");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());

            try (InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(32, image.getWidth());
                assertEquals(28, image.getHeight());
            }
        }
    }

    /**
     * 4.1
     */
    @Test
    void percentageRegionWithFloats() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/pct:20.2,20.6,50.2,50.6/full/0/color.jpg");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());

            try (InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(32, image.getWidth());
                assertEquals(28, image.getHeight());
            }
        }
    }

    /**
     * 4.1. "If the request specifies a region which extends beyond the
     * dimensions of the source image, then the service should return an image
     * cropped at the boundary of the source image."
     */
    @Test
    void absolutePixelRegionLargerThanSource() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/0,0,99999,99999/full/0/color.jpg");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());

            try (InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(64, image.getWidth());
                assertEquals(56, image.getHeight());
            }
        }
    }

    /**
     * 4.1. "If the requested region's height or width is zero ... then the
     * server MUST return a 400 (bad request) status code."
     */
    @Test
    void zeroRegion() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/0,0,0,0/full/0/native.jpg");
        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(400, e.getStatus().code());
        }
    }

    /**
     * 4.1. "If the region is entirely outside the bounds of the source image,
     * then the server MUST return a 400 (bad request) status code."
     */
    @Test
    void originOutOfBounds() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/99999,99999,50,50/full/0/native.jpg");
        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(400, e.getStatus().code());
        }
    }

    /**
     * 4.2
     */
    @Test
    void fullSize() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/color.jpg");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());

            try (InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(64, image.getWidth());
                assertEquals(56, image.getHeight());
            }
        }
    }

    /**
     * 4.2. "The extracted region should be scaled so that its width is
     * exactly equal to w, and the height will be a calculated value that
     * maintains the aspect ratio of the requested region."
     */
    @Test
    void sizeScaledToFitWidth() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/50,/0/color.jpg");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());

            try (InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(50, image.getWidth());
                assertEquals(44, image.getHeight());
            }
        }
    }

    /**
     * 4.2. "The extracted region should be scaled so that its height is
     * exactly equal to h, and the width will be a calculated value that
     * maintains the aspect ratio of the requested region."
     */
    @Test
    void sizeScaledToFitHeight() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/,50/0/color.jpg");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());

            try (InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(57, image.getWidth());
                assertEquals(50, image.getHeight());
            }
        }
    }

    /**
     * 4.2. "The width and height of the returned image is scaled to n% of the
     * width and height of the extracted region. The aspect ratio of the
     * returned image is the same as that of the extracted region."
     */
    @Test
    void sizeScaledToPercent() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/pct:50/0/color.jpg");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());

            try (InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(32, image.getWidth());
                assertEquals(28, image.getHeight());
            }
        }
    }

    /**
     * 4.2. "The width and height of the returned image are exactly w and h.
     * The aspect ratio of the returned image MAY be different than the
     * extracted region, resulting in a distorted image."
     */
    @Test
    void absoluteWidthAndHeight() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/50,50/0/color.jpg");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());

            try (InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(50, image.getWidth());
                assertEquals(50, image.getHeight());
            }
        }
    }

    /**
     * 4.2. "The image content is scaled for the best fit such that the
     * resulting width and height are less than or equal to the requested width
     * and height. The exact scaling MAY be determined by the service provider,
     * based on characteristics including image quality and system performance.
     * The dimensions of the returned image content are calculated to maintain
     * the aspect ratio of the extracted region."
     */
    @Test
    void sizeScaledToFitInside() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/20,20/0/native.jpg");
        try (Response response = client.send();
             InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(20, image.getWidth());
                assertEquals(20, image.getHeight());
        }
    }

    /**
     * 4.2. "If the resulting height or width is zero, then the server MUST
     * return a 400 (bad request) status code."
     */
    @Test
    void resultingWidthOrHeightIsZero() {
        assertStatus(400, getHTTPURI(
                IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER + "/full/pct:0/15/color.jpg"));
        assertStatus(400, getHTTPURI(
                IIIF1Resource.getURIPath() + "/sample-images%2Fwide.jpg/full/3,0/15/color.jpg"));
    }

    /**
     * IIIF Image API 1.1 doesn't say anything about an invalid size
     * parameter, so we will check for an HTTP 400.
     */
    @Test
    void invalidSize() {
        assertStatus(400, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/cats/0/native.jpg"));
        assertStatus(400, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/cats,50/0/native.jpg"));
        assertStatus(400, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/50,cats/0/native.jpg"));
        assertStatus(400, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/cats,/0/native.jpg"));
        assertStatus(400, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/,cats/0/native.jpg"));
        assertStatus(400, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/!cats,50/0/native.jpg"));
    }

    /**
     * 4.3. "The rotation value represents the number of degrees of clockwise
     * rotation from the original, and may be any floating point number from 0
     * to 360. Initially most services will only support 0, 90, 180 or 270 as
     * valid values."
     */
    @Test
    void rotation() {
        assertStatus(200, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/15.5/color.jpg"));
    }

    /**
     * IIIF Image API 1.1 doesn't say anything about a negative rotation
     * parameter, so we will check for an HTTP 400.
     */
    @Test
    void negativeRotation() {
        assertStatus(400, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/-15/native.jpg"));
    }

    /**
     * IIIF Image API 1.1 doesn't say anything about a >360-degree rotation
     * parameter, so we will check for an HTTP 400.
     */
    @Test
    void greaterThanFullRotation() {
        assertStatus(400, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/4855/native.jpg"));
    }

    /**
     * 4.4. "The image is returned at an unspecified bit-depth."
     */
    @Test
    void nativeQuality() {
        assertStatus(200, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/native.jpg"));
    }

    /**
     * 4.4. "The image is returned in full color, typically using 24 bits per
     * pixel."
     */
    @Test
    void colorQuality() {
        assertStatus(200, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.jpg"));
    }

    /**
     * 4.4. "The image is returned in greyscale, where each pixel is black,
     * white or any degree of grey in between, typically using 8 bits per
     * pixel."
     */
    @Test
    void greyQuality() {
        assertStatus(200, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/grey.jpg"));
    }

    /**
     * 4.4. "The image returned is bitonal, where each pixel is either black or
     * white, using 1 bit per pixel when the format permits."
     */
    @Test
    void bitonalQuality() {
        assertStatus(200, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/bitonal.jpg"));
    }

    /**
     * The IIIF Image API 1.1 doesn't say anything about unsupported qualities,
     * so we will check for an HTTP 400.
     */
    @Test
    void unsupportedQuality() {
        assertStatus(400, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/bogus.jpg"));
    }

    /**
     * 4.5
     */
    @Test
    void formats() throws Exception {
        testFormat(Format.get("jpg"));
        testFormat(Format.get("tif"));
        testFormat(Format.get("png"));
        testFormat(Format.get("gif"));
        testFormat(new Format(
                "jp2", "JPEG2000",
                List.of(new MediaType("image", "jp2")),
                List.of("jp2"),
                true, false, true));
        testFormat(new Format(
                "pdf", "PDF",
                List.of(new MediaType("application", "pdf")),
                List.of("pdf"),
                false, false, true));
    }

    private void testFormat(Format outputFormat) throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/native." + outputFormat.getPreferredExtension());
        Format sourceFormat = Format.get("jpg");

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(sourceFormat, arena);
             Encoder encoder = new MockEncoderPlugin()) {
            decoder.setSource(TestUtils.getSampleImage("jpg/jpg"));
            Processor processor = ProcessorFactory.newProcessor();
            processor.setDecoder(decoder);
            processor.setEncoder(encoder);

            if (EncoderFactory.getAllSupportedFormats().contains(outputFormat)) {
                try (Response response = client.send()) {
                    assertEquals(Status.OK, response.getStatus());
                    assertEquals(outputFormat.getPreferredMediaType().toString(),
                            response.getHeaders().getFirstValue("Content-Type"));
                }
            } else {
                try (Response response = client.send()) {
                    fail("Expected exception");
                } catch (HTTPException e) {
                    assertEquals(Status.UNSUPPORTED_MEDIA_TYPE, e.getStatus());
                }
            }
        }
    }

    /**
     * 4.5
     */
    @Test
    void unsupportedFormat() {
        assertStatus(415, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/native.bogus"));
    }

    /**
     * 4.5 "If the format is not specified in the URI, then the server SHOULD
     * use the HTTP Accept header to determine the client’s preferences for the
     * format. The server may either do 200 (return the representation in the
     * response) or 30x (redirect to the correct URI with a format extension)
     * style content negotiation."
     */
    @Test
    void formatInAcceptHeader() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/native");
        client.getHeaders().set("Accept", "image/png");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());
            assertEquals("image/png",
                    response.getHeaders().getFirstValue("Content-Type"));
        }
    }

    /**
     * 4.5 "If neither [format in URL or in Accept header] are given, then the
     * server should use a default format of its own choosing."
     */
    @Test
    void noFormat() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/native");
        client.getHeaders().set("Accept", "*/*");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());
            assertEquals("image/jpeg",
                    response.getHeaders().getFirstValue("Content-Type"));
        }
    }

    /**
     * 5. "The service MUST return technical information about the requested
     * image in the JSON format."
     */
    @Test
    void informationRequest() {
        assertStatus(200, getHTTPURI(IIIF1Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json"));
    }

    /**
     * 5. "The content-type of the response must be either “application/json”,
     * (regular JSON), or “application/ld+json” (JSON-LD)."
     */
    @Test
    void informationRequestContentType() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/info.json");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());
            assertTrue("application/json;charset=utf-8".equalsIgnoreCase(
                    response.getHeaders().getFirstValue("Content-Type").replace(" ", "")));
        }
    }

    /**
     * 5.
     */
    @Test
    void informationRequestJSON() {
        // this will be tested in InformationFactoryTest
    }

    /**
     * 6.2 "Requests are limited to 1024 characters."
     */
    @Test
    void URITooLong() {
        // information endpoint
        String uriStr = IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/info.json?bogus=";
        uriStr = StringUtils.rightPad(uriStr, 1025, "a");
        assertStatus(414, getHTTPURI(uriStr));

        // image endpoint
        uriStr = IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/native.jpg?bogus=";
        uriStr = StringUtils.rightPad(uriStr, 1025, "a");
        assertStatus(414, getHTTPURI(uriStr));
    }

    /**
     * 8. "A service should specify on all responses the extent to which the
     * API is supported. This is done by including an HTTP Link header
     * (RFC5988) entry pointing to the description of the highest level of
     * conformance of which ALL of the requirements are met."
     */
    @Test
    void complianceLevelLinkHeaderInInformationResponse() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER + "/info.json");
        try (Response response = client.send()) {
            assertEquals("<http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2>;rel=\"profile\";",
                    response.getHeaders().getFirstValue("Link"));
        }
    }

    /**
     * 8. "A service should specify on all responses the extent to which the
     * API is supported. This is done by including an HTTP Link header
     * (RFC5988) entry pointing to the description of the highest level of
     * conformance of which ALL of the requirements are met."
     */
    @Test
    void complianceLevelLinkHeaderInImageResponse() throws Exception {
        client = newClient(IIIF1Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/native.jpg");
        try (Response response = client.send()) {
        assertEquals("<http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2>;rel=\"profile\";",
                response.getHeaders().getFirstValue("Link"));
        }
    }

}
