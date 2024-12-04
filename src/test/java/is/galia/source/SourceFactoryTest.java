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

package is.galia.source;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.delegate.Delegate;
import is.galia.image.Identifier;
import is.galia.plugin.PluginManager;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SourceFactoryTest extends BaseTest {

    /* getAllSources() */

    @Test
    void getAllSources() {
        assertFalse(SourceFactory.getAllSources().isEmpty());
    }

    /* getPluginSourceByName() */

    @Test
    void getPluginSourceByNameWithInvalidName() {
        assertNull(SourceFactory.getPluginSourceByName("bogus"));
    }

    @Test
    void getPluginSourceByNameWithValidName() {
        assertNotNull(SourceFactory.getPluginSourceByName(
                MockSourcePlugin.class.getSimpleName()));
    }

    /* getPluginSources() */

    @Test
    void getPluginSources() {
        Path pluginsDir = PluginManager.getPluginsDir();
        try {
            PluginManager.setPluginsDir(Path.of("/bogus"));
            assertFalse(SourceFactory.getPluginSources().isEmpty());
        } finally {
            PluginManager.setPluginsDir(pluginsDir);
        }
    }

    /* getSelectionStrategy() */

    @Test
    void getSelectionStrategy() {
        Configuration config = Configuration.forApplication();

        config.setProperty(Key.SOURCE_DELEGATE, "false");
        assertEquals(SourceFactory.SelectionStrategy.STATIC,
                SourceFactory.getSelectionStrategy());

        config.setProperty(Key.SOURCE_DELEGATE, "true");
        assertEquals(SourceFactory.SelectionStrategy.DELEGATE_SCRIPT,
                SourceFactory.getSelectionStrategy());
    }

    /* newSource(String) */

    @Test
    void newSource1WithExistingUnqualifiedName() throws Exception {
        assertInstanceOf(FilesystemSource.class,
                SourceFactory.newSource(FilesystemSource.class.getSimpleName()));
    }

    @Test
    void newSource1WithNonExistingUnqualifiedName() {
        assertThrows(ClassNotFoundException.class, () ->
                SourceFactory.newSource("Bogus"));
    }

    @Test
    void newSource1WithExistingQualifiedName() throws Exception {
        assertInstanceOf(FilesystemSource.class,
                SourceFactory.newSource(FilesystemSource.class.getName()));
    }

    @Test
    void newSource1WithNonExistingQualifiedName() {
        assertThrows(ClassNotFoundException.class, () ->
                SourceFactory.newSource(SourceFactory.class.getPackage().getName() + ".Bogus"));
    }

    @Test
    void newSourceWithPluginName() throws Exception {
        assertNotNull(SourceFactory.newSource(MockSourcePlugin.class.getSimpleName()));
    }

    @Test
    void newSourceInitializesPlugin() throws Exception {
        MockSourcePlugin source = (MockSourcePlugin)
                SourceFactory.newSource(MockSourcePlugin.class.getSimpleName());
        assertTrue(source.isInitialized);
    }

    /* newSource(Identifier, Delegate) */

    @Test
    void newSource2WithValidStaticSourceAndSimpleClassName()
            throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.SOURCE_STATIC,
                HTTPSource.class.getSimpleName());

        Identifier identifier = new Identifier("cats");
        Source source = SourceFactory.newSource(identifier, null);
        assertInstanceOf(HTTPSource.class, source);
    }

    @Test
    void newSource2WithValidStaticSourceAndFullClassName()
            throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.SOURCE_STATIC, HTTPSource.class.getName());

        Identifier identifier = new Identifier("cats");
        Source source = SourceFactory.newSource(identifier, null);

        assertInstanceOf(HTTPSource.class, source);
    }

    @Test
    void newSource2WithInvalidStaticSource() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.SOURCE_STATIC, "BogusSource");

        Identifier identifier = new Identifier("cats");
        assertThrows(ClassNotFoundException.class,
                () -> SourceFactory.newSource(identifier, null));
    }

    @Test
    void newSource2UsingDelegateScript() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.SOURCE_DELEGATE, true);

        Identifier identifier = new Identifier("http");
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(identifier);

        Source source = SourceFactory.newSource(identifier, delegate);
        assertInstanceOf(HTTPSource.class, source);

        identifier = new Identifier("anythingelse");
        delegate.getRequestContext().setIdentifier(identifier);

        source = SourceFactory.newSource(identifier, delegate);
        assertInstanceOf(FilesystemSource.class, source);
    }

}
