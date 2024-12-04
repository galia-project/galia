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

package is.galia.resource.iiif.v2;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Method;
import is.galia.http.Status;
import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.image.Info;
import is.galia.image.Metadata;
import is.galia.image.Orientation;
import is.galia.image.StatResult;
import is.galia.operation.OperationList;
import is.galia.operation.Scale;
import is.galia.resource.IllegalClientArgumentException;
import is.galia.resource.Resource;
import is.galia.resource.Route;
import is.galia.resource.ImageRequestHandler;
import is.galia.resource.iiif.SizeRestrictedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Handles image requests.
 *
 * @see <a href="http://iiif.io/api/image/2.1/#image-request-parameters">Image
 * Request Operations</a>
 */
public class ImageResource extends IIIF2Resource implements Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageResource.class);

    /**
     * Map of response headers to be added to the response upon success.
     */
    private final Map<String,String> queuedHeaders = new HashMap<>();

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
                Set.of(Pattern.compile("^" + getURIPath() + "/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/]+)\\.([^/]+)$"))));
    }

    /**
     * Responds to image requests.
     */
    @Override
    public void doGET() throws Exception {
        if (redirectToNormalizedScaleConstraint()) {
            return;
        }

        // Assemble the URI path segments into a Parameters object.
        final List<String> args = getRequest().getPathArguments();
        final Parameters params = new Parameters(
                decodePathComponent(getIdentifierPathComponent()),
                args.get(1), args.get(2), args.get(3), args.get(4),
                args.get(5));
        // Convert it into an OperationList.
        final OperationList ops = params.toOperationList(getDelegate());
        ops.getOptions().putAll(getRequest().getReference().getQuery().toMap());
        final int pageIndex = getPageIndex();
        final String disposition = getRepresentationDisposition(
                ops.getMetaIdentifier().toString(), ops.getOutputFormat());

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
                if (is.galia.resource.iiif.v2.Size.ScaleMode.MAX.equals(params.getSize().getScaleMode())) {
                    constrainSizeToMaxPixels(info.getSize(), ops);
                }
                final Metadata metadata       = info.getMetadata();
                final Orientation orientation = (metadata != null) ?
                        metadata.getOrientation() : Orientation.ROTATE_0;
                final Size virtualSize        = orientation.adjustedSize(info.getSize(pageIndex));
                final Size resultingSize      = ops.getResultingSize(info.getSize());
                validateScale(
                        virtualSize,
                        (Scale) ops.getFirst(Scale.class),
                        Status.FORBIDDEN);
                validateSize(resultingSize, virtualSize);
                try {
                    enqueueHeaders(params, info.getSize(pageIndex), disposition);
                } catch (IndexOutOfBoundsException e) {
                    throw new IllegalClientArgumentException(e);
                }
            }

            @Override
            public void willStreamImageFromVariantCache(StatResult result) {
                setLastModifiedHeader(result.getLastModified());
                sendHeaders();
            }

            @Override
            public void willProcessImage(Info info) {
                sendHeaders();
            }
        }

        ImageRequestHandler handler = ImageRequestHandler.builder()
                .withReference(getRequest().getReference())
                .withOperationList(ops)
                .withDelegate(getDelegate())
                .withRequestContext(getRequestContext())
                .withCallback(new CustomCallback())
                .build();
        handler.handle(getResponse());
    }

    //endregion
    //region Private methods

    /**
     * Adds {@code Content-Disposition}, {@code Content-Type}, and {@code Link}
     * response headers to a queue which will be sent upon a success response.
     */
    private void enqueueHeaders(Parameters params,
                                Size fullSize,
                                String disposition) {
        // Content-Disposition
        if (disposition != null) {
            queuedHeaders.put("Content-Disposition", disposition);
        }
        Format outputFormat = params.getOutputFormat().toFormat();
        if (outputFormat != null) {
            // Content-Type
            queuedHeaders.put("Content-Type",
                    outputFormat.getPreferredMediaType().toString());
            // Link
            Parameters paramsCopy = new Parameters(params);
            paramsCopy.setIdentifier(getPublicIdentifier());
            String paramsStr = paramsCopy.toCanonicalString(fullSize);
            queuedHeaders.put("Link",
                    String.format("<%s%s/%s>;rel=\"canonical\"",
                            getPublicRootReference(),
                            getURIPath(),
                            paramsStr));
        }
    }

    private void sendHeaders() {
        queuedHeaders.forEach((k, v) -> getResponse().addHeader(k, v));
    }

    private void validateSize(Size resultingSize,
                              Size virtualSize) throws SizeRestrictedException {
        final var config = Configuration.forApplication();
        if (config.getBoolean(Key.IIIF_RESTRICT_TO_SIZES, false)) {
            new InformationFactory().getSizes(virtualSize)
                    .stream()
                    .filter(s -> s.get("width") == resultingSize.longWidth() &&
                            s.get("height") == resultingSize.longHeight())
                    .findAny()
                    .orElseThrow(() -> new SizeRestrictedException(
                            "Available sizes are limited to those in the information response."));
        }
    }

}
