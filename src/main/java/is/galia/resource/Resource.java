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

import java.util.Set;

/**
 * <p>HTTP resource.</p>
 *
 * <p>Implementations should extend {@link AbstractResource} and note the
 * documentation of all of its methods that they override.</p>
 *
 * <p>Instances are used only once and are not shared across threads.</p>
 */
public interface Resource {

    /**
     * @return All routes supported by the implementation.
     */
    Set<Route> getRoutes();

    /**
     * @return The instance provided to {@link #setRequest(Request)}.
     */
    Request getRequest();

    /**
     * @return The instance provided to {@link #setResponse(Response)}.
     */
    Response getResponse();

    /**
     * Invoked before {@link #doInit()}.
     *
     * @param request The client's request for the resource.
     */
    void setRequest(Request request);

    /**
     * Invoked before {@link #doInit()}.
     *
     * @param response The response that will be returned to the client.
     */
    void setResponse(Response response);

    /**
     * Invoked before the main request handler ({@link #doGET()}, etc.).
     *
     * @throws Exception upon any error. This may be a {@link
     *         ResourceException} to enable a custom status.
     */
    void doInit() throws Exception;

    /**
     * Handles {@code DELETE} requests.
     *
     * @see AbstractResource#doDELETE()
     * @throws Exception upon any error. This may be a {@link
     *         ResourceException} to enable a custom status.
     */
    void doDELETE() throws Exception;

    /**
     * Handles {@code GET} requests.
     *
     * @see AbstractResource#doGET()
     * @throws Exception upon any error. This may be a {@link
     *         ResourceException} to enable a custom status.
     */
    void doGET() throws Exception;

    /**
     * Handles {@code HEAD} requests.
     *
     * @see AbstractResource#doHEAD()
     * @throws Exception upon any error. This may be a {@link
     *         ResourceException} to enable a custom status.
     */
    void doHEAD() throws Exception;

    /**
     * Handles {@code OPTIONS} requests.
     *
     * @see AbstractResource#doOPTIONS()
     */
    void doOPTIONS();

    /**
     * Handles {@code PATCH} requests.
     *
     * @see AbstractResource#doPATCH()
     * @throws Exception upon any error. This may be a {@link
     *         ResourceException} to enable a custom status.
     */
    void doPATCH() throws Exception;

    /**
     * Handles {@code POST} requests.
     *
     * @see AbstractResource#doPOST()
     * @throws Exception upon any error. This may be a {@link
     *         ResourceException} to enable a custom status.
     */
    void doPOST() throws Exception;

    /**
     * Handles {@code PUT} requests.
     *
     * @see AbstractResource#doPUT()
     * @throws Exception upon any error. This may be a {@link
     *         ResourceException} to enable a custom status.
     */
    void doPUT() throws Exception;

    /**
     * Invoked at the end of the instance's lifecycle.
     */
    void destroy();

}
