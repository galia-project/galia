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

package is.galia.codec.iptc;

import is.galia.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <p>IPTC IIM reader.</p>
 *
 * <p>The default character set for data sets is ASCII. This can be overridden
 * as UTF-8 or ISO-8859-1 depending on the data set corresponding to the
 * {@literal CodedCharacterSet} tag. Many other encodings are supported by IIM
 * but not by this reader.</p>
 *
 * @see <a href="https://www.iptc.org/std/IIM/4.1/specification/IIMV4.1.pdf">
 *     IPTC-NAA Information Interchange Model Version 4</a>
 */
public final class IIMReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(IIMReader.class);

    private static final int INITIAL_DATASET_LIST_SIZE = 50;
    private static final byte[] ISO_8859_1_CCS = new byte[] { 0x1b, 0x25, 0x47 };
    private static final byte[] UTF_8_CCS      = new byte[] { 0x1b, 0x2e, 0x41 };

    private ByteBuffer buffer;

    private final List<DataSet> dataSets =
            new ArrayList<>(INITIAL_DATASET_LIST_SIZE);

    public void setSource(byte[] iptcBytes) {
        buffer = ByteBuffer.wrap(iptcBytes);
    }

    /**
     * @return Unmodifiable instance.
     */
    public List<DataSet> read() {
        final Stopwatch watch = new Stopwatch();

        readNextDataSet();
        updateDataSetEncodings();

        LOGGER.trace("read(): read in {}", watch);
        return Collections.unmodifiableList(dataSets);
    }

    private void readNextDataSet() {
        try {
            if (buffer.get() != 0x1c) {
                return;
            }
        } catch (BufferUnderflowException e) {
            return;
        }
        final int recordNum  = buffer.get() & 0xff;
        final int dataSetNum = buffer.get() & 0xff;
        final Tag tag = Arrays
                .stream(Tag.values())
                .filter(t -> t.getRecord().getRecordNum() == recordNum &&
                        t.getDataSetNum() == dataSetNum)
                .findFirst()
                .orElse(null);
        if (tag != null) {
            int b4 = buffer.get() & 0xff;
            int b5 = buffer.get() & 0xff;
            int dataLength = (b4 << 8) | b5;
            // if it's an extended tag (sec. 4.5.3; highly unlikely since
            // these shouldn't fit into the segment)
            if (b4 >> 7 == 1) {
                int numLengthOctets = (b4 << 8) | b5;
                byte[] lengthOctets = readBytes(numLengthOctets);
                ByteBuffer buf      = ByteBuffer.wrap(lengthOctets);
                dataLength          = buf.getInt();
            }
            byte[] data = readBytes(dataLength);
            dataSets.add(new DataSet(tag, data));
        }
        readNextDataSet();
    }

    private byte[] readBytes(int length) {
        int endIndex = buffer.position() + length;
        byte[] bytes = Arrays.copyOfRange(
                buffer.array(), buffer.position(), endIndex);
        buffer.position(endIndex);
        return bytes;
    }

    /**
     * Searches for a data set with a {@link Tag#CODED_CHARACTER_SET
     * CodedCharacterSet tag}, and updates all the data sets with a
     * corresponding {@link Charset}.
     */
    private void updateDataSetEncodings() {
        dataSets.stream()
                .filter(ds -> Tag.CODED_CHARACTER_SET.equals(ds.getTag()))
                .findFirst()
                .ifPresent(ds -> {
                    // This is an ISO 2022 value.
                    final byte[] ccsBytes = ds.getDataField();
                    Charset charset = null;
                    if (Arrays.equals(UTF_8_CCS, ccsBytes)) {
                        charset = StandardCharsets.UTF_8;
                    } else if (Arrays.equals(ISO_8859_1_CCS, ccsBytes)) {
                        charset = StandardCharsets.ISO_8859_1;
                    }
                    if (charset != null) {
                        final Charset c = charset;
                        dataSets.forEach(s -> s.setStringEncoding(c));
                    }
                });
    }

}
