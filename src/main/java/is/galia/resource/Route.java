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

package is.galia.resource;

import is.galia.http.Method;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>Maps one or more HTTP methods and a URI path regex/pattern to a {@link
 * Resource}.</p>
 *
 * <h2>Notes on path pattern implementation</h2>
 *
 * <p>Patterns should match only whole paths recognized by a single resource
 * and no others. Matching whole paths can be accomplished using start and end
 * anchors ({@code ^} and {@code $}). Otherwise, it is possible for a client to
 * supply a longer path that will still match.</p>
 *
 * <p>Patterns should not conflict with the ones used by built-in resources.
 * The built-in patterns are not centrally documented, but built-in resources
 * are matched before plugin resources, so if you find that a built-in resource
 * is handling the requests that your plugin should be, you should reconsider
 * your pattern.</p>
 *
 * <p>An unmatched path will cause an HTTP 404 response.</p>
 *
 * <p>You must decide how strict to make the regex. For example, if your
 * resource supports {@code /path/red} and {@code /path/orange}, but not
 * {@code /path/blue}, do you want {@code /path/blue} to return a generic HTTP
 * 404 response, or do you want to make the color part of the path dynamic (see
 * below), and check it in your {@link Resource} implementation, in order to
 * return your own custom response?</p>
 *
 * <p>A pattern must consider the path extension, if supported, as this is
 * part of the path. It must <em>not</em> consider the query nor any other part
 * of the URL.</p>
 *
 * <h3>Dynamic path segments</h3>
 *
 * <p>Dynamic path segments are supported using regex groups. The matches will
 * be made available via {@link Request#getPathArguments()}.</p>
 *
 * <h3>Example</h3>
 *
 * <p>The following pattern matches a resource at {@code
 * /my-resource/:identifier}, and makes the {@code identifier} portion
 * available as a path argument:</p>
 *
 * <p>{@code ^/my-resource/([^/]+)$}</p>
 *
 * <p>This translates to: &quot;Match a path that starts with {@code
 * /my-resource} and ends with a dynamic path segment at least one character
 * long that does not include a slash.&quot;</p>
 *
 * @param requestMethods Supported request methods. If {@link Method#GET} is
 *                       included, {@link Method#HEAD} will be assumed as well.
 *                       It is not necessary to include {@link Method#OPTIONS}
 *                       or {@link Method#TRACE}.
 * @param pathPatterns   Pattern(s) describing the URI paths at which a {@link
 *                       Resource} is available. (Most will have only one.)
 *                       See above.
 */
public record Route(Set<Method> requestMethods, Set<Pattern> pathPatterns) {
}
