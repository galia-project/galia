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

import is.galia.http.Cookies;
import is.galia.http.Headers;
import is.galia.http.Method;
import is.galia.http.Reference;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface Request {

    Cookies getCookies();

    Headers getHeaders();

    /**
     * @return Stream for reading the request entity. May be {@code null}.
     */
    InputStream openBodyStream() throws IOException;

    Method getMethod();

    /**
     * @return Arguments in the URL path corresponding to the regex groups in
     *         the pattern of the resource's route.
     */
    List<String> getPathArguments();

    /**
     * @return Full request URI including query. Note that this may not be the
     *         URI that the user agent supplies or sees.
     * @see AbstractResource#getPublicReference()
     */
    Reference getReference();

    /**
     * @return Client IP address. Note that this may not be the user agent IP
     *         address, as in the case of e.g. running behind a reverse proxy
     *         server.
     * @see AbstractResource#getCanonicalClientIPAddress()
     */
    String getRemoteAddr();

    /**
     * For testing purposes only.
     */
    void setPathArguments(List<String> pathArguments);

}
