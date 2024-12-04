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

package is.galia.resource.api;

import is.galia.Application;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.resource.AbstractResource;
import is.galia.resource.EndpointDisabledException;

abstract class AbstractAPIResource extends AbstractResource {

    static final String BASIC_REALM = Application.getName() + " API Realm";

    @Override
    public void doInit() throws Exception {
        super.doInit();

        getResponse().setHeader("Cache-Control", "no-cache");

        final Configuration config = Configuration.forApplication();

        if (!config.getBoolean(Key.API_ENABLED, false)) {
            throw new EndpointDisabledException();
        }

        authenticateUsingBasic(BASIC_REALM, user -> {
            final String configUser = config.getString(Key.API_USERNAME, "");
            if (!configUser.isEmpty() && configUser.equals(user)) {
                return config.getString(Key.API_SECRET);
            }
            return null;
        });
    }

}
