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

import is.galia.codec.EncoderFactory;
import is.galia.config.Key;
import is.galia.http.Method;
import is.galia.http.Status;
import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.image.Info;
import is.galia.image.MediaType;
import is.galia.image.StatResult;
import is.galia.operation.OperationList;
import is.galia.operation.Scale;
import is.galia.resource.ImageRequestHandler;
import is.galia.resource.Resource;
import is.galia.resource.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Handles IIIF Image API 1.x image requests.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#url-syntax-image-request">Image
 * Request Operations</a>
 */
public class ImageResource extends IIIF1Resource implements Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageResource.class);

    private static final List<String> AVAILABLE_OUTPUT_MEDIA_TYPES =
            List.of("image/jpeg", "image/tiff", "image/png", "image/gif");

    /**
     * Format to assume when no extension is present in the URI path.
     */
    private static final Format DEFAULT_FORMAT = Format.get("jpg");

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
                Set.of(Pattern.compile("^" + getURIPath() + "/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/.]+)$"),
                        Pattern.compile("^" + getURIPath() + "/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/.]+)\\.([^/]+)$"))));
    }

    /**
     * <p>Responds to image requests.</p>
     *
     * <p>N.B.: This method only respects {@link
     * Key#CACHE_SERVER_RESOLVE_FIRST} for infos, as doing so with images is
     * not really possible using current API.</p>
     */
    @Override
    public void doGET() throws Exception {
        if (redirectToNormalizedScaleConstraint()) {
            return;
        }

        final OperationList opList = getOperationList();

        class CustomCallback implements ImageRequestHandler.Callback {
            @Override
            public boolean authorizeBeforeAccess() throws Exception {
                return ImageResource.this.authorizeBeforeAccess();
            }

            @Override
            public boolean authorize() throws Exception {
                return ImageResource.this.authorize();
            }

            @Override
            public void sourceAccessed(StatResult result) {
                setLastModifiedHeader(result.getLastModified());
            }

            @Override
            public void infoAvailable(Info info) {
            }

            @Override
            public void willStreamImageFromVariantCache(StatResult result) {
                setLastModifiedHeader(result.getLastModified());
            }

            @Override
            public void willProcessImage(Info info) {
                final Size fullSize = info.getSize(getPageIndex());
                validateScale(info.getMetadata().getOrientation().adjustedSize(fullSize),
                        (Scale) opList.getFirst(Scale.class),
                        Status.FORBIDDEN);

                final String disposition = getRepresentationDisposition(
                        getMetaIdentifier().toString(),
                        opList.getOutputFormat());
                addHeaders(EncoderFactory.getAllSupportedFormats(),
                        opList.getOutputFormat(), disposition);
            }
        }

        ImageRequestHandler handler = ImageRequestHandler.builder()
                .withReference(getRequest().getReference())
                .withOperationList(opList)
                .withDelegate(getDelegate())
                .withRequestContext(getRequestContext())
                .withCallback(new CustomCallback())
                .build();
        handler.handle(getResponse());
    }

    //endregion
    //region Private methods

    private void addHeaders(Set<Format> availableOutputFormats,
                            Format outputFormat,
                            String disposition) {
        if (disposition != null) {
            getResponse().setHeader("Content-Disposition", disposition);
        }
        getResponse().setHeader("Content-Type",
                outputFormat.getPreferredMediaType().toString());

        final ComplianceLevel complianceLevel = ComplianceLevel.getLevel(
                availableOutputFormats);
        getResponse().addHeader("Link",
                String.format("<%s>;rel=\"profile\";", complianceLevel.getURI()));
    }

    private OperationList getOperationList() {
        final List<String> args = getRequest().getPathArguments();

        // If the URI path contains a format extension, try to use that.
        // Otherwise, negotiate it based on the Accept header per Image API 1.1
        // spec section 4.5.
        String outputFormat;
        try {
            outputFormat = args.get(5);
        } catch (IndexOutOfBoundsException e) {
            outputFormat = getEffectiveOutputFormat().getPreferredExtension();
        }
        final Parameters params = new Parameters(
                decodePathComponent(getIdentifierPathComponent()),
                args.get(1), args.get(2), args.get(3), args.get(4),
                outputFormat);
        final OperationList ops = params.toOperationList(getDelegate());
        ops.getOptions().putAll(getRequest().getReference().getQuery().toMap());
        return ops;
    }

    /**
     * Negotiates an output format.
     *
     * @return The best output format based on the URI extension, {@code
     *         Accept} header, or default.
     */
    private Format getEffectiveOutputFormat() {
        // Check for a format extension in the URI.
        final String extension = getRequest().getReference().getPathExtension();

        Format format = Format.all().stream()
                .filter(f -> f.getPreferredExtension().equals(extension))
                .findFirst()
                .orElse(null);
        if (format == null) { // if none, check the Accept header.
            String contentType = negotiateContentType(AVAILABLE_OUTPUT_MEDIA_TYPES);
            if (contentType != null) {
                format = MediaType.fromString(contentType).toFormat();
            } else {
                format = DEFAULT_FORMAT;
            }
        }
        if (format == null) {
            format = DEFAULT_FORMAT;
        }
        return format;
    }

}
