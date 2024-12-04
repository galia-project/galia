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

import is.galia.http.Headers;
import is.galia.http.Reference;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Components;
import org.eclipse.jetty.server.ConnectionMetaData;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Context;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpStream;
import org.eclipse.jetty.server.Session;
import org.eclipse.jetty.server.TunnelSupport;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class MockJettyRequest implements org.eclipse.jetty.server.Request {

    private String hostname;
    private int port;
    private String contextPath  = "/";
    private String method       = "GET";
    private Reference reference = new Reference("http://example.org");

    private final Headers headers = new Headers();

    //region org.eclipse.jetty.server.Request methods

    @Override
    public void addFailureListener(Consumer<Throwable> consumer) {
    }

    @Override
    public void addHttpStreamWrapper(Function<HttpStream, HttpStream> function) {
    }

    @Override
    public void addIdleTimeoutListener(Predicate<TimeoutException> predicate) {
    }

    @Override
    public boolean consumeAvailable() {
        return false;
    }

    @Override
    public void demand(Runnable runnable) {
    }

    @Override
    public void fail(Throwable throwable) {
    }

    @Override
    public Object getAttribute(String s) {
        return null;
    }

    @Override
    public Set<String> getAttributeNameSet() {
        return Set.of();
    }

    @Override
    public long getBeginNanoTime() {
        return 0;
    }

    @Override
    public Components getComponents() {
        return null;
    }

    @Override
    public ConnectionMetaData getConnectionMetaData() {
        return new ConnectionMetaData() {
            @Override
            public Object removeAttribute(String s) {
                return null;
            }

            @Override
            public Object setAttribute(String s, Object o) {
                return null;
            }

            @Override
            public Object getAttribute(String s) {
                return null;
            }

            @Override
            public Set<String> getAttributeNameSet() {
                return Set.of();
            }

            @Override
            public HttpConfiguration getHttpConfiguration() {
                return null;
            }

            @Override
            public HttpVersion getHttpVersion() {
                return null;
            }

            @Override
            public String getProtocol() {
                return "";
            }

            @Override
            public Connection getConnection() {
                return null;
            }

            @Override
            public Connector getConnector() {
                return null;
            }

            @Override
            public boolean isPersistent() {
                return false;
            }

            @Override
            public SocketAddress getRemoteSocketAddress() {
                return new InetSocketAddress(hostname, port);
            }

            @Override
            public SocketAddress getLocalSocketAddress() {
                return null;
            }

            @Override
            public String getId() {
                return "";
            }
        };
    }

    @Override
    public Context getContext() {
        return null;
    }

    @Override
    public HttpFields getHeaders() {
        return i -> headers.stream()
                .map(h -> new HttpField(h.name(), h.value()))
                .toList()
                .listIterator();
    }

    @Override
    public long getHeadersNanoTime() {
        return 0;
    }

    @Override
    public HttpURI getHttpURI() {
        return HttpURI.build(reference.toString());
    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public Session getSession(boolean b) {
        return null;
    }

    @Override
    public TunnelSupport getTunnelSupport() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public HttpFields getTrailers() {
        return null;
    }

    @Override
    public Content.Chunk read() {
        return null;
    }

    @Override
    public Object removeAttribute(String s) {
        return null;
    }

    @Override
    public Object setAttribute(String s, Object o) {
        return null;
    }

    //endregion
    //region Custom accessors

    public Headers getMutableHeaders() {
        return headers;
    }

    public Reference getReference() {
        return reference;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setReference(Reference reference) {
        this.reference = reference;
    }

    public void setHost(String host) {
        this.hostname = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
