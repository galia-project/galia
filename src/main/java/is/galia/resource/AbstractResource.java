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

import is.galia.Application;
import is.galia.auth.CredentialStore;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.delegate.Delegate;
import is.galia.delegate.DelegateException;
import is.galia.delegate.DelegateFactory;
import is.galia.http.Method;
import is.galia.http.Reference;
import is.galia.http.Status;
import is.galia.image.Format;
import is.galia.image.ScaleConstraint;
import is.galia.util.StringUtils;
import org.slf4j.Logger;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>Abstract HTTP resource. Instances should subclass and override one or
 * more of the HTTP-method-specific methods {@link #doGET()} etc., and may
 * optionally use {@link #doInit()} and {@link #destroy()}.</p>
 */
public abstract class AbstractResource {

    public static final String PUBLIC_IDENTIFIER_HEADER = "X-Forwarded-ID";

    static final String RESPONSE_CONTENT_DISPOSITION_QUERY_ARG =
            "response-content-disposition";

    /**
     * Set by {@link #getDelegate()}.
     */
    private Delegate delegate;

    private final RequestContext requestContext = new RequestContext();
    private Request request;
    private Response response;

    /**
     * URL-decodes and un-slashes the given path component.
     *
     * @param pathComponent Raw path component.
     * @return              Decoded path component.
     */
    protected static String decodePathComponent(String pathComponent) {
        final String decodedComponent = Reference.decode(pathComponent);
        return StringUtils.decodeSlashes(decodedComponent);
    }

    /**
     * <p>Returns a sanitized value for a {@code Content-Disposition} header
     * based on the value of the {@link #RESPONSE_CONTENT_DISPOSITION_QUERY_ARG}
     * query argument.</p>
     *
     * <p>If the disposition is {@code attachment} and the filename is not
     * set, it is set to a reasonable value based on the given identifier and
     * output format.</p>
     *
     * @param queryArg      Value of the unsanitized {@link
     *                      #RESPONSE_CONTENT_DISPOSITION_QUERY_ARG} query
     *                      argument.
     * @param identifierStr Identifier or meta-identifier.
     * @param outputFormat  Output format.
     * @return              Value for a {@code Content-Disposition} header,
     *                      which may be {@code null}.
     */
    private static String getSafeContentDisposition(String queryArg,
                                                    String identifierStr,
                                                    Format outputFormat) {
        String disposition = null;
        if (queryArg != null) {
            queryArg = URLDecoder.decode(queryArg, StandardCharsets.UTF_8);
            if (queryArg.startsWith("inline")) {
                disposition = "inline; filename=\"" +
                        safeContentDispositionFilename(identifierStr, outputFormat) + "\"";
            } else if (queryArg.startsWith("attachment")) {
                final List<String> dispositionParts = new ArrayList<>(3);
                dispositionParts.add("attachment");

                // Check for ISO-8859-1 filename pattern
                Pattern pattern = Pattern.compile(".*filename=\"?([^\"]*)\"?.*");
                Matcher matcher = pattern.matcher(queryArg);
                String filename;
                if (matcher.matches()) {
                    // Filter out filename-unsafe characters as well as "..".
                    filename = StringUtils.sanitize(
                            matcher.group(1),
                            Pattern.compile("\\.\\."),
                            Pattern.compile(StringUtils.ASCII_FILENAME_UNSAFE_REGEX));
                } else {
                    filename = safeContentDispositionFilename(identifierStr,
                            outputFormat);
                }
                dispositionParts.add("filename=\"" + filename + "\"");

                // Check for Unicode filename pattern
                pattern = Pattern.compile(".*filename\\*= ?(utf-8|UTF-8)''([^\"]*).*");
                matcher = pattern.matcher(queryArg);
                if (matcher.matches()) {
                    // Filter out filename-unsafe characters as well as "..".
                    filename = StringUtils.sanitize(
                            matcher.group(2),
                            Pattern.compile("\\.\\."),
                            Pattern.compile(StringUtils.UNICODE_FILENAME_UNSAFE_REGEX,
                                    Pattern.UNICODE_CHARACTER_CLASS));
                    filename = Reference.encode(filename);
                    dispositionParts.add("filename*= UTF-8''" + filename);
                }
                disposition = String.join("; ", dispositionParts);
            }
        }
        return disposition;
    }

    private static String safeContentDispositionFilename(String identifierStr,
                                                         Format outputFormat) {
        return identifierStr.replaceAll(StringUtils.ASCII_FILENAME_UNSAFE_REGEX, "_") +
                "." + outputFormat.getPreferredExtension();
    }

    /**
     * <p>Initialization method, called after all necessary setters have been
     * called but before any request handler method (like {@link #doGET()}
     * etc.)</p>
     *
     * <p>If an implementation class has anything to do with {@link
     * is.galia.image.Identifier image identifiers}, it should add the
     * request identifier to the {@link #getRequestContext() request
     * context}.</p>
     *
     * <p>Overrides must call {@code super}.</p>
     *
     * @throws Exception upon any error. This may be a {@link
     *         ResourceException} to enable a custom status.
     */
    public void doInit() throws Exception {
        // Add X-Powered-By header.
        response.addHeader("X-Powered-By",
                Application.getName() + "/" + Application.getVersion());

        // Log request info.
        getLogger().info("Handling {} {}",
                request.getMethod(), request.getReference().getPath());
        getLogger().debug("Request headers: {}",
                request.getHeaders().stream()
                        .map(h -> h.name() + ": " +
                                ("Authorization".equals(h.name()) ? "******" : h.value()))
                        .collect(Collectors.joining("; ")));
        // Add general request info to the request context. Subclasses may
        // override (not forgetting to call super) to add more.
        if (DelegateFactory.isDelegateAvailable()) {
            RequestContext context = getRequestContext();
            context.setLocalURI(getRequest().getReference());
            context.setRequestURI(getPublicReference());
            context.setRequestHeaders(getRequest().getHeaders().toMap());
            context.setResourceClass(getClass().getName());
            context.setClientIP(getCanonicalClientIPAddress());
            context.setCookies(getRequest().getCookies().toMap());
            context.setPageNumber(1); // may be overridden below
            context.setScaleConstraint(new ScaleConstraint(1, 1));
        }
    }

    /**
     * <p>Called at the end of the instance's lifecycle.</p>
     *
     * <p>Overrides must call {@code super}.</p>
     */
    public void destroy() {
    }

    /**
     * <p>Must be overridden by implementations that support {@code
     * DELETE}.</p>
     *
     * <p>Overrides must not call {@code super}.</p>
     *
     * @throws Exception upon any error. This may be a {@link
     *         ResourceException} to enable a custom status.
     */
    public void doDELETE() throws Exception {
        response.setStatus(Status.METHOD_NOT_ALLOWED);
    }

    /**
     * <p>Must be overridden by implementations that support {@code GET}.</p>
     *
     * <p>Overrides must not call {@code super}.</p>
     *
     * @throws Exception upon any error. This may be a {@link
     *         ResourceException} to enable a custom status.
     */
    public void doGET() throws Exception {
        response.setStatus(Status.METHOD_NOT_ALLOWED);
    }

    /**
     * This implementation simply calls {@link #doGET}. When that is
     * overridden, this may also be overridden in order to set headers only and
     * not compute a response body.
     *
     * @throws Exception upon any error. This may be a {@link
     *         ResourceException} to enable a custom status.
     */
    public void doHEAD() throws Exception {
        doGET();
    }

    /**
     * If the implementation {@link #getSupportedMethods() supports any
     * methods} other than {@code OPTIONS}, this method sends a correct
     * response to an {@code OPTIONS} request. Otherwise, it returns HTTP 405
     * (Method Not Allowed).
     */
    public final void doOPTIONS() {
        final Set<Method> supportedMethods = getSupportedMethods();
        supportedMethods.add(Method.OPTIONS);
        if (supportedMethods.size() > 1) {
            response.setStatus(Status.NO_CONTENT);
            response.setHeader("Allow", supportedMethods.stream()
                    .map(Method::toString)
                    .collect(Collectors.joining(",")));
        } else {
            response.setStatus(Status.METHOD_NOT_ALLOWED);
        }
    }

    /**
     * <p>Must be overridden by implementations that support {@code PATCH}.</p>
     *
     * <p>Overrides must not call {@code super}.</p>
     *
     * @throws Exception upon any error. This may be a {@link
     *         ResourceException} to enable a custom status.
     */
    public void doPATCH() throws Exception {
        response.setStatus(Status.METHOD_NOT_ALLOWED);
    }

    /**
     * <p>Must be overridden by implementations that support {@code POST}.</p>
     *
     * <p>Overrides must not call {@code super}.</p>
     *
     * @throws Exception upon any error. This may be a {@link
     *         ResourceException} to enable a custom status.
     */
    public void doPOST() throws Exception {
        response.setStatus(Status.METHOD_NOT_ALLOWED);
    }

    /**
     * <p>Must be overridden by implementations that support {@code PUT}.</p>
     *
     * <p>Overrides must not call {@code super}.</p>
     *
     * @throws Exception upon any error. This may be a {@link
     *         ResourceException} to enable a custom status.
     */
    public void doPUT() throws Exception {
        response.setStatus(Status.METHOD_NOT_ALLOWED);
    }

    /**
     * Checks the {@code Authorization} header for credentials that exist in
     * the given {@link CredentialStore}. If not found, sends a {@code
     * WWW-Authenticate} header and throws an exception.
     *
     * @param realm           Basic realm.
     * @param credentialStore Credential store.
     * @throws ResourceException if authentication failed.
     */
    protected final void authenticateUsingBasic(String realm,
                                                CredentialStore credentialStore) {
        boolean isAuthenticated = false;
        String header = getRequest().getHeaders().getFirstValue("Authorization", "");
        if ("Basic ".equals(header.substring(0, Math.min(header.length(), 6)))) {
            String encoded = header.substring(6);
            String decoded = new String(Base64.getDecoder().decode(encoded.getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length == 2) {
                String user = parts[0];
                String secret = parts[1];
                if (secret.equals(credentialStore.getSecret(user))) {
                    isAuthenticated = true;
                }
            }
        }
        if (!isAuthenticated) {
            getResponse().setHeader("WWW-Authenticate",
                    "Basic realm=\"" + realm + "\" charset=\"UTF-8\"");
            throw new ResourceException(Status.UNAUTHORIZED);
        }
    }

    /**
     * Returns the user agent's IP address, respecting the {@code
     * X-Forwarded-For} request header, if present.
     *
     * @return User agent's IP address.
     */
    protected String getCanonicalClientIPAddress() {
        // The value is expected to be in the format: "client, proxy1, proxy2"
        final String forwardedFor =
                getRequest().getHeaders().getFirstValue("X-Forwarded-For", "");
        if (!forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        } else {
            // Fall back to the client IP address.
            return getRequest().getRemoteAddr();
        }
    }

    /**
     * Part of the {@link Resource} contract.
     *
     * @return All routes to which the instance responds.
     */
    abstract public Set<Route> getRoutes();

    /**
     * @return All methods supported by any of the implementation's {@link
     *         #getRoutes() routes}.
     */
    final Set<Method> getSupportedMethods() {
        final Set<Method> supportedMethods = EnumSet.noneOf(Method.class);
        for (Route route : getRoutes()) {
            supportedMethods.addAll(route.requestMethods());
        }
        return supportedMethods;
    }

    /**
     * Returns a map of template variables common to most or all templates.
     *
     * @return Common template variables.
     */
    protected final Map<String, Object> getCommonTemplateVars() {
        final Map<String,Object> vars = new HashMap<>();
        vars.put("version", Application.getVersion());
        try {
            String baseURI = getPublicRootReference().toString();
            // Normalize the base URI. Note that the <base> tag will need it to
            // have a trailing slash.
            if (baseURI.endsWith("/")) {
                baseURI = baseURI.substring(0, baseURI.length() - 2);
            }
            vars.put("baseUri", baseURI);
        } catch (IllegalArgumentException e) {
            throw new IllegalClientArgumentException(e);
        }
        return vars;
    }

    /**
     * Returns the {@link Delegate} instance for the current request. The
     * result is cached and may be {@code null}.
     *
     * @return Instance for the current request.
     */
    protected final Delegate getDelegate() {
        if (delegate == null && DelegateFactory.isDelegateAvailable()) {
            try {
                delegate = DelegateFactory.newDelegate(getRequestContext());
            } catch (DelegateException e) {
                getLogger().error("getDelegate(): {}", e.getMessage());
            }
        }
        return delegate;
    }

    protected String getLocalBasePath() {
        String publicPath = getRequest().getReference().getPath();
        String publicBasePath;
        // If base_uri is set in the configuration, use that.
        String baseURI = Configuration.forApplication().getString(Key.BASE_URI, "");
        if (!baseURI.isEmpty()) {
            Reference baseRef = new Reference(baseURI);
            publicBasePath    = baseRef.getPath();
        } else {
            // If X-Forwarded-BasePath is set, use that.
            publicBasePath = getRequest().getHeaders().getFirstValue("X-Forwarded-BasePath", "");
        }
        return publicPath.replaceAll("^" + publicBasePath, "");
    }

    abstract protected Logger getLogger();

    /**
     * Returns a list of client-preferred media types as expressed in the
     * {@code Accept} request header.
     *
     * @return List of client-preferred media types.
     * @see    <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">
     *         RFC 2616</a>
     */
    protected final List<String> getPreferredMediaTypes() {
        class Preference implements Comparable<Preference> {
            private String mediaType;
            private float qValue;

            @Override
            public int compareTo(Preference o) {
                if (o.qValue < qValue) {
                    return -1;
                } else if (o.qValue > o.qValue) {
                    return 1;
                }
                return 0;
            }
        }

        final List<Preference> preferences = new ArrayList<>();
        final String acceptHeader = request.getHeaders().getFirstValue("Accept");
        if (acceptHeader != null) {
            String[] clauses = acceptHeader.split(",");
            for (String clause : clauses) {
                String[] parts        = clause.split(";");
                Preference preference = new Preference();
                preference.mediaType  = parts[0].trim();
                if ("*/*".equals(preference.mediaType)) {
                    continue;
                }
                if (parts.length > 1) {
                    String q = parts[1].trim();
                    if (q.startsWith("q=")) {
                        q = q.substring(2);
                        preference.qValue = Float.parseFloat(q);
                    }
                } else {
                    preference.qValue = 1;
                }
                preferences.add(preference);
            }
        }
        return preferences.stream()
                .sorted()
                .map(p -> p.mediaType)
                .toList();
    }

    /**
     * <p>The {@link #getRequest() request URI} is not necessarily the URI that
     * the client has supplied, or sees, or should see, as it may have come
     * from a reverse proxy server. This method generates a new instance based
     * on the {@link #getRequest() request URI} and either the {@link
     * Key#BASE_URI} configuration key or {@code X-Forwarded-*} request
     * headers, resulting in something that is more appropriate to expose to a
     * client. For example, when generating URIs of application resources to
     * include in a response, it would be appropriate to generate them using
     * this method.</p>
     *
     * <p>Note that the return value may not be appropriate for exposure to a
     * client in all cases&mdash;for example, any identifier present in the URI
     * path is not translated (slashes substituted, etc.). In those cases it
     * will be necessary to use adjust the path manually.</p>
     *
     * @return New instance.
     * @see #getPublicRootReference()
     */
    protected Reference getPublicReference() {
        Reference ref  = getRequest().getReference();
        String baseURI = Configuration.forApplication().getString(Key.BASE_URI, "");
        if (!baseURI.isEmpty()) {
            Reference baseRef = new Reference(baseURI);
            ref = ref.rebuilder()
                    .withScheme(baseRef.getScheme())
                    .withHost(baseRef.getHost())
                    .withPort(baseRef.getPort())
                    .prependPath(baseRef.getPath())
                    .build();
        } else {
            ref = ref.rebuilder()
                    .applyProxyHeaders(getRequest().getHeaders())
                    .build();
        }
        return ref;
    }

    /**
     * <p>Returns a reference to the base URI of the application.</p>
     *
     * <p>{@link Key#BASE_URI} is respected, if set. Otherwise, the {@code
     * X-Forwarded-*} request headers are respected, if available.</p>
     *
     * @return New instance.
     * @see #getPublicReference()
     */
    protected Reference getPublicRootReference() {
        Reference ref = getRequest().getReference().rebuilder()
                .withoutPath()
                .withoutQuery()
                .build();
        String baseURI = Configuration.forApplication().getString(Key.BASE_URI, "");
        if (!baseURI.isEmpty()) {
            Reference baseRef = new Reference(baseURI);
            ref = ref.rebuilder()
                    .withScheme(baseRef.getScheme())
                    .withHost(baseRef.getHost())
                    .withPort(baseRef.getPort())
                    .withPath(baseRef.getPath())
                    .build();
        } else {
            ref = ref.rebuilder()
                    .applyProxyHeaders(getRequest().getHeaders())
                    .build();
        }
        return ref;
    }

    /**
     * <p>Returns a sanitized value for a {@code Content-Disposition} header
     * based on the value of the {@link #RESPONSE_CONTENT_DISPOSITION_QUERY_ARG}
     * query argument.</p>
     *
     * <p>If the disposition is {@code attachment} and the filename is not
     * set, it will be set to a reasonable value based on the given identifier
     * and output format.</p>
     *
     * @param identifierStr URI identifier.
     * @param outputFormat  Requested image format.
     * @return Value for a {@code Content-Disposition} header, which may be
     *         {@code null}.
     */
    protected String getRepresentationDisposition(String identifierStr,
                                                  Format outputFormat) {
        var queryArg = getRequest().getReference().getQuery()
                .getFirstValue(RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);
        return getSafeContentDisposition(queryArg, identifierStr, outputFormat);
    }

    /**
     * Returns the request being handled.
     *
     * @return The request being handled.
     */
    public final Request getRequest() {
        return request;
    }

    /**
     * @return Instance corresponding to the request. Its properties are
     *         gradually filled in as they become available.
     */
    protected final RequestContext getRequestContext() {
        return requestContext;
    }

    /**
     * Returns the response to be sent.
     *
     * @return The response to be sent.
     */
    public final Response getResponse() {
        return response;
    }

    /**
     * Negotiates a content/media type according to client preferences as
     * expressed in an {@code Accept} header.
     *
     * @param limitToTypes Media types to limit the result to, in order of most
     *                     to least preferred by the application.
     * @return             Negotiated media type, or {@code null} if
     *                     negotiation failed.
     */
    protected final String negotiateContentType(List<String> limitToTypes) {
        return getPreferredMediaTypes().stream()
                .filter(limitToTypes::contains)
                .findFirst()
                .orElse(null);
    }

    /**
     * Sets the request being handled.
     *
     * @param request Request being handled.
     */
    public final void setRequest(Request request) {
        this.request = request;
    }

    /**
     * Sets the response to send.
     *
     * @param response Response that will be sent.
     */
    public final void setResponse(Response response) {
        this.response = response;
    }

}
