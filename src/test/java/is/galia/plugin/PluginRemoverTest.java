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

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.plugin.repository.MockArtifactRepository;
import is.galia.test.BaseTest;
import is.galia.util.DeletingFileVisitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PluginRemoverTest extends BaseTest {

    private final MockArtifactRepository repo = new MockArtifactRepository();
    private final PluginRemover instance      = new PluginRemover();
    private Path pluginsDir;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        pluginsDir = Files.createTempDirectory(getClass().getSimpleName());
        PluginManager.setPluginsDir(pluginsDir);
        Configuration.forApplication().setProperty(Key.CUSTOMER_KEY,
                MockArtifactRepository.CUSTOMER_KEY);
        repo.start();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        repo.stop();
        DeletingFileVisitor visitor = new DeletingFileVisitor();
        visitor.addExclude(pluginsDir);
        visitor.addExclude(pluginsDir.resolve(".keep"));
        Files.walkFileTree(pluginsDir, visitor);
        PluginManager.setPluginsDir(PluginManager.DEFAULT_PLUGINS_DIR);
    }

    private void installPlugin(String pluginName)
            throws PluginException, IOException {
        final PluginInstaller installer = new PluginInstaller();
        installer.setRepositoryBaseURI(repo.getURI());
        installer.installPlugin(pluginName);
    }

    /* backupPluginDirSuffix() */

    @Test
    void backupPluginDirSuffix() {
        String suffix = PluginRemover.backupPluginDirSuffix();
        assertTrue(suffix.matches(".\\d{14}"));
    }

    /* backupPlugin() */

    @Test
    void backupPlugin() throws Exception {
        final String pluginName = "public-plugin";
        installPlugin(pluginName);

        Path pluginDir = PluginManager.getPluginDir(pluginName);
        Path backupDir = pluginDir.resolveSibling(pluginDir.getFileName() +
                PluginRemover.backupPluginDirSuffix());
        assertFalse(Files.exists(backupDir));

        instance.backupPlugin(pluginName);

        assertFalse(Files.exists(pluginDir));
        assertTrue(Files.exists(backupDir));
    }

    @Test
    void backupPluginWithNonExistingPlugin() {
        String pluginName = PluginRemoverTest.class.getSimpleName();
        assertThrows(PluginNotInstalledException.class, () ->
                instance.backupPlugin(pluginName));
    }

    /* removePlugin() */

    @Test
    void removePluginWithNonExistingPluginsDir() {
        PluginManager.setPluginsDir(Path.of("bogus", "bogus"));
        String pluginName = PluginRemoverTest.class.getSimpleName();
        assertThrows(NoSuchFileException.class,
                () -> instance.removePlugin(pluginName));
    }

    @Test
    void removePluginWithNonExistingPlugin() {
        String pluginName = PluginRemoverTest.class.getSimpleName();
        assertThrows(PluginNotInstalledException.class, () ->
                instance.removePlugin(pluginName));
    }

    @Test
    void removePluginWithExistingPlugin() throws Exception {
        String pluginName = PluginRemoverTest.class.getSimpleName();
        String pluginDirName = pluginName + "-1.0-SNAPSHOT";
        Path pluginDir = PluginManager.getPluginsDir().resolve(pluginDirName);
        Path pluginFile = pluginDir.resolve("plugin.jar");
        Files.createDirectory(pluginDir);
        Files.writeString(pluginFile, "Imagine this is a plugin.");
        instance.removePlugin(pluginName);
        assertFalse(Files.exists(pluginFile));
        assertFalse(Files.exists(pluginDir));
    }

    @Test
    void removePluginDoesNotDeleteBackupDirs() throws Exception {
        String pluginName    = PluginRemoverTest.class.getSimpleName();
        // Plugin dir
        String pluginDirName = pluginName + "-1.0-SNAPSHOT";
        Path pluginDir = PluginManager.getPluginsDir().resolve(pluginDirName);
        Path pluginFile = pluginDir.resolve("plugin.jar");
        Files.createDirectory(pluginDir);
        Files.writeString(pluginFile, "Imagine this is a plugin.");
        // Backup dir
        String backupDirName = pluginName + "-1.0-SNAPSHOT" +
                PluginRemover.backupPluginDirSuffix();
        pluginDir  = PluginManager.getPluginsDir().resolve(backupDirName);
        pluginFile = pluginDir.resolve("plugin.jar");
        Files.createDirectory(pluginDir);
        Files.writeString(pluginFile, "Imagine this is a plugin.");

        instance.removePlugin(pluginName);
        assertTrue(Files.exists(pluginFile));
        assertTrue(Files.exists(pluginDir));
    }

    /* removePluginBackups() */

    @Test
    void removePluginBackups() throws Exception {
        String pluginName    = PluginRemoverTest.class.getSimpleName();
        String backupDirName = pluginName + "-1.0-SNAPSHOT" +
                PluginRemover.backupPluginDirSuffix();
        Path backupDir  = PluginManager.getPluginsDir().resolve(backupDirName);
        Path pluginFile = backupDir.resolve("plugin.jar");
        Files.createDirectory(backupDir);
        Files.writeString(pluginFile, "Imagine this is a plugin.");
        instance.removePluginBackups(pluginName);
        assertFalse(Files.exists(pluginFile));
        assertFalse(Files.exists(backupDir));
    }

}
