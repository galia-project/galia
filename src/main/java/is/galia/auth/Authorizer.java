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

package is.galia.auth;

import java.io.IOException;

/**
 * Authorizes a request.
 */
public interface Authorizer {

    /**
     * Authorizes a request early in the request cycle, before any information
     * is available about the source image.
     *
     * @return Instance reflecting the authorization status of the request.
     */
    AuthInfo authorizeBeforeAccess() throws IOException;

    /**
     * Authorizes a request after full information is available about the
     * source image.
     *
     * @return Instance reflecting the authorization status of the request.
     */
    AuthInfo authorize() throws IOException;

}
