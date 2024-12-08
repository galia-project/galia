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
import java.util.Collections;
import java.util.List;

public class MutableRequest implements Request {

    public static final class Builder {

        private final MutableRequest request = new MutableRequest();

        public MutableRequest.Builder withHeaders(Headers headers) {
            request.headers = headers;
            return this;
        }

        public MutableRequest.Builder withMethod(Method method) {
            request.method = method;
            return this;
        }

        public MutableRequest.Builder withReference(Reference reference) {
            request.reference = reference;
            return this;
        }

        public MutableRequest.Builder withRemoteAddr(String remoteAddr) {
            request.remoteAddr = remoteAddr;
            return this;
        }

        public MutableRequest build() {
            return request;
        }
    }

    private InputStream bodyStream;
    private Method method;
    private Reference reference;
    private Headers headers = new Headers();
    private String remoteAddr;
    private List<String> pathArguments = Collections.emptyList();

    public static MutableRequest.Builder builder() {
        return new MutableRequest.Builder();
    }

    @Override
    public Cookies getCookies() {
        final Cookies allCookies = new Cookies();
        headers.getAll("Cookie").forEach(h ->
                allCookies.addAll(Cookies.fromHeaderValue(h.value())));
        return allCookies;
    }

    @Override
    public Headers getHeaders() {
        return headers;
    }

    /**
     * @return Stream for reading the request entity. May be {@code null}.
     */
    @Override
    public InputStream openBodyStream() throws IOException {
        return bodyStream;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    /**
     * @return Arguments in the URL path corresponding to the regex groups in
     *         the pattern of the resource's route.
     */
    @Override
    public List<String> getPathArguments() {
        return Collections.unmodifiableList(pathArguments);
    }

    /**
     * @return Full request URI including query. Note that this may not be the
     *         URI that the user agent supplies or sees.
     * @see AbstractResource#getPublicReference()
     */
    @Override
    public Reference getReference() {
        return reference;
    }

    /**
     * @return Client IP address. Note that this may not be the user agent IP
     *         address, as in the case of e.g. running behind a reverse proxy
     *         server.
     * @see AbstractResource#getCanonicalClientIPAddress()
     */
    @Override
    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setBodyStream(InputStream bodyStream) {
        this.bodyStream = bodyStream;
    }

    @Override
    public void setPathArguments(List<String> pathArguments) {
        this.pathArguments = pathArguments;
    }

}
