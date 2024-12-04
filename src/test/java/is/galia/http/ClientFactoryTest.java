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

package is.galia.http;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientFactoryTest extends BaseTest {

    /* newClient() */

    @Test
    void newClientWithNoConfigValue() {
        assertInstanceOf(OkHttpClientAdapter.class, ClientFactory.newClient());
    }

    @Test
    void newClientWithInvalidConfigValue() {
        Configuration.forApplication()
                .setProperty(Key.HTTP_CLIENT_IMPLEMENTATION, "bogus");
       assertThrows(IllegalArgumentException.class,
               () -> ClientFactory.newClient());
    }

    @Test
    void newClientWithJDKConfigValue() {
        Configuration.forApplication()
                .setProperty(Key.HTTP_CLIENT_IMPLEMENTATION, "jdk");
        assertInstanceOf(JDKHttpClientAdapter.class, ClientFactory.newClient());
    }

    @Test
    void newClientWithOkHttpConfigValue() {
        Configuration.forApplication()
                .setProperty(Key.HTTP_CLIENT_IMPLEMENTATION, "okhttp");
        assertInstanceOf(OkHttpClientAdapter.class, ClientFactory.newClient());
    }

}
