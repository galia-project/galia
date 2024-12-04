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

/**
 * <p>Contains authentication- and authorization-related classes.</p>
 *
 * <h2>Authentication</h2>
 *
 * <p>{@link is.galia.auth.CredentialStore} can be used
 * for username/password authentication.</p>
 *
 * <h2>Authorization</h2>
 *
 * <p>The general usage pattern is to use an {@link
 * is.galia.auth.AuthorizerFactory} to instantiate an
 * {@link is.galia.auth.Authorizer}, whose {@link
 * is.galia.auth.Authorizer#authorize()} method returns
 * an authorization result.</p>
 */
package is.galia.auth;