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
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static is.galia.test.Assert.HTTPAssert.*;

/**
 * <p>Functional test of conformance to the IIIF Image API 2.0 spec. Methods
 * are implemented in the order of the assertions in the spec document.</p>
 *
 * @see <a href="http://iiif.io/api/image/2.0/#image-information">IIIF Image
 * API 2.0</a>
 */
class API2_0ConformanceITCase extends ResourceITCase {

    private static final String DECODED_IDENTIFIER =
            "sample-images/jpg/rgb-64x56x8-baseline.jpg";
    private static final String ENCODED_IDENTIFIER =
            Reference.encode(DECODED_IDENTIFIER);

    /**
     * 2. "When the base URI is dereferenced, the interaction should result in
     * the Image Information document. It is recommended that the response be a
     * 303 status redirection to the Image Information document’s URI."
     */
    @Test
    void baseURIReturnsImageInfoViaHttp303() throws Exception {
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER);

        try (Response response = client.send()) {
            assertEquals(Status.SEE_OTHER, response.getStatus());
            assertEquals(
                    getHTTPURI(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER + "/info.json").toString(),
                    response.getHeaders().getFirstValue("Location"));
        }
    }

    /**
     * 3. "All special characters (e.g. ? or #) [in an identifier] must be URI
     * encoded to avoid unpredictable client behaviors. The URI syntax relies
     * upon slash (/) separators so any slashes in the identifier must be URI
     * encoded (also called “percent encoded”).
     */
    @Test
    void identifierWithEncodedCharacters() {
        assertTrue(ENCODED_IDENTIFIER.contains("%"));
        // image endpoint
        assertStatus(200, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/default.jpg"));
        // information endpoint
        assertStatus(200, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json"));
    }

    /**
     * 4.1
     */
    @Test
    void fullRegion() throws Exception {
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/default.jpg");
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
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
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
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
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
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
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
     * dimensions reported in the Image Information document, then the service
     * should return an image cropped at the image’s edge, rather than adding
     * empty space."
     */
    @Test
    void absolutePixelRegionLargerThanSource() throws Exception {
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
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
     * 4.1. "If the requested region’s height or width is zero ... then the
     * server should return a 400 status code."
     */
    @Test
    void zeroRegion() throws Exception {
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/0,0,0,0/full/0/default.jpg");
        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(400, e.getStatus().code());
        }
    }

    /**
     * 4.1. "If the requested region ... is entirely outside the bounds of the
     * reported dimensions, then the server should return a 400 status code."
     */
    @Test
    void originOutOfBounds() throws Exception {
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/99999,99999,50,50/full/0/default.jpg");
        try (Response response = client.send()) {
            fail("Expected exception");
        } catch (HTTPException e) {
            assertEquals(400, e.getStatus().code());
        }
    }

    /**
     * The IIIF API Validator wants the server to return 400 for a bogus
     * (junk characters) region.
     */
    @Test
    void bogusRegion() {
        assertStatus(400, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/ca%20ioU/full/0/default.jpg"));
    }

    /**
     * 4.2
     */
    @Test
    void fullSize() throws Exception {
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
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
     * maintains the aspect ratio of the extracted region."
     */
    @Test
    void sizeScaledToFitWidth() throws Exception {
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
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
     * maintains the aspect ratio of the extracted region."
     */
    @Test
    void sizeScaledToFitHeight() throws Exception {
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
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
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
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
     * The aspect ratio of the returned image may be different than the
     * extracted region, resulting in a distorted image."
     */
    @Test
    void absoluteWidthAndHeight() throws Exception {
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
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
     * resulting width and height are less than or equal to the requested
     * width and height. The exact scaling may be determined by the service
     * provider, based on characteristics including image quality and system
     * performance. The dimensions of the returned image content are
     * calculated to maintain the aspect ratio of the extracted region."
     */
    @Test
    void sizeScaledToFitInside() throws Exception {
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/20,20/0/default.jpg");
        try (Response response = client.send();
             InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(20, image.getWidth());
            assertEquals(20, image.getHeight());
        }
    }

    /**
     * 4.2. "If the resulting height or width is zero, then the server should
     * return a 400 (bad request) status code."
     */
    @Test
    void resultingWidthOrHeightIsZero() {
        assertStatus(400, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/pct:0/15/color.jpg"));
        assertStatus(400, getHTTPURI(IIIF2Resource.getURIPath() +
                "/sample-images%2Fwide.jpg/full/3,0/15/color.jpg"));
    }

    /**
     * IIIF Image API 2.0 doesn't say anything about an invalid size
     * parameter, so we will check for an HTTP 400.
     */
    @Test
    void invalidSize() {
        assertStatus(400, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/cats/0/default.jpg"));
        assertStatus(400, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/cats,50/0/default.jpg"));
        assertStatus(400, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/50,cats/0/default.jpg"));
        assertStatus(400, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/cats,/0/default.jpg"));
        assertStatus(400, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/,cats/0/default.jpg"));
        assertStatus(400, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/!cats,50/0/default.jpg"));
    }

    /**
     * 4.3. "The degrees of clockwise rotation from 0 up to 360."
     */
    @Test
    void rotation() {
        assertStatus(200, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/15.5/color.jpg"));
    }

    /**
     * 4.3. "The image should be mirrored and then rotated as above."
     */
    @Test
    void mirroredRotation() {
        assertStatus(200, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/!15/color.jpg"));
    }

    /**
     * 4.3. "A rotation value that is out of range or unsupported should result
     * in a 400 status code."
     */
    @Test
    void negativeRotation() {
        assertStatus(400, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/-15/default.jpg"));
    }

    /**
     * 4.3. "A rotation value that is out of range or unsupported should result
     * in a 400 status code."
     */
    @Test
    void greaterThanFullRotation() {
        assertStatus(400, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/4855/default.jpg"));
    }

    /**
     * 4.4. "The image is returned in full color."
     */
    @Test
    void colorQuality() {
        assertStatus(200, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/color.jpg"));
    }

    /**
     * 4.4. "The image is returned in grayscale, where each pixel is black,
     * white or any shade of gray in between."
     */
    @Test
    void grayQuality() {
        assertStatus(200, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/gray.jpg"));
    }

    /**
     * 4.4. "The image returned is bitonal, where each pixel is either black or
     * white."
     */
    @Test
    void bitonalQuality() {
        assertStatus(200, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/bitonal.jpg"));
    }

    /**
     * 4.4. "The image is returned using the server’s default quality (e.g.
     * color, gray or bitonal) for the image."
     */
    @Test
    void defaultQuality() {
        assertStatus(200, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/default.jpg"));
    }

    /**
     * 4.4. "A quality value that is unsupported should result in a 400 status
     * code."
     */
    @Test
    void unsupportedQuality() {
        assertStatus(400, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
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
        testFormat(new Format(
                "webp", "WebP",
                List.of(new MediaType("image", "webp")),
                List.of("webp"),
                true, false, true));
    }

    private void testFormat(Format outputFormat) throws Exception {
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/full/0/default." + outputFormat.getPreferredExtension());
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
        assertStatus(415, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/full/0/default.bogus"));
    }

    /**
     * 4.7. "When the client requests an image, the server may add a link
     * header to the response that indicates the canonical URI for that
     * request."
     */
    @Test
    void canonicalURILinkHeader() throws Exception {
        final String path   = IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/pct:50,50,50,50/,50/0/default.jpg";
        final Reference uri = getHTTPURI(path);
        final String uriStr = uri.toString();
        final String expectedURI = uriStr.substring(0, uriStr.indexOf(ENCODED_IDENTIFIER) + ENCODED_IDENTIFIER.length()) +
                "/32,28,32,28/57,/0/default.jpg";
        client = newClient(path);

        try (Response response = client.send()) {
            assertEquals("<" + expectedURI + ">;rel=\"canonical\"",
                    response.getHeaders().getFirstValue("Link"));
        }
    }

    /**
     * 5. "The service must return this information about the image."
     */
    @Test
    void informationRequest() {
        assertStatus(200, getHTTPURI(IIIF2Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json"));
    }

    /**
     * 5. "The content-type of the response must be either “application/json”,
     * (regular JSON), or “application/ld+json” (JSON-LD)."
     */
    @Test
    void informationRequestContentType() throws Exception {
        client = newClient(
                IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER + "/info.json");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());
            assertTrue("application/json;charset=utf-8".equalsIgnoreCase(
                    response.getHeaders().getFirstValue("Content-Type").replace(" ", "")));
        }
    }

    /**
     * 5. "If the client explicitly wants the JSON-LD content-type, then it
     * must specify this in an Accept header, otherwise the server must return
     * the regular JSON content-type."
     */
    @Test
    void informationRequestContentTypeJSONLD() throws Exception {
        client = newClient(
                IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER + "/info.json");

        client.getHeaders().set("Accept", "application/ld+json");
        try (Response response = client.send()) {
            assertEquals("application/ld+json;charset=UTF-8",
                    response.getHeaders().getFirstValue("Content-Type"));
        }

        client.getHeaders().set("Accept", "application/json");
        try (Response response = client.send()) {
            assertTrue("application/json;charset=UTF-8".equalsIgnoreCase(
                    response.getHeaders().getFirstValue("Content-Type").replace(" ", "")));
        }
    }

    /**
     * 5. "Servers should send the Access-Control-Allow-Origin header with the
     * value * in response to information requests."
     */
    @Test
    void informationRequestCORSHeader() throws Exception {
        client = newClient(
                IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER + "/info.json");
        try (Response response = client.send()) {
            assertEquals("*",
                    response.getHeaders().getFirstValue("Access-Control-Allow-Origin"));
        }
    }

    /**
     * 5.1
     */
    @Test
    void informationRequestJSON() {
        // this will be tested in InformationFactoryTest
    }

    /**
     * 5.1. "If any of formats, qualities, or supports have no additional
     * values beyond those specified in the referenced compliance level, then
     * the property should be omitted from the response rather than being
     * present with an empty list."
     */
    @Test
    void informationRequestEmptyJSONProperties() throws Exception {
        client = newClient(
                IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER + "/info.json");
        try (Response response = client.send()) {
            assertFalse(response.getBodyAsString().contains("null"));
        }
    }

    /**
     * 6. "The Image Information document must ... include a compliance level
     * URI as the first entry in the profile property."
     */
    @Test
    void complianceLevel() throws Exception {
        client = newClient(
                IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER + "/info.json");
        try (Response response = client.send()) {
            String json = response.getBodyAsString();
            ObjectMapper mapper = new ObjectMapper();
            Map<?, ?> info = mapper.readValue(json, Map.class);
            List<?> profile = (List<?>) info.get("profile");
            assertEquals("http://iiif.io/api/image/2/level2.json",
                    profile.getFirst());
        }
    }

}
