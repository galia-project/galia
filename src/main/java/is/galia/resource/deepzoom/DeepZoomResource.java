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
import is.galia.image.Identifier;
import is.galia.image.MetaIdentifier;
import is.galia.resource.AbstractImageResource;
import is.galia.resource.EndpointDisabledException;
import is.galia.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Abstract base class for Deep Zoom endpoints.
 *
 * @see <a href="https://learn.microsoft.com/en-us/previous-versions/windows/silverlight/dotnet-windows-silverlight/cc645077(v=vs.95)">
 *     Deep Zoom File Format Overview</a>
 * @see <a href="https://github.com/openseadragon/openseadragon/wiki/The-DZI-File-Format">
 *     The DZI File Format</a>
 * @see <a href="https://learn.microsoft.com/en-us/previous-versions/windows/silverlight/dotnet-windows-silverlight/cc645022(v=vs.95)">
 *     Deep Zoom Schema Reference</a>
 */
abstract class DeepZoomResource extends AbstractImageResource {

    /**
     * Path that will be used if not overridden by {@link
     * Key#DEEPZOOM_ENDPOINT_PATH} in the application configuration.
     */
    public static final String DEFAULT_URI_PATH = "/dzi";

    private Identifier identifier;
    private MetaIdentifier metaIdentifier;

    /**
     * @return URI path prefix without trailing slash.
     */
    public static String getURIPath() {
        String path = Configuration.forApplication()
                .getString(Key.DEEPZOOM_ENDPOINT_PATH, DEFAULT_URI_PATH);
        return StringUtils.stripEnd(path, "/");
    }

    @Override
    public void doInit() throws Exception {
        super.doInit();
        if (!Configuration.forApplication().
                getBoolean(Key.DEEPZOOM_ENDPOINT_ENABLED, true)) {
            throw new EndpointDisabledException();
        }
        addHeaders();
    }

    private void addHeaders() {
        getResponse().setHeader("Access-Control-Allow-Origin", "*");
        getResponse().setHeader("Vary",
                "Accept, Accept-Charset, Accept-Encoding, Accept-Language, Origin");
    }

    @Override
    public Identifier getIdentifier() {
        if (identifier == null) {
            String pathComponent = getRequest().getPathArguments().getFirst();
            identifier = Identifier.fromURI(pathComponent);
        }
        return identifier;
    }

    /**
     * <p>Returns the first path argument, which is always an identifier.</p>
     *
     * <p>The result is not decoded and may be a {@link MetaIdentifier
     * meta-identifier}. If so, it is not usable without additional
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
            String pathComponent = getRequest().getPathArguments().getFirst();
            if (pathComponent != null) {
                metaIdentifier = MetaIdentifier.fromURI(
                        pathComponent, getDelegate());
            }
        }
        return metaIdentifier;
    }

    void setLastModifiedHeader(Instant lastModified) {
        if (lastModified != null) {
            getResponse().setHeader("Last-Modified",
                    DateTimeFormatter.RFC_1123_DATE_TIME
                            .withLocale(Locale.UK)
                            .withZone(ZoneId.of("UTC"))
                            .format(lastModified));
        }
    }

}
