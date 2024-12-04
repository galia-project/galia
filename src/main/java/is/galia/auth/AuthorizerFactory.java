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

public class AuthorizerFactory {

    /**
     * @param args Arguments to pass to the instance's constructor.
     * @return     New instance. If {@code args} is empty, the instance will be
     *             maximally permissive.
     */
    public Authorizer newAuthorizer(Object... args) {
        if (args.length > 0 && args[0] != null) {
            return new DelegateAuthorizer(args);
        }
        return new PermissiveAuthorizer();
    }

}
