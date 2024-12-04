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

import is.galia.http.Method;
import is.galia.http.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Permanently redirects (via HTTP 301) {@code /some/path/} to {@code
 * /some/path}, respecting the {@link is.galia.config.Key#BASE_URI}
 * configuration key, {@code X-Forwarded-BasePath} header, and other factors.
 */
public class TrailingSlashResource extends AbstractResource
        implements Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TrailingSlashResource.class);

    //region AbstractResource overrides

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Set<Route> getRoutes() {
        return Set.of(new Route(
                Set.of(Method.values()),
                Set.of(Pattern.compile("/$"))));
    }

    //endregion
    //region Resource methods

    @Override
    public void doGET() {
        final String location = getPublicReference().rebuilder()
                .withoutTrailingSlashInPath()
                .build()
                .toString();
        getResponse().setStatus(Status.MOVED_PERMANENTLY);
        getResponse().setHeader("Location", location);
    }

}
