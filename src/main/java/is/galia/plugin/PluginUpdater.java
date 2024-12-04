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

import com.fasterxml.jackson.databind.JsonNode;
import is.galia.http.Reference;
import is.galia.util.ArchiveUtils;
import is.galia.util.DeletingFileVisitor;
import is.galia.util.SoftwareVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ConcurrentModificationException;
import java.util.Objects;

/**
 * Thread-safe plugin updater.
 */
public final class PluginUpdater {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PluginUpdater.class);

    private final ArtifactRepositoryClient client = new ArtifactRepositoryClient();

    PluginUpdater() {}

    /**
     * Updates the plugin with the given name to the latest compatible version.
     * The current plugin is backed up.
     *
     * @param pluginName Plugin name.
     * @throws IOException if any network or filesystem operations fail.
     * @throws PluginException and a variety of subtypes for many reasons.
     */
    public void updatePlugin(String pluginName)
            throws IOException, PluginException {
        Objects.requireNonNull(pluginName);
        validateState(pluginName);

        JsonNode pluginNode = client.findProduct(pluginName);
        if (pluginNode == null) {
            throw new PluginNotFoundException("Plugin not found: " + pluginName);
        }

        JsonNode versionNode    = client.findLatestCompatibleVersion(pluginNode);
        String remoteVersionStr = versionNode.get("name").textValue();
        final SoftwareVersion remoteVersion    =
                SoftwareVersion.parse(remoteVersionStr);
        final SoftwareVersion installedVersion =
                PluginManager.getVersion(pluginName);

        if (!remoteVersion.isGreaterThan(installedVersion)) {
            throw new VersionNotFoundException(
                    "There are no newer compatible versions of the " +
                            pluginName + " plugin.");
        }

        Path pluginDir = PluginManager.getPluginDir(pluginName);
        PluginManager.getRemover().backupPlugin(pluginName);
        Files.walkFileTree(pluginDir, new DeletingFileVisitor());

        LOGGER.info("Updating plugin: {}", pluginName);
        PluginManager.MUTATING_PLUGIN_NAMES.add(pluginName);
        try {
            JsonNode artifactsNode = versionNode.get("artifacts");
            JsonNode artifactNode  = client.findArtifact(artifactsNode);
            Path artifactFile      = client.downloadArtifact(artifactNode);
            try {
                Path pluginsDir = PluginManager.getPluginsDir();
                ArchiveUtils.unzip(artifactFile, pluginsDir);
            } finally {
                Files.delete(artifactFile);
            }
            LOGGER.info("Plugin updated: {}", pluginName);
        } finally {
            PluginManager.MUTATING_PLUGIN_NAMES.remove(pluginName);
        }
    }

    /**
     * Updates all plugins to their latest compatible version. The updated
     * plugins are backed up.
     *
     * @throws IOException if any network or filesystem operations fail.
     * @throws PluginException and a variety of subtypes for many reasons.
     */
    public void updatePlugins() throws IOException, PluginException {
        for (String name : PluginManager.getPluginDirNames()) {
            updatePlugin(name);
        }
    }

    private void validateState(String pluginName)
            throws IOException, PluginNotInstalledException {
        if (!PluginManager.isPluginInstalled(pluginName)) {
            throw new PluginNotInstalledException(pluginName);
        } else if (PluginManager.MUTATING_PLUGIN_NAMES.contains(pluginName)) {
            throw new ConcurrentModificationException("The plugin " +
                    pluginName + " is currently being modified in another thread.");
        }
    }

    void setApplicationSpecificationVersion(SoftwareVersion version) {
        client.setApplicationSpecificationVersion(version);
    }

    void setRepositoryBaseURI(Reference baseURI) {
        client.setBaseURI(baseURI);
    }

}
