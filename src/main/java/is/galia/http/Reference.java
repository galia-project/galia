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

import is.galia.util.StringUtils;
import okhttp3.HttpUrl;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * <p>Immutable URI class.</p>
 *
 * <p>Although instances cannot be modified, new modified versions can be
 * created via {@link #rebuilder()}.</p>
 *
 * <p>Only the schemes in {@link #SUPPORTED_SCHEMES} are supported.</p>
 *
 * <p>Components are stored unencoded in order to enable assembly of a
 * partially or fully encoded URI string via one of the {@link #toString}
 * overloads.</p>
 *
 * <p>Also, no checked exceptions are thrown, making it less of a pain to use
 * than {@link java.net.URL} and {@link java.net.URI}.</p>
 *
 * @see <a href="https://tools.ietf.org/html/rfc3986">RFC 3986: Uniform
 * Resource Identifier</a>
 */
public final class Reference {

    /**
     * {@link Reference#builder() Builds new instances from scratch} and also
     * {@link Reference#rebuilder() builds new instances based on existing
     * ones}.
     */
    public static final class Builder {

        private final Reference reference;

        private Builder() {
            this.reference = new Reference();
        }

        private Builder(Reference reference) {
            this.reference = new Reference(reference);
        }

        /**
         * @param path Path segment(s) to add to the end of the current path.
         *             If a leading slash is missing, it is added.
         */
        public Builder appendPath(String path) {
            reference.appendPath(path);
            return this;
        }

        /**
         * <p>Applies any reverse proxying-related request headers present in
         * the argument. The supported headers are:</p>
         *
         * <ul>
         *     <li>{@code X-Forwarded-Proto}</li>
         *     <li>{@code X-Forwarded-Host}</li>
         *     <li>{@code X-Forwarded-Port}</li>
         *     <li>{@code X-Forwarded-BasePath}</li>
         * </ul>
         *
         * @param headers Request headers.
         */
        public Builder applyProxyHeaders(Headers headers) {
            reference.applyProxyHeaders(headers);
            return this;
        }

        /**
         * @param path Path to prepend to the current path.
         */
        public Builder prependPath(String path) {
            reference.prependPath(path);
            return this;
        }

        public Builder withFragment(String fragment) {
            reference.setFragment(fragment);
            return this;
        }

        public Builder withHost(String host) {
            reference.setHost(host);
            return this;
        }

        /**
         * @param path New path. Any trailing slash is stripped.
         * @see #prependPath(String)
         */
        public Builder withPath(String path) {
            reference.setPath(path);
            return this;
        }

        /**
         * Updates a segment (token between slashes) of the path.
         *
         * @param segmentIndex Zero-based path segment index.
         * @param pathSegment  Path segment.
         * @throws IndexOutOfBoundsException if the given index is greater than
         *         the number of path segments.
         */
        public Builder withPathSegment(int segmentIndex,
                                       String pathSegment) {
            reference.setPathSegment(segmentIndex, pathSegment);
            return this;
        }

        public Builder withPort(int port) {
            reference.setPort(port);
            return this;
        }

        public Builder withQuery(Query query) {
            reference.setQuery(query);
            return this;
        }

        public Builder withScheme(String scheme) {
            reference.setScheme(scheme);
            return this;
        }

        public Builder withSecret(String secret) {
            reference.setSecret(secret);
            return this;
        }

        public Builder withUser(String user) {
            reference.setUser(user);
            return this;
        }

        public Builder withoutFragment() {
            reference.setFragment(null);
            return this;
        }

        public Builder withoutPath() {
            reference.setPath(null);
            return this;
        }

        public Builder withoutPort() {
            reference.setPort(-1);
            return this;
        }

        public Builder withoutQuery() {
            reference.setQuery(null);
            return this;
        }

        public Builder withoutTrailingSlashInPath() {
            reference.setPath(StringUtils.stripEnd(reference.path, "/"));
            return this;
        }

        public Builder withoutSecret() {
            reference.setSecret(null);
            return this;
        }

        public Builder withoutUser() {
            reference.setUser(null);
            return this;
        }

        public Reference build() {
            if (reference.scheme.isBlank()) {
                throw new NullPointerException("Scheme is required.");
            } else if (!SUPPORTED_SCHEMES.contains(reference.scheme)) {
                throw new IllegalArgumentException(
                        "Only the following schemes are supported: " +
                                String.join(", ", SUPPORTED_SCHEMES));
            } else if (reference.host.isBlank() &&
                    ("http".equals(reference.scheme) ||
                            "https".equals(reference.scheme))) {
                throw new NullPointerException("Host is required with the " +
                        reference.scheme + " scheme.");
            } else if (!reference.secret.isBlank() && reference.user.isBlank()) {
                throw new IllegalArgumentException(
                        "User is required when a secret is present.");
            }
            return reference;
        }

    }

    public static final Set<String> SUPPORTED_SCHEMES =
            Set.of("http", "https", "file");

    /**
     * N.B.: An empty path is represented by an empty string rather than a
     * slash.
     */
    private String scheme = "http", user = "", secret = "", host = "",
            path = "", fragment = "";
    private int port = -1;
    private Query query = new Query();

    public static Builder builder() {
        return new Builder();
    }

    public static String decode(String encoded) {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    public static String encode(String decoded) {
        return URLEncoder.encode(decoded, StandardCharsets.UTF_8)
                .replace("%3A", ":").replace("%3B", ";").replace("%7E", "~");
    }

    /**
     * Constructs an &quot;empty&quot; instance with the {@code http} scheme.
     */
    public Reference() {}

    /**
     * (Deep) copy constructor.
     */
    public Reference(Reference reference) {
        setUser(reference.getUser());
        setSecret(reference.getSecret());
        setScheme(reference.getScheme());
        setHost(reference.getHost());
        setPort(reference.getPort());
        setPath(reference.getPath());
        setQuery(new Query(reference.getQuery()));
        setFragment(reference.getFragment());
    }

    /**
     * @throws IllegalArgumentException if the argument is not a valid URI or
     *         has an unrecognized scheme.
     */
    public Reference(String uri) {
        if (uri.startsWith("file://")) {
            setScheme("file");
            String pathStr = uri.replace("\\", "/").replaceAll("^file://", "");
            setPath(pathStr);
        } else if (uri.startsWith("s3://")) {
            setScheme("s3");
            String pathStr = uri.replaceAll("^s3://", "");
            setPath(pathStr);
        } else {
            HttpUrl okURL = HttpUrl.get(uri);
            setUser(okURL.username());
            setSecret(okURL.password());
            setScheme(okURL.scheme());
            setHost(okURL.host());
            setPort(okURL.port());
            String path = okURL.encodedPath();
            setPath("/".equals(path) ? "" : path);
            if (okURL.query() != null) {
                setQuery(new Query(okURL.encodedQuery()));
            }
            setFragment(okURL.fragment());
        }
    }

    public Reference(URI uri) {
        String userInfo = uri.getUserInfo();
        if (userInfo != null) {
            String[] parts = userInfo.split(":");
            setUser(parts[0]);
            setSecret(parts[1]);
        }
        setScheme(uri.getScheme());
        setHost(uri.getHost());
        setPort(uri.getPort());
        setPath(uri.getPath());
        if (uri.getQuery() != null) {
            setQuery(new Query(uri.getQuery()));
        }
        setFragment(uri.getFragment());
    }

    /**
     * Constructor for a file URI.
     */
    public Reference(Path path) {
        setScheme("file");
        String pathStr = path.toString().replace("\\", "/");
        setPath(pathStr);
    }

    /**
     * @param addedPath Path to add to the end of the current path. If a
     *                  leading slash is missing, it is added, and any trailing
     *                  slash is stripped.
     */
    private void appendPath(String addedPath) {
        if (addedPath == null) {
            return;
        }
        if (!addedPath.startsWith("/")) {
            addedPath = "/" + addedPath;
        }
        if (path != null) {
            path = StringUtils.stripEnd(path, "/") + addedPath;
        } else {
            path = addedPath;
        }
    }

    private void applyProxyHeaders(Headers headers) {
        // N.B.: Header values may be comma-separated lists indicating a
        // chain of reverse proxies in order from closest-to-the-client to
        // closest-to-this-application.

        // Apply the protocol.
        final String protoHeader = headers.getFirstValue("X-Forwarded-Proto", "");
        if (!protoHeader.isBlank()) {
            String proto = protoHeader.split(",")[0].trim();
            setScheme(proto);
        }
        // Apply the host.
        boolean hostContainsPort = false;
        final String hostHeader = headers.getFirstValue("X-Forwarded-Host", "");
        if (!hostHeader.isBlank()) {
            String host = hostHeader.split(",")[0];
            host = host.substring(host.indexOf("://") + 1).trim();
            // The host may include a colon-separated port number.
            String[] parts = host.split(":");
            setHost(parts[0]);
            if (parts.length > 1) {
                hostContainsPort = true;
                setPort(Integer.parseInt(parts[1]));
            }
        }
        // Apply the port.
        // The port is obtained from the following in order of preference:
        // 1. The X-Forwarded-Port header
        // 2. The port in the X-Forwarded-Host header
        // 3. The default port of the protocol in the X-Forwarded-Proto header
        final String portHeader = headers.getFirstValue("X-Forwarded-Port", "");
        if (!portHeader.isBlank()) {
            String portStr = portHeader.split(",")[0].trim();
            setPort(Integer.parseInt(portStr));
        } else if (!hostContainsPort && !protoHeader.isBlank()) {
            setPort("https".equalsIgnoreCase(protoHeader) ? 443 : 80);
        }
        // Apply the path.
        final String basePathHeader = headers.getFirstValue("X-Forwarded-BasePath", "");
        if (!basePathHeader.isBlank()) {
            String path = basePathHeader.split(",")[0].trim();
            prependPath(StringUtils.stripEnd(path, "/"));
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Reference) {
            return obj.toString().equals(toString());
        }
        return false;
    }

    /**
     * @return Authority (userinfo + host + port). Never {@code null}. If the
     *         port is the standard port for the scheme, it is omitted.
     */
    public String getAuthority() {
        final StringBuilder builder = new StringBuilder();
        if (!"file".equalsIgnoreCase(getScheme())) {
            if (getUser() != null && !getUser().isBlank()) {
                builder.append(encode(getUser()));
                if (getSecret() != null && !getSecret().isBlank()) {
                    builder.append(":");
                    builder.append(encode(getSecret()));
                }
                builder.append("@");
            }
            builder.append(getHost());
            if (("http".equalsIgnoreCase(getScheme()) && getPort() > 0 && getPort() != 80) ||
                    ("https".equalsIgnoreCase(getScheme()) && getPort() > 0 && getPort() != 443)) {
                builder.append(":");
                builder.append(getPort());
            }
        }
        return builder.toString();
    }

    /**
     * @return The fragment, or an empty string if not set.
     */
    public String getFragment() {
        return fragment;
    }

    /**
     * @return The host, or an empty string if not set.
     */
    public String getHost() {
        return host;
    }

    /**
     * @return The path, or an empty string if not set.
     */
    public String getPath() {
        return path;
    }

    /**
     * @return The path segments, or an empty instance if not set.
     */
    public List<String> getPathSegments() {
        String path = StringUtils.stripStart(getPath(), "/");
        if (!path.isBlank()) {
            return Arrays.asList(path.split("/"));
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * @return String after the last period in the last path segment, or an
     *         empty string if the last path segment does not contain a
     *         period or the period is at the first index in the segment.
     */
    public String getPathExtension() {
        List<String> segments = getPathSegments();
        if (!segments.isEmpty()) {
            String segment = segments.getLast();
            int index = segment.lastIndexOf(".");
            if (index > 0) {
                return segment.substring(index + 1);
            }
        }
        return "";
    }

    /**
     * @return {@code -1} unless the instance has a nonstandard port number
     *         set.
     */
    public int getPort() {
        return port;
    }

    /**
     * @return The query, or an empty instance if not set.
     */
    public Query getQuery() {
        return query;
    }

    /**
     * @return Lowercase scheme, or an empty string if not set.
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * @return The secret, or an empty string if not set.
     */
    public String getSecret() {
        return secret;
    }

    /**
     * @return The user, or an empty string if not set.
     */
    public String getUser() {
        return user;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * @param path Path to prepend. Any trailing slash is stripped.
     */
    private void prependPath(String path) {
        if (path == null) {
            return;
        } else if (!path.startsWith("/")) {
            path = "/" + path;
        }
        this.path = StringUtils.stripEnd(path, "/") + this.path;
    }

    /**
     * @return {@link Builder} of a new instance based on this one.
     */
    public Builder rebuilder() {
        return new Builder(this);
    }

    private void setFragment(String fragment) {
        if (fragment == null) {
            fragment = "";
        }
        this.fragment = fragment;
    }

    private void setHost(String host) {
        if (host == null) {
            host = "";
        }
        this.host = host;
    }

    /**
     * @param path New path. Any trailing slash is stripped.
     * @see #prependPath(String)
     */
    private void setPath(String path) {
        if (path == null) {
            path = "";
        } else if (!path.startsWith("/") && !path.isBlank()) {
            path = "/" + path;
        }
        this.path = path;
    }

    /**
     * Updates a segment (token between slashes) of the path.
     *
     * @param segmentIndex Zero-based path component index.
     * @param segment      Path segment.
     * @throws IndexOutOfBoundsException if the given index is greater than the
     *                                   number of path components minus one.
     */
    public void setPathSegment(int segmentIndex, String segment) {
        List<String> segments = new ArrayList<>(getPathSegments());
        int length = segments.size();
        segment = StringUtils.stripStart(segment, "/");
        segment = StringUtils.stripEnd(segment, "/");
        if (segmentIndex < length) {
            segments.set(segmentIndex, segment);
        } else if (segmentIndex == length) {
            segments.add(segment);
        } else {
            throw new IndexOutOfBoundsException("Index " + segmentIndex +
                    " out of bounds for length " + length);
        }
        setPath("/" + String.join("/", segments));
    }

    private void setPort(int port) {
        this.port = port;
    }

    private void setQuery(Query query) {
        if (query == null) {
            query = new Query();
        }
        this.query = query;
    }

    private void setScheme(String scheme) {
        if (scheme != null) {
            this.scheme = scheme.toLowerCase();
        } else {
            this.scheme = null;
        }
    }

    private void setSecret(String secret) {
        if (secret == null) {
            secret = "";
        }
        this.secret = secret;
    }

    private void setUser(String user) {
        if (user == null) {
            user = "";
        }
        this.user = user;
    }

    /**
     * @return URI string with no additional encoding applied.
     */
    @Override
    public String toString() {
        return toString(false);
    }

    /**
     * Overload that encodes all parts of the URI except the path, which is
     * assumed to already be encoded.
     */
    public String toString(boolean encodePath) {
        StringBuilder builder = new StringBuilder();
        builder.append(getScheme());
        builder.append("://");
        builder.append(getAuthority());
        if ("s3".equals(scheme)) {
            builder.append(path.substring(1));
        } else if (!path.isBlank() && !"/".equals(path)) {
            if (encodePath) {
                builder.append(encode(path));
            } else {
                builder.append(path);
            }
        }
        if (!query.isEmpty()) {
            if (path.isBlank()) {
                builder.append("/");
            }
            builder.append("?");
            builder.append(query);
        }
        if (!fragment.isBlank()) {
            builder.append("#");
            builder.append(fragment);
        }
        return builder.toString();
    }

    public URI toURI() {
        return URI.create(toString());
    }

}
