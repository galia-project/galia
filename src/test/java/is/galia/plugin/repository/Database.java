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

package is.galia.plugin.repository;

import is.galia.http.Reference;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The world's simplest object database.
 */
public final class Database {

    private final Set<Entity> backingDB = new HashSet<>();
    private final Reference baseURL;

    /**
     * @param baseURL Base URL of the owning {@link MockArtifactRepository}, which will get
     *                passed to all {@link Entity entities} that are added to
     *                the instance.
     */
    Database(Reference baseURL) {
        this.baseURL = baseURL;
    }

    public void add(Entity entity) {
        entity.setBaseURL(baseURL.toString());
        entity.setDatabase(this);
        backingDB.add(entity);
    }

    public Entity find(long id) {
        return backingDB.stream()
                .filter(e -> e.getID() == id)
                .findFirst()
                .orElse(null);
    }

    public void purge() {
        backingDB.clear();
    }

    public void remove(Entity entity) {
        backingDB.removeIf(e -> e == entity);
    }

    public Stream<Entity> stream() {
        return backingDB.stream();
    }

}
