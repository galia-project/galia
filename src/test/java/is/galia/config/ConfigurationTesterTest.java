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
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTesterTest extends BaseTest {

    /* getMissingKeys() */

    @Test
    void getMissingKeys() {
        Configuration config       = new MapConfiguration();
        ConfigurationTester tester = new ConfigurationTester(config);

        Set<String> publicKeys = Arrays.stream(Key.values())
                .filter(Key::isPublic)
                .map(Key::key)
                .collect(Collectors.toSet());

        List<String> keys = tester.getMissingKeys();
        // There may be plugins installed which would make keys bigger than publicKeys
        assertTrue(keys.size() >= publicKeys.size());
    }

    @Test
    void getMissingKeysOmitsPrivateKeys() {
        Configuration config       = new MapConfiguration();
        ConfigurationTester tester = new ConfigurationTester(config);

        Set<String> privateKeys = Arrays.stream(Key.values())
                .filter(k -> !k.isPublic())
                .map(Key::key)
                .collect(Collectors.toSet());

        List<String> keys = tester.getMissingKeys();
        privateKeys.forEach(privateKey -> assertFalse(keys.contains(privateKey)));
    }

    /* getUnrecognizedKeys() */

    @Test
    void getUnrecognizedKeys() {
        Configuration config = new MapConfiguration();
        config.setProperty("bogus2", "value");
        config.setProperty("bogus1", "value");
        ConfigurationTester tester = new ConfigurationTester(config);

        List<String> keys = tester.getUnrecognizedKeys();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("bogus1"));
        assertTrue(keys.contains("bogus2"));
    }

    @Test
    void getUnrecognizedKeysOmitsPrivateKeys() {
        Configuration config = new MapConfiguration();
        config.setProperty(Key.ARTIFACT_REPOSITORY_BASE_URI, "value");
        config.setProperty("bogus", "value");
        ConfigurationTester tester = new ConfigurationTester(config);

        List<String> keys = tester.getUnrecognizedKeys();
        assertEquals(1, keys.size());
        assertTrue(keys.contains("bogus"));
    }

}
