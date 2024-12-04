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

import is.galia.auth.AuthInfo;
import is.galia.auth.Authorizer;
import is.galia.auth.AuthorizerFactory;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.delegate.DelegateFactory;
import is.galia.http.Reference;
import is.galia.http.Status;
import is.galia.image.Identifier;
import is.galia.image.MetaIdentifier;
import is.galia.image.ScaleConstraint;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class to be extended by all {@link Resource} implementations that
 * respond to requests for images.
 */
public abstract class AbstractImageResource extends AbstractResource {

    @Override
    public void doInit() throws Exception {
        super.doInit();
        if (DelegateFactory.isDelegateAvailable()) {
            RequestContext context = getRequestContext();
            context.setIdentifier(getIdentifier());
        }
        addHeaders();
    }

    private void addHeaders() {
        if (!isBypassingCache()) {
            final Configuration config = Configuration.forApplication();
            if (config.getBoolean(Key.CLIENT_CACHE_ENABLED, false)) {
                final List<String> directives = new ArrayList<>();
                final String maxAge = config.getString(Key.CLIENT_CACHE_MAX_AGE, "");
                if (!maxAge.isEmpty()) {
                    directives.add("max-age=" + maxAge);
                }
                String sMaxAge = config.getString(Key.CLIENT_CACHE_SHARED_MAX_AGE, "");
                if (!sMaxAge.isEmpty()) {
                    directives.add("s-maxage=" + sMaxAge);
                }
                if (config.getBoolean(Key.CLIENT_CACHE_PUBLIC, true)) {
                    directives.add("public");
                } else if (config.getBoolean(Key.CLIENT_CACHE_PRIVATE, false)) {
                    directives.add("private");
                }
                if (config.getBoolean(Key.CLIENT_CACHE_NO_CACHE, false)) {
                    directives.add("no-cache");
                }
                if (config.getBoolean(Key.CLIENT_CACHE_NO_STORE, false)) {
                    directives.add("no-store");
                }
                if (config.getBoolean(Key.CLIENT_CACHE_MUST_REVALIDATE, false)) {
                    directives.add("must-revalidate");
                }
                if (config.getBoolean(Key.CLIENT_CACHE_PROXY_REVALIDATE, false)) {
                    directives.add("proxy-revalidate");
                }
                if (config.getBoolean(Key.CLIENT_CACHE_NO_TRANSFORM, false)) {
                    directives.add("no-transform");
                }
                getResponse().setHeader("Cache-Control",
                        String.join(", ", directives));
            }
        }
    }

    /**
     * <p>Uses an {@link Authorizer} to determine how to respond to the
     * request. The response is modified if necessary.</p>
     *
     * <p>The authorization system supports simple boolean authorization which
     * maps to the HTTP 200 and 403 statuses.</p>
     *
     * <p>Authorization can simultaneously be used in the context of the
     * <a href="https://iiif.io/api/auth/1.0/">IIIF Authentication API</a>,
     * where it works a little differently. Here, HTTP 401 is returned instead
     * of 403, and the response body <strong>does</strong> include image
     * information. (See
     * <a href="https://iiif.io/api/auth/1.0/#interaction-with-access-controlled-resources">
     * Interaction with Access-Controlled Resources</a>. This means that IIIF
     * information endpoints should swallow any {@link ResourceException}s with
     * HTTP 401 status.</p>
     *
     * @return Whether authorization was successful. {@code false} indicates a
     *         redirect, and client code should abort.
     * @throws IOException if there was an I/O error while checking
     *         authorization.
     * @throws ResourceException if authorization resulted in an HTTP 400-level
     *         response.
     */
    protected boolean authorize() throws IOException, ResourceException {
        final Authorizer authorizer =
                new AuthorizerFactory().newAuthorizer(getDelegate());
        final AuthInfo info = authorizer.authorize();
        if (info != null) {
            return handleAuthInfo(info);
        }
        return true;
    }

    /**
     * <p>Uses an {@link Authorizer} to determine how to respond to the
     * request. The response is modified if necessary.</p>
     *
     * <p>The authorization system supports simple boolean authorization which
     * maps to the HTTP 200 and 403 statuses.</p>
     *
     * <p>Authorization can simultaneously be used in the context of the
     * <a href="https://iiif.io/api/auth/1.0/">IIIF Authentication API</a>,
     * where it works a little differently. Here, HTTP 401 is returned instead
     * of 403, and the response body <strong>does</strong> include image
     * information. (See
     * <a href="https://iiif.io/api/auth/1.0/#interaction-with-access-controlled-resources">
     * Interaction with Access-Controlled Resources</a>. This means that IIIF
     * information endpoints should swallow any {@link ResourceException}s with
     * HTTP 401 status.</p>
     *
     * @return Whether authorization was successful. {@code false} indicates a
     *         redirect, and client code should abort.
     * @throws IOException if there was an I/O error while checking
     *         authorization.
     * @throws ResourceException if authorization resulted in an HTTP 400-level
     *         response.
     */
    protected boolean authorizeBeforeAccess() throws IOException, ResourceException {
        final Authorizer authorizer =
                new AuthorizerFactory().newAuthorizer(getDelegate());
        final AuthInfo info = authorizer.authorizeBeforeAccess();
        if (info != null) {
            return handleAuthInfo(info);
        }
        return true;
    }

    /**
     * Handles an {@link AuthInfo} which has been constructed from the return
     * value of the pre-authorization or authorization delegate method. The
     * {@link AuthInfo} may signal us to redirect (via HTTP 3xx), to
     * authenticate (via HTTP 401), to forbid (via HTTP 403), or something
     * else. We honor that by setting an appropriate status, setting
     * appropriate response headers, maybe streaming an appropriate response
     * entity, and ultimately returning either {@code true} if the request is
     * authorized as-is, or {@code false} if not.
     */
    protected boolean handleAuthInfo(AuthInfo info)
            throws IOException, ResourceException {
        final int code                      = info.getResponseStatus();
        final String location               = info.getRedirectURI();
        final MetaIdentifier metaIdentifier = getMetaIdentifier().rebuilder()
                .withScaleConstraint(info.getScaleConstraint())
                .build();
        if (location != null) {
            getResponse().setStatus(code);
            getResponse().setHeader("Cache-Control", "no-cache");
            getResponse().setHeader("Location", location);
            try (OutputStream os = getResponse().openBodyStream()) {
                new StringRepresentation("Redirect: " + location).write(os);
            }
            return false;
        } else if (metaIdentifier.scaleConstraint() != null) {
            Reference publicRef = getPublicReference(metaIdentifier);
            getResponse().setStatus(code);
            getResponse().setHeader("Cache-Control", "no-cache");
            getResponse().setHeader("Location", publicRef.toString());
            try (OutputStream os = getResponse().openBodyStream()) {
                new StringRepresentation("Redirect: " + publicRef).write(os);
            }
            return false;
        } else if (code >= 400) {
            getResponse().setStatus(code);
            getResponse().setHeader("Cache-Control", "no-cache");
            if (code == 401) {
                getResponse().setHeader("WWW-Authenticate",
                        info.getChallengeValue());
            }
            throw new ResourceException(new Status(code));
        }
        return true;
    }

    /**
     * Returns the identifier of the source image, which may be a filename, an
     * object store key, or something else.
     *
     * @return The identifier of the source image.
     */
    abstract protected Identifier getIdentifier();

    /**
     * Returns the decoded meta-identifier path component of the URI, which may
     * include page number or other information. (This may not be the path
     * component that the client supplies or sees.)
     *
     * @return Instance corresponding to the first path argument, or {@code
     *         null} if no path arguments are available.
     * @see #getIdentifier()
     */
    abstract protected MetaIdentifier getMetaIdentifier();

    /**
     * @return The page index (i.e. {@literal page number - 1}) from the {@link
     *         #getMetaIdentifier() meta-identifier}. If it does not exist,
     *         {@code 0} is returned.
     */
    protected int getPageIndex() {
        int index = 0;
        if (getMetaIdentifier() != null &&
                getMetaIdentifier().pageNumber() != null) {
            index = getMetaIdentifier().pageNumber() - 1;
        }
        return index;
    }

    /**
     * Variant of {@link #getPublicReference()} that replaces the identifier
     * path component's meta-identifier if an identifier path component is
     * available.
     *
     * @param newMetaIdentifier Meta-identifier.
     */
    abstract protected Reference getPublicReference(MetaIdentifier newMetaIdentifier);

    /**
     * Returns whether there is a {@code cache} argument set to {@code false}
     * or {@code nocache} in the URI query string, indicating that cache reads
     * and writes are both bypassed.
     *
     * @return Whether there is a {@code cache} argument set to {@code false}
     *         or {@code nocache} in the URI query string.
     */
    protected final boolean isBypassingCache() {
        String value = getRequest().getReference().getQuery().getFirstValue("cache");
        return (value != null) &&
                ImageRequestHandler.CACHE_BYPASS_ARGUMENTS.contains(value);
    }

    /**
     * Returns whether there is a {@code cache} argument set to {@code recache}
     * in the URI query string, indicating that cache reads are bypassed.
     *
     * @return Whether there is a {@code cache} argument set to {@code recache}
     *         in the URI query string.
     */
    protected final boolean isBypassingCacheRead() {
        String value = getRequest().getReference().getQuery().getFirstValue("cache");
        return "recache".equals(value);
    }

    /**
     * <p>If an identifier is present in the URI, and it contains a scale
     * constraint suffix in a non-normalized form, this method redirects to
     * a normalized URI.</p>
     *
     * <p>Examples:</p>
     *
     * <dl>
     *     <dt>1:2</dt>
     *     <dd>No redirect</dd>
     *     <dt>2:4</dt>
     *     <dd>Redirect to 1:2</dd>
     *     <dt>1:1 &amp; 5:5</dt>
     *     <dd>Redirect to no constraint</dd>
     * </dl>
     *
     * @return {@code true} if redirecting. Clients should stop processing if
     *         this is the case.
     */
    protected final boolean redirectToNormalizedScaleConstraint()
            throws IOException {
        MetaIdentifier metaIdentifier = getMetaIdentifier();
        // If a meta-identifier is present in the URI...
        if (metaIdentifier != null) {
            final ScaleConstraint scaleConstraint =
                    metaIdentifier.scaleConstraint();
            // and it contains a scale constraint...
            if (scaleConstraint != null) {
                Reference newRef = null;
                // ...and the numerator and denominator are equal, redirect to
                // the non-suffixed identifier.
                if (!scaleConstraint.hasEffect()) {
                    metaIdentifier = metaIdentifier.rebuilder()
                            .withScaleConstraint(null)
                            .build();
                    newRef = getPublicReference(metaIdentifier);
                } else {
                    ScaleConstraint reducedConstraint = scaleConstraint.reduced();
                    // ...and the fraction is not in lowest terms, redirect to
                    // the reduced version.
                    if (!reducedConstraint.equals(scaleConstraint)) {
                        metaIdentifier = metaIdentifier.rebuilder()
                                .withScaleConstraint(reducedConstraint)
                                .build();
                        newRef = getPublicReference(metaIdentifier);
                    }
                }
                if (newRef != null) {
                    getResponse().setStatus(Status.MOVED_PERMANENTLY);
                    getResponse().setHeader("Location", newRef.toString());
                    try (OutputStream os = getResponse().openBodyStream()) {
                        new StringRepresentation("Redirect: " + newRef + "\n")
                                .write(os);
                    }
                    return true;
                }
            }
        }
        return false;
    }

}
