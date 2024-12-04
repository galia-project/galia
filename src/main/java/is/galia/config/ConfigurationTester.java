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

import is.galia.plugin.PluginManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class ConfigurationTester {

    private final Configuration config;

    /**
     * @param config Instance to test.
     */
    public ConfigurationTester(Configuration config) {
        this.config = config;
    }

    /**
     * @return All keys recognized by the application or any of its plugins
     *         that are not present in the instance under test.
     */
    public List<String> getMissingKeys() {
        final Set<String> presentKeys = presentKeys();
        final Set<String> missingKeys = new HashSet<>();
        knownPublicApplicationKeys()
                .filter(k -> !presentKeys.contains(k))
                .forEach(missingKeys::add);
        knownPluginKeys()
                .filter(k -> !presentKeys.contains(k))
                .forEach(missingKeys::add);
        return missingKeys.stream().sorted().toList();
    }

    /**
     * @return All keys present in the instance under test that are not
     *         recognized by the application or any of its plugins.
     */
    public List<String> getUnrecognizedKeys() {
        final List<String> knownKeys = new ArrayList<>(Key.values().length);
        knownApplicationKeys().forEach(knownKeys::add);
        knownPluginKeys().forEach(knownKeys::add);
        return presentKeys()
                .stream()
                .filter(k -> !knownKeys.contains(k))
                .sorted()
                .toList();
    }

    private Stream<String> knownApplicationKeys() {
        return Arrays.stream(Key.values())
                .map(Key::toString);
    }

    private Stream<String> knownPluginKeys() {
        return PluginManager.getPlugins().stream()
                .flatMap(p -> p.getPluginConfigKeys().stream());
    }

    private Stream<String> knownPublicApplicationKeys() {
        return Arrays.stream(Key.values())
                .filter(Key::isPublic)
                .map(Key::toString);
    }

    private Set<String> presentKeys() {
        final Set<String> configKeys = new HashSet<>(Key.values().length);
        final Iterator<String> configKeyIter = config.getKeys();
        while (configKeyIter.hasNext()) {
            String key = configKeyIter.next();
            configKeys.add(key);
        }
        return configKeys;
    }

}
