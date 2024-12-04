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
import is.galia.http.Reference;
import is.galia.plugin.repository.MockArtifactRepository;
import is.galia.plugin.repository.Product;
import is.galia.test.BaseTest;
import is.galia.util.DeletingFileVisitor;
import is.galia.util.SoftwareVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PluginUpdaterTest extends BaseTest {

    private final MockArtifactRepository repo = new MockArtifactRepository();
    private PluginUpdater instance;
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

        instance = new PluginUpdater();
        instance.setRepositoryBaseURI(repo.getURI());
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        repo.stop();
        Files.walkFileTree(pluginsDir, new DeletingFileVisitor());
        PluginManager.setPluginsDir(PluginManager.DEFAULT_PLUGINS_DIR);
    }

    private void installPlugin(String pluginName)
            throws PluginException, IOException {
        final PluginInstaller installer = new PluginInstaller();
        installer.setRepositoryBaseURI(repo.getURI());
        installer.installPlugin(pluginName);
    }

    private void installPlugin(String pluginName, String version)
            throws PluginException, IOException {
        final PluginInstaller installer = new PluginInstaller();
        installer.setRepositoryBaseURI(repo.getURI());
        installer.installPlugin(pluginName, version);
    }

    /* updatePlugin(String) */

    @Test
    void updatePluginWithNullArgument() {
        assertThrows(NullPointerException.class,
                () -> instance.updatePlugin(null));
    }

    @Test
    void updatePluginWithUnknownHost() throws Exception {
        final String pluginName = "public-plugin";
        installPlugin(pluginName);

        instance.setRepositoryBaseURI(new Reference("http://bogus.example.org"));
        assertThrows(ConnectException.class,
                () -> instance.updatePlugin(pluginName));
    }

    @Test
    void updatePluginWithPublicPluginWithMissingCustomerKey() throws Exception {
        final String pluginName = "public-plugin";
        installPlugin(pluginName, "1.0");
        Configuration.forApplication().clearProperty(Key.CUSTOMER_KEY);
        instance.updatePlugin(pluginName);
        assertEquals(new SoftwareVersion(1, 1),
                PluginManager.getVersion(pluginName));
    }

    @Test
    void updatePluginWithPublicPluginWithBogusCustomerKey() throws Exception {
        final String pluginName = "public-plugin";
        installPlugin(pluginName);

        Configuration.forApplication().setProperty(Key.CUSTOMER_KEY,
                "bogus");
        assertThrows(AccountNotFoundException.class,
                () -> instance.updatePlugin(pluginName));
    }

    @Test
    void updatePluginWithPrivatePluginWithMissingCustomerKey() throws Exception {
        final String pluginName = "private-plugin";
        installPlugin(pluginName);
        Configuration.forApplication().clearProperty(Key.CUSTOMER_KEY);
        assertThrows(PluginNotFoundException.class,
                () -> instance.updatePlugin(pluginName));
    }

    @Test
    void updatePluginWithPrivatePluginWithBogusCustomerKey() throws Exception {
        final String pluginName = "private-plugin";
        installPlugin(pluginName);

        Configuration.forApplication().setProperty(Key.CUSTOMER_KEY,
                "bogus");
        assertThrows(AccountNotFoundException.class,
                () -> instance.updatePlugin(pluginName));
    }

    @Test
    void updatePluginWithInvalidPluginName() throws Exception {
        final String pluginName = "public-plugin";
        installPlugin(pluginName);

        repo.getDatabase().stream()
                .filter(e -> e instanceof Product)
                .map(e -> (Product) e)
                .filter(p -> p.getName().equals(pluginName))
                .findFirst()
                .ifPresent(p -> repo.getDatabase().remove(p));

        PluginNotFoundException e = assertThrows(
                PluginNotFoundException.class,
                () -> instance.updatePlugin(pluginName));
        assertEquals("Plugin not found: " + pluginName, e.getMessage());
    }

    @Test
    void updatePluginWithPluginNotInstalled() {
        final String pluginName = "public-plugin";
        PluginNotInstalledException e = assertThrows(
                PluginNotInstalledException.class,
                () -> instance.updatePlugin(pluginName));
        assertEquals("Plugin not installed: " + pluginName, e.getMessage());
    }

    @Test
    void updatePluginWithNoNewerCompatibleVersion() throws Exception {
        final String pluginName = "public-plugin";
        installPlugin(pluginName);

        VersionNotFoundException e = assertThrows(
                VersionNotFoundException.class,
                () -> instance.updatePlugin(pluginName));
        assertEquals("There are no newer compatible versions of the " +
                pluginName + " plugin.", e.getMessage());
    }

    @Test
    void updatePluginWithNewerButIncompatibleVersion() throws Exception {
        final String pluginName = "public-plugin";
        installPlugin(pluginName, "1.1");

        VersionNotFoundException e = assertThrows(
                VersionNotFoundException.class,
                () -> instance.updatePlugin(pluginName));
        assertEquals("There are no newer compatible versions of the " +
                pluginName + " plugin.", e.getMessage());
    }

    @Test
    void updatePluginWithChecksumMismatch() {
        // TODO: write this (will have to make seed data editable)
    }

    @Test
    void updatePluginBacksUpCurrentVersion() throws Exception {
        final String pluginName = "public-plugin";
        installPlugin(pluginName, "1.0");

        Path pluginDir = PluginManager.getPluginDir(pluginName);
        assertTrue(PluginManager.getPluginBackupDirs(pluginName).isEmpty());

        instance.updatePlugin(pluginName);

        assertFalse(Files.exists(pluginDir));
        assertFalse(PluginManager.getPluginBackupDirs(pluginName).isEmpty());
    }

    @Test
    void updatePlugin() throws Exception {
        final String pluginName = "public-plugin";
        installPlugin(pluginName, "1.0");

        instance.updatePlugin(pluginName);

        assertEquals(new SoftwareVersion(1, 1),
                PluginManager.getVersion(pluginName));
    }

    /* updatePlugins() */

    @Test
    void updatePlugins() throws Exception {
        final String pluginName = "public-plugin";
        installPlugin(pluginName, "1.0");

        instance.updatePlugins();

        assertEquals(new SoftwareVersion(1, 1),
                PluginManager.getVersion(pluginName));
    }

}
