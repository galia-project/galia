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
import is.galia.util.SoftwareVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ConcurrentModificationException;
import java.util.Objects;

/**
 * Thread-safe plugin installer.
 */
public final class PluginInstaller {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PluginInstaller.class);

    private final ArtifactRepositoryClient client = new ArtifactRepositoryClient();;

    PluginInstaller() {}

    /**
     * <p>Installs the plugin with the given name from the artifact repository.
     * The latest plugin version with valid properties and a compatible
     * specification version is chosen.</p>
     * 
     * @see #installPlugin(String, String)
     */
    public void installPlugin(String pluginName)
            throws PluginException, IOException {
        installPlugin(pluginName, null);
    }

    /**
     * <p>Installs a particular version of a plugin from:</p>
     *
     * <ol>
     *     <li>The account representation, which contains a list of all
     *     purchased plugins</li>
     *     <li>The public plugin index, which contains a list of all publicly
     *     available plugins</li>
     * </ol>
     *
     * <p>The latest plugin version with valid properties and a compatible
     * specification version is chosen.</p>
     *
     * @param pluginName Name of the plugin to install.
     * @param version    Version of the plugin to install. May be {@code null}.
     * @throws PluginAlreadyInstalledException if the plugin is already
     *         installed.
     * @throws PluginNotFoundException if the plugin was not found remotely.
     * @throws ArtifactDigestException if the checksum of the downloaded file
     *         does not match the one listed in the repository.
     */
    public void installPlugin(String pluginName, String version)
            throws PluginException, IOException {
        Objects.requireNonNull(pluginName);
        validateState(pluginName);

        LOGGER.info("Installing plugin: {}", pluginName);
        PluginManager.MUTATING_PLUGIN_NAMES.add(pluginName);
        try {
            JsonNode pluginNode = client.findProduct(pluginName);
            if (pluginNode == null) {
                throw new PluginNotFoundException("Plugin not found: " + pluginName);
            }

            JsonNode versionNode;
            if (version != null) {
                versionNode = client.findVersion(pluginNode, version);
            } else {
                versionNode = client.findLatestCompatibleVersion(pluginNode);
            }

            JsonNode artifactsNode = versionNode.get("artifacts");
            JsonNode artifactNode  = client.findArtifact(artifactsNode);
            Path artifactFile      = client.downloadArtifact(artifactNode);
            try {
                Path pluginsDir = PluginManager.getPluginsDir();
                ArchiveUtils.unzip(artifactFile, pluginsDir);
            } finally {
                Files.delete(artifactFile);
            }
            LOGGER.info("Plugin installed: {}", pluginName);
        } finally {
            PluginManager.MUTATING_PLUGIN_NAMES.remove(pluginName);
        }
    }

    private void validateState(String pluginName)
            throws IOException, PluginAlreadyInstalledException {
        if (PluginManager.isPluginInstalled(pluginName)) {
            throw new PluginAlreadyInstalledException("The plugin " +
                    pluginName + " is already installed.");
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
