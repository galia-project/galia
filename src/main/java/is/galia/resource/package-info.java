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
 * <p>HTTP resource classes and server request handler class.</p>
 *
 * <p>The request handler, {@link is.galia.resource.RequestHandler}, serves as
 * the "front controller," dispatching to the various {@link
 * is.galia.resource.Resource} implementations within this package, underneath
 * it, and provided by {@link is.galia.plugin.Plugin plugins}.</p>
 */
package is.galia.resource;
