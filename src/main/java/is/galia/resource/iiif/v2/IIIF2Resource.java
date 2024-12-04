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

package is.galia.resource.iiif.v2;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.resource.EndpointDisabledException;
import is.galia.resource.iiif.IIIFResource;
import is.galia.util.StringUtils;

public abstract class IIIF2Resource extends IIIFResource {

    /**
     * Path that will be used if not overridden by {@link
     * Key#IIIF_2_ENDPOINT_PATH} in the application configuration.
     */
    public static final String DEFAULT_URI_PATH = "/iiif/2";

    /**
     * @return URI path prefix without trailing slash.
     */
    public static String getURIPath() {
        String path = Configuration.forApplication()
                .getString(Key.IIIF_2_ENDPOINT_PATH, DEFAULT_URI_PATH);
        return StringUtils.stripEnd(path, "/");
    }

    @Override
    public void doInit() throws Exception {
        super.doInit();

        if (!Configuration.forApplication().
                getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)) {
            throw new EndpointDisabledException();
        }
    }

}
