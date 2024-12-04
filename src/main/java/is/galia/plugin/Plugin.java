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

package is.galia.plugin;

import is.galia.Application;
import is.galia.util.SoftwareVersion;

import java.util.Set;

/**
 * <p>Interface to be implemented by all plugins.</p>
 *
 * <p>See the package documentation for detailed a plugin specification.</p>
 */
public interface Plugin {

    /**
     * <p>Returns whether the given plugin specification version is compatible
     * with that of the application (i.e. whether the plugin is compatible with
     * the application).</p>
     *
     * <p>Versions are compared semantically, where major version advances
     * break compatibility, and minor version advances only add functionality
     * without breaking compatibility. For example:</p>
     *
     * <table>
     *     <caption>Version compatibility</caption>
     *     <thead>
     *         <tr>
     *             <th>Application specification version</th>
     *             <th>Plugin specification version</th>
     *             <th>Compatible?</th>
     *         </tr>
     *     </thead>
     *     <tbody>
     *         <tr>
     *             <td>1.0</td>
     *             <td>1.0</td>
     *             <td>Yes</td>
     *         </tr>
     *         <tr>
     *             <td>1.0</td>
     *             <td>1.1</td>
     *             <td>No</td>
     *         </tr>
     *         <tr>
     *             <td>1.1</td>
     *             <td>1.0</td>
     *             <td>Yes</td>
     *         </tr>
     *         <tr>
     *             <td>2.0</td>
     *             <td>1.1</td>
     *             <td>No</td>
     *         </tr>
     *     </tbody>
     * </table>
     *
     * @param pluginSpecVersion Plugin specification version.
     * @return Whether the given plugin specification version is compatible
     *         with that of the application.
     */
    static boolean isCompatible(SoftwareVersion pluginSpecVersion) {
        SoftwareVersion appSpecVersion = Application.getSpecificationVersion();
        return appSpecVersion.major() == pluginSpecVersion.major() &&
                appSpecVersion.minor() <= pluginSpecVersion.minor();
    }

    /**
     * @return All configuration keys defined by the plugin.
     */
    Set<String> getPluginConfigKeys();

    /**
     * @return Name of the plugin. This should be brief yet distinctive in
     *         order to avoid name clashes.
     */
    String getPluginName();

    /**
     * This default implementation reads the {@code Specification-Version} from
     * {@code META-INF/MANIFEST.MF}. There should normally be no reason to
     * override it.
     *
     * @return The {@code Specification-Version} from {@code
     *         META-INF/MANIFEST.MF}, or {@code null} if not running from a
     *         JAR.
     */
    default SoftwareVersion getPluginSpecificationVersion() {
        Package myPackage  = getClass().getPackage();
        String specVersion = myPackage.getSpecificationVersion();
        return (specVersion != null) ?
                SoftwareVersion.parse(specVersion) : null;
    }

    /**
     * This default implementation reads the {@code Implementation-Version}
     * from {@code META-INF/MANIFEST.MF}. There should normally be no reason to
     * override it.
     *
     * @return The {@code Implementation-Version} from {@code
     *         META-INF/MANIFEST.MF}, or {@code null} if not running from a
     *         JAR.
     */
    default SoftwareVersion getPluginVersion() {
        Package myPackage  = getClass().getPackage();
        String implVersion = myPackage.getImplementationVersion();
        return (implVersion != null) ?
                SoftwareVersion.parse(implVersion) : null;
    }

    /**
     * Invoked <strong>once</strong> on <strong>one</strong> instance upon
     * application start. Implementations should use this instead of e.g. a
     * static initializer to perform non-instance-specific setup.
     */
    void onApplicationStart();

    /**
     * <p>Invoked on every instance immediately after instantiation.</p>
     *
     * <p>Implementations should use this method instead of a constructor to
     * perform initialization.</p>
     */
    void initializePlugin();

    /**
     * Invoked <strong>once</strong> on <strong>one</strong> instance upon
     * application stop. Implementations should use this instead of e.g. a
     * VM shutdown hook to perform non-instance-specific shutdown.
     */
    void onApplicationStop();

}
