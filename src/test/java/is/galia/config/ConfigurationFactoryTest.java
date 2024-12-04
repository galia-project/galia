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

package is.galia.config;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import is.galia.test.TestUtils;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationFactoryTest extends BaseTest {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        ConfigurationFactory.setAppInstance(new MapConfiguration());
    }

    /* appInstance() */

    @Test
    void appInstanceReturnsTheAppInstance() {
        Configuration config = new MapConfiguration();
        ConfigurationFactory.setAppInstance(config);
        assertSame(config, ConfigurationFactory.appInstance());
    }

    /* fromPath() */

    @Test
    void fromPathWithNullArgument() {
        assertThrows(NullPointerException.class,
                () -> ConfigurationFactory.fromPath(null));
    }

    @Test
    void fromPathWithUnsupportedFilenameExtension() {
        assertThrows(IllegalArgumentException.class,
                () -> ConfigurationFactory.fromPath(Path.of("/bogus/bogus/bogus")));
    }

    @Test
    void fromPathWithInvalidPath() {
        assertThrows(NoSuchFileException.class,
                () -> ConfigurationFactory.fromPath(Path.of("/bogus/bogus/bogus.yml")));
    }

    @Test
    void fromPathWithValidYAMLFile() throws Exception {
        Configuration config = ConfigurationFactory.fromPath(
                TestUtils.getFixture("config.yml"));
        assertEquals("hello world!", config.getString("plain_value"));
    }

    /* newProviderFromPath() */

    @Test
    void newProviderFromPathWithNullArgument() {
        assertThrows(NullPointerException.class,
                () -> ConfigurationFactory.newProviderFromPath(null));
    }

    @Test
    void newProviderFromPathWithUnsupportedFilenameExtension() {
        assertThrows(IllegalArgumentException.class,
                () -> ConfigurationFactory.newProviderFromPath(Path.of("/bogus/bogus/bogus")));
    }

    @Test
    void newProviderFromPathWithInvalidPath() {
        assertThrows(NoSuchFileException.class,
                () -> ConfigurationFactory.newProviderFromPath(Path.of("/bogus/bogus/bogus.yml")));
    }

    @Test
    void newProviderFromPathWithValidYAMLFile() throws Exception {
        ConfigurationProvider config = ConfigurationFactory.newProviderFromPath(
                TestUtils.getFixture("config.yml"));
        assertInstanceOf(EnvironmentConfiguration.class, config.getWrappedConfigurations().get(0));
        assertInstanceOf(YAMLConfiguration.class, config.getWrappedConfigurations().get(1));
        assertEquals("hello world!", config.getString("plain_value"));
    }

    /* setAppInstance() */

    @Test
    void setAppInstanceSetsTheAppInstance() {
        Configuration config = new MapConfiguration();
        ConfigurationFactory.setAppInstance(config);
        assertSame(config, ConfigurationFactory.appInstance());
    }

}
