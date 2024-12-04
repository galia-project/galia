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

package is.galia.resource.iiif.v3;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Method;
import is.galia.http.Status;
import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.image.Info;
import is.galia.image.Metadata;
import is.galia.image.Orientation;
import is.galia.image.ScaleConstraint;
import is.galia.image.StatResult;
import is.galia.operation.OperationList;
import is.galia.operation.Scale;
import is.galia.operation.ScaleByPixels;
import is.galia.resource.IllegalClientArgumentException;
import is.galia.resource.ImageRequestHandler;
import is.galia.resource.Resource;
import is.galia.resource.Route;
import is.galia.resource.ScaleRestrictedException;
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
 * @see <a href="https://iiif.io/api/image/3.0/#4-image-requests">Image
 * Requests</a>
 */
public class ImageResource extends IIIF3Resource implements Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageResource.class);

    private static final double DELTA = 0.00000001;

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
        final OperationList ops = params.toOperationList(
                getDelegate(), getMaxScale());
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
                if (is.galia.resource.iiif.v3.Size.Type.MAX.equals(params.getSize().getType())) {
                    constrainSizeToMaxPixels(info.getSize(), ops);
                }
                final Metadata metadata       = info.getMetadata();
                final Orientation orientation = (metadata != null) ?
                        metadata.getOrientation() : Orientation.ROTATE_0;
                final Size fullSize           = info.getSize(pageIndex);
                final Size orientedFullSize   = orientation.adjustedSize(fullSize);
                final Size resultingSize      = ops.getResultingSize(orientedFullSize);
                Scale scale                   = (Scale) ops.getFirst(Scale.class);
                if (scale != null) {
                    if (params.getSize().isExact() && !params.getSize().isUpscalingAllowed()) {
                        validateScale(orientedFullSize, scale);
                    } else if (!params.getSize().isUpscalingAllowed()) {
                        Scale newScale = limitScale(orientedFullSize, scale);
                        ops.replace(scale, newScale);
                        scale = newScale;
                    }
                }
                validateScale(orientedFullSize, scale, Status.BAD_REQUEST);
                validateSize(orientedFullSize, resultingSize);
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

    private static double getMaxScale() {
        return Configuration.forApplication().getDouble(Key.MAX_SCALE, 1);
    }

    private void sendHeaders() {
        queuedHeaders.forEach((k, v) -> getResponse().addHeader(k, v));
    }

    /**
     * Limits the given scale to {@link Key#MAX_SCALE} when upscaling ability
     * is not requested (i.e. the IIIF {@code size} URI path component does not
     * begin with {@code ^}).
     *
     * @param orientedFullSize Source image size adjusted for its orientation.
     * @param scale            May be {@code null}.
     */
    private Scale limitScale(Size orientedFullSize,
                             Scale scale) throws ScaleRestrictedException {
        if (scale != null) {
            final double maxScale = getMaxScale();
            if (maxScale > DELTA) {
                final ScaleConstraint constraint =
                        (getMetaIdentifier().scaleConstraint() != null) ?
                                getMetaIdentifier().scaleConstraint() :
                                new ScaleConstraint(1, 1);
                Size scaledSize = scale.getResultingSize(orientedFullSize, constraint);
                if (scaledSize.width() / orientedFullSize.width() > maxScale ||
                        scaledSize.height() / orientedFullSize.height() > maxScale) {
                    scaledSize = new Size(
                            orientedFullSize.width() * maxScale,
                            orientedFullSize.height() * maxScale);
                }
                scale = new ScaleByPixels(
                        scaledSize.intWidth(), scaledSize.intHeight(),
                        ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
            }
        }
        return scale;
    }

    /**
     * Ensures that the resulting scale is less than or equal to 1 if the
     * {@code size} URI path component does not begin with {@code ^}.
     *
     * @param orientedFullSize Source image size post-rotation and post-scale
     *                         constraint.
     * @param scale            May be {@code null}.
     */
    private void validateScale(Size orientedFullSize,
                               Scale scale) throws ScaleRestrictedException {
        if (scale != null) {
            final ScaleConstraint constraint =
                    (getMetaIdentifier().scaleConstraint() != null) ?
                            getMetaIdentifier().scaleConstraint() :
                            new ScaleConstraint(1, 1);
            if (scale.isWidthUp(orientedFullSize, constraint) ||
                    scale.isHeightUp(orientedFullSize, constraint)) {
                throw new ScaleRestrictedException("Requests for scales in " +
                        "excess of 100% must prefix the size path component " +
                        "with a ^ character.",
                        Status.BAD_REQUEST);
            }
        }
    }

    /**
     * Ensures that {@code resultingSize} is valid if {@link
     * Key#IIIF_RESTRICT_TO_SIZES} is set to {@code true}.
     *
     * @param virtualSize   Source image size post-rotation and post-scale
     *                      constraint.
     * @param resultingSize Requested size.
     */
    private void validateSize(Size virtualSize,
                              Size resultingSize) throws SizeRestrictedException {
        final Configuration config = Configuration.forApplication();
        if (config.getBoolean(Key.IIIF_RESTRICT_TO_SIZES, false)) {
            new InformationFactory().getSizes(virtualSize)
                    .stream()
                    .filter(s -> s.get("width") == resultingSize.longWidth() &&
                            s.get("height") == resultingSize.longHeight())
                    .findAny()
                    .orElseThrow(() -> new SizeRestrictedException(
                            "Available sizes are limited to those listed in " +
                                    "the information response."));
        }
    }

}
