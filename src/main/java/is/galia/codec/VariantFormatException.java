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

package is.galia.codec;

import is.galia.image.Format;

public class VariantFormatException extends FormatException {

    public VariantFormatException() {
        super(buildMessage(Format.UNKNOWN));
    }

    public VariantFormatException(String message) {
        super(message);
    }

    public VariantFormatException(Format format) {
        super(buildMessage(format));
    }

    private static String buildMessage(Format format) {
        String message = "Unsupported variant format";
        if (format == null) {
            return message;
        }
        if (!Format.UNKNOWN.equals(format)) {
            message += ": " + format.name();
        }
        return message;
    }

}
