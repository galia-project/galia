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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.lang.ref.WeakReference;
import java.util.Random;

/**
 * <p>Base class for all entities.</p>
 *
 * <h3>Implementation requirements</h3>
 *
 * <h4>Equality</h4>
 *
 * <p>Because entities are added to a {@link Database}, which is backed by a
 * {@link java.util.Set}, entities must have a useful implementation of {@link
 * Object#hashCode()}.</p>
 *
 * <h4>References</h4>
 *
 * <p>Instances must not maintain strong references to each other. (This is
 * because we want to have a central place&mdash;{@link
 * Database#remove(Entity)}&mdash;from which to remove them from the graph
 * without having to manually nullify all their references.) instead, they
 * should refer to each other by {@link #getID() ID}.</p>
 *
 * <p>Therefore, any setter implementations should:</p>
 *
 * <ol>
 *     <li>Store a copy of the ID</li>
 *     <li>{@link Database#add(Entity) Add the argument to the database}</li>
 * </ol>
 *
 * <p>Correspondingly, getter implementations can simply wrap {@link
 * Database#find(long)}.</p>
 */
public abstract class Entity {

    private WeakReference<Database> database;
    private final long id = new Random().nextLong();
    private String baseURL;

    @JsonIgnore
    String getBaseURL() {
        return baseURL;
    }

    @JsonIgnore
    public Database getDatabase() {
        return database.get();
    }

    @JsonIgnore
    public long getID() {
        return id;
    }

    /**
     * Set when the instance is {@link Database#add(Entity) added to its
     * database}.
     */
    void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    void setDatabase(Database database) {
        this.database = new WeakReference<>(database);
    }

    @JsonIgnore
    public abstract String getURLPath();

    public String getURL() {
        return getBaseURL() + getURLPath();
    }

}
