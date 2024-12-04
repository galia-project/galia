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

import is.galia.util.DeletingFileVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Thread-safe plugin remover.
 */
public final class PluginRemover {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PluginRemover.class);

    /**
     * All backup directory names will match this regex.
     */
    static final Pattern BACKUP_PLUGIN_DIR_PATTERN =
            Pattern.compile("_\\d{14}$");

    static String backupPluginDirSuffix() {
        String pattern = "yyyyMMddHHmmss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return "_" + formatter.format(LocalDateTime.now());
    }

    PluginRemover() {}

    /**
     * @param pluginName Plugin name.
     * @throws NoSuchFileException if either the plugin or plugins directory
     *         does not exist.
     * @throws PluginNotInstalledException if the given plugin is not
     *         installed.
     */
    public void backupPlugin(String pluginName)
            throws IOException, PluginException {
        Objects.requireNonNull(pluginName);
        validateState(pluginName);
        if (!PluginManager.isPluginInstalled(pluginName)) {
            throw new PluginNotInstalledException(pluginName);
        }
        PluginManager.MUTATING_PLUGIN_NAMES.add(pluginName);
        try {
            Path pluginDir = PluginManager.getPluginDir(pluginName);
            Path backupDir = pluginDir.resolveSibling(pluginDir.getFileName() +
                    backupPluginDirSuffix());
            LOGGER.debug("Backing up directory: {} to {}", pluginDir, backupDir);
            Files.move(pluginDir, backupDir);
        } finally {
            PluginManager.MUTATING_PLUGIN_NAMES.remove(pluginName);
        }
    }

    /**
     * <p>Deletes the plugin with the given name from the plugins
     * directory.</p>
     *
     * @param pluginName Plugin name.
     * @throws NoSuchFileException if either the plugin or plugins directory
     *         does not exist.
     * @throws PluginNotInstalledException if the given plugin is not
     *         installed.
     * @throws IOException if any filesystem operations fail.
     */
    public void removePlugin(String pluginName)
            throws IOException, PluginException {
        Objects.requireNonNull(pluginName);
        validateState(pluginName);
        if (!PluginManager.isPluginInstalled(pluginName)) {
            throw new PluginNotInstalledException(pluginName);
        }
        PluginManager.MUTATING_PLUGIN_NAMES.add(pluginName);
        try {
            LOGGER.info("Deleting plugin: {}", pluginName);
            DeletingFileVisitor visitor = new DeletingFileVisitor();
            visitor.setLogger(LOGGER);
            Path pluginDir = PluginManager.getPluginDir(pluginName);
            Files.walkFileTree(pluginDir, visitor);
            LOGGER.info("Plugin deleted: {}", pluginName);
        } finally {
            PluginManager.MUTATING_PLUGIN_NAMES.remove(pluginName);
        }
    }

    /**
     * <p>Deletes all backups of the plugin with the given name from the
     * plugins directory.</p>
     *
     * @param pluginName Plugin name.
     * @throws NoSuchFileException if either the plugin or plugins directory
     *         does not exist.
     * @throws IOException if any filesystem operations fail.
     */
    public void removePluginBackups(String pluginName) throws IOException {
        Objects.requireNonNull(pluginName);
        if (PluginManager.getPluginBackupDirs(pluginName).isEmpty()) {
            return;
        }
        validateState(pluginName);
        PluginManager.MUTATING_PLUGIN_NAMES.add(pluginName);
        try {
            LOGGER.info("Deleting backups of plugin: {}", pluginName);
            DeletingFileVisitor visitor = new DeletingFileVisitor();
            visitor.setLogger(LOGGER);
            int numDeleted = 0;
            for (Path dir : PluginManager.getPluginBackupDirs(pluginName)) {
                Files.walkFileTree(dir, visitor);
                numDeleted++;
            }
            LOGGER.info("Deleted {} backups of plugin: {}",
                    numDeleted, pluginName);
        } finally {
            PluginManager.MUTATING_PLUGIN_NAMES.remove(pluginName);
        }
    }

    private void validateState(String pluginName) {
        if (PluginManager.MUTATING_PLUGIN_NAMES.contains(pluginName)) {
            throw new ConcurrentModificationException("The plugin " +
                    pluginName + " is currently being modified in another thread.");
        }
    }

}
