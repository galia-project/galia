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

import is.galia.http.Reference;
import is.galia.util.SoftwareVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static is.galia.plugin.PluginRemover.BACKUP_PLUGIN_DIR_PATTERN;

/**
 * <p>Provides programmatic access to plugins, including listing, installing,
 * and removing them.</p>
 *
 * <p>N.B.: There are two types of what are referred to by this class as
 * plugins: plugin directories and plugin implementation classes. The former
 * are expected to have the following  name format: {@literal
 * <plugin-name>-<version>}.</p>
 */
public final class PluginManager {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PluginManager.class);

    static final Path DEFAULT_PLUGINS_DIR = Paths.get(".", "plugins");

    /**
     * Set of names of all plugins that are currently being installed,
     * updated, or removed in any thread. This allows for multiple plugins to
     * be operated on simultaneously, as long as they all have different names.
     */
    static final Set<String> MUTATING_PLUGIN_NAMES =
            ConcurrentHashMap.newKeySet();

    private static Path pluginsDir = DEFAULT_PLUGINS_DIR;
    private static ClassLoader pluginClassLoader;

    private static final PluginInstaller INSTALLER = new PluginInstaller();
    private static final PluginUpdater UPDATER = new PluginUpdater();
    private static final PluginRemover REMOVER = new PluginRemover();

    private static synchronized ClassLoader getPluginClassLoader() {
        if (pluginClassLoader != null) {
            return pluginClassLoader;
        }
        final List<URL> jarURLs = new ArrayList<>();
        final Path pluginsDir = getPluginsDir();

        // Crawl through the plugins directory looking for JARs.
        try (Stream<Path> stream = Files.walk(pluginsDir)) {
            stream.forEach(path -> {
                try {
                    if (path.toString().toLowerCase().endsWith(".jar") &&
                            // Skip JARs inside backup folders.
                            !BACKUP_PLUGIN_DIR_PATTERN.matcher(path.toString().toLowerCase()).find() &&
                            Files.isRegularFile(path)) {
                        jarURLs.add(path.toUri().toURL());
                    }
                } catch (MalformedURLException e) {
                    // This should be exceedingly unlikely...
                    LOGGER.error("getPluginClassLoader(): {}", e.getMessage(), e);
                }
            });
        } catch (NoSuchFileException e) {
            LOGGER.warn("Plugin directory does not exist: {}", e.getMessage());
        } catch (IOException e) {
            LOGGER.error("getPluginClassLoader(): {}", e.getMessage(), e);
        }
        URL[] urls = jarURLs.toArray(new URL[0]);
        pluginClassLoader = new URLClassLoader(
                urls, Plugin.class.getClassLoader());
        return pluginClassLoader;
    }

    /**
     * Similar to {@link #getPluginDir(String)}, but includes plugin backup
     * directories.
     */
    public static Set<Path> getPluginBackupDirs(String pluginName)
            throws IOException {
        final Set<Path> paths = new HashSet<>();
        try (Stream<Path> stream = Files.list(PluginManager.getPluginsDir())) {
            for (Path path : stream.toList()) {
                final String lcDirname = path.getFileName().toString().toLowerCase();
                if (Files.isDirectory(path) &&
                        lcDirname.startsWith(pluginName.toLowerCase()) &&
                        BACKUP_PLUGIN_DIR_PATTERN.matcher(lcDirname).find()) {
                    paths.add(path);
                }
            }
        }
        return paths;
    }

    /**
     * @param pluginName Plugin name.
     * @return The directory of the active plugin with the given name. Backup
     *         directories are not considered.
     * @see #getPluginBackupDirs(String)
     */
    public static Path getPluginDir(String pluginName) throws IOException {
        try (Stream<Path> stream = Files.list(PluginManager.getPluginsDir())) {
            for (Path path : stream.toList()) {
                final String lcDirname = path.getFileName().toString().toLowerCase();
                if (Files.isDirectory(path) &&
                        lcDirname.startsWith(pluginName.toLowerCase()) &&
                        !BACKUP_PLUGIN_DIR_PATTERN.matcher(lcDirname).find()) {
                    return path;
                }
            }
        }
        return null;
    }

    /**
     * @return Names of all non-backup plugin directories, <strong>excluding
     *         their version suffix</strong>,
     */
    public static Set<String> getPluginDirNames() throws IOException {
        return getPluginDirs().stream()
                .map(d -> d.getFileName().toString().toLowerCase().replaceAll("-[^-]*$", ""))
                .collect(Collectors.toSet());
    }

    /**
     * @return All plugin directories, excluding backups.
     */
    public static Set<Path> getPluginDirs() throws IOException {
        Set<Path> paths = new HashSet<>();
        try (Stream<Path> stream = Files.list(PluginManager.getPluginsDir())) {
            for (Path path : stream.toList()) {
                final String lcDirname = path.getFileName().toString().toLowerCase();
                if (Files.isDirectory(path) &&
                        !BACKUP_PLUGIN_DIR_PATTERN.matcher(lcDirname).find()) {
                    paths.add(path);
                }
            }
        }
        return Collections.unmodifiableSet(paths);
    }

    /**
     * N.B.: <strong>Do not use any of these instances.</strong> They may not
     * have been initialized properly. Obtain them instead from a relevant
     * factory class.
     *
     * @return All installed instances.
     */
    public static Set<Plugin> getPlugins() {
        return ServiceLoader.load(Plugin.class, getPluginClassLoader())
                .stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toSet());
    }

    /**
     * @return Root plugins directory.
     */
    public static synchronized Path getPluginsDir() {
        return pluginsDir;
    }

    /**
     * @return Shared instance.
     */
    public static PluginInstaller getInstaller() {
        return INSTALLER;
    }

    /**
     * @return Shared instance.
     */
    public static PluginRemover getRemover() {
        return REMOVER;
    }

    /**
     * @return Shared instance.
     */
    public static PluginUpdater getUpdater() {
        return UPDATER;
    }

    /**
     * Parses the name of the version from the directory of the installed
     * plugin with the given name.
     *
     * @param pluginName Name of an installed plugin.
     * @return The version of the installed plugin.
     * @throws IOException if there are any file access issues.
     * @throws PluginNotInstalledException if there is no installed plugin with
     *         the given name.
     */
    public static SoftwareVersion getVersion(String pluginName)
            throws IOException, PluginNotInstalledException {
        Path pluginDir = getPluginDir(pluginName);
        if (pluginDir == null) {
            throw new PluginNotInstalledException(pluginName);
        }
        String dirName       = pluginDir.getFileName().toString();
        String versionSuffix = dirName
                .replace(pluginName + "-", "")
                .replaceAll(BACKUP_PLUGIN_DIR_PATTERN.pattern(), "");
        return SoftwareVersion.parse(versionSuffix);
    }

    public static boolean isPluginInstalled(String pluginName)
            throws IOException {
        return getPluginDir(pluginName) != null;
    }

    /**
     * Overrides the artifact repository base URI used for plugin installation
     * and updating. This is used in testing.
     *
     * @param baseURI New base URI.
     */
    public static void setRepositoryBaseURI(Reference baseURI) {
        synchronized (INSTALLER) {
            INSTALLER.setRepositoryBaseURI(baseURI);
        }
        synchronized (UPDATER) {
            UPDATER.setRepositoryBaseURI(baseURI);
        }
    }

    /**
     * Overrides the default plugin path.
     *
     * @param pluginsDir New root plugin directory.
     */
    public static synchronized void setPluginsDir(Path pluginsDir) {
        PluginManager.pluginsDir = pluginsDir;
        pluginClassLoader = null;
    }

    private PluginManager() {}

}
