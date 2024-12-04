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

package is.galia.http;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>HTTP cookie.</p>
 *
 * @see <a href="http://www.ietf.org/rfc/rfc6265.txt">RFC 6265: HTTP State
 *   Management Mechanism</a>
 */
public record Cookie(String name,
                     String value,
                     String domain,
                     String path,
                     Duration maxAge,
                     boolean secure,
                     boolean httpOnly) {

    public Cookie {
        validateName(name);
        validateValue(value);
    }

    /**
     * @param name  May be empty but not {@code null}.
     * @param value May be empty but not {@code null}.
     * @throws IllegalArgumentException if either argument is {@code null}.
     */
    public Cookie(String name, String value) {
        this(name, value, null, "/", null, false, false);
    }

    /**
     * Copy constructor.
     */
    public Cookie(Cookie cookie) {
        this(cookie.name, cookie.value, cookie.domain, cookie.path,
                cookie.maxAge, cookie.secure, cookie.httpOnly);
    }

    private void validateName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name is null");
        } else if (name.contains(";") || name.contains("=")) {
            throw new IllegalArgumentException("Name contains illegal characters");
        }
    }

    private void validateValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value is null");
        } else if (value.contains(";") || value.contains("=")) {
            throw new IllegalArgumentException("Value contains illegal characters");
        }
    }

    @Override
    public String toString() {
        final List<String> parts = new ArrayList<>();
        parts.add(name + "=" + value);
        if (domain != null && !domain.isBlank()) {
            parts.add("Domain=" + domain);
        }
        if (path != null && !path.isBlank() && !"/".equals(path)) {
            parts.add("Path=" + path);
        }
        if (maxAge != null) {
            parts.add("Max-Age=" + maxAge.toSeconds());
        }
        if (secure) {
            parts.add("Secure");
        }
        if (httpOnly) {
            parts.add("HttpOnly");
        }
        return String.join("; ", parts);
    }

}
