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

import is.galia.codec.xmp.XMPUtils;
import is.galia.util.ArrayUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.StmtIterator;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static is.galia.codec.jpeg.Constants.EXIF_SEGMENT_HEADER;
import static is.galia.codec.jpeg.Constants.EXTENDED_XMP_SEGMENT_HEADER;
import static is.galia.codec.jpeg.Constants.HAS_EXTENDED_XMP_PREDICATE;
import static is.galia.codec.jpeg.Constants.PHOTOSHOP_SEGMENT_HEADER;
import static is.galia.codec.jpeg.Constants.STANDARD_XMP_SEGMENT_HEADER;

final class JPEGUtils {

    /**
     * @param xmp XMP data with a root {@literal <rdf:RDF>} element.
     * @return    Full {@literal APP1} segment data including marker and length.
     */
    static byte[] assembleAPP1Segment(String xmp) {
        final byte[] headerBytes = STANDARD_XMP_SEGMENT_HEADER;
        final byte[] xmpBytes = XMPUtils.wrapInXPacket(xmp).
                getBytes(StandardCharsets.UTF_8);
        final ByteBuffer buffer = ByteBuffer.allocate(
                Marker.APP1.marker().length + // segment marker length
                        2 +                   // segment length
                        headerBytes.length +  // segment header length
                        xmpBytes.length +     // XMP length
                        1);                   // null terminator
        // write segment marker
        buffer.put(Marker.APP1.marker());
        // write segment length
        buffer.putShort((short) (headerBytes.length + xmpBytes.length + 3));
        // write segment header
        buffer.put(headerBytes);
        // write XMP data
        buffer.put(xmpBytes);
        // write null terminator
        buffer.put((byte) 0x00);
        return buffer.array();
    }

    /**
     * @param xmpChunks Zero or more chunks of XMP data (one per {@literal
     *                  APP1} segment).
     * @return          Fully formed XML tree, or {@code null} if no chunks
     *                  were supplied.
     */
    static String assembleXMP(final List<byte[]> xmpChunks) throws IOException {
        String standardXMP  = null;
        final int numChunks = xmpChunks.size();
        if (numChunks > 0) {
            standardXMP = new String(xmpChunks.getFirst(), StandardCharsets.UTF_8);
            standardXMP = XMPUtils.trimXMP(standardXMP);
            if (numChunks > 1) {
                String extendedXMP = new String(
                        ArrayUtils.merge(xmpChunks.subList(1, numChunks)),
                        StandardCharsets.UTF_8);
                extendedXMP = XMPUtils.trimXMP(extendedXMP);
                return mergeXMPModels(standardXMP, extendedXMP);
            }
        }
        return standardXMP;
    }

    /**
     * @param segmentData Segment data including marker.
     */
    static boolean isAdobeSegment(byte[] segmentData) {
        return (segmentData.length >= 12 &&
                segmentData[0] == 'A' &&
                segmentData[1] == 'd' &&
                segmentData[2] == 'o' &&
                segmentData[3] == 'b' &&
                segmentData[4] == 'e');
    }

    /**
     * @param segmentData Segment data including marker.
     */
    static boolean isEXIFSegment(byte[] segmentData) {
        return Arrays.equals(
                EXIF_SEGMENT_HEADER,
                Arrays.copyOfRange(segmentData, 0, EXIF_SEGMENT_HEADER.length));
    }

    /**
     * @param segmentData Segment data including marker.
     */
    static boolean isExtendedXMPSegment(byte[] segmentData) {
        return Arrays.equals(
                EXTENDED_XMP_SEGMENT_HEADER,
                Arrays.copyOfRange(segmentData, 0, EXTENDED_XMP_SEGMENT_HEADER.length));
    }

    /**
     * @param segmentData Segment data including marker.
     */
    static boolean isPhotoshopSegment(byte[] segmentData) {
        return Arrays.equals(
                PHOTOSHOP_SEGMENT_HEADER,
                Arrays.copyOfRange(segmentData, 0, PHOTOSHOP_SEGMENT_HEADER.length));
    }

    /**
     * @param segmentData Segment data including marker.
     */
    static boolean isStandardXMPSegment(byte[] segmentData) {
        return Arrays.equals(
                STANDARD_XMP_SEGMENT_HEADER,
                Arrays.copyOfRange(segmentData, 0, STANDARD_XMP_SEGMENT_HEADER.length));
    }

    /**
     * <p>Merges the "StandardXMP" and "ExtendedXMP" models.</p>
     *
     * <p>Note: certain properties that are known to be large are excluded from
     * the merge and discarded. An explanation follows:</p>
     *
     * <p>If the serialized form of the returned model is larger than 65502
     * bytes, the XMP will have to be split again during writing. Most XMP
     * properties are simple and it would take hundreds of them to reach this
     * length, and since there probably aren't that many, it's almost certainly
     * an embedded thumbnail or some other property from a small set of known
     * offenders that is bloating up the model.</p>
     *
     * <p>Large models are slower to process and deliver. 65KB is already quite
     * a lot of data and maybe even larger than the image it's going to be
     * embedded in. One of the use cases of this application is thumbnail
     * delivery, so why embed a thumbnail inside a thumbnail, especially when
     * doing so will slow down the delivery?</p>
     *
     * <p>The Adobe XMP Spec Part 3 recommends a procedure for writing XMP that
     * may require iterative serialization&mdash;for example, "find a large
     * property value in StandardXMP, move it into ExtendedXMP, serialize the
     * StandardXMP, check its length, find another large value, move it,
     * serialize again, check length again," etc. This may be viable for
     * Photoshop, but we are trying to be mindful of efficiency here.</p>
     *
     * @param standardXMP Fully formed "StandardXMP" tree.
     * @param extendedXMP Fully formed "ExtendedXMP" tree.
     * @return            Merged tree.
     */
    private static String mergeXMPModels(String standardXMP,
                                         String extendedXMP) throws IOException {
        // Merge the models.
        Model model = XMPUtils.readModel(standardXMP);
        model = model.union(XMPUtils.readModel(extendedXMP));

        // Normalize the merged model.
        normalize(model);

        // Write the model to RDF/XML.
        try (StringWriter writer = new StringWriter()) {
            model.write(writer);
            return writer.toString();
        }
    }

    /**
     * Removes any {@code xmpNote:HasExtendedXMP} property per the Adobe XMP
     * Spec Part 3. Also removes other known-large properties (see {@link
     * #mergeXMPModels}).
     */
    private static void normalize(Model model) {
        for (String property : Set.of(
                HAS_EXTENDED_XMP_PREDICATE,
                "http://ns.adobe.com/xap/1.0/Thumbnails",
                "http://ns.adobe.com/xap/1.0/g/img/image",
                "http://ns.adobe.com/photoshop/1.0/History")) {
            final StmtIterator it = model.listStatements(
                    null,
                    model.createProperty(property),
                    (RDFNode) null);
            while (it.hasNext()) {
                it.removeNext();
            }
        }
    }

    /**
     * @param segment APP1 segment data.
     */
    static byte[] readEXIFData(byte[] segment) {
        final int dataLength = segment.length -
                EXIF_SEGMENT_HEADER.length;
        byte[] data = new byte[dataLength];
        System.arraycopy(segment, EXIF_SEGMENT_HEADER.length,
                data, 0, dataLength);
        return data;
    }

    /**
     * @param segment APP1 segment data.
     */
    static byte[] readStandardXMPData(byte[] segment) {
        // Note that XMP models > 65502 bytes will be split across multiple
        // APP1 segments. In this case, the first one (the "StandardXMP")
        // will be a fully formed tree, and will contain an
        // xmpNote:HasExtendedXMP property containing the GUID of the
        // "ExtendedXMP."
        final int dataLength = segment.length -
                STANDARD_XMP_SEGMENT_HEADER.length;
        byte[] data = new byte[dataLength];
        System.arraycopy(segment, STANDARD_XMP_SEGMENT_HEADER.length,
                data, 0, dataLength);
        return data;
    }

    /**
     * @param segment APP1 segment data.
     */
    static byte[] readExtendedXMPData(byte[] segment) {
        // If the ExtendedXMP is <= 65502 bytes, it will be a fully formed
        // tree; otherwise it will be split across however many more APP1
        // segments without regard to XML or even UTF-8 structure.
        // The structure of an ExtendedXMP segment is:
        // 1. 32-byte GUID, which is an ASCII MD5 digest of the full
        //    ExtendedXMP serialization
        // 2. 4-byte unsigned data length
        // 3. 4-byte unsigned offset
        // 4. ExtendedXMP data
        int dataLength = segment.length;
        // Google Pixel 6 & 7a) do not terminate the ExtendedXMP section
        // properly. They start the signature of the next segment immediately
        // after the first ExtendedXMP data with no bytes in between, and then
        // start the next segment. This is why this class has no
        // isExtendedXMPSegment() -- because the second ExtendedXMP segment
        // does not include a signature that would identify it.
        if (ArrayUtils.indexOf(segment, EXTENDED_XMP_SEGMENT_HEADER,
                segment.length - EXTENDED_XMP_SEGMENT_HEADER.length - 32 - 4 - 4) > 0) {
            dataLength = dataLength - EXTENDED_XMP_SEGMENT_HEADER.length - 32 - 4 - 4;
        }
        segment = Arrays.copyOfRange(
                segment,
                EXTENDED_XMP_SEGMENT_HEADER.length + 32 + 4 + 4,
                dataLength);
        return segment;
    }

    private JPEGUtils() {}

}
