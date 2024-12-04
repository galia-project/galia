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

/**
 * <p>Package relating to all aspects of caching of images and their
 * characteristics.</p>
 *
 * <p>There are three main kinds of caches:</p>
 *
 * <ul>
 *     <li>{@link is.galia.cache.VariantCache
 *     Variant caches} cache post-processed (variant) images.</li>
 *     <li>{@link is.galia.cache.InfoCache Info caches} cache image
 *     information.</li>
 *     <li>The {@link is.galia.cache.HeapInfoCache heap info cache} caches
 *     image information in the heap, and may be used either on its own, or as
 *     a faster "level 1" cache in front of a "level 2" info cache.
 *     </li>
 * </ul>
 *
 * <p>Clients are encouraged to use a
 * {@link is.galia.cache.CacheFacade} to simplify
 * interactions with the caching architecture.</p>
 *
 * <h2>Writing Custom Caches</h2>
 *
 * <p>Custom variant caches must implement {@link
 * is.galia.cache.VariantCache}. A single instance of the
 * cache will be shared across threads, so implementations must be
 * thread-safe.</p>
 *
 * <p>Custom caches are auto-detected using {@link
 * java.util.ServiceLoader}. Their JAR must include a file named {@link
 * is.galia.cache.Cache} inside {@literal
 * resources/META-INF/services} containing the fully qualified class name of
 * the implementation. Also, any references to the implementation from the
 * configuration file or delegate must use the fully qualified class name.</p>
 */
package is.galia.cache;
