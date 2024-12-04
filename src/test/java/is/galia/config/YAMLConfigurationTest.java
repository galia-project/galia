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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import is.galia.test.TestUtils;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class YAMLConfigurationTest extends AbstractConfigurationTest {

    private YAMLConfiguration instance;
    private Path configPath;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        setUpFileSource();
        instance = YAMLConfiguration.fromPath(configPath);
        instance.clear();
    }

    private void setUpFileSource() {
        configPath = TestUtils.getFixture("YAMLConfiguration.yml");
    }

    protected Configuration getInstance() {
        return instance;
    }

    /* fromPath() */

    @Test
    void fromPathWithNullArgument() {
        assertThrows(NullPointerException.class,
                () -> YAMLConfiguration.fromPath(null));
    }

    @Test
    void fromPathWithInvalidPath() {
        assertThrows(NoSuchFileException.class,
                () -> YAMLConfiguration.fromPath(Path.of("/bogus/bogus/bogus")));
    }

    @Test
    void fromPathWithNonYAMLFile() {
        assertThrows(IOException.class, () ->
                YAMLConfiguration.fromPath(TestUtils.getFixture("delegates.rb")));

    }

    @Test
    void fromPathWithValidYAMLFile() throws Exception {
        YAMLConfiguration config = YAMLConfiguration.fromPath(
                TestUtils.getFixture("config.yml"));
        assertEquals("hello world!", config.getString("plain_value"));
    }

    /* getFile() */

    @Test
    void getFile() throws Exception {
        setUpFileSource();
        instance = YAMLConfiguration.fromPath(configPath);
        assertEquals(configPath, instance.getFile().get());
    }

}
