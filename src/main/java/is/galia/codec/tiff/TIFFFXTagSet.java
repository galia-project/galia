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

/**
 * TIFF for Fax eXtended (TIFF-FX) adds several tags to the main IFD and does
 * not define its own private IFD.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc3949">RFC 3949: File
 * Format for Internet Fax</a>
 */
public class TIFFFXTagSet extends BaselineTIFFTagSet {

    public static final Tag BAD_FAX_LINES             = new Tag(326, "BadFaxLines", false);
    public static final Tag CLEAN_FAX_DATA            = new Tag(327, "CleanFaxData", false);
    public static final Tag CONSECUTIVE_BAD_FAX_LINES = new Tag(328, "ConsecutiveBadFaxLines", false);

    public TIFFFXTagSet() {
        super();
        addTag(BAD_FAX_LINES);
        addTag(CLEAN_FAX_DATA);
        addTag(CONSECUTIVE_BAD_FAX_LINES);
    }

}
