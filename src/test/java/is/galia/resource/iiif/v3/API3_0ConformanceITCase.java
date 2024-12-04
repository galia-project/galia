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

package is.galia.resource.iiif.v3;

import is.galia.codec.Decoder;
import is.galia.codec.DecoderFactory;
import is.galia.codec.Encoder;
import is.galia.codec.EncoderFactory;
import is.galia.codec.MockEncoderPlugin;
import is.galia.config.Configuration;
import is.galia.config.Key;
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

import static org.junit.jupiter.api.Assertions.*;
import static is.galia.test.Assert.HTTPAssert.*;

/**
 * <p>Functional test of conformance to the IIIF Image API 3.0 spec. Methods
 * are implemented in the order of the assertions in the spec document.</p>
 *
 * @see <a href="http://iiif.io/api/image/3.0/">IIIF Image API 3.0</a>
 */
class API3_0ConformanceITCase extends ResourceITCase {

    private static final String DECODED_IDENTIFIER =
            "sample-images/jpg/rgb-64x56x8-baseline.jpg";
    private static final String ENCODED_IDENTIFIER =
            Reference.encode(DECODED_IDENTIFIER);

    /**
     * 2. "When the base URI is dereferenced, the interaction should result in
     * the Image Information document. It is recommended that the response be a
     * 303 status redirection to the image information document’s URI."
     */
    @Test
    void baseURIReturnsImageInfoViaHttp303() throws Exception {
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER);

        try (Response response = client.send()) {
            assertEquals(Status.SEE_OTHER, response.getStatus());
            assertEquals(
                    getHTTPURI(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER + "/info.json").toString(),
                    response.getHeaders().getFirstValue("Location"));
        }
    }

    /**
     * 3. "All special characters (e.g. ? or #) must be URI encoded to avoid
     * unpredictable client behaviors. The URI syntax relies upon slash (/)
     * separators so any slashes in the identifier must be URI encoded (also
     * called “percent encoded”)."
     */
    @Test
    void identifierWithEncodedCharacters() {
        assertTrue(ENCODED_IDENTIFIER.contains("%"));
        // image endpoint
        assertStatus(200, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/max/0/default.jpg"));
        // information endpoint
        assertStatus(200, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json"));
    }

    /**
     * 4.1
     */
    @Test
    void fullRegion() throws Exception {
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/max/0/default.jpg");
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
    void squareRegion() throws Exception {
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/square/max/0/default.jpg");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());

            try (InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(56, image.getWidth());
                assertEquals(56, image.getHeight());
            }
        }
    }

    /**
     * 4.1
     */
    @Test
    void absolutePixelRegion() throws Exception {
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/20,20,100,100/max/0/color.jpg");
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
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/pct:20,20,50,50/max/0/color.jpg");
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
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/pct:20.2,20.6,50.2,50.6/max/0/color.jpg");
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
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/0,0,99999,99999/max/0/color.jpg");
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
    void zeroRegion() {
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/0,0,0,0/max/0/default.jpg");
        HTTPException e = assertThrows(
                HTTPException.class,
                () -> client.send().close());
        assertEquals(Status.BAD_REQUEST, e.getStatus());
    }

    /**
     * 4.1. "If the requested region ... is entirely outside the bounds of the
     * reported dimensions, then the server should return a 400 status code."
     */
    @Test
    void originOutOfBounds() {
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/99999,99999,50,50/max/0/default.jpg");
        HTTPException e = assertThrows(
                HTTPException.class,
                () -> client.send().close());
        assertEquals(Status.BAD_REQUEST, e.getStatus());
    }

    /**
     * The IIIF Image API Validator wants the server to return 400 for a bogus
     * (junk characters) region.
     */
    @Test
    void bogusRegion() {
        assertStatus(400, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/ca%20ioU/max/0/default.jpg"));
    }

    /**
     * 4.2. (max) "The extracted region is returned at the maximum size
     * available, but will not be upscaled. The resulting image will have the
     * pixel dimensions of the extracted region, unless it is constrained to a
     * smaller size by maxWidth, maxHeight, or maxArea."
     */
    @Test
    void maxSize() throws Exception {
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/max/0/color.jpg");
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
     * 4.2. (^max) "The extracted region is scaled to the maximum size
     * permitted by maxWidth, maxHeight, or maxArea. ... If the resulting
     * dimensions are greater than the pixel width and height of the extracted
     * region, the extracted region is upscaled."
     */
    @Test
    void maxSizeWithUpscaling() throws Exception {
        final int maxScale = 2;
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_SCALE, maxScale);

        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/^max/0/color.jpg");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());

            try (InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(64 * maxScale, image.getWidth());
                assertEquals(56 * maxScale, image.getHeight());
            }
        }
    }

    /**
     * 4.2. (w,) "The extracted region should be scaled so that its width is
     * exactly equal to w."
     */
    @Test
    void sizeDownscaledToFitWidth() throws Exception {
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
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
     * 4.2. (w,) "The value of w must not be greater than the width of the
     * extracted region."
     */
    @Test
    void sizeDownscaledToFitWidthWithIllegalArgument() {
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/100,/0/color.jpg");
        HTTPException e = assertThrows(
                HTTPException.class,
                () -> client.send().close());
        assertEquals(Status.BAD_REQUEST, e.getStatus());
    }

    /**
     * 4.2. (^w,) "The extracted region should be scaled so that the width of
     * the returned image is exactly equal to w. If w is greater than the pixel
     * width of the extracted region, the extracted region is upscaled."
     */
    @Test
    void sizeUpscaledToFitWidth() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_SCALE, 999);

        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/^100,/0/color.jpg");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());

            try (InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(100, image.getWidth());
                assertEquals(88, image.getHeight());
            }
        }
    }

    /**
     * 4.2. (^w,) "Requests for sizes prefixed with ^ that require upscaling
     * should result in a 501 (Not Implemented) status code if the server does
     * not support upscaling."
     *
     * Note that because supporting upscaling is not an on/off switch, but
     * rather a continuum based on {@link Key#MAX_SCALE}, this implementation
     * returns 400 in this situation instead of 501.
     */
    @Test
    void sizeUpscaledToFitWidthWithoutServerSupport() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_SCALE, 1.0);

        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/^100,/0/color.jpg");
        HTTPException e = assertThrows(
                HTTPException.class,
                () -> client.send().close());
        assertEquals(Status.BAD_REQUEST, e.getStatus());
    }

    /**
     * 4.2. (,h) "The extracted region should be scaled so that its height is
     * exactly equal to h."
     */
    @Test
    void sizeDownscaledToFitHeight() throws Exception {
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
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
     * 4.2. (,h) "The value of h must not be greater than the height of the
     * extracted region."
     */
    @Test
    void sizeDownscaledToFitHeightWithIllegalArgument() {
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/,100/0/color.jpg");
        HTTPException e = assertThrows(HTTPException.class,
                () -> client.send().close());
        assertEquals(Status.BAD_REQUEST, e.getStatus());
    }

    /**
     * 4.2. (^,h) "The extracted region should be scaled so that the height of
     * the returned image is exactly equal to h. If h is greater than the pixel
     * height of the extracted region, the extracted region is upscaled."
     */
    @Test
    void sizeUpscaledToFitHeight() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_SCALE, 999);

        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/^,100/0/color.jpg");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());

            try (InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(114, image.getWidth());
                assertEquals(100, image.getHeight());
            }
        }
    }

    /**
     * 4.2. (^,h) "Requests for sizes prefixed with ^ that require upscaling
     * should result in a 501 (Not Implemented) status code if the server does
     * not support upscaling."
     *
     * Note that because supporting upscaling is not an on/off switch, but
     * rather a continuum based on {@link Key#MAX_SCALE}, this implementation
     * returns 400 in this situation instead of 501.
     */
    @Test
    void sizeUpscaledToFitHeightWithoutServerSupport() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_SCALE, 1.0);

        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/^,100/0/color.jpg");
        HTTPException e = assertThrows(
                HTTPException.class,
                () -> client.send().close());
        assertEquals(Status.BAD_REQUEST, e.getStatus());
    }

    /**
     * 4.2. (pct:n) "The width and height of the returned image is scaled to n
     * percent of the width and height of the extracted region."
     */
    @Test
    void sizeDownscaledToPercent() throws Exception {
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
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
     * 4.2. (pct:n) "The width and height of the returned image is scaled to n
     * percent of the width and height of the extracted region. The value of n
     * must not be greater than 100."
     */
    @Test
    void sizeToPercentWithIllegalArgument() {
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/pct:110/0/color.jpg");
        HTTPException e = assertThrows(HTTPException.class,
                () -> client.send().close());
        assertEquals(Status.BAD_REQUEST, e.getStatus());
    }

    /**
     * 4.2. (^pct:n) "The width and height of the returned image is scaled to n
     * percent of the width and height of the extracted region. For values of n
     * greater than 100, the extracted region is upscaled."
     */
    @Test
    void sizeUpscaledToPercent() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_SCALE, 999);

        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/^pct:110/0/color.jpg");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());

            try (InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(70, image.getWidth());
                assertEquals(62, image.getHeight());
            }
        }
    }

    /**
     * 4.2. (^pct:n) "Requests for sizes prefixed with ^ that require upscaling
     * should result in a 501 (Not Implemented) status code if the server does
     * not support upscaling."
     *
     * Note that because supporting upscaling is not an on/off switch, but
     * rather a continuum based on {@link Key#MAX_SCALE}, this implementation
     * returns 400 in this situation instead of 501.
     */
    @Test
    void sizeUpscaledToPercentWithoutServerSupport() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_SCALE, 1.0);

        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/^pct:110/0/color.jpg");
        HTTPException e = assertThrows(
                HTTPException.class,
                () -> client.send().close());
        assertEquals(Status.BAD_REQUEST, e.getStatus());
    }

    /**
     * 4.2. (w,h) "The width and height of the returned image are exactly w and
     * h. The aspect ratio of the returned image may be significantly different
     * than the extracted region, resulting in a distorted image."
     */
    @Test
    void absoluteWidthAndHeight() throws Exception {
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
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
     * 4.2. (w,h) "The values of w and h must not be greater than the
     * corresponding pixel dimensions of the extracted region."
     */
    @Test
    void absoluteWidthAndHeightWithIllegalWidth() {
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/100,20/0/color.jpg");

        HTTPException e = assertThrows(
                HTTPException.class,
                () -> client.send().close());
        assertEquals(Status.BAD_REQUEST, e.getStatus());
    }

    /**
     * 4.2. (w,h) "The values of w and h must not be greater than the
     * corresponding pixel dimensions of the extracted region."
     */
    @Test
    void absoluteWidthAndHeightWithIllegalHeight() {
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/20,100/0/color.jpg");
        HTTPException e = assertThrows(
                HTTPException.class,
                () -> client.send().close());
        assertEquals(Status.BAD_REQUEST, e.getStatus());
    }

    /**
     * 4.2. (^w,h) "The width and height of the returned image are exactly w
     * and h. The aspect ratio of the returned image may be significantly
     * different than the extracted region, resulting in a distorted image. If
     * w and/or h are greater than the corresponding pixel dimensions of the
     * extracted region, the extracted region is upscaled."
     */
    @Test
    void upscaleToAbsoluteWidthAndHeight() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_SCALE, 999);

        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/^100,100/0/color.jpg");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());

            try (InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(100, image.getWidth());
                assertEquals(100, image.getHeight());
            }
        }
    }

    /**
     * 4.2. (^w,h) "Requests for sizes prefixed with ^ that require upscaling
     * should result in a 501 (Not Implemented) status code if the server does
     * not support upscaling."
     *
     * Note that because supporting upscaling is not an on/off switch, but
     * rather a continuum based on {@link Key#MAX_SCALE}, this implementation
     * returns 400 in this situation instead of 501.
     */
    @Test
    void upscaleToAbsoluteWidthAndHeightWithoutServerSupport() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_SCALE, 1.0);

        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/^100,100/0/color.jpg");
        HTTPException e = assertThrows(
                HTTPException.class,
                () -> client.send().close());
        assertEquals(Status.BAD_REQUEST, e.getStatus());
    }

    /**
     * 4.2. (!w,h) "The extracted region is scaled so that the width and height
     * of the returned image are not greater than w and h, while maintaining
     * the aspect ratio. The returned image must be as large as possible but
     * not larger than the extracted region, w or h, or server-imposed limits."
     */
    @Test
    void sizeDownscaledToFitInside() throws Exception {
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/!30,30/0/default.jpg");
        try (Response response = client.send();
             InputStream is = new ByteArrayInputStream(response.getBody())) {
                BufferedImage image = ImageIO.read(is);
                assertEquals(30, image.getWidth());
                assertEquals(26, image.getHeight());
        }
    }

    /**
     * 4.2. (!w,h) "The extracted region is scaled so that the width and height
     * of the returned image are not greater than w and h, while maintaining
     * the aspect ratio. The returned image must be as large as possible but
     * not larger than the extracted region, w or h, or server-imposed limits."
     */
    @Test
    void sizeDownscaledToFitInsideWithSizeGreaterThanMaxScale() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_SCALE, 1);
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/!300,300/0/default.jpg");
        try (Response response = client.send();
             InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(64, image.getWidth());
            assertEquals(56, image.getHeight());
        }
    }

    /**
     * 4.2. (^!w,h) "The extracted region is scaled so that the width and
     * height of the returned image are not greater than w and h, while
     * maintaining the aspect ratio. The returned image must be as large as
     * possible but not larger than w, h, or server-imposed limits."
     */
    @Test
    void sizeUpscaledToFitInside() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_SCALE, 999);

        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/^!100,100/0/default.jpg");
        try (Response response = client.send();
             InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(100, image.getWidth());
            assertEquals(88, image.getHeight());
        }
    }

    /**
     * 4.2. (^!w,h) "Requests for sizes prefixed with ^ that require upscaling
     * should result in a 501 (Not Implemented) status code if the server does
     * not support upscaling."
     *
     * Note that because supporting upscaling is not an on/off switch, but
     * rather a continuum based on {@link Key#MAX_SCALE}, this implementation
     * returns 400 in this situation instead of 501.
     */
    @Test
    void sizeUpscaledToFitInsideWithoutServerSupport() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.MAX_SCALE, 1.0);
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/^!150,150/0/color.jpg");

        HTTPException e = assertThrows(
                HTTPException.class,
                () -> client.send().close());
        assertEquals(Status.BAD_REQUEST, e.getStatus());
    }

    /**
     * 4.2. "For all requests the pixel dimensions of the scaled region must
     * not be less than 1 pixel or greater than the server-imposed limits.
     * Requests that would generate images of these sizes are errors that
     * should result in a 400 (Bad Request) status code."
     */
    @Test
    void resultingWidthOrHeightIsZero() {
        assertStatus(400, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/pct:0/15/color.jpg"));
        assertStatus(400, getHTTPURI(IIIF3Resource.getURIPath() +
                "/sample-images%2Fwide.jpg/full/3,0/15/color.jpg"));
    }

    /**
     * 4.2. "... a 400 (Bad Request) status code should be returned in response
     * to other client request syntax errors."
     */
    @Test
    void invalidSize() {
        assertStatus(400, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/cats/0/default.jpg"));
        assertStatus(400, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/cats,50/0/default.jpg"));
        assertStatus(400, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/^cats,50/0/default.jpg"));
        assertStatus(400, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/50,cats/0/default.jpg"));
        assertStatus(400, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/^50,cats/0/default.jpg"));
        assertStatus(400, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/cats,/0/default.jpg"));
        assertStatus(400, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/^cats,/0/default.jpg"));
        assertStatus(400, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/,cats/0/default.jpg"));
        assertStatus(400, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/^,cats/0/default.jpg"));
        assertStatus(400, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/!cats,50/0/default.jpg"));
        assertStatus(400, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/^!cats,50/0/default.jpg"));
    }

    /**
     * 4.3. "The degrees of clockwise rotation from 0 up to 360."
     */
    @Test
    void rotation() {
        assertStatus(200, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/max/0/color.jpg"));
        assertStatus(200, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/max/15.5/color.jpg"));
        assertStatus(200, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/max/360/color.jpg"));
    }

    /**
     * 4.3. "The image should be mirrored and then rotated as above."
     */
    @Test
    void mirroredRotation() {
        assertStatus(200, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/max/!15/color.jpg"));
    }

    /**
     * 4.3. "A rotation value that is out of range or unsupported should result
     * in a 400 status code."
     */
    @Test
    void negativeRotation() {
        assertStatus(400, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/max/-15/default.jpg"));
    }

    /**
     * 4.3. "A rotation value that is out of range or unsupported should result
     * in a 400 status code."
     */
    @Test
    void greaterThanFullRotation() {
        assertStatus(400, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/max/4855/default.jpg"));
    }

    /**
     * 4.4. "The image is returned in full color."
     */
    @Test
    void colorQuality() {
        assertStatus(200, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/max/0/color.jpg"));
    }

    /**
     * 4.4. "The image is returned in grayscale, where each pixel is black,
     * white or any shade of gray in between."
     */
    @Test
    void grayQuality() {
        assertStatus(200, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/max/0/gray.jpg"));
    }

    /**
     * 4.4. "The image returned is bitonal, where each pixel is either black or
     * white."
     */
    @Test
    void bitonalQuality() {
        assertStatus(200, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/max/0/bitonal.jpg"));
    }

    /**
     * 4.4. "The image is returned using the server’s default quality (e.g.
     * color, gray or bitonal) for the image."
     */
    @Test
    void defaultQuality() {
        assertStatus(200, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/max/0/default.jpg"));
    }

    /**
     * 4.4. "A quality value that is unsupported should result in a 400 status
     * code."
     */
    @Test
    void unsupportedQuality() {
        assertStatus(400, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/max/0/bogus.jpg"));
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
        client = newClient(IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/full/max/0/default." + outputFormat.getPreferredExtension());
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
        assertStatus(415, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/full/max/0/default.bogus"));
    }

    /**
     * 4.7
     */
    @Test
    void canonicalURIInLinkHeader() throws Exception {
        final String path        = IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/pct:50,50,50,50/,50/15/gray.jpg";
        final Reference uri      = getHTTPURI(path);
        final String uriStr      = uri.toString();
        final String expectedURI = uriStr.substring(0,
                uriStr.indexOf(ENCODED_IDENTIFIER) +
                        ENCODED_IDENTIFIER.length()) +
                "/32,28,32,28/57,50/15/gray.jpg";
        client = newClient(path);

        try (Response response = client.send()) {
            assertEquals("<" + expectedURI + ">;rel=\"canonical\"",
                    response.getHeaders().getFirstValue("Link"));
        }
    }

    /**
     * 5. "Servers must support requests for image information."
     */
    @Test
    void informationRequest() {
        assertStatus(200, getHTTPURI(IIIF3Resource.getURIPath() + "/" +
                ENCODED_IDENTIFIER + "/info.json"));
    }

    /**
     * 5.1. "If the server receives a request with an Accept header, it should
     * respond following the rules of content negotiation. Note that content
     * types provided in the Accept header of the request may include
     * parameters, for example profile or charset."
     */
    @Test
    void informationRequestContentTypeWithAcceptHeader() throws Exception {
        client = newClient(
                IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER + "/info.json");

        client.getHeaders().set("Accept", "application/ld+json");
        try (Response response = client.send()) {
            assertEquals("application/ld+json;charset=UTF-8;profile=\"http://iiif.io/api/image/3/context.json\"",
                    response.getHeaders().getFirstValue("Content-Type"));
        }

        client.getHeaders().set("Accept", "application/json");
        try (Response response = client.send()) {
            assertTrue("application/json;charset=UTF-8;profile=\"http://iiif.io/api/image/3/context.json\"".equalsIgnoreCase(
                    response.getHeaders().getFirstValue("Content-Type").replace(" ", "")));
        }
    }

    /**
     * 5.1. "If the request does not include an Accept header, the HTTP
     * Content-Type header of the response should have the value
     * application/ld+json (JSON-LD) with the profile parameter given as the
     * context document."
     */
    @Test
    void informationRequestContentTypeWithoutAcceptHeader() throws Exception {
        client = newClient(
                IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER + "/info.json");
        try (Response response = client.send()) {
            assertEquals(Status.OK, response.getStatus());
            assertTrue("application/ld+json;charset=utf-8;profile=\"http://iiif.io/api/image/3/context.json\"".equalsIgnoreCase(
                    response.getHeaders().getFirstValue("Content-Type").replace(" ", "")));
        }
    }

    /**
     * 5.1. "Servers should support CORS on image information responses."
     */
    @Test
    void informationRequestCORSHeader() throws Exception {
        client = newClient(
                IIIF3Resource.getURIPath() + "/" + ENCODED_IDENTIFIER + "/info.json");
        try (Response response = client.send()) {
            assertEquals("*",
                    response.getHeaders().getFirstValue("Access-Control-Allow-Origin"));
        }
    }

    /**
     * 5.2
     */
    @Test
    void informationRequestJSON() {
        // this is tested in InformationFactoryTest
    }

}
