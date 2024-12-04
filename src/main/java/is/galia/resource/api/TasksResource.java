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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import is.galia.async.TaskQueue;
import is.galia.http.Method;
import is.galia.http.Reference;
import is.galia.http.Status;
import is.galia.resource.IllegalClientArgumentException;
import is.galia.resource.Resource;
import is.galia.resource.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/**
 * Resource to enable an RPC-style asynchronous API for performing potentially
 * long-running tasks.
 */
public class TasksResource extends AbstractAPIResource implements Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TasksResource.class);

    public static final String URI_PATH = "/tasks";

    //region AbstractResource overrides

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Set<Route> getRoutes() {
        return Set.of(new Route(
                Set.of(Method.POST),
                Set.of(Pattern.compile("^" + URI_PATH + "$"))));
    }

    //endregion
    //region Resource methods

    /**
     * Accepts a JSON object in the request entity with, at a minimum, a
     * {@literal verb} key with a value of one of the {@link
     * com.fasterxml.jackson.annotation.JsonSubTypes.Type} annotations on
     * {@link APITask}.
     */
    @Override
    public void doPOST() throws Exception {
        // N.B.: ObjectMapper will deserialize into the correct subclass.
        ObjectReader reader = new ObjectMapper().readerFor(Command.class);

        try {
            Command command = reader.readValue(getRequest().openBodyStream());
            Callable<?> callable = (Callable<?>) command;
            APITask<?> task = new APITask<>(callable);

            // The task may take a while to complete, so we accept it for
            // processing and immediately return a response, which we submit
            // to a queue rather than a thread pool to avoid having multiple
            // expensive tasks running in parallel, and also to prevent them
            // from interfering with each other.
            TaskQueue.getInstance().submit(task);

            // TaskQueue will discard it when it's complete, so we also submit
            // it to TaskMnnitor which will hold onto it for status reporting.
            TaskMonitor.getInstance().add(task);

            // Return 202 Accepted and a Location header pointing to the task
            // URI.
            getResponse().setStatus(Status.ACCEPTED);

            String taskLocation = getPublicRootReference().rebuilder()
                    .appendPath(URI_PATH + "/" + task.getUUID())
                    .build()
                    .toString();
            getResponse().setHeader("Location", taskLocation);
        } catch (NullPointerException | JsonProcessingException e) {
            throw new IllegalClientArgumentException(e.getMessage(), e);
        }
    }

}
