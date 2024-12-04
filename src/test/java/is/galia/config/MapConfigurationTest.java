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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapConfigurationTest extends AbstractConfigurationTest {

    private MapConfiguration instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new MapConfiguration();
    }

    protected Configuration getInstance() {
        return instance;
    }

    @Test
    void constructor1() {
        assertTrue(instance.getBackingMap().isEmpty());
        instance.setProperty("new", "cats"); // test mutability
    }

    @Test
    void constructor2() {
        Map<String,Object> map = Map.of("key", "value");
        instance = new MapConfiguration(map);
        assertEquals("value", instance.getString("key"));
    }

    @Test
    void getFile() {
        assertFalse(instance.getFile().isPresent());
    }

}
