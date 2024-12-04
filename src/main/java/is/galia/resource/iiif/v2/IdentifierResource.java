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

import is.galia.http.Method;
import is.galia.http.Reference;
import is.galia.http.Status;
import is.galia.resource.Resource;
import is.galia.resource.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Redirects {@literal /:identifier} to {@literal /:identifier/info.json}.
 */
public class IdentifierResource extends IIIF2Resource implements Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(IdentifierResource.class);

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
                Set.of(Pattern.compile("^" + getURIPath() + "/([^/]+)$"))));
    }

    @Override
    public void doGET() {
        final Reference newRef = new Reference(
                getPublicRootReference() + getURIPath() +
                        "/" + getPublicIdentifier() +
                        "/info.json");
        getResponse().setStatus(Status.SEE_OTHER);
        getResponse().setHeader("Location", newRef.toString(false));
    }

}
