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
import org.eclipse.jetty.io.Content;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation backed by a {@link org.eclipse.jetty.server.Request}.
 */
public final class JettyRequestWrapper implements Request {

    private final org.eclipse.jetty.server.Request jettyRequest;
    private List<String> pathArguments = new ArrayList<>();

    public JettyRequestWrapper(org.eclipse.jetty.server.Request jettyRequest) {
        this.jettyRequest = jettyRequest;
    }

    /**
     * @return New instance. Any changes to it will not be reflected in the
     *         wrapped {@link org.eclipse.jetty.server.Request}.
     */
    @Override
    public Cookies getCookies() {
        final Cookies allCookies = new Cookies();
        getHeaders().getAll("Cookie").forEach(h ->
                allCookies.addAll(Cookies.fromHeaderValue(h.value())));
        return allCookies;
    }

    /**
     * @return New instance. Any changes to it will not be reflected in the
     *         wrapped {@link org.eclipse.jetty.server.Request}.
     */
    @Override
    public Headers getHeaders() {
        final Headers headers = new Headers();
        jettyRequest.getHeaders().forEach(h ->
                headers.add(h.getName(), h.getValue()));
        return new Headers(headers);
    }

    @Override
    public Method getMethod() {
        return Method.valueOf(jettyRequest.getMethod());
    }

    /**
     * @return Arguments in the URL path corresponding to the regex groups in
     *         one of the resource's route patterns.
     */
    @Override
    public List<String> getPathArguments() {
        return Collections.unmodifiableList(pathArguments);
    }

    /**
     * @return Full request URI including query. Note that this may not be the
     *         URI that the user agent supplies or sees. Any changes to the
     *         instance will not be reflected in the wrapped {@link
     *         org.eclipse.jetty.server.Request}.
     * @see AbstractResource#getPublicReference()
     */
    @Override
    public Reference getReference() {
        return new Reference(jettyRequest.getHttpURI().toString());
    }

    /**
     * @return Client IP address. Note that this may not be the user agent IP
     *         address, as in the case of e.g. running behind a reverse proxy
     *         server.
     * @see AbstractResource#getCanonicalClientIPAddress()
     */
    @Override
    public String getRemoteAddr() {
        String addr = jettyRequest.getConnectionMetaData().getRemoteSocketAddress().toString();
        return addr.substring(1, addr.length() - 2);
    }

    /**
     * @return Stream for reading the request entity. May be {@code null}.
     */
    @Override
    public InputStream openBodyStream() throws IOException {
        return Content.Source.asInputStream(jettyRequest);
    }

    @Override
    public void setPathArguments(List<String> pathArguments) {
        this.pathArguments = pathArguments;
    }

}
