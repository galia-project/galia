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
import org.eclipse.jetty.io.Content;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Implementation backed by a {@link org.eclipse.jetty.server.Response}.
 */
public final class JettyResponseWrapper implements Response {

    private final org.eclipse.jetty.server.Response jettyResponse;
    private boolean isStreamOpen;

    public JettyResponseWrapper(org.eclipse.jetty.server.Response jettyResponse) {
        this.jettyResponse = jettyResponse;
    }

    @Override
    public void addCookie(Cookie cookie) {
        jettyResponse.getHeaders().put("Set-Cookie", cookie.toString());
    }

    @Override
    public void addHeader(String name, String value) {
        jettyResponse.getHeaders().add(name, value);
    }

    @Override
    public Headers getHeaders() {
        final Headers headers = new Headers();
        jettyResponse.getHeaders().forEach(h ->
                headers.add(h.getName(), h.getValue()));
        return headers;
    }

    @Override
    public OutputStream openBodyStream() throws IOException {
        if (isStreamOpen) {
            throw new IllegalStateException("A stream has already been opened");
        }
        isStreamOpen = true;
        return Content.Sink.asOutputStream(jettyResponse);
    }

    @Override
    public Status getStatus() {
        return new Status(jettyResponse.getStatus());
    }

    @Override
    public void setHeader(String name, String value) {
        jettyResponse.getHeaders().put(name, value);
    }

    @Override
    public void setStatus(Status status) {
        setStatus(status.code());
    }

    @Override
    public void setStatus(int status) {
        jettyResponse.setStatus(status);
    }
}
