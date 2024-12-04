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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.util.Set;
import java.util.regex.Pattern;

public class LandingResource extends AbstractResource implements Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(LandingResource.class);

    //region AbstractResource overrides

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Set<Route> getRoutes() {
        return Set.of(new Route(
                Set.of(Method.GET),
                Set.of(Pattern.compile("\\A\\z"), Pattern.compile("^/$"))));
    }

    //endregion
    //region Resource methods

    @Override
    public void doGET() throws Exception {
        addHeaders();
        try (OutputStream os = getResponse().openBodyStream()) {
            new VelocityRepresentation("/landing.vm", getCommonTemplateVars())
                    .write(os);
        }
    }

    private void addHeaders() {
        getResponse().setHeader("Content-Type", "text/html;charset=UTF-8");
        getResponse().setHeader("Cache-Control",
                "public, max-age=" + Integer.MAX_VALUE);
    }

}
