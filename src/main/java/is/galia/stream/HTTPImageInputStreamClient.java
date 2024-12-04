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

package is.galia.stream;

import is.galia.http.Range;
import is.galia.http.Reference;
import is.galia.http.Response;

import java.io.IOException;

/**
 * Adapts an HTTP client to work with {@link HTTPImageInputStream}. The client
 * should be initialized to the URI of the resource that the stream is supposed
 * to access.
 */
public interface HTTPImageInputStreamClient {

    /**
     * @return URI of the resource.
     */
    Reference getReference();

    /**
     * @return Response. In particular, the {@link Response#getStatus() status}
     *         is set, and {@literal Accept-Ranges} and {@literal
     *         Content-Length} {@link Response#getHeaders() headers} are
     *         included if the server sent them.
     * @throws IOException upon failure to receive a valid response
     *         (<strong>not</strong> a response with an error status code).
     */
    Response sendHEADRequest() throws IOException;

    /**
     * @param range Byte range to request.
     * @return      Same as {@link #sendHEADRequest()}, but the {@link
     *              Response#getBody() body} is also included.
     * @throws IOException upon failure to receive a valid response
     *         (<strong>not</strong> a response with an error status code).
     */
    Response sendGETRequest(Range range) throws IOException;

}
