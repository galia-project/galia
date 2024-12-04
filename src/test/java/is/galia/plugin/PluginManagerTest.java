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
import is.galia.util.SoftwareVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PluginManagerTest extends BaseTest {

    private MockArtifactRepository repo;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        repo = new MockArtifactRepository();
        repo.start();
        final Configuration config = Configuration.forApplication();
        config.setProperty(Key.CUSTOMER_KEY, MockArtifactRepository.CUSTOMER_KEY);

        Path pluginsDir = Files.createTempDirectory(getClass().getSimpleName());
        PluginManager.setPluginsDir(pluginsDir);
        PluginManager.setRepositoryBaseURI(repo.getURI());
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        repo.stop();
        Path pluginsDir = PluginManager.getPluginsDir();
        DeletingFileVisitor visitor = new DeletingFileVisitor();
        visitor.addExclude(pluginsDir);
        visitor.addExclude(pluginsDir.resolve(".keep"));
        Files.walkFileTree(pluginsDir, visitor);
        PluginManager.setPluginsDir(PluginManager.DEFAULT_PLUGINS_DIR);
        Configuration.forApplication().clearProperty(Key.CUSTOMER_KEY);
    }

    /* getPluginBackupDirs() */

    @Test
    void getPluginBackupDirs() throws Exception {
        String pluginName    = PluginManagerTest.class.getSimpleName();
        String backupDirName = pluginName + "-1.0-SNAPSHOT" +
                PluginRemover.backupPluginDirSuffix();
        Path pluginDir  = PluginManager.getPluginsDir().resolve(backupDirName);
        Path pluginFile = pluginDir.resolve("plugin.jar");
        Files.createDirectory(pluginDir);
        Files.writeString(pluginFile, "Imagine this is a plugin.");

        Set<Path> dirs = PluginManager.getPluginBackupDirs(pluginName);
        assertEquals(1, dirs.size());
    }

    /* getPluginDir() */

    @Test
    void getPluginDir() throws Exception {
        final String pluginName = "public-plugin";
        PluginManager.getInstaller().installPlugin(pluginName);
        Path actual = PluginManager.getPluginDir(pluginName);
        assertEquals(PluginManager.getPluginsDir().resolve(pluginName + "-1.1"), actual);
    }

    @Test
    void getPluginDirDoesNotReturnBackupDirs() throws Exception {
        final String pluginName = "public-plugin";
        PluginManager.getInstaller().installPlugin(pluginName);
        Path actual = PluginManager.getPluginDir(pluginName);
        Files.move(actual, actual.resolveSibling(actual.getFileName() +
                PluginRemover.backupPluginDirSuffix()));
        assertNull(PluginManager.getPluginDir(pluginName));
    }

    /* getPluginDirNames() */

    @Test
    void getPluginDirNamesWithNoInstalledPlugins() throws Exception {
        assertTrue(PluginManager.getPluginDirNames().isEmpty());
    }

    @Test
    void getPluginDirNames() throws Exception {
        final String pluginName = "public-plugin";
        PluginManager.getInstaller().installPlugin(pluginName);

        Set<String> names = PluginManager.getPluginDirNames();
        assertEquals(1, names.size());
        assertEquals(pluginName, names.iterator().next());
    }

    /* getPluginDirs() */

    @Test
    void getPluginDirsWithNoInstalledPlugins() throws Exception {
        assertTrue(PluginManager.getPluginDirs().isEmpty());
    }

    @Test
    void getPluginDirs() throws Exception {
        final String pluginName = "public-plugin";
        PluginManager.getInstaller().installPlugin(pluginName);

        Set<Path> dirs = PluginManager.getPluginDirs();
        assertEquals(1, dirs.size());
    }

    /* getPlugins() */

    @Test
    void getPlugins() throws Exception {
        final Set<Plugin> plugins = PluginManager.getPlugins();
        assertTrue(plugins.stream().anyMatch(p -> p instanceof MockPlugin));

        int numMocks = 9;
        // Make sure that installed plugins are included in the count.
        int numJARs = 0;
        Path root = PluginManager.getPluginsDir();
        if (Files.exists(root) && Files.isDirectory(root)) {
            try (Stream<Path> files = Files.walk(root)) {
                numJARs = (int) files
                        .filter(f -> f.toString().endsWith(".jar")).count();
            }
        }
        assertEquals(numMocks + numJARs, plugins.size());
    }

    /* getInstaller() */

    @Test
    void getInstaller() {
        assertNotNull(PluginManager.getInstaller());
    }

    /* getRemover() */

    @Test
    void getRemover() {
        assertNotNull(PluginManager.getRemover());
    }

    /* getUpdater() */

    @Test
    void getUpdater() {
        assertNotNull(PluginManager.getUpdater());
    }

    /* getVersion() */

    @Test
    void getVersionWithNonInstalledPlugin() {
        final String pluginName = "bogus";
        assertThrows(PluginNotInstalledException.class,
                () -> PluginManager.getVersion(pluginName));
    }

    @Test
    void getVersionWithInstalledPluginWithUnreadableVersionSuffix()
            throws Exception {
        final String pluginName = "public-plugin";
        PluginManager.getInstaller().installPlugin(pluginName);
        Path pluginDir = PluginManager.getPluginDir(pluginName);
        Files.move(pluginDir, pluginDir.resolveSibling(pluginName));
        assertThrows(IllegalArgumentException.class,
                () -> PluginManager.getVersion(pluginName));
    }

    @Test
    void getVersionWithInstalledPlugin() throws Exception {
        final String pluginName = "public-plugin";
        PluginManager.getInstaller().installPlugin(pluginName);
        SoftwareVersion version = PluginManager.getVersion(pluginName);
        assertEquals(new SoftwareVersion(1, 1), version);
    }

    @Test
    void getVersionWithBackupPlugin() throws Exception {
        final String pluginName = "public-plugin";
        PluginManager.getInstaller().installPlugin(pluginName);
        Path actual = PluginManager.getPluginDir(pluginName);
        Files.move(actual, actual.resolveSibling(actual.getFileName() +
                PluginRemover.backupPluginDirSuffix()));
        assertThrows(PluginNotInstalledException.class,
                () -> PluginManager.getVersion(pluginName));
    }

    /* getPluginDir() */

    @Test
    void isPluginInstalledWithInstalledPlugin() throws Exception {
        final String pluginName = "public-plugin";
        PluginManager.getInstaller().installPlugin(pluginName);
        assertTrue(PluginManager.isPluginInstalled(pluginName));
    }

    @Test
    void isPluginInstalledWithNonInstalledPlugin() throws Exception {
        final String pluginName = "bogus";
        assertFalse(PluginManager.isPluginInstalled(pluginName));
    }

}