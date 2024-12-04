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

import is.galia.cache.CacheFacade;
import is.galia.cache.CacheFactory;
import is.galia.cache.InfoCache;
import is.galia.codec.Decoder;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.delegate.Delegate;
import is.galia.http.Reference;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.image.Info;
import is.galia.image.InfoReader;
import is.galia.image.StatResult;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Set;

abstract class AbstractRequestHandler {

    /**
     * URL argument values that can be used with the {@code cache} query key
     * to bypass all caching.
     */
    public static final Set<String> CACHE_BYPASS_ARGUMENTS =
            Set.of("false", "nocache");
    public static final Set<String> CACHE_READ_BYPASS_ARGUMENTS =
            Set.of("recache");

    Reference reference;
    Delegate delegate;
    RequestContext requestContext;

    abstract Logger getLogger();

    /**
     * <p>Returns the info for the source image corresponding to the given
     * identifier as efficiently as possible.</p>
     *
     * @param identifier Image identifier.
     * @param format     Source image format.
     * @param decoder    Instance from which to read the info if it can't be
     *                   retrieved from a cache.
     * @return           Instance for the image with the given identifier.
     */
    Info getOrReadInfo(final Identifier identifier,
                       final Format format,
                       final Decoder decoder) throws IOException {
        Info info;
        if (!isBypassingCache()) {
            if (!isBypassingCacheRead()) {
                info = new CacheFacade()
                        .fetchOrReadInfo(identifier, format, decoder)
                        .orElseThrow();
            } else {
                InfoReader reader = new InfoReader();
                reader.setDecoder(decoder);
                reader.setFormat(format);
                info = reader.read();
                InfoCache cache = CacheFactory.getInfoCache().orElse(null);
                if (cache != null) {
                    cache.put(identifier, info);
                }
            }
            info.setIdentifier(identifier);
        } else {
            getLogger().debug("getOrReadInfo(): bypassing the cache, as requested");
            InfoReader reader = new InfoReader();
            reader.setDecoder(decoder);
            reader.setFormat(format);
            info = reader.read();
            info.setIdentifier(identifier);
        }
        return info;
    }

    /**
     * <p>Returns the info for the source image corresponding to the
     * given identifier as efficiently as possible.</p>
     *
     * @param identifier Image identifier.
     * @param format     Source image format.
     * @param decoder    Instance from which to read the info if it can't be
     *                   retrieved from a cache.
     * @param statResult Will be populated with information about the info.
     * @return           Instance for the image with the given identifier.
     */
    Info getOrReadInfo(final Identifier identifier,
                       final Format format,
                       final Decoder decoder,
                       final StatResult statResult) throws IOException {
        Info info = getOrReadInfo(identifier, format, decoder);
        statResult.setLastModified(info.getSerializationTimestamp());
        return info;
    }

    /**
     * @return Whether there is a {@code cache} argument set to {@code false}
     *         or {@code nocache} in the URI query string indicating that cache
     *         reads and writes are both bypassed.
     */
    boolean isBypassingCache() {
        String value = reference.getQuery().getFirstValue("cache");
        return (value != null) && CACHE_BYPASS_ARGUMENTS.contains(value);
    }

    /**
     * @return Whether there is a {@code cache} argument set to {@code recache}
     *         in the URI query string indicating that cache reads are
     *         bypassed.
     */
    boolean isBypassingCacheRead() {
        String value = reference.getQuery().getFirstValue("cache");
        return (value != null) && CACHE_READ_BYPASS_ARGUMENTS.contains(value);
    }

    boolean isResolvingFirst() {
        return Configuration.forApplication().
                getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST, true);
    }

}
