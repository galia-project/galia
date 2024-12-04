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

package is.galia.cache;

import is.galia.image.Identifier;
import is.galia.image.Info;

import java.io.IOException;
import java.util.Optional;

/**
 * Caches image information.
 */
public non-sealed interface InfoCache extends Cache {

    /**
     * Deletes all cached infos.
     *
     * @throws IOException upon fatal error. Implementations should do the
     *         best they can to complete the operation and swallow and log
     *         non-fatal errors.
     */
    void evictInfos() throws IOException;

    /**
     * <p>Reads the cached image information corresponding to the given
     * identifier.</p>
     *
     * <p>If invalid image information exists in the cache, implementations
     * should delete it&mdash;ideally asynchronously.</p>
     *
     * @param identifier Image identifier for which to retrieve information.
     * @return           Info corresponding to the given identifier, or {@link
     *                   Optional#empty()} if no such instance exists.
     */
    Optional<Info> fetchInfo(Identifier identifier) throws IOException;

    /**
     * <p>Synchronously adds image information to the cache.</p>
     *
     * <p>If the information corresponding to the given identifier already
     * exists, it will be overwritten.</p>
     *
     * @param identifier Image identifier.
     * @param info       Information about the image corresponding with the
     *                   given identifier.
     */
    void put(Identifier identifier, Info info) throws IOException;

    /**
     * <p>Alternative to {@link #put(Identifier, Info)} that adds a raw UTF-8
     * string to the cache, trusting that it is a serialized {@link Info}
     * instance that is deserializable by {@link #fetchInfo(Identifier)}.</p>
     *
     * <p>This method is used for testing. {@link
     * #put(Identifier, Info)} should normally be used instead.</p>
     *
     * @param identifier Image identifier.
     * @param info       JSON-encoded information about the image corresponding
     *                   with the given identifier, obtained (for example) from
     *                   {@link Info#toJSON()}.
     */
    void put(Identifier identifier, String info) throws IOException;

}
