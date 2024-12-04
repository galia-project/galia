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
 * <p>Contains classes related to local plugin management, as well as the
 * main plugin interface, {@link is.galia.plugin.Plugin}.
 *
 * <h2>Interfaces</h2>
 *
 * <p>All plugins implement {@link is.galia.plugin.Plugin} in addition to
 * other interface(s) related to their function:
 * {@link is.galia.cache.VariantCache}, {@link is.galia.codec.Decoder},
 * etc.</p>
 *
 * <p>Plugin classes may reside in any package, but they should not share
 * package names with other plugins or with the application itself.</p>
 *
 * <h2>Versioning</h2>
 *
 * <p>The core application has two distinct versions: an {@link
 * is.galia.Application#getVersion() implementation version} and a {@link
 * is.galia.Application#getSpecificationVersion() specification version}.
 * Plugins are independently versioned and may have any implementation version,
 * but their specification version must be {@link
 * is.galia.plugin.Plugin#isCompatible compatible with that of the
 * application}.</p>
 *
 * <h2>Packaging</h2>
 *
 * <p>Plugins are distributed as zip files with names in the following
 * format:</p>
 *
 * <p><code>plugin_name-major.minor.patch.zip</code></p>
 *
 * <p>After extraction, a plugin has the following structure:</p>
 *
 * <ul>
 *     <li><code>plugin_name-major.minor.patch</code>
 *         <ul>
 *             <li>{@code lib/}
 *                 <ul>
 *                     <li><code>plugin_name-major.minor.patch.jar</code></li>
 *                     <li>(possibly other dependencies)</li>
 *                 </ul>
 *             </li>
 *             <li><code>LICENSE.txt</code></li>
 *             <li><code>README.md</code></li>
 *             <li><code>(possibly others)</code></li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <p>Plugins rely on the JDK's {@link java.util.ServiceLoader} mechanism. In
 * order to make that work, the plugin JAR file contains a {@code
 * META-INF/services/[plugin interface]} file containing the fully qualified
 * class names of all plugin classes in the JAR.</p>
 *
 * <p>It also contains a {@code META-INF/MANIFEST.MF} file which contains a
 * {@code Specification-Version} line referring to the version of the
 * application API contract. A plugin implementation is considered to be
 * compatible with that version and all later minor versions within the same
 * major version, but not with any later major versions.</p>
 *
 * <h2>Installation</h2>
 *
 * <p>Plugin folders physically reside in the {@link
 * is.galia.plugin.PluginManager#getPluginsDir() plugins directory}. A plugin's
 * JAR file is all that is needed for it to function, but its containing
 * directory is what the plugin management tools consider to be &quot;the
 * plugin&quot;&mdash;so, when a plugin is installed, its containing folder is
 * what is added to the plugins directory; when it is removed, its containing
 * folder is what is removed; etc. {@link is.galia.plugin.PluginManager} is the
 * main plugin management tool.</p>
 *
 * <h2>Updating</h2>
 *
 * <p>The update process is similar to the installation process. After state
 * has been validated and a newer compatible plugin has been confirmed to
 * exist, the current plugin is backed up (by adding a backup suffix to its
 * folder name) and the new version is extracted in its place. See {@link
 * is.galia.plugin.PluginUpdater}.</p>
 *
 * <h2>Removal</h2>
 *
 * <p>Removal is a simple matter of either deleting the plugin JAR, or
 * appending a backup suffix to its name. See {@link
 * is.galia.plugin.PluginRemover}.</p>
 */
package is.galia.plugin;
