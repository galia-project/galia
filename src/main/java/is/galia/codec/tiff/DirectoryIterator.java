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

import is.galia.util.NumberUtils;

import javax.imageio.stream.ImageInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Iterator over the top-level sibling directories (IFDs) in a TIFF file,
 * reading them on-demand as they are iterated.
 */
public final class DirectoryIterator {

    private final DirectoryReader reader;
    private final ImageInputStream inputStream;
    private final boolean isBigTIFF;

    private long nextIFDOffset = -1;

    DirectoryIterator(DirectoryReader reader) {
        Objects.requireNonNull(reader);
        this.reader      = reader;
        this.inputStream = reader.getInputStream();
        this.isBigTIFF   = reader.isBigTIFF();
    }

    public boolean hasNext() {
        return nextIFDOffset != 0;
    }

    public Directory next() throws IOException {
        if (!hasNext()) {
            throw new NoSuchElementException("No more directories to read");
        }
        TagSet tagSet = reader.getTagSet(0);
        Directory dir = readDirectory(tagSet);
        if (isBigTIFF) {
            nextIFDOffset = inputStream.readLong();
        } else {
            nextIFDOffset = inputStream.readUnsignedInt();
        }
        if (nextIFDOffset > 0) {
            seek(nextIFDOffset);
        }
        return dir;
    }


    private Directory readDirectory(TagSet tagSet) throws IOException {
        final Directory dir  = new Directory(tagSet);
        long numEntries;
        if (isBigTIFF) {
            numEntries = inputStream.readLong();
        } else {
            numEntries = inputStream.readUnsignedShort();
        }
        for (int i = 0; i < numEntries; i++) {
            // Bytes 0-1
            final int tagNum          = inputStream.readUnsignedShort();
            // Bytes 2-3
            final int dataFormat      = inputStream.readUnsignedShort();
            long count;
            if (isBigTIFF) {
                count = inputStream.readLong(); // bytes 4-11
            } else {
                count = inputStream.readUnsignedInt(); // bytes 4-7
            }
            // Will contain a value if <= 4 bytes (or <= 8 bytes for BigTIFF),
            // otherwise an offset
            byte[] rawValue;
            long valueOrOffset;
            if (isBigTIFF) {
                rawValue      = readBytes(8);
                valueOrOffset = NumberUtils.readSignedLong(
                        rawValue, inputStream.getByteOrder());
            } else {
                rawValue      = readBytes(4);
                valueOrOffset = NumberUtils.readUnsignedInt(
                        rawValue, inputStream.getByteOrder());
            }

            if (!tagSet.containsTag(tagNum)) {
                continue;
            }
            final Tag tag             = tagSet.getTag(tagNum);
            // Is the tag value a pointer to a sub-IFD?
            final TagSet subIFDTagSet = reader.getTagSets().stream()
                    .filter(ts -> ts.getIFDPointerTag() == tagNum)
                    .findAny()
                    .orElse(null);

            if (subIFDTagSet != null && tagNum != 0) {
                final long pos = getRelativeStreamPosition();
                seek(valueOrOffset);
                Directory subDir = readDirectory(subIFDTagSet);
                dir.add(tag, subDir);
                seek(pos);
            } else {
                final DataType dataType = DataType.forValue(dataFormat);
                if (dataType == null) {
                    continue; // skip unsupported field types
                }
                final long dataTypeLength = dataType.getNumBytesPerComponent();
                final long valueLength    = dataTypeLength * count;
                if ((valueLength > 4 && !isBigTIFF) || valueLength > 8) {
                    // valueOrOffset contains an offset; seek to it and read it.
                    final long pos = getRelativeStreamPosition();
                    seek(valueOrOffset);
                    rawValue = readBytes((int) valueLength);
                    seek(pos);
                } else {
                    // rawValue contains `count` number of values
                }
                Field field = newField(tag, dataType, rawValue, count);
                dir.add(field);
            }
        }
        return dir;
    }

    /**
     * @param tag      Field tag.
     * @param dataType Data type of the values in {@code rawValue} (not the
     *                 whole {@code rawValue}).
     * @param rawValue Raw field value. There are {@code count} many values
     *                 packed into this value.
     * @param count    Number of values packed in {@code rawValue}.
     */
    private Field newField(final Tag tag,
                           final DataType dataType,
                           final byte[] rawValue,
                           final long count) {
        final int dataTypeLength = dataType.getNumBytesPerComponent();
        final Field field        = Field.forTag(tag, dataType);
        // N.B.: All this special per-tag and per-data type handling must be
        // duplicated in DirectorySerializer and DirectoryDeserializer.

        // For ASCII and UTF8, the count is the character count. In that case
        // we ignore it and just assemble the chars into a string.
        // For BYTE and UNDEFINED, the count is the byte length, which we also
        // ignore, and assemble a byte array.
        // The EXIF spec does define some UNDEFINED types and explains their
        // data format. We will just store their raw bytes and then convert
        // them on the way out.
        if (DataType.ASCII.equals(dataType) || DataType.UTF8.equals(dataType)) {
            ((StringField) field).setValue((String) dataType.decode(rawValue, inputStream.getByteOrder()));
        } else if (DataType.BYTE.equals(dataType) || DataType.UNDEFINED.equals(dataType)) {
            ((ByteArrayField) field).setValue(rawValue);
        } else {
            for (int c = 0; c < count; c++) {
                byte[] value = Arrays.copyOfRange(
                        rawValue,
                        c * dataTypeLength,
                        c * dataTypeLength + dataTypeLength);
                Object decodedValue = dataType.decode(value, inputStream.getByteOrder());
                ((MultiValueField) field).addValue(decodedValue);
            }
        }
        return field;
    }

    private byte[] readBytes(int length) throws IOException {
        byte[] bytes = new byte[length];
        inputStream.readFully(bytes, 0, length);
        return bytes;
    }

    private long getRelativeStreamPosition() throws IOException {
        return inputStream.getStreamPosition() - reader.getStartOffset();
    }

    /**
     * @param offset Offset relative to the beginning of the data.
     */
    private void seek(long offset) throws IOException {
        // Adjust the offset to be relative to the beginning of the TIFF
        // signature.
        try {
            inputStream.seek(offset + reader.getStartOffset());
        } catch (IndexOutOfBoundsException e) {
            throw new EOFException(e.getMessage());
        }
    }

}
