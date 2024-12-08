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

import is.galia.codec.SourceFormatException;
import is.galia.codec.tiff.Directory;
import is.galia.codec.tiff.DirectoryIterator;
import is.galia.codec.tiff.DirectoryReader;
import is.galia.codec.tiff.EXIFBaselineTIFFTagSet;
import is.galia.stream.ByteArrayImageInputStream;
import is.galia.util.ArrayUtils;

import javax.imageio.stream.ImageInputStream;
import java.awt.color.ICC_Profile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Reads various metadata from a JPEG image.</p>
 *
 * <p>This is far from a comprehensive reader&mdash;essentially it is a grab
 * bag of functionality needed by various application components and
 * plugins.</p>
 *
 * @see <a href="https://www.w3.org/Graphics/JPEG/jfif3.pdf">JPEG File
 * Interchange Format Version 1.02</a>
 */
public final class JPEGMetadataReader {

    /**
     * Represents a color transform in an Adobe APP14 marker segment.
     */
    public enum AdobeColorTransform {
        YCBCR(1), YCCK(2), UNKNOWN(0);

        private final int app14Value;

        private static AdobeColorTransform forAPP14Value(int value) {
            for (AdobeColorTransform transform : AdobeColorTransform.values()) {
                if (transform.getAPP14Value() == value) {
                    return transform;
                }
            }
            return null;
        }

        AdobeColorTransform(int app14Value) {
            this.app14Value = app14Value;
        }

        private int getAPP14Value() {
            return app14Value;
        }
    }

    /**
     * Set to {@literal true} once reading begins.
     */
    private boolean isReadAttempted;

    /**
     * Stream from which to read the image data.
     */
    private ImageInputStream inputStream;

    private AdobeColorTransform colorTransform;

    private boolean hasAdobeSegment;

    private final List<byte[]> iccProfileChunks = new ArrayList<>();
    private final List<byte[]> xmpChunks        = new ArrayList<>();
    private byte[] exif, iptc;
    private int width, height, exifOffset = -1;
    private boolean isProgressive, isReadingExtendedXMP;
    private int thumbCompression = -1, thumbDirOffset = -1, thumbLength;
    private byte[] thumbData;
    private String xmp;

    /**
     * @return Color transform from the {@literal APP14} segment.
     */
    public AdobeColorTransform getColorTransform() throws IOException {
        readImage();
        return colorTransform;
    }

    /**
     * @return EXIF data from the {@literal APP1} segment.
     */
    public byte[] getEXIF() throws IOException {
        readImage();
        return exif;
    }

    /**
     * @return Offset of the EXIF IFD relative to the start of the stream, or
     *         {@code -1} if there is no EXIF IFD.
     */
    public int getEXIFOffset() throws IOException {
        readImage();
        return exifOffset;
    }

    /**
     * <p>Reads an embedded ICC profile from an {@literal APP2} segment.</p>
     *
     * <p>N.B.: ICC profiles can also be extracted from {@link
     * javax.imageio.metadata.IIOMetadata} objects, but as of JDK 11, the
     * JDK Image I/O JPEG reader does not support JPEGs with CMYK color and
     * will throw an exception before the metadata can be read.</p>
     *
     * @return ICC profile, or {@code null} if one is not contained in the
     *         stream.
     * @see <a href="http://www.color.org/specification/ICC1v43_2010-12.pdf">
     *     ICC Specification ICC.1: 2010</a> Annex B.4
     */
    public ICC_Profile getICCProfile() throws IOException {
        readImage();
        if (!iccProfileChunks.isEmpty()) {
            byte[] data = ArrayUtils.merge(iccProfileChunks);
            return ICC_Profile.getInstance(data);
        }
        return null;
    }

    /**
     * @return IPTC data from the {@literal APP13} segment.
     */
    public byte[] getIPTC() throws IOException {
        readImage();
        return iptc;
    }

    public int getWidth() throws IOException {
        readImage();
        return width;
    }

    public int getHeight() throws IOException {
        readImage();
        return height;
    }

    /**
     * @return Thumbnail compression value from EXIF data, or {@code -1} if
     *         there is no thumbnail.
     */
    public int getThumbnailCompression() throws IOException {
        readThumbnailInfo();
        return thumbCompression;
    }

    /**
     * @return Raw embedded thumbnail data from EXIF data, if available.
     * @see #getThumbnailCompression()
     */
    public byte[] getThumbnailData() throws IOException {
        if (thumbData == null) {
            readThumbnailInfo();
            if (thumbCompression > -1) {
                long initialPos = inputStream.getStreamPosition();
                inputStream.seek(thumbDirOffset);
                thumbData = new byte[thumbLength];
                inputStream.readFully(thumbData);
                inputStream.seek(initialPos);
            }
        }
        return thumbData;
    }

    /**
     * @return Fully formed XMP tree from one or more {@literal APP1} segments.
     *         {@code rdf:RDF} is the outermost element and some properties
     *         (especially large ones) may be removed.
     */
    public String getXMP() throws IOException {
        readImage();
        if (xmp == null) {
            xmp = JPEGUtils.assembleXMP(xmpChunks);
        }
        return xmp;
    }

    public boolean hasAdobeSegment() throws IOException {
        readImage();
        return hasAdobeSegment;
    }

    public boolean isProgressive() throws IOException {
        readImage();
        return isProgressive;
    }

    /**
     * @param inputStream Fresh stream from which to read the image.
     */
    public void setSource(ImageInputStream inputStream) {
        this.inputStream     = inputStream;
        this.xmp             = null;
        this.isReadAttempted = false;
    }

    /**
     * <p>Main reading method. Reads image info into instance variables. May
     * call other private reading methods that will all expect {@link
     * #inputStream} to be pre-positioned for reading.</p>
     *
     * <p>Safe to call multiple times.</p>
     */
    private void readImage() throws IOException {
        if (isReadAttempted) {
            return;
        } else if (inputStream == null) {
            throw new IllegalStateException("Source not set");
        } else {
            inputStream.seek(0);
            byte b1 = (byte) inputStream.read();
            byte b2 = (byte) inputStream.read();
            if (!Marker.SOI.equals(Marker.forBytes(b1, b2))) {
                throw new SourceFormatException(
                        "Invalid SOI marker (is this a JPEG?)");
            }
        }

        isReadAttempted = true;

        //noinspection StatementWithEmptyBody
        while (readSegment() != -1) {
        }
    }

    /**
     * @return {@literal -1} when there are no more segments to read; some
     *         other value otherwise.
     */
    private int readSegment() throws IOException {
        byte b1 = (byte) inputStream.read();
        byte b2 = (byte) inputStream.read();
        switch (Marker.forBytes(b1, b2)) {
            case SOF0:
                isProgressive = false;
                readSOFSegment();
                break;
            case SOF1:
                readSOFSegment();
                break;
            case SOF2:
                isProgressive = true;
            case SOF3:
            case SOF5:
            case SOF6:
            case SOF7:
            case SOF9:
            case SOF10:
            case SOF11:
            case SOF13:
            case SOF14:
            case SOF15:
                readSOFSegment();
                break;
            case APP1:
                readAPP1Segment();
                break;
            case APP2:
                readAPP2Segment();
                break;
            case APP13:
                readAPP13Segment();
                break;
            case APP14:
                readAPP14Segment();
                break;
            case DHT:
                return -1;
            default:
                skipSegment();
                break;
        }
        return 0;
    }

    private int readSegmentLength() throws IOException {
        return 256 * inputStream.read() + inputStream.read() - 2;
    }

    private void skipSegment() throws IOException {
        int segmentLength = readSegmentLength();
        inputStream.skipBytes(segmentLength);
    }

    /**
     * Reads the SOFn segment, which contains image dimensions.
     */
    private void readSOFSegment() throws IOException {
        int segmentLength = readSegmentLength();
        byte[] data = read(segmentLength);
        height = ((data[1] & 0xff) << 8) | (data[2] & 0xff);
        width  = ((data[3] & 0xff) << 8) | (data[4] & 0xff);
    }

    /**
     * Reads EXIF or XMP data from the APP1 segment.
     */
    private void readAPP1Segment() throws IOException {
        int segmentLength               = readSegmentLength();
        long pos                        = inputStream.getStreamPosition();
        byte[] segment                  = read(segmentLength);
        final byte[] closingXMPTagBytes =
                "</x:xmpmeta>".getBytes(StandardCharsets.UTF_8);

        if (JPEGUtils.isEXIFSegment(segment)) {
            exifOffset = (int) pos + 6;
            exif = JPEGUtils.readEXIFData(segment);
        } else if (JPEGUtils.isStandardXMPSegment(segment)) {
            byte[] data = JPEGUtils.readStandardXMPData(segment);
            xmpChunks.add(data);
        } else if (JPEGUtils.isExtendedXMPSegment(segment)) {
            // Although the XMP spec pt. 3 says that each ExtendedXMP chunk
            // must start with a null-terminated signature string, a GUID,
            // etc., some JPEG writers (e.g. Google Pixel 6 & 7a) will include
            // the signature string in the first ExtendedXMP segment and then
            // just continue the data in subsequent segments without any
            // signatures.
            byte[] data = JPEGUtils.readExtendedXMPData(segment);
            xmpChunks.add(data);

            isReadingExtendedXMP = (ArrayUtils.indexOf(
                    segment,
                    closingXMPTagBytes,
                    segmentLength - closingXMPTagBytes.length) == -1);
        } else if (isReadingExtendedXMP) {
            xmpChunks.add(segment);
            isReadingExtendedXMP = (ArrayUtils.indexOf(
                    segment,
                    closingXMPTagBytes,
                    segmentLength - closingXMPTagBytes.length) == -1);
        }
    }

    /**
     * <p>ICC profiles are packed into {@literal APP2} segments. Profiles
     * larger than 65,533 bytes are split into chunks which appear in
     * sequential segments. The segment marker is immediately followed (in
     * order) by:</p>
     *
     * <ol>
     *     <li>{@link Constants#ICC_SEGMENT_HEADER}</li>
     *     <li>The sequence number of the chunk (one byte)</li>
     *     <li>The total number of chunks (one byte)</li>
     *     <li>Profile data</li>
     * </ol>
     */
    private void readAPP2Segment() throws IOException {
        int segmentLength = readSegmentLength();
        byte[] data = read(segmentLength);

        if (data[0] == 'I' && data[1] == 'C' && data[2] == 'C') {
            final int headerLength = Constants.ICC_SEGMENT_HEADER.length + 2; // +2 for chunk sequence and chunk count
            data = Arrays.copyOfRange(data, headerLength, segmentLength);
            iccProfileChunks.add(data);
        }
    }

    private void readAPP13Segment() throws IOException {
        int segmentLength = readSegmentLength();
        byte[] data = read(segmentLength);

        if (JPEGUtils.isPhotoshopSegment(data)) {
            // Check for IPTC data.
            if (data[14] == '8' && data[15] == 'B' && data[16] == 'I' &&
                    data[17] == 'M' && data[18] == 0x04 && data[19] == 0x04) {
                iptc = Arrays.copyOfRange(data, 26, data.length);
            }
        }
    }

    private void readAPP14Segment() throws IOException {
        int segmentLength = readSegmentLength();
        byte[] data = read(segmentLength);

        if (JPEGUtils.isAdobeSegment(data)) {
            hasAdobeSegment = true;
            colorTransform = AdobeColorTransform.forAPP14Value(data[11] & 0xff);
        }
    }

    private byte[] read(int length) throws IOException {
        byte[] data = new byte[length];
        inputStream.readFully(data);
        return data;
    }

    private void readThumbnailInfo() throws IOException {
        readImage();
        if (thumbCompression == -1) {
            byte[] exifBytes = getEXIF();
            if (exifBytes != null) {
                DirectoryReader reader = new DirectoryReader();
                reader.addTagSet(new EXIFBaselineTIFFTagSet());
                try (ImageInputStream is = new ByteArrayImageInputStream(exifBytes)) {
                    reader.setSource(is);
                    DirectoryIterator it = reader.iterator();
                    if (it.hasNext()) {
                        it.next();
                    }
                    if (it.hasNext()) {
                        Directory dir    = it.next();
                        thumbCompression = (int) dir.getField(EXIFBaselineTIFFTagSet.COMPRESSION).getFirstValue();
                        int soiOffset    = Math.toIntExact((long) dir.getField(EXIFBaselineTIFFTagSet.JPEG_INTERCHANGE_FORMAT).getFirstValue());
                        thumbDirOffset   = soiOffset + getEXIFOffset();
                        thumbLength      = Math.toIntExact((long) dir.getField(EXIFBaselineTIFFTagSet.JPEG_INTERCHANGE_FORMAT_LENGTH).getFirstValue());
                    }
                }
            }
        }
    }

}
