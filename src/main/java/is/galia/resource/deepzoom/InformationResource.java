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

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Method;
import is.galia.http.Reference;
import is.galia.image.Format;
import is.galia.image.Info;
import is.galia.image.MetaIdentifier;
import is.galia.image.Metadata;
import is.galia.image.Orientation;
import is.galia.image.ScaleConstraint;
import is.galia.image.Size;
import is.galia.image.StatResult;
import is.galia.resource.InformationRequestHandler;
import is.galia.resource.Resource;
import is.galia.resource.Route;
import is.galia.resource.StringRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Handles requests for DZI image descriptions.
 */
public class InformationResource extends DeepZoomResource implements Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(InformationResource.class);
    private static final String[] SUPPORTED_EXTENSIONS = { ".dzi", ".xml" };

    //region AbstractResource overrides

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected Reference getPublicReference(MetaIdentifier newMetaIdentifier) {
        final Reference publicRef            = getPublicReference();
        final String extensions              = String.join("|", SUPPORTED_EXTENSIONS);
        final List<String> pathComponents    = publicRef.getPathSegments()
                .stream()
                .map(p -> p.replaceAll("(" + extensions + ")$", ""))
                .toList();
        final String identifierComponent     = getIdentifierPathComponent();
        final int identifierIndex            = pathComponents.indexOf(identifierComponent);
        final String newMetaIdentifierString =
                newMetaIdentifier.forURI(getDelegate()) + ".dzi";
        return publicRef.rebuilder()
                .withPathSegment(identifierIndex, newMetaIdentifierString)
                .build();
    }

    //endregion
    //region Resource methods

    @Override
    public Set<Route> getRoutes() {
        final String extensions = String.join("|", SUPPORTED_EXTENSIONS);
        return Set.of(new Route(
                Set.of(Method.GET),
                Set.of(Pattern.compile("^" + getURIPath() + "/([^/]+)(" + extensions + ")$"))));
    }

    @Override
    public void doInit() throws Exception {
        super.doInit();
        getRequestContext().setIdentifier(getIdentifier());
    }

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
        Info info        = handler.handle();
        Info.Image image = info.getImages().get(getPageIndex());
        Size fullSize    = image.getSize();
        Size dziTileSize = DZIUtils.getTileSize(image);

        Metadata metadata       = info.getMetadata();
        Orientation orientation = (metadata != null) ?
                metadata.getOrientation() : Orientation.ROTATE_0;
        ScaleConstraint scaleConstraint = getMetaIdentifier().scaleConstraint();
        if (scaleConstraint == null) {
            scaleConstraint = new ScaleConstraint();
        }
        Size virtualSize = orientation.adjustedSize(fullSize)
                .scaled(scaleConstraint.rational().doubleValue());

        String xml = String.format("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Image xmlns="http://schemas.microsoft.com/deepzoom/2008"\
                          Format="%s"\
                          Overlap="%d"\
                          TileSize="%d">
                          <Size Width="%d" Height="%d"/>
                        </Image>
                        """,
                getFormat().getPreferredExtension(), getOverlap(),
                dziTileSize.intWidth(),
                virtualSize.intWidth(), virtualSize.intHeight());

        if (info.getSerializationTimestamp() != null) {
            setLastModifiedHeader(info.getSerializationTimestamp());
        }
        getResponse().setHeader("Content-Type", "application/xml;charset=UTF-8");
        try (OutputStream os = getResponse().openBodyStream()) {
            new StringRepresentation(xml).write(os);
        }
    }

    //endregion
    //region Private methods

    static Format getFormat() {
        String formatKey = Configuration.forApplication()
                .getString(Key.DEEPZOOM_FORMAT, "jpg");
        return Format.get(formatKey);
    }

    static int getOverlap() {
        //return Configuration.forApplication().getInt(Key.DEEPZOOM_OVERLAP, 0);
        return 0;
    }

}
