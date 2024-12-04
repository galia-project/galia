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

public final class ClientFactory {

    private static final String JDK_IMPL_CONFIG_VALUE    = "jdk";
    private static final String OKHTTP_IMPL_CONFIG_VALUE = "okhttp";
    private static final String DEFAULT_IMPL             = OKHTTP_IMPL_CONFIG_VALUE;

    /**
     * @return New instance corresponding to the value of the {@link
     *         Key#HTTP_CLIENT_IMPLEMENTATION} config key. If not set, a
     *         default implementation is used.
     * @throws IllegalArgumentException if the value of the {@link
     *         Key#HTTP_CLIENT_IMPLEMENTATION} config key is not supported.
     */
    public static Client newClient() {
        String value = getImplConfigValue();
        return switch (value) {
            case OKHTTP_IMPL_CONFIG_VALUE -> new OkHttpClientAdapter();
            case JDK_IMPL_CONFIG_VALUE    -> new JDKHttpClientAdapter();
            default -> throw new IllegalArgumentException(
                    "Unsupported value for " + Key.HTTP_CLIENT_IMPLEMENTATION +
                            ": " + value);
        };
    }

    private static String getImplConfigValue() {
        return Configuration.forApplication().getString(
                Key.HTTP_CLIENT_IMPLEMENTATION, DEFAULT_IMPL);
    }

    private ClientFactory() {}

}
