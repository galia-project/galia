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

enum Marker {

    /**
     * Start Of Image, expected to be the very first marker in the stream.
     */
    SOI((byte) 0xff, (byte) 0xd8),

    /**
     * Start Of Frame for baseline DCT images.
     */
    SOF0((byte) 0xff, (byte) 0xc0),

    /**
     * Start Of Frame for extended sequential DCT images.
     */
    SOF1((byte) 0xff, (byte) 0xc1),

    /**
     * Start Of Frame for progressive DCT images.
     */
    SOF2((byte) 0xff, (byte) 0xc2),

    /**
     * Start Of Frame for lossless (sequential) images.
     */
    SOF3((byte) 0xff, (byte) 0xc3),

    /**
     * Start Of Frame for differential sequential DCT images.
     */
    SOF5((byte) 0xff, (byte) 0xc5),

    /**
     * Start Of Frame for differential progressive DCT images.
     */
    SOF6((byte) 0xff, (byte) 0xc6),

    /**
     * Start Of Frame for differential lossless (sequential) images.
     */
    SOF7((byte) 0xff, (byte) 0xc7),

    /**
     * Start Of Frame for extended sequential DCT images.
     */
    SOF9((byte) 0xff, (byte) 0xc9),

    /**
     * Start Of Frame for progressive DCT images.
     */
    SOF10((byte) 0xff, (byte) 0xca),

    /**
     * Start Of Frame for lossless (sequential) images.
     */
    SOF11((byte) 0xff, (byte) 0xcb),

    /**
     * Start Of Frame for differential sequential DCT images.
     */
    SOF13((byte) 0xff, (byte) 0xcd),

    /**
     * Start Of Frame for differential progressive DCT images.
     */
    SOF14((byte) 0xff, (byte) 0xce),

    /**
     * Start Of Frame for differential lossless (sequential) images.
     */
    SOF15((byte) 0xff, (byte) 0xcf),

    /**
     * EXIF data.
     */
    APP1((byte) 0xff, (byte) 0xe1),

    /**
     * ICC profile.
     */
    APP2((byte) 0xff, (byte) 0xe2),

    /**
     * Photoshop.
     */
    APP13((byte) 0xff, (byte) 0xed),

    /**
     * Adobe.
     */
    APP14((byte) 0xff, (byte) 0xee),

    /**
     * Define Huffman Table marker; our effective "stop reading" marker.
     */
    DHT((byte) 0xff, (byte) 0xc4),

    /**
     * Marker not recognized by this reader, which may still be perfectly
     * valid.
     */
    UNKNOWN((byte) 0x00, (byte) 0x00);

    static Marker forBytes(byte byte1, byte byte2) {
        for (Marker marker : values()) {
            if (marker.byte1 == byte1 && marker.byte2 == byte2) {
                return marker;
            }
        }
        return UNKNOWN;
    }

    private final byte byte1, byte2;

    Marker(byte byte1, byte byte2) {
        this.byte1 = byte1;
        this.byte2 = byte2;
    }

    byte[] marker() {
        return new byte[] { byte1, byte2 };
    }

}
