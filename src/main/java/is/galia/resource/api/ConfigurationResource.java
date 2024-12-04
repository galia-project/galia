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

import com.fasterxml.jackson.databind.ObjectMapper;
import is.galia.config.Configuration;
import is.galia.config.ConfigurationProvider;
import is.galia.config.MapConfiguration;
import is.galia.http.Method;
import is.galia.http.Status;
import is.galia.resource.JacksonRepresentation;
import is.galia.resource.Resource;
import is.galia.resource.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class ConfigurationResource extends AbstractAPIResource
        implements Resource {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(ConfigurationResource.class);

    static final String URI_PATH = "/configuration";

    //region AbstractResource overrides

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Set<Route> getRoutes() {
        return Set.of(new Route(
                Set.of(Method.GET, Method.PUT),
                Set.of(Pattern.compile("^" + URI_PATH + "$"))));
    }

    //endregion
    //region Resource methods

    /**
     * Returns JSON application configuration. <strong>This may contain
     * sensitive info and must be protected.</strong>
     */
    @Override
    public void doGET() throws IOException {
        getResponse().setHeader("Content-Type",
                "application/json;charset=UTF-8");

        final Configuration config = Configuration.forApplication();
        List<Configuration> wrappedConfigs;
        Map<String,Object> configMap = Map.of();
        if (config instanceof ConfigurationProvider provider) {
            wrappedConfigs = provider.getWrappedConfigurations();
        } else {
            wrappedConfigs = List.of(config);
        }
        for (Configuration wrappedConfig : wrappedConfigs) {
            if (wrappedConfig.getFile().isPresent()) {
                configMap = wrappedConfig.toMap();
            } else if (wrappedConfig instanceof MapConfiguration) {
                configMap = ((MapConfiguration) wrappedConfig).getBackingMap();
            }
        }
        try (OutputStream os = getResponse().openBodyStream()) {
            new JacksonRepresentation(configMap).write(os);
        }
    }

    /**
     * Deserializes submitted JSON data and updates the application
     * configuration instance with it.
     */
    @Override
    public void doPUT() throws IOException {
        final Configuration config = Configuration.forApplication();
        final Map<?, ?> submittedConfig = new ObjectMapper().readValue(
                getRequest().openBodyStream(), HashMap.class);

        LOGGER.info("Updating {} configuration keys", submittedConfig.size());

        // Copy configuration keys and values from the request JSON payload to
        // the application configuration.
        submittedConfig.forEach((key, value) ->
                config.setProperty((String) key, value));

        getResponse().setStatus(Status.NO_CONTENT);
    }

}
