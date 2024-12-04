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

package is.galia.resource.health;

import com.fasterxml.jackson.databind.SerializationFeature;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Method;
import is.galia.http.Status;
import is.galia.resource.AbstractResource;
import is.galia.resource.EndpointDisabledException;
import is.galia.resource.JacksonRepresentation;
import is.galia.resource.Resource;
import is.galia.resource.Route;
import is.galia.status.Health;
import is.galia.status.HealthChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Provides health checks via the HTTP API.
 */
public class HealthResource extends AbstractResource implements Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HealthResource.class);

    static final String URI_PATH = "/health";

    private static final Map<SerializationFeature, Boolean> SERIALIZATION_FEATURES =
            Map.of(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);

    @Override
    public void doInit() throws Exception {
        super.doInit();
        getResponse().setHeader("Cache-Control", "no-cache");

        final Configuration config = Configuration.forApplication();
        if (!config.getBoolean(Key.HEALTH_ENDPOINT_ENABLED, false)) {
            throw new EndpointDisabledException();
        }
    }

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
        Health health;
        final var config = Configuration.forApplication();
        if (config.getBoolean(Key.HEALTH_DEPENDENCY_CHECK, false)) {
            health = new HealthChecker().checkConcurrently();
        } else {
            health = new Health();
        }

        if (!Health.Color.GREEN.equals(health.getColor())) {
            getResponse().setStatus(Status.INTERNAL_SERVER_ERROR);
        }
        getResponse().setHeader("Content-Type",
                "application/json;charset=UTF-8");
        try (OutputStream os = getResponse().openBodyStream()) {
            new JacksonRepresentation(health).write(os, SERIALIZATION_FEATURES);
        }
    }

}
