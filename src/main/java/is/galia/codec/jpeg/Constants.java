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

package is.galia.codec.jpeg;

import java.nio.charset.StandardCharsets;

final class Constants {

    /**
     * Header immediately following an {@literal APP1} segment marker
     * indicating that the segment contains EXIF data.
     */
    static final byte[] EXIF_SEGMENT_HEADER =
            "Exif\0\0".getBytes(StandardCharsets.US_ASCII);

    /**
     * Header immediately following an {@literal APP1} segment marker
     * indicating that the segment contains "ExtendedXMP" XMP data.
     */
    static final byte[] EXTENDED_XMP_SEGMENT_HEADER =
            "http://ns.adobe.com/xmp/extension/\0".getBytes(StandardCharsets.US_ASCII);

    static final String HAS_EXTENDED_XMP_PREDICATE =
            "http://ns.adobe.com/xmp/note/HasExtendedXMP";

    /**
     * Header immediately following an {@literal APP2} segment marker
     * indicating that the segment contains an ICC profile.
     */
    static final byte[] ICC_SEGMENT_HEADER =
            "ICC_PROFILE\0".getBytes(StandardCharsets.US_ASCII);

    /**
     * Header immediately following an {@literal APP13} segment marker
     * indicating that the segment contains Photoshop/IPTC data.
     */
    static final byte[] PHOTOSHOP_SEGMENT_HEADER =
            "Photoshop 3.0\0".getBytes(StandardCharsets.US_ASCII);

    /**
     * Header immediately following an {@literal APP1} segment marker
     * indicating that the segment contains "StandardXMP" XMP data.
     */
    static final byte[] STANDARD_XMP_SEGMENT_HEADER =
            "http://ns.adobe.com/xap/1.0/\0".getBytes(StandardCharsets.US_ASCII);

    private Constants() {}

}
