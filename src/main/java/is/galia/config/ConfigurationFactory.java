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

package is.galia.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class ConfigurationFactory {

    /**
     * Use an in-memory instance by default, which will remain the case (such
     * as when testing) unless this is overridden by {@link
     * #setAppInstance(Configuration)}.
     */
    private static Configuration appInstance = new MapConfiguration();

    /**
     * <p>Returns the shared application configuration instance.</p>
     *
     * <p>By default, the instance is an empty {@link MapConfiguration}. This
     * may be used in testing.</p>
     *
     * <p>During application bootup, a new instance is obtained from {@link
     * #newProviderFromPath(Path)} and passed to {@link
     * #setAppInstance(Configuration)}, so that this method can return it.</p>
     *
     * @return Shared application configuration instance.
     */
    static synchronized Configuration appInstance() {
        return appInstance;
    }

    /**
     * @param path Configuration file path.
     * @return New instance for the given path.
     */
    public static Configuration fromPath(Path path) throws IOException {
        final String pathStr   = path.toString();
        final String extension = pathStr.substring(pathStr.lastIndexOf(".") + 1);
        return switch (extension) {
            case "yml", "yaml" -> YAMLConfiguration.fromPath(path);
            default -> throw new IllegalArgumentException(
                    "Unsupported extension: " + extension);
        };
    }

    /**
     * @param path Configuration file path.
     * @return New instance suitable for using as the application
     *         configuration. The instance will contain an {@link
     *         EnvironmentConfiguration} at position 0 and a {@link
     *         YAMLConfiguration} at position 1.
     * @throws IOException if there is an error reading the file.
     */
    public static ConfigurationProvider newProviderFromPath(Path path)
            throws IOException {
        final List<Configuration> configs = List.of(
                new EnvironmentConfiguration(),
                fromPath(path));
        configs.forEach(c -> {
            try {
                c.reload();
            } catch (Exception e) {
                System.err.println("newProviderFromPath(): " + e.getMessage());
            }
        });
        return new ConfigurationProvider(configs);
    }

    public static synchronized void setAppInstance(Configuration config) {
        appInstance = config;
    }

    private ConfigurationFactory() {}

}
