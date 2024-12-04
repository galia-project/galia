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

package is.galia.resource.iiif;

import is.galia.auth.AuthInfo;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.delegate.DelegateFactory;
import is.galia.http.Reference;
import is.galia.http.Status;
import is.galia.image.Size;
import is.galia.image.Identifier;
import is.galia.image.MetaIdentifier;
import is.galia.image.ScaleConstraint;
import is.galia.operation.Crop;
import is.galia.operation.OperationList;
import is.galia.operation.Scale;
import is.galia.operation.ScaleByPixels;
import is.galia.resource.AbstractImageResource;
import is.galia.resource.RequestContext;
import is.galia.resource.ResourceException;
import is.galia.resource.ScaleRestrictedException;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public abstract class IIIFResource extends AbstractImageResource {

    /**
     * Cached by {@link #getIdentifier()}.
     */
    private Identifier identifier;

    /**
     * Cached by {@link #getMetaIdentifier()}.
     */
    private MetaIdentifier metaIdentifier;

    @Override
    public void doInit() throws Exception {
        super.doInit();
        if (DelegateFactory.isDelegateAvailable()) {
            RequestContext context = getRequestContext();
            MetaIdentifier metaID = getMetaIdentifier();
            if (metaID != null) {
                context.setIdentifier(metaID.identifier());
                Integer pageNumber = metaID.pageNumber();
                if (pageNumber != null) {
                    context.setPageNumber(pageNumber);
                }
                ScaleConstraint scaleConstraint = metaID.scaleConstraint();
                if (scaleConstraint != null) {
                    context.setScaleConstraint(scaleConstraint);
                }
            }
        }
        addHeaders();
    }

    private void addHeaders() {
        getResponse().setHeader("Access-Control-Allow-Origin", "*");
        getResponse().setHeader("Access-Control-Allow-Headers", "Authorization");
        getResponse().setHeader("Vary",
                "Accept, Accept-Charset, Accept-Encoding, Accept-Language, Origin");
    }

    /**
     * <p>Returns the decoded identifier path component of the URI. (This may
     * not be the identifier that the client supplies or sees; for that, use
     * {@link #getPublicIdentifier()}.)</p>
     *
     * <p>N.B.: Depending on the image request endpoint API, The return value
     * may include "meta-information" that is not part of the identifier but is
     * encoded along with it. In that case, it is not safe to consume via this
     * method, and {@link #getMetaIdentifier()} should be used instead.</p>
     *
     * @return Identifier, or {@code null} if the URI does not have an
     *         identifier path component.
     * @see #getMetaIdentifier()
     * @see #getPublicIdentifier()
     */
    @Override
    protected Identifier getIdentifier() {
        if (identifier == null) {
            String pathComponent = getIdentifierPathComponent();
            if (pathComponent != null) {
                identifier = Identifier.fromURI(pathComponent);
            }
        }
        return identifier;
    }

    /**
     * <p>Returns the first path argument, which, for IIIF Image API resources,
     * is always an identifier.</p>
     *
     * <p>The result is not decoded and may be a {@link MetaIdentifier
     * meta-identifier}. As such, it is not usable without additional
     * processing.</p>
     *
     * @return Identifier, or {@code null} if no path arguments are available.
     */
    protected String getIdentifierPathComponent() {
        List<String> args = getRequest().getPathArguments();
        return (!args.isEmpty()) ? args.getFirst() : null;
    }


    @Override
    protected MetaIdentifier getMetaIdentifier() {
        if (metaIdentifier == null) {
            String pathComponent = getIdentifierPathComponent();
            if (pathComponent != null) {
                metaIdentifier = MetaIdentifier.fromURI(
                        pathComponent, getDelegate());
            }
        }
        return metaIdentifier;
    }

    /**
     * <p>Returns the identifier that the client sees. This will be the value
     * of the {@link #PUBLIC_IDENTIFIER_HEADER} header, if available, or else
     * the {@code identifier} URI path component.</p>
     *
     * <p>The result is not decoded, as the encoding may be influenced by
     * {@link Key#SLASH_SUBSTITUTE}, for example.</p>
     *
     * @see #getIdentifier()
     */
    protected String getPublicIdentifier() {
        return getRequest().getHeaders().getFirstValue(
                PUBLIC_IDENTIFIER_HEADER,
                getIdentifierPathComponent());
    }

    /**
     * Variant of {@link #getPublicReference()} that replaces the identifier
     * path component's meta-identifier if an identifier path component is
     * available.
     *
     * @param newMetaIdentifier Meta-identifier.
     */
    @Override
    protected Reference getPublicReference(MetaIdentifier newMetaIdentifier) {
        final Reference publicRef            = getPublicReference();
        final List<String> pathComponents    = publicRef.getPathSegments();
        final String identifierComponent     = getIdentifierPathComponent();
        final int identifierIndex            = pathComponents.indexOf(identifierComponent);
        final String newMetaIdentifierString = newMetaIdentifier.forURI(getDelegate());
        return publicRef.rebuilder()
                .withPathSegment(identifierIndex, newMetaIdentifierString)
                .build();
    }

    /**
     * <p>Variant of {@link #handleAuthInfo(AuthInfo)} For information requests,
     * because information requests need to return a valid representation even
     * for HTTP 4xx statuses per the
     * <a href="https://iiif.io/api/auth/1.0/#interaction-with-access-controlled-resources">
     * IIIF Authentication API 1.0</a>:</p>
     *
     * <blockquote>In cases other than 302, the body of the response must be a
     * valid Description Resource because the client needs to see the
     * Authentication service descriptions in order to follow the appropriate
     * workflow.</blockquote>
     *
     * <p>Using this method also requires overriding {@link #authorize()} and
     * {@link #authorizeBeforeAccess()}.
     */
    protected boolean handleAuthInfoForInfoRequest(AuthInfo info)
            throws IOException, ResourceException {
        final int code = info.getResponseStatus();
        if (code >= 400) {
            // Here is where the logic differs from image requests...
            getResponse().setStatus(code);
            getResponse().setHeader("Cache-Control",
                    "no-cache, no-store, must-revalidate");
            getResponse().setHeader("WWW-Authenticate",
                    info.getChallengeValue());
        } else {
            return handleAuthInfo(info);
        }
        return true;
    }

    protected void setLastModifiedHeader(Instant lastModified) {
        if (lastModified != null) {
            getResponse().setHeader("Last-Modified",
                    DateTimeFormatter.RFC_1123_DATE_TIME
                            .withLocale(Locale.UK)
                            .withZone(ZoneId.of("UTC"))
                            .format(lastModified));
        }
    }

    /**
     * When the size expressed in the endpoint URI is {@code max}, and the
     * resulting image dimensions are larger than {@link Key#MAX_PIXELS}, the
     * image must be downscaled to fit that area.
     */
    protected void constrainSizeToMaxPixels(Size requestedSize,
                                            OperationList opList) {
        final var config    = Configuration.forApplication();
        final int maxPixels = config.getInt(Key.MAX_PIXELS, 0);
        if (maxPixels > 0 && requestedSize.intArea() > maxPixels) {
            Scale scaleOp = (Scale) opList.getFirst(Scale.class);
            // This should be null because the client requested max size...
            if (scaleOp != null) {
                opList.remove(scaleOp);
            }
            Size scaledSize = Size.ofScaledArea(requestedSize, maxPixels);
            // The scale dimensions must be floored because rounding up could
            // cause max_pixels to be exceeded.
            scaleOp = new ScaleByPixels(
                    (int) Math.floor(scaledSize.width()),
                    (int) Math.floor(scaledSize.height()),
                    ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
            if (opList.getFirst(Crop.class) != null) {
                opList.addAfter(scaleOp, Crop.class);
            } else {
                opList.add(0, scaleOp);
            }
        }
    }

    /**
     * Ensures that the effective scale (considering scale constraint and any
     * scale operation) is not greater than the value of {@link Key#MAX_SCALE}
     * in the application configuration.
     *
     * @param orientedFullSize Orientation-adjusted full source image size.
     * @param scale            May be {@code null}.
     * @param invalidStatus    Status code to assign to the {@link
     *                         ScaleRestrictedException} when validation fails.
     */
    protected void validateScale(Size orientedFullSize,
                                 Scale scale,
                                 Status invalidStatus) throws ScaleRestrictedException {
        final ScaleConstraint scaleConstraint =
                (getMetaIdentifier().scaleConstraint() != null) ?
                        getMetaIdentifier().scaleConstraint() : new ScaleConstraint(1, 1);
        double scalePct = scaleConstraint.rational().doubleValue();
        if (scale != null) {
            scalePct = Arrays.stream(
                            scale.getResultingScales(orientedFullSize, scaleConstraint))
                    .max().orElse(1);
        }
        final Configuration config = Configuration.forApplication();
        final double maxScale      = config.getDouble(Key.MAX_SCALE, 1.0);
        if (maxScale > 0.0001 && scalePct > maxScale) {
            throw new ScaleRestrictedException(invalidStatus, maxScale);
        }
    }

}
