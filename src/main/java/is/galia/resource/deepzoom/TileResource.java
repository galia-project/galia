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

package is.galia.resource.deepzoom;

import is.galia.auth.AuthInfo;
import is.galia.auth.Authorizer;
import is.galia.auth.AuthorizerFactory;
import is.galia.codec.VariantFormatException;
import is.galia.http.Method;
import is.galia.http.Reference;
import is.galia.http.Status;
import is.galia.image.Format;
import is.galia.image.Info;
import is.galia.image.MetaIdentifier;
import is.galia.image.ReductionFactor;
import is.galia.image.Size;
import is.galia.image.StatResult;
import is.galia.operation.Crop;
import is.galia.operation.CropByPixels;
import is.galia.operation.Encode;
import is.galia.operation.OperationList;
import is.galia.operation.Scale;
import is.galia.operation.ScaleByPercent;
import is.galia.resource.ImageRequestHandler;
import is.galia.resource.Resource;
import is.galia.resource.ResourceException;
import is.galia.resource.Route;
import is.galia.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Handles requests for DZI image tiles.
 */
public class TileResource extends DeepZoomResource implements Resource {

    private class ImageRequestHandlerCallback
            implements ImageRequestHandler.Callback {
        @Override
        public boolean authorizeBeforeAccess() throws Exception {
            final Authorizer authorizer =
                    new AuthorizerFactory().newAuthorizer(getDelegate());
            final AuthInfo info = authorizer.authorizeBeforeAccess();
            if (info != null) {
                return handleAuthInfo(info);
            }
            return true;
        }

        @Override
        public boolean authorize() throws Exception {
            final Authorizer authorizer =
                    new AuthorizerFactory().newAuthorizer(getDelegate());
            final AuthInfo info = authorizer.authorize();
            if (info != null) {
                return handleAuthInfo(info);
            }
            return true;
        }

        @Override
        public void sourceAccessed(StatResult result) {
            setLastModifiedHeader(result.getLastModified());
        }

        @Override
        public void infoAvailable(Info info) {
            final Info.Image image = info.getImages().get(getPageIndex());
            final List<Size> sizes = DZIUtils.computeFullSizes(image);
            if (resolutionLevel >= sizes.size()) {
                throw new ResourceException(Status.NOT_FOUND);
            }
            final Size scaledSize = sizes.get(resolutionLevel);
            final Size tileSize   = DZIUtils.getTileSize(image);
            final ReductionFactor reductionFactor =
                    new ReductionFactor(sizes.size() - 1 - resolutionLevel);
            final double scale    = reductionFactor.getScale();
            final int tileX       = tileIndexX * tileSize.intWidth();
            final int tileY       = tileIndexY * tileSize.intHeight();
            final int regionX     = (int) Math.round(tileIndexX * (tileSize.intWidth() / scale));
            final int regionY     = (int) Math.round(tileIndexY * (tileSize.intHeight() / scale));

            // Add a Crop
            if (tileIndexX > 0 || tileIndexY > 0 ||
                    tileSize.intWidth() < scaledSize.intWidth() ||
                    tileSize.intHeight() < scaledSize.intHeight()) {
                // Return 404 if the tile origin is off the edge
                if (tileX >= scaledSize.intWidth() || tileY >= scaledSize.intHeight()) {
                    throw new ResourceException(Status.NOT_FOUND);
                }
                Crop crop = new CropByPixels(
                        regionX, regionY,
                        (int) Math.ceil(tileSize.width() / scale),
                        (int) Math.ceil(tileSize.height() / scale));
                opList.add(crop);
            }
            // Add a Scale
            if (resolutionLevel < sizes.size() - 1) {
                Scale scaleOp = new ScaleByPercent(scale);
                opList.add(scaleOp);
            }
            // Add an Encode
            Encode encode = new Encode(format);
            opList.add(encode);

            getResponse().setHeader("Content-Type",
                    format.getPreferredMediaType().toString());
        }

        @Override
        public void willStreamImageFromVariantCache(StatResult result) {
            setLastModifiedHeader(result.getLastModified());
        }

        @Override
        public void willProcessImage(Info info) {}
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TileResource.class);

    private OperationList opList;
    private int resolutionLevel, tileIndexX, tileIndexY;
    private Format format;

    //region AbstractResource overrides

    @Override
    public void doInit() throws Exception {
        super.doInit();

        final List<String> pathArgs = getRequest().getPathArguments();
        resolutionLevel  = Integer.parseInt(pathArgs.get(1));
        tileIndexX       = Integer.parseInt(pathArgs.get(2));
        tileIndexY       = Integer.parseInt(pathArgs.get(3));
        String formatStr = pathArgs.get(4);
        format           = Format.all()
                .stream()
                .filter(f -> f.extensions().contains(formatStr))
                .findFirst()
                .orElseThrow(VariantFormatException::new);
        if (!InformationResource.getFormat().equals(format)) {
            throw new VariantFormatException("The " + format +
                    " format is not allowed by the configuration.");
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected Reference getPublicReference(MetaIdentifier newMetaIdentifier) {
        final Reference publicRef            = getPublicReference();
        final List<String> pathComponents    = publicRef.getPathSegments()
                .stream()
                .map(p -> StringUtils.stripEnd(p, "_files"))
                .toList();
        final String identifierComponent     = getIdentifierPathComponent();
        final int identifierIndex            = pathComponents.indexOf(identifierComponent);
        final String newMetaIdentifierString =
                newMetaIdentifier.forURI(getDelegate()) + "_files";
        return publicRef.rebuilder()
                .withPathSegment(identifierIndex, newMetaIdentifierString)
                .build();
    }

    //endregion
    //region Resource methods

    @Override
    public Set<Route> getRoutes() {
        return Set.of(new Route(
                Set.of(Method.GET),
                Set.of(Pattern.compile("^" + getURIPath() +
                        "/([^/]+)_files/(\\d+)/(\\d+)_(\\d+).(.+)$"))));
    }

    @Override
    public void doGET() throws Exception {
        if (redirectToNormalizedScaleConstraint()) {
            return;
        }
        opList = OperationList.builder()
                .withMetaIdentifier(getMetaIdentifier())
                .withOperations(new Encode(format))
                .build();

        ImageRequestHandler handler = ImageRequestHandler.builder()
                .withReference(getRequest().getReference())
                .withRequestContext(getRequestContext())
                .withDelegate(getDelegate())
                .withOperationList(opList)
                .withCallback(new ImageRequestHandlerCallback())
                .build();
        handler.handle(getResponse());
        // Setup continues in the callback's infoAvailable()
    }

}
