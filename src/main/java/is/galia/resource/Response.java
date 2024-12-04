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

package is.galia.resource;

import is.galia.http.Cookie;
import is.galia.http.Headers;
import is.galia.http.Status;

import java.io.IOException;
import java.io.OutputStream;

public interface Response {

    void addCookie(Cookie cookie);

    /**
     * Adds a header with the given name without replacing any same-named
     * headers that already exist.
     */
    void addHeader(String name, String value);

    /**
     * @return Read-only instance.
     */
    Headers getHeaders();

    Status getStatus();

    OutputStream openBodyStream() throws IOException;

    /**
     * Replaces all headers with the given name with a header with the given
     * value.
     */
    void setHeader(String name, String value);

    void setStatus(Status status);

    void setStatus(int status);

}
