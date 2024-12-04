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
import is.galia.test.BaseTest;
import is.galia.util.DeletingFileVisitor;
import is.galia.util.SoftwareVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PluginInstallerTest extends BaseTest {

    private final MockArtifactRepository repo = new MockArtifactRepository();
    private PluginInstaller instance;
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

        instance = new PluginInstaller();
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

    private void assertPluginInstalled(final String pluginName) {
        assertPluginInstalled(pluginName, null);
    }

    private void assertPluginInstalled(final String pluginName,
                                       final String version) {
        final List<Path> files = new ArrayList<>();
        try {
            try (Stream<Path> paths = Files.walk(pluginsDir)) {
                paths.forEach(files::add);
            }
            assertEquals(1, files.size());

            instance.installPlugin(pluginName, version);

            files.clear();
            try (Stream<Path> paths = Files.walk(pluginsDir)) {
                paths.filter(p -> p.toString().contains(pluginName))
                        .forEach(files::add);
            }
            assertTrue(files.size() > 1);
        } catch (Exception e) {
            fail(e);
        }
    }

    /* installPlugin(String) */

    @Test
    void installPluginWithNullArgument() {
        assertThrows(NullPointerException.class,
                () -> instance.installPlugin(null));
    }

    @Test
    void installPluginWithUnknownHost() {
        instance.setRepositoryBaseURI(new Reference("http://bogus.example.org"));
        assertThrows(ConnectException.class,
                () -> instance.installPlugin("what"));
    }

    @Test
    void installPrivatePluginWithMissingCustomerKey() {
        Configuration.forApplication().clearProperty(Key.CUSTOMER_KEY);
        assertThrows(PluginNotFoundException.class,
                () -> instance.installPlugin("private-plugin"));
    }

    @Test
    void installPrivatePluginWithBogusCustomerKey() {
        Configuration.forApplication().setProperty(Key.CUSTOMER_KEY,
                "bogus");
        assertThrows(AccountNotFoundException.class,
                () -> instance.installPlugin("private-plugin"));
    }

    @Test
    void installPublicPluginWithMissingCustomerKey() throws Exception {
        final String pluginName = "public-plugin";
        Configuration.forApplication().clearProperty(Key.CUSTOMER_KEY);
        instance.installPlugin(pluginName);
        assertTrue(PluginManager.isPluginInstalled(pluginName));
    }

    @Test
    void installPublicPluginWithBogusCustomerKey() {
        final String pluginName = "public-plugin";
        Configuration.forApplication().setProperty(Key.CUSTOMER_KEY,
                "bogus");
        assertThrows(AccountNotFoundException.class,
                () -> instance.installPlugin(pluginName));
    }

    @Test
    void installPluginWithInvalidPluginName() {
        PluginNotFoundException e = assertThrows(PluginNotFoundException.class,
                () -> instance.installPlugin("bogus"));
        assertEquals("Plugin not found: bogus", e.getMessage());
    }

    @Test
    void installPluginWithPluginAlreadyInstalled() {
        final String pluginName = "public-plugin";

        assertPluginInstalled(pluginName);

        PluginAlreadyInstalledException e = assertThrows(
                PluginAlreadyInstalledException.class,
                () -> instance.installPlugin(pluginName));
        assertEquals("The plugin " + pluginName + " is already installed.",
                e.getMessage());
    }

    @Test
    void installPluginFromAccountIndexWithNoVersions() {
        final String pluginName = "private-plugin-no-versions";
        VersionNotFoundException e = assertThrows(
                VersionNotFoundException.class,
                () -> instance.installPlugin(pluginName));
        assertEquals("No releases found for plugin: " + pluginName,
                e.getMessage());
    }

    @Test
    void installPluginFromAccountIndexWithNoArtifact() {
        final String pluginName = "private-plugin-no-artifact";
        ArtifactNotFoundException e = assertThrows(
                ArtifactNotFoundException.class,
                () -> instance.installPlugin(pluginName));
        assertEquals("The plugin " + pluginName +
                        " (version 1.0) has no artifacts to download.",
                e.getMessage());
    }

    @Test
    void installPluginFromAccountIndexWithChecksumMismatch() {
        final String pluginName = "private-plugin-incorrect-checksum";
        assertThrows(ArtifactDigestException.class,
                () -> instance.installPlugin(pluginName));
    }

    @Test
    void installPluginFromAccountIndexWithOlderIncompatibleSpecVersions() {
        instance.setApplicationSpecificationVersion(new SoftwareVersion(2, 0));
        final String pluginName = "private-plugin";
        CompatibilityException e = assertThrows(
                CompatibilityException.class,
                () -> instance.installPlugin(pluginName));
        assertEquals("The plugin " + pluginName + " has no compatible releases.",
                e.getMessage());
    }

    @Test
    void installPluginFromAccountIndexWithOlderButStillCompatibleSpecVersion() {
        // Since the app spec version is 1.0 at the time this test was written,
        // we have to override it.
        instance.setApplicationSpecificationVersion(new SoftwareVersion(1, 9));
        assertPluginInstalled("private-plugin");
    }

    @Test
    void installPluginFromAccountIndexWithNewerIncompatibleSpecVersions() {
        instance.setApplicationSpecificationVersion(new SoftwareVersion(0, 5));
        final String pluginName = "private-plugin";
        CompatibilityException e = assertThrows(
                CompatibilityException.class,
                () -> instance.installPlugin(pluginName));
        assertEquals("The plugin " + pluginName + " has no compatible releases.",
                e.getMessage());
    }

    @Test
    void installPluginFromAccountIndex() {
        assertPluginInstalled("private-plugin");
    }

    @Test
    void installPluginFromPublicIndexWithNoVersions() {
        final String pluginName = "public-plugin-no-versions";
        VersionNotFoundException e = assertThrows(
                VersionNotFoundException.class,
                () -> instance.installPlugin(pluginName));
        assertEquals("No releases found for plugin: " + pluginName,
                e.getMessage());
    }

    @Test
    void installPluginFromPublicIndexWithNoArtifact() {
        final String pluginName = "public-plugin-no-artifact";
        ArtifactNotFoundException e = assertThrows(
                ArtifactNotFoundException.class,
                () -> instance.installPlugin(pluginName));
        assertEquals("The plugin " + pluginName +
                        " (version 1.0) has no artifacts to download.",
                e.getMessage());
    }

    @Test
    void installPluginFromPublicIndexWithChecksumMismatch() {
        final String pluginName = "public-plugin-incorrect-checksum";
        assertThrows(ArtifactDigestException.class,
                () -> instance.installPlugin(pluginName));
    }

    @Test
    void installPluginFromPublicIndexWithOlderIncompatibleSpecVersions() {
        instance.setApplicationSpecificationVersion(new SoftwareVersion(9999));
        final String pluginName = "public-plugin";
        CompatibilityException e = assertThrows(
                CompatibilityException.class,
                () -> instance.installPlugin(pluginName));
        assertEquals("The plugin " + pluginName + " has no compatible releases.",
                e.getMessage());
    }

    @Test
    void installPluginFromPublicIndexWithOlderButStillCompatibleSpecVersion() {
        // Since the app spec version is 1.0 at the time this test was written,
        // we have to override it.
        instance.setApplicationSpecificationVersion(new SoftwareVersion(1, 9));
        assertPluginInstalled("public-plugin");
    }

    @Test
    void installPluginFromPublicIndexWithNewerIncompatibleSpecVersions() {
        instance.setApplicationSpecificationVersion(new SoftwareVersion(0, 5));
        final String pluginName = "public-plugin";
        CompatibilityException e = assertThrows(
                CompatibilityException.class,
                () -> instance.installPlugin(pluginName));
        assertEquals("The plugin " + pluginName + " has no compatible releases.",
                e.getMessage());
    }

    @Test
    void installPluginFromPublicIndex() {
        assertPluginInstalled("public-plugin");
    }

    /* installPlugin(String, String) */

    @Test
    void installPluginWithNullName() {
        assertThrows(NullPointerException.class,
                () -> instance.installPlugin(null, "1.0"));
    }

    @Test
    void installPluginFromPublicIndexWithSpecificVersionThatIsAvailable() {
        assertPluginInstalled("public-plugin", "1.1");
    }

    @Test
    void installPluginWithSpecificVersionThatIsNotAvailable() {
        final String pluginName = "public-plugin";
        VersionNotFoundException e = assertThrows(
                VersionNotFoundException.class,
                () -> instance.installPlugin(pluginName, "99.99"));
        assertEquals("The plugin " + pluginName + " does not have a 99.99 version.",
                e.getMessage());
    }

    @Test
    void installPluginWithSpecificVersionThatIsNotCompatible() {
        final SoftwareVersion appSpecVersion = new SoftwareVersion(0, 5);
        instance.setApplicationSpecificationVersion(appSpecVersion);
        final String pluginName = "public-plugin";
        CompatibilityException e = assertThrows(
                CompatibilityException.class,
                () -> instance.installPlugin(pluginName, "1.1"));
        assertEquals("The plugin " + pluginName + " has no compatible releases.",
                e.getMessage());
    }

    @Test
    void installPluginWithSpecificVersionWithNoArtifact() {
        final String pluginName = "public-plugin-no-artifact";
        ArtifactNotFoundException e = assertThrows(
                ArtifactNotFoundException.class,
                () -> instance.installPlugin(pluginName, "1.0"));
        assertEquals("The plugin " + pluginName +
                        " (version 1.0) has no artifacts to download.",
                e.getMessage());
    }

    @Test
    void installPluginWithSpecificVersionWithChecksumMismatch() {
        final String pluginName = "public-plugin-incorrect-checksum";
        assertThrows(ArtifactDigestException.class,
                () -> instance.installPlugin(pluginName, "1.1"));
    }

}
