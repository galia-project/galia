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

package is.galia.resource.api;

import com.fasterxml.jackson.databind.SerializationFeature;
import is.galia.resource.Resource;
import is.galia.resource.Route;
import is.galia.status.ApplicationStatus;
import is.galia.http.Method;
import is.galia.resource.JacksonRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Provides live status updates via the HTTP API.
 */
public class StatusResource extends AbstractAPIResource implements Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(StatusResource.class);

    static final String URI_PATH = "/status";

    //region AbstractResource overrides

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Set<Route> getRoutes() {
        return Set.of(new Route(
                Set.of(Method.GET),
                Set.of(Pattern.compile("^" + URI_PATH + "$"))));
    }

    //endregion
    //region Resource methods

    @Override
    public void doGET() throws IOException {
        Map<SerializationFeature, Boolean> features = new HashMap<>();
        features.put(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);

        getResponse().setHeader("Content-Type",
                "application/json;charset=UTF-8");
        try (OutputStream os = getResponse().openBodyStream()) {
            new JacksonRepresentation(new ApplicationStatus().toMap())
                    .write(os, features);
        }
    }

}
