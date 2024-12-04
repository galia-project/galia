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
 * <p>Package for sources, which locate and provide uniform access to source
 * images.</p>
 *
 * <p>A source translates an identifier to an image locator, such as a
 * pathname, in the particular type of underlying storage it is written to
 * interface with. It can then check whether the underlying image object exists
 * and is accessible, and if so, provide access to it to other application
 * components. The rest of of the application does not need to know where an
 * image resides, or how to access it natively&mdash;it can simply ask the
 * source it has access to.</p>
 *
 * <h2>Writing Custom Sources</h2>
 *
 * <p>Sources must implement {@link
 * is.galia.source.Source}. Inheriting from {@link
 * is.galia.source.AbstractSource} will get you a
 * couple of free method implementations.</p>
 *
 * <p>Custom sources are auto-detected using {@link
 * java.util.ServiceLoader}. Their JAR must include a file named {@link
 * is.galia.source.Source} inside {@literal
 * resources/META-INF/services} containing the fully qualified class name of
 * the implementation. Also, any references to the implementation from the
 * configuration file or delegate must use the fully qualified class name.</p>
 */
package is.galia.source;
