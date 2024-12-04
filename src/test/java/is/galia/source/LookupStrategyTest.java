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
import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LookupStrategyTest extends BaseTest {

    /* from(Key) */

    @Test
    void fromWithKeyWithBasicStrategy() {
        final Configuration config = Configuration.forApplication();
        final Key key = Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY;
        config.setProperty(key, "BasicLookupStrategy");

        LookupStrategy strategy = LookupStrategy.from(key);
        assertEquals(LookupStrategy.BASIC, strategy);
    }

    @Test
    void fromWithKeyWithDelegateDelegateStrategy() {
        final Configuration config = Configuration.forApplication();
        final Key key = Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY;
        config.setProperty(key, "DelegateLookupStrategy");

        LookupStrategy strategy = LookupStrategy.from(key);
        assertEquals(LookupStrategy.DELEGATE_SCRIPT, strategy);
    }

    @Test
    void fromWithKeyWithIllegalStrategy() {
        final Configuration config = Configuration.forApplication();
        final Key key = Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY;
        config.setProperty(key, "bogus");

        LookupStrategy strategy = LookupStrategy.from(key);
        assertEquals(LookupStrategy.UNDEFINED, strategy);
    }

    /* from(String) */

    @Test
    void fromWithStringWithBasicStrategy() {
        final Configuration config = Configuration.forApplication();
        final String key = Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY.key();
        config.setProperty(key, "BasicLookupStrategy");

        LookupStrategy strategy = LookupStrategy.from(key);
        assertEquals(LookupStrategy.BASIC, strategy);
    }

    @Test
    void fromWithStringWithDelegateDelegateStrategy() {
        final Configuration config = Configuration.forApplication();
        final String key = Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY.key();
        config.setProperty(key, "DelegateLookupStrategy");

        LookupStrategy strategy = LookupStrategy.from(key);
        assertEquals(LookupStrategy.DELEGATE_SCRIPT, strategy);
    }

    @Test
    void fromWithStringWithIllegalStrategy() {
        final Configuration config = Configuration.forApplication();
        final String key = Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY.key();
        config.setProperty(key, "bogus");

        LookupStrategy strategy = LookupStrategy.from(key);
        assertEquals(LookupStrategy.UNDEFINED, strategy);
    }

}
