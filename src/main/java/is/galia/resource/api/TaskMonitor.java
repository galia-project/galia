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

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Retains tasks submitted via {@link
 * TasksResource} for observation
 * by {@link TaskResource}.
 */
public final class TaskMonitor {

    private static TaskMonitor instance;

    private final List<APITask<?>> tasks = new CopyOnWriteArrayList<>();

    /**
     * @return Shared instance.
     */
    public static synchronized TaskMonitor getInstance() {
        if (instance == null) {
            instance = new TaskMonitor();
        }
        return instance;
    }

    static synchronized void clearInstance() {
        instance = null;
    }

    private TaskMonitor() {}

    void add(APITask<?> task) {
        tasks.add(task);
    }

    public APITask<?> get(final UUID uuid) {
        return tasks.stream()
                .filter(t -> t.getUUID().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    /**
     * @return Immutable list of all tasks.
     */
    public List<APITask<?>> getAll() {
        return Collections.unmodifiableList(tasks);
    }

}
