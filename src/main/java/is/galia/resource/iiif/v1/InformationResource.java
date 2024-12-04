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

package is.galia.resource.iiif.v1;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import is.galia.auth.AuthInfo;
import is.galia.auth.Authorizer;
import is.galia.auth.AuthorizerFactory;
import is.galia.delegate.DelegateFactory;
import is.galia.http.Method;
import is.galia.image.Info;
import is.galia.image.StatResult;
import is.galia.resource.JacksonRepresentation;
import is.galia.resource.Resource;
import is.galia.resource.ResourceException;
import is.galia.resource.Route;
import is.galia.resource.InformationRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles IIIF Image API 1.x information requests.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#image-info-request">Information
 * Requests</a>
 */
public class InformationResource extends IIIF1Resource implements Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(InformationResource.class);

    //region AbstractResource overrides

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    //endregion
    //region Resource methods

    @Override
    public Set<Route> getRoutes() {
        return Set.of(new Route(
                Set.of(Method.GET),
                Set.of(Pattern.compile("^" + getURIPath() + "/([^/]+)/info\\.json$"))));
    }

    /**
     * Writes a JSON-serialized information instance to the response.
     */
    @Override
    public void doGET() throws Exception {
        if (redirectToNormalizedScaleConstraint()) {
            return;
        }

        class CustomCallback implements InformationRequestHandler.Callback {
            @Override
            public boolean authorizeBeforeAccess() throws Exception {
                return InformationResource.this.authorizeBeforeAccess();
            }

            @Override
            public boolean authorize() throws Exception {
                return InformationResource.this.authorize();
            }

            @Override
            public void sourceAccessed(StatResult result) {
                setLastModifiedHeader(result.getLastModified());
            }

            @Override
            public void cacheAccessed(StatResult result) {
                setLastModifiedHeader(result.getLastModified());
            }
        }

        InformationRequestHandler handler = InformationRequestHandler.builder()
                .withReference(getRequest().getReference())
                .withIdentifier(getMetaIdentifier().identifier())
                .withDelegate(getDelegate())
                .withRequestContext(getRequestContext())
                .withCallback(new CustomCallback())
                .build();
        Info info = handler.handle();

        Map<String,Object> iiifInfo = new InformationFactory().newImageInfo(
                getImageURI(),
                info,
                getPageIndex(),
                getMetaIdentifier().scaleConstraint());
        addHeaders(info, iiifInfo);

        // If a delegate is available, pass the info to it for customization.
        if (DelegateFactory.isDelegateAvailable()) {
            getDelegate().customizeIIIF1InformationResponse(iiifInfo);
        }
        try (OutputStream os = getResponse().openBodyStream()) {
            new JacksonRepresentation(iiifInfo).write(os);
        }
    }

    //endregion
    //region Private methods

    private void addHeaders(Info info, Map<String,Object> iiifInfo) {
        // Content-Type
        getResponse().setHeader("Content-Type", getNegotiatedContentType());
        // Link
        getResponse().addHeader("Link",
                String.format("<%s>;rel=\"profile\";", iiifInfo.get("profile")));
        // Last-Modified
        if (info.getSerializationTimestamp() != null) {
            setLastModifiedHeader(info.getSerializationTimestamp());
        }
    }

    /**
     * @return Image URI corresponding to the given identifier, respecting the
     *         {@code X-Forwarded-*} and {@link #PUBLIC_IDENTIFIER_HEADER}
     *         reverse proxy headers.
     */
    private String getImageURI() {
        return getPublicRootReference() + getURIPath() + "/" +
                getPublicIdentifier();
    }

    private String getNegotiatedContentType() {
        String mediaType;
        // If the client has requested JSON-LD, set the content type to
        // that; otherwise set it to JSON.
        final List<String> preferences = getPreferredMediaTypes();
        if (!preferences.isEmpty() && preferences.getFirst()
                .startsWith("application/ld+json")) {
            mediaType = "application/ld+json";
        } else {
            mediaType = "application/json";
        }
        return mediaType + ";charset=UTF-8";
    }

    @Override
    protected boolean authorize() throws IOException, ResourceException {
        final Authorizer authorizer =
                new AuthorizerFactory().newAuthorizer(getDelegate());
        final AuthInfo info = authorizer.authorize();
        if (info != null) {
            return handleAuthInfoForInfoRequest(info);
        }
        return true;
    }

    @Override
    protected boolean authorizeBeforeAccess() throws IOException, ResourceException {
        final Authorizer authorizer =
                new AuthorizerFactory().newAuthorizer(getDelegate());
        final AuthInfo info = authorizer.authorizeBeforeAccess();
        if (info != null) {
            return handleAuthInfoForInfoRequest(info);
        }
        return true;
    }

}
