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

import is.galia.http.Reference;
import is.galia.http.Status;
import is.galia.resource.ResourceITCase;
import org.junit.jupiter.api.Test;
import is.galia.http.Response;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>Functional test of conformance to the IIIF Image API 2.1 spec. Methods
 * are implemented in the order of the assertions in the spec document.</p>
 *
 * @see <a href="http://iiif.io/api/image/2.1/#image-information">IIIF Image
 * API 2.1</a>
 */
class API2_1ConformanceITCase extends ResourceITCase {

    private static final String DECODED_IDENTIFIER =
            "sample-images/jpg/rgb-64x56x8-baseline.jpg";
    private static final String ENCODED_IDENTIFIER =
            Reference.encode(DECODED_IDENTIFIER);

    /**
     * 4.1
     */
    @Test
    void squareRegion() throws Exception {
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
                "/square/full/0/default.jpg");
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
     * 4.2
     */
    @Test
    void maxSize() throws Exception {
        client = newClient(IIIF2Resource.getURIPath() + "/" + ENCODED_IDENTIFIER +
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

}
