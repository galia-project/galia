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

import is.galia.http.Method;
import is.galia.resource.JacksonRepresentation;
import is.galia.resource.Resource;
import is.galia.resource.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.file.NoSuchFileException;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Resource for monitoring of tasks invoked by {@link TasksResource}.
 */
public class TaskResource extends AbstractAPIResource implements Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TaskResource.class);

    //region AbstractResource overrides

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Set<Route> getRoutes() {
        return Set.of(new Route(
                Set.of(Method.GET),
                Set.of(Pattern.compile("^" + TasksResource.URI_PATH + "/([^/]+)$"))));
    }

    //endregion
    //region Resource methods

    /**
     * Writes a JSON task representation to the response output stream.
     */
    public void doGET() throws Exception {
        final String uuidStr = getRequest().getPathArguments().getFirst();
        try {
            final UUID uuid = UUID.fromString(uuidStr);
            APITask<?> task = TaskMonitor.getInstance().get(uuid);

            if (task != null) {
                getResponse().setHeader("Content-Type",
                        "application/json;charset=UTF-8");

                try (OutputStream os = getResponse().openBodyStream()) {
                    new JacksonRepresentation(task).write(os);
                }
            } else {
                throw new NoSuchFileException("No such task");
            }
        } catch (IllegalArgumentException e) {
            throw new NoSuchFileException("No such task");
        }
    }

}
