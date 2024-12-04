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

package is.galia.codec.tiff;

import is.galia.codec.SourceFormatException;

import javax.imageio.stream.ImageInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>Combination TIFF and EXIF Image File Directory (IFD) reader.</p>
 *
 * <p>The EXIF container structure is similar to and based on TIFF, and this
 * reader can parse the IFD portions of a TIFF 6.0 file.</p>
 *
 * <p>EXIF 3.0 is supported.</p>
 *
 * <p>BigTIFF is also supported.</p>
 *
 * <p>The EXIF standard includes four tag sets:</p>
 *
 * <ol>
 *     <li>Baseline TIFF</li>
 *     <li>EXIF</li>
 *     <li>GPS</li>
 *     <li>Interoperability</li>
 * </ol>
 *
 * <p>The {@link EXIFBaselineTIFFTagSet Baseline TIFF tags defined in EXIF} are
 * a subset of {@link BaselineTIFFTagSet the tags in the TIFF
 * specification}.</p>
 *
 * <p>Instances must be initialized with one or more {@link TagSet tag sets}
 * prior to reading. There are several such implementations in this package,
 * and custom ones can also be supplied. {@link TagSet#newBaselineSuperset()}
 * can be used to obtain a superset of all baseline tags, including support for
 * the EXIF sub-IFDs and all other known sub-IFDs.</p>
 */
public final class DirectoryReader {

    private static final byte[] INTEL_BYTE_ORDER = { 0x49, 0x49 };
    private static final char[] JFIF_APP1_EXIF_HEADER =
            "Exif\u0000\u0000".toCharArray();

    private final Set<TagSet> tagSets = new HashSet<>();

    private ImageInputStream inputStream;
    private boolean isReadingStarted, isBigTIFF;

    /**
     * Offset of the TIFF signature within the stream.
     */
    private long startOffset = JFIF_APP1_EXIF_HEADER.length;

    public void addTagSet(TagSet tagSet) {
        tagSets.add(tagSet);
    }

    ImageInputStream getInputStream() {
        return inputStream;
    }

    /**
     * When reading from e.g. a JPEG APP1 segment, the IFD data may be preceded
     * by an EXIF marker, which is not part of the IFD. If that is the case,
     * the return value will be the offset of the IFD relative to the starting
     * position of the stream.
     */
    long getStartOffset() {
        return startOffset;
    }

    public TagSet getTagSet(int ifdPointer) {
        return tagSets.stream()
                .filter(t -> t.getIFDPointerTag() == ifdPointer)
                .findFirst()
                .orElse(null);
    }

    /**
     * @return Unmodifiable instance containing all {@link TagSet tag sets}
     *         recognized by the reader.
     */
    public Set<TagSet> getTagSets() {
        return Collections.unmodifiableSet(tagSets);
    }

    public boolean isBigTIFF() {
        if (!isReadingStarted) {
            throw new IllegalStateException(
                    "This method can only be called after reading.");
        }
        return isBigTIFF;
    }

    /**
     * @param inputStream Instance initialized to the starting position of the
     *                    EXIF data (or JPEG APP0 EXIF segment data).
     */
    public void setSource(ImageInputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * @return Iterator over all top-level sibling IFDs.
     */
    public DirectoryIterator iterator() throws IOException {
        if (tagSets.stream().noneMatch(ts -> ts.getIFDPointerTag() == 0)) {
            throw new IllegalStateException("You must add at least one " +
                    "baseline " + TagSet.class.getSimpleName() + " to read.");
        } else if (inputStream == null) {
            throw new IllegalStateException("Source not set");
        }

        isReadingStarted = true;
        final long initialPos = inputStream.getStreamPosition();

        // If we are reading the IFD from e.g. a JPEG APP1 segment, it may
        // be preceded by an EXIF marker, which is not part of the IFD.
        // Here we'll check for that and skip past it if it exists.
        inputStream.mark();
        final char[] chars = readChars(JFIF_APP1_EXIF_HEADER.length);
        if (Arrays.equals(chars, JFIF_APP1_EXIF_HEADER)) {
            startOffset = inputStream.getStreamPosition();
        } else {
            startOffset = initialPos;
            inputStream.reset();
        }
        // Bytes 0-1 contain the byte alignment.
        byte[] bytes = readBytes(2);
        if (Arrays.equals(bytes, INTEL_BYTE_ORDER)) {
            inputStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        }
        // Bytes 2-3 must contain either the number 42, identifying the
        // data as TIFF, or 43, identifying it as BigTIFF.
        int typeCode = inputStream.readShort();
        if (typeCode == 42) {
            isBigTIFF = false;
        } else if (typeCode == 43) {
            isBigTIFF = true;
            if (inputStream.readShort() != 8 ||     // bytes 4-5
                    inputStream.readShort() != 0) { // bytes 6-7
                throw new SourceFormatException("Invalid BigTIFF file " +
                        "(unexpected value at offset " +
                        inputStream.getStreamPosition() + ")");
            }
        } else {
            throw new SourceFormatException(
                    "Unsupported file type: " + typeCode);
        }

        // Read the offset of the first IFD (4 bytes for standard TIFF, 8 bytes
        // for BigTIFF). The first IFD may be anywhere in the file, but is
        // often immediately next.
        if (isBigTIFF) {
            long ifdOffset = inputStream.readLong(); // bytes 8-15
            seek(ifdOffset);
        } else {
            long ifdOffset = inputStream.readUnsignedInt();
            seek(ifdOffset);
        }
        return new DirectoryIterator(this);
    }

    /**
     * Reads all directories.
     */
    public List<Directory> readAll() throws IOException {
        List<Directory> dirs = new ArrayList<>();
        DirectoryIterator it = iterator();
        while (it.hasNext()) {
            dirs.add(it.next());
        }
        return dirs;
    }

    /**
     * Assists in testing.
     */
    void removeTagSets() {
        tagSets.clear();
    }

    /**
     * Reads only the first directory.
     */
    public Directory readFirst() throws IOException {
        DirectoryIterator it = iterator();
        if (it.hasNext()) {
            return it.next();
        }
        return null;
    }

    private byte[] readBytes(int length) throws IOException {
        byte[] bytes = new byte[length];
        inputStream.readFully(bytes);
        return bytes;
    }

    private char[] readChars(int length) throws IOException {
        byte[] bytes = readBytes(length);
        return new String(bytes, StandardCharsets.US_ASCII).toCharArray();
    }

    /**
     * @param offset Offset relative to the beginning of the data.
     */
    private void seek(long offset) throws IOException {
        try {
            // Adjust the offset to be relative to the beginning of the TIFF
            // signature.
            inputStream.seek(offset + startOffset);
        } catch (IndexOutOfBoundsException e) {
            throw new EOFException(e.getMessage());
        }
    }

}
