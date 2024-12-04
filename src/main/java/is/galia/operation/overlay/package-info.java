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
 * <p>Package for classes related to string and image overlays.</p>
 *
 * <p>The main public class of interest is
 * {@link is.galia.operation.overlay.OverlayFactory},
 * which is used to acquire an
 * {@link is.galia.operation.overlay.Overlay}
 * appropriate for a given request based on the configuration and (possibly)
 * the return value of a delegate method.</p>
 */
package is.galia.operation.overlay;