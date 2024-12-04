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

package is.galia;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.plugin.PluginManager;
import is.galia.plugin.repository.MockArtifactRepository;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import is.galia.util.SoftwareVersion;
import is.galia.util.SystemUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static is.galia.CommandLineArgumentHandler.INSTALL_PLUGIN_ARGUMENT;
import static is.galia.CommandLineArgumentHandler.LIST_FORMATS_ARGUMENT;
import static is.galia.CommandLineArgumentHandler.CONFIG_ARGUMENT;
import static is.galia.CommandLineArgumentHandler.CONFIGTEST_ARGUMENT;
import static is.galia.CommandLineArgumentHandler.LIST_FONTS_ARGUMENT;
import static is.galia.CommandLineArgumentHandler.LIST_PLUGINS_ARGUMENT;
import static is.galia.CommandLineArgumentHandler.REMOVE_PLUGIN_ARGUMENT;
import static is.galia.CommandLineArgumentHandler.UPDATE_PLUGINS_ARGUMENT;
import static is.galia.CommandLineArgumentHandler.UPDATE_PLUGIN_ARGUMENT;
import static is.galia.CommandLineArgumentHandler.USAGE_ARGUMENT;
import static is.galia.CommandLineArgumentHandler.VERSION_ARGUMENT;
import static org.junit.jupiter.api.Assertions.*;

class CommandLineArgumentHandlerTest extends BaseTest {

    private static final PrintStream CONSOLE_OUTPUT = System.out;
    private static final PrintStream CONSOLE_ERROR  = System.err;

    private final ByteArrayOutputStream redirectedStdout =
            new ByteArrayOutputStream();
    private final ByteArrayOutputStream redirectedStderr =
            new ByteArrayOutputStream();

    private CommandLineArgumentHandler instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        SystemUtils.clearExitRequest();
        redirectOutput();
        instance = new CommandLineArgumentHandler();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        resetOutput();
    }

    /**
     * Redirects stdout/stderr output to byte arrays for analysis.
     */
    private void redirectOutput() {
        System.setOut(new PrintStream(redirectedStdout));
        System.setErr(new PrintStream(redirectedStderr));
    }

    private void resetOutput() {
        System.setOut(CONSOLE_OUTPUT);
        System.setErr(CONSOLE_ERROR);
    }

    // -help

    @Test
    void handleWithUsageArgument() throws Exception {
        instance.handle("-" + USAGE_ARGUMENT);
        assertTrue(redirectedStdout.toString().contains("Usage:"));
        assertTrue(SystemUtils.exitRequested());
        assertEquals(0, SystemUtils.requestedExitCode());
    }

    // -install-plugin

    @Test
    void handleWithInstallPluginArgumentWithExistingPlugin() throws Exception {
        final String pluginName           = "public-plugin";
        final Configuration config        = Configuration.forApplication();
        final MockArtifactRepository repo = new MockArtifactRepository();
        repo.start();
        config.setProperty(Key.CUSTOMER_KEY, MockArtifactRepository.CUSTOMER_KEY);
        PluginManager.setRepositoryBaseURI(repo.getURI());
        try {
            instance.handle("-" + INSTALL_PLUGIN_ARGUMENT, pluginName);
            Path pluginDir = PluginManager.getPluginsDir().resolve(pluginName + "-1.1");
            assertTrue(Files.exists(pluginDir));
        } finally {
            repo.stop();
            PluginManager.getRemover().removePlugin(pluginName);
            config.clearProperty(Key.CUSTOMER_KEY);
        }
    }

    /**
     * N.B. There are numerous other errors that could happen during the
     * installation process, which are covered thoroughly in the tests of the
     * plugin installer. This merely tests tha behavior of <em>an</em> error.
     */
    @Test
    void handleWithInstallPluginArgumentWithInvalidCustomerKey()
            throws Exception {
        final String pluginName           = "public-plugin";
        final Configuration config        = Configuration.forApplication();
        final MockArtifactRepository repo = new MockArtifactRepository();
        repo.start();
        config.setProperty(Key.CUSTOMER_KEY, "bogus");
        PluginManager.setRepositoryBaseURI(repo.getURI());
        try {
            instance.handle("-" + INSTALL_PLUGIN_ARGUMENT, pluginName);
            assertTrue(redirectedStderr.toString().contains(
                    "Account not found. Check the value of your " +
                    Key.CUSTOMER_KEY + " configuration key. "));
            assertTrue(SystemUtils.exitRequested());
            assertEquals(-1, SystemUtils.requestedExitCode());
        } finally {
            repo.stop();
            config.clearProperty(Key.CUSTOMER_KEY);
        }
    }

    // -list-fonts

    @Test
    void handleWithListFontsArgument() throws Exception {
        instance.handle("-" + LIST_FONTS_ARGUMENT);
        assertTrue(redirectedStdout.toString().contains("SansSerif"));
        assertTrue(SystemUtils.exitRequested());
        assertEquals(0, SystemUtils.requestedExitCode());
    }

    // -list-formats

    @Test
    void handleWithListFormatsArgument() throws Exception {
        instance.handle("-" + LIST_FORMATS_ARGUMENT);
        assertTrue(redirectedStdout.toString().contains("FORMATS SUPPORTED BY"));
        assertTrue(SystemUtils.exitRequested());
        assertEquals(0, SystemUtils.requestedExitCode());
    }

    // -list-plugins

    @Test
    void handleWithListPluginsArgumentWithMissingPluginsDir() throws Exception {
        Path pluginsDir = PluginManager.getPluginsDir();
        try {
            PluginManager.setPluginsDir(Path.of("bogus", "bogus"));
            instance.handle("-" + LIST_PLUGINS_ARGUMENT);
            assertTrue(redirectedStderr.toString().contains("does not exist"));
            assertEquals(-1, SystemUtils.requestedExitCode());
        } finally {
            PluginManager.setPluginsDir(pluginsDir);
        }
    }

    @Test
    void handleWithListPluginsArgument() throws Exception {
        instance.handle("-" + LIST_PLUGINS_ARGUMENT);
        assertTrue(redirectedStdout.toString().contains("DECODERS"));
        assertTrue(SystemUtils.exitRequested());
        assertEquals(0, SystemUtils.requestedExitCode());
    }

    // -remove-plugin

    @Test
    void handleWithRemovePluginArgument() throws Exception {
        final String plugin   = getClass().getSimpleName();
        final Path pluginsDir = PluginManager.getPluginsDir();
        Path pluginDir  = pluginsDir.resolve(plugin);
        try {
            Files.createDirectory(pluginDir);
            instance.handle("-" + REMOVE_PLUGIN_ARGUMENT, plugin);
            // Assert that the plugin dir as originally named no longer exists
            assertFalse(Files.exists(pluginDir));
            // ... but that one with a timestamp suffix does exist
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDir)) {
                for (Path path : stream) {
                    if (path.toString().matches(".*" + plugin + "_[0-9]{14}$")) {
                        pluginDir = path;
                        return;
                    }
                }
                fail("No backup plugin dir exists");
            }
        } finally {
            Files.deleteIfExists(pluginDir);
        }
    }

    // -update-plugin

    @Test
    void handleWithUpdatePluginArgumentWithExistingPlugin() throws Exception {
        final String pluginName           = "public-plugin";
        final Configuration config        = Configuration.forApplication();
        final MockArtifactRepository repo = new MockArtifactRepository();
        repo.start();
        config.setProperty(Key.CUSTOMER_KEY, MockArtifactRepository.CUSTOMER_KEY);
        PluginManager.setRepositoryBaseURI(repo.getURI());
        try {
            PluginManager.getInstaller().installPlugin(pluginName, "1.0");

            instance.handle("-" + UPDATE_PLUGIN_ARGUMENT, pluginName);
            assertEquals(new SoftwareVersion(1, 1),
                    PluginManager.getVersion(pluginName));
        } finally {
            repo.stop();
            PluginManager.getRemover().removePlugin(pluginName);
            PluginManager.getRemover().removePluginBackups(pluginName);
            config.clearProperty(Key.CUSTOMER_KEY);
        }
    }

    /**
     * N.B. There are numerous other errors that could happen during the
     * update process, which are covered thoroughly in the tests of the plugin
     * updater. This merely tests tha behavior of <em>an</em> error.
     */
    @Test
    void handleWithUpdatePluginArgumentWithInvalidPluginName()
            throws Exception {
        final String pluginName           = "bogus";
        final Configuration config        = Configuration.forApplication();
        final MockArtifactRepository repo = new MockArtifactRepository();
        repo.start();
        config.setProperty(Key.CUSTOMER_KEY, MockArtifactRepository.CUSTOMER_KEY);
        PluginManager.setRepositoryBaseURI(repo.getURI());
        try {
            instance.handle("-" + UPDATE_PLUGIN_ARGUMENT, pluginName);
            assertTrue(redirectedStderr.toString()
                    .contains("Plugin not installed: bogus"));
            assertTrue(SystemUtils.exitRequested());
            assertEquals(-1, SystemUtils.requestedExitCode());
        } finally {
            repo.stop();
            config.clearProperty(Key.CUSTOMER_KEY);
        }
    }

    @Test
    void handleWithUpdatePluginsArgument() throws Exception {
        final String pluginName           = "public-plugin";
        final Configuration config        = Configuration.forApplication();
        final MockArtifactRepository repo = new MockArtifactRepository();
        repo.start();
        config.setProperty(Key.CUSTOMER_KEY, MockArtifactRepository.CUSTOMER_KEY);
        PluginManager.setRepositoryBaseURI(repo.getURI());
        try {
            PluginManager.getInstaller().installPlugin(pluginName, "1.0");

            instance.handle("-" + UPDATE_PLUGINS_ARGUMENT);
            assertEquals(new SoftwareVersion(1, 1),
                    PluginManager.getVersion(pluginName));
        } finally {
            repo.stop();
            PluginManager.getRemover().removePlugin(pluginName);
            PluginManager.getRemover().removePluginBackups(pluginName);
            config.clearProperty(Key.CUSTOMER_KEY);
        }
    }

    // -config

    @Test
    void handleWithMissingConfigArgument() {
        assertThrows(CommandLineArgumentException.class,
                () -> instance.handle());
    }

    @Test
    void handleWithEmptyConfigArgument() {
        assertThrows(CommandLineArgumentException.class,
                () -> instance.handle("-" + CONFIG_ARGUMENT, ""));
    }

    @Test
    void handleWithInvalidConfigPathArgument() {
        String path = Paths.get("bogus.yml").toAbsolutePath().toString();
        assertThrows(NoSuchFileException.class,
                () -> instance.handle("-" + CONFIG_ARGUMENT, path));
    }

    @Test
    void handleWithValidConfigPathArgument() throws Exception {
        String path = TestUtils.getFixture("config.yml").toString();
        instance.handle("-" + CONFIG_ARGUMENT, path);
    }

    // -configtest

    @Test
    void handleWithConfigtestArgumentButNoConfigArgument() {
        assertThrows(CommandLineArgumentException.class,
                () -> instance.handle("-" + CONFIGTEST_ARGUMENT));
    }

    @Test
    void handleWithConfigtestArgument() throws Exception {
        instance.handle("-" + CONFIG_ARGUMENT,
                TestUtils.getFixture("config.yml").toString(),
                "-" + CONFIGTEST_ARGUMENT);
        assertTrue(redirectedStdout.toString().contains("MISSING KEYS"));
        assertEquals(0, SystemUtils.requestedExitCode());
    }

    // -version

    @Test
    void handleWithVersionArgument() throws Exception {
        instance.handle("-" + VERSION_ARGUMENT);
        assertTrue(redirectedStdout.toString().contains("Application version:"));
        assertTrue(redirectedStdout.toString().contains("Specification version:"));
        assertTrue(SystemUtils.exitRequested());
        assertEquals(0, SystemUtils.requestedExitCode());
    }

}
